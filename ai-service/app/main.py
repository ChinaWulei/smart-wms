import httpx
from fastapi import FastAPI, HTTPException

from app.core.deep_agent import deep_agent, final_text, response_metadata
from app.core.model import model_configured
from app.core.schemas import ChatRequest, ChatResponse


app = FastAPI(title="Smart WMS Multi-Agent Service", version="1.0.0")


def _is_model_unavailable(exc: Exception) -> bool:
    message = str(exc).lower()
    return (
        "503 unavailable" in message
        or "high demand" in message
        or "service unavailable" in message
    )


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "modelConfigured": model_configured()}


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    if deep_agent is None:
        raise HTTPException(status_code=503, detail="Gemini is not configured")
    try:
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
            config={"configurable": {"thread_id": request.session_id}},
        )
        agent, report_id, answer = response_metadata(final_text(result))
        return ChatResponse(
            answer=answer,
            agent=agent,
            reportId=report_id,
            downloadUrl=f"/api/ai/reports/{report_id}/pdf" if report_id else None,
        )
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"WMS backend unavailable: {exc}") from exc
    except Exception as exc:
        if _is_model_unavailable(exc):
            raise HTTPException(
                status_code=503,
                detail="Gemini model is temporarily busy after automatic retries. Please try again later.",
            ) from exc
        raise HTTPException(status_code=500, detail=str(exc)) from exc
