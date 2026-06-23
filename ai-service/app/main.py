import json
import queue
import threading

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse

from app.core.deep_agent import deep_agent, final_text, response_metadata
from app.core.model import model_configured
from app.core.schemas import ChatRequest, ChatResponse
from app.core.trace import AgentTraceHandler


app = FastAPI(title="Smart WMS Multi-Agent Service", version="1.0.0")


def _is_model_unavailable(exc: Exception) -> bool:
    message = str(exc).lower()
    return (
        "503 unavailable" in message
        or "high demand" in message
        or "service unavailable" in message
    )


def _invoke(request: ChatRequest, callbacks: list | None = None) -> ChatResponse:
    result = deep_agent.invoke(
        {
            "messages": [{
                "role": "user",
                "content": (
                    f"{request.message}\n\n"
                    f"Runtime context: warehouse_id={request.warehouse_id}, "
                    f"locale={request.locale}."
                ),
            }],
        },
        config={
            "configurable": {"thread_id": request.session_id},
            "callbacks": callbacks or [],
        },
    )
    agent, report_id, answer = response_metadata(final_text(result))
    return ChatResponse(
        answer=answer,
        agent=agent,
        reportId=report_id,
        downloadUrl=f"/api/ai/reports/{report_id}/pdf" if report_id else None,
    )


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "modelConfigured": model_configured()}


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    if deep_agent is None:
        raise HTTPException(status_code=503, detail="Gemini is not configured")
    try:
        return _invoke(request)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"WMS backend unavailable: {exc}") from exc
    except Exception as exc:
        if _is_model_unavailable(exc):
            raise HTTPException(
                status_code=503,
                detail="Gemini model is temporarily busy after automatic retries. Please try again later.",
            ) from exc
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/chat/stream")
def chat_stream(request: ChatRequest) -> StreamingResponse:
    if deep_agent is None:
        raise HTTPException(status_code=503, detail="Gemini is not configured")

    events: queue.Queue[dict | None] = queue.Queue()

    def emit(event: dict) -> None:
        events.put(event)

    def worker() -> None:
        trace = AgentTraceHandler(emit)
        trace.event("request", "running", "supervisor", "开始处理对话任务")
        try:
            response = _invoke(request, [trace])
            trace.event("request", "completed", response.agent, "任务处理完成")
            emit({"type": "result", "status": "completed", "data": response.model_dump()})
        except Exception as exc:
            status = 503 if _is_model_unavailable(exc) else 500
            trace.event("request", "failed", trace.current_agent, "任务处理失败", str(exc))
            emit({
                "type": "error",
                "status": "failed",
                "statusCode": status,
                "message": (
                    "AI 模型当前繁忙，系统已自动重试，请稍后再试"
                    if status == 503 else str(exc)
                ),
            })
        finally:
            events.put(None)

    threading.Thread(target=worker, daemon=True).start()

    def generate():
        while True:
            event = events.get()
            if event is None:
                break
            yield json.dumps(event, ensure_ascii=False) + "\n"

    return StreamingResponse(
        generate(),
        media_type="application/x-ndjson",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
