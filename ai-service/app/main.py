import json
import os
from pathlib import Path
from typing import Annotated, Literal, TypedDict

import httpx
from fastapi import FastAPI, HTTPException
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langgraph.graph import END, START, StateGraph
from langgraph.graph.message import add_messages
from langgraph.checkpoint.memory import MemorySaver
from pydantic import BaseModel, Field


BACKEND_URL = os.getenv("BACKEND_URL", "http://backend:8080").rstrip("/")
INTERNAL_TOKEN = os.getenv("AI_INTERNAL_TOKEN", "change-me")
MODEL = os.getenv("AI_MODEL", "gpt-4o-mini")
API_KEY = os.getenv("AI_API_KEY", "")
BASE_URL = os.getenv("AI_BASE_URL", "").strip() or None
RULES = (Path(__file__).resolve().parents[1] / "knowledge" / "wms_rules.md").read_text(encoding="utf-8")


class ChatRequest(BaseModel):
    session_id: str = Field(min_length=1, max_length=120)
    message: str = Field(min_length=1, max_length=4000)
    warehouse_id: int | None = None
    locale: str = "zh"


class ChatResponse(BaseModel):
    answer: str
    agent: str
    reportId: int | None = None
    downloadUrl: str | None = None


class AgentState(TypedDict):
    messages: Annotated[list, add_messages]
    route: str
    warehouse_id: int | None
    locale: str
    answer: str
    report_id: int | None


def model() -> ChatOpenAI | None:
    if not API_KEY:
        return None
    options = {"model": MODEL, "api_key": API_KEY, "temperature": 0}
    if BASE_URL:
        options["base_url"] = BASE_URL
    return ChatOpenAI(**options)


LLM = model()


def backend_headers() -> dict[str, str]:
    return {"X-AI-Internal-Token": INTERNAL_TOKEN}


def get_context(warehouse_id: int | None) -> dict:
    params = {}
    if warehouse_id is not None:
        params["warehouseId"] = warehouse_id
    with httpx.Client(timeout=30) as client:
        response = client.get(
            f"{BACKEND_URL}/api/internal/ai/context",
            params=params,
            headers=backend_headers(),
        )
        response.raise_for_status()
        payload = response.json()
        if not payload.get("success"):
            raise RuntimeError(payload.get("message", "Failed to load WMS context"))
        return payload["data"]


def save_report(title: str, analysis: str, context: dict) -> int:
    with httpx.Client(timeout=30) as client:
        response = client.post(
            f"{BACKEND_URL}/api/internal/ai/reports",
            headers=backend_headers(),
            json={
                "title": title,
                "analysisText": analysis,
                "structuredDataJson": json.dumps(context, ensure_ascii=False),
            },
        )
        response.raise_for_status()
        payload = response.json()
        if not payload.get("success"):
            raise RuntimeError(payload.get("message", "Failed to save report"))
        return int(payload["data"]["id"])


def latest_question(state: AgentState) -> str:
    for message in reversed(state["messages"]):
        if isinstance(message, HumanMessage):
            return str(message.content)
    return ""


def supervisor(state: AgentState) -> dict:
    question = latest_question(state)
    lowered = question.lower()
    if any(word in lowered for word in ["导出", "报表", "报告", "pdf", "export", "report"]):
        route = "report"
    elif any(word in lowered for word in [
        "多少", "库存", "订单", "趋势", "统计", "数据", "sku", "shipping",
        "quantity", "count", "dashboard", "trend",
    ]):
        route = "analytics"
    else:
        route = "rules"
    return {"route": route}


def rules_agent(state: AgentState) -> dict:
    question = latest_question(state)
    if LLM is None:
        answer = (
            "当前未配置大模型。以下是系统规则摘要：库存不可为负；库存变化必须记录流水；"
            "出库单完成拣货后才能确认出库；Shipping Job 只能绑定同仓库订单，且全部订单完成后才能发运。"
        )
    else:
        response = LLM.invoke([
            SystemMessage(content=(
                "你是 WMS 规则助手。只能依据给定规则回答；规则没有覆盖时明确说明，不要编造。"
                "回答简洁、可执行。\n\n" + RULES
            )),
            *state["messages"][-10:],
        ])
        answer = str(response.content)
    return {"answer": answer, "messages": [AIMessage(content=answer)]}


def analytics_agent(state: AgentState) -> dict:
    question = latest_question(state)
    context = get_context(state.get("warehouse_id"))
    if LLM is None:
        answer = (
            "当前未配置大模型，已读取仓储数据。"
            f"商品总数：{context.get('productTotal', 0)}；"
            f"库存总量：{context.get('stockTotal', 0)}；"
            f"今日入库：{context.get('todayInbound', 0)}；"
            f"今日出库：{context.get('todayOutbound', 0)}；"
            f"未解决预警：{context.get('warningCount', 0)}。"
        )
    else:
        response = LLM.invoke([
            SystemMessage(content=(
                "你是 WMS 数据分析助手。只依据后端提供的 JSON 数据回答。"
                "不要声称直接访问数据库，不要虚构 JSON 中没有的数据。"
                "若数据不足以回答，明确指出缺少哪个指标。"
            )),
            *state["messages"][-10:],
            SystemMessage(content=f"本次查询可用的 WMS 数据：\n{json.dumps(context, ensure_ascii=False)}"),
        ])
        answer = str(response.content)
    return {"answer": answer, "messages": [AIMessage(content=answer)]}


def report_agent(state: AgentState) -> dict:
    question = latest_question(state)
    context = get_context(state.get("warehouse_id"))
    if LLM is None:
        analysis = (
            "WMS 运营报表\n\n"
            f"- 商品总数：{context.get('productTotal', 0)}\n"
            f"- 库存总量：{context.get('stockTotal', 0)}\n"
            f"- 今日入库：{context.get('todayInbound', 0)}\n"
            f"- 今日出库：{context.get('todayOutbound', 0)}\n"
            f"- 未解决预警：{context.get('warningCount', 0)}\n"
        )
    else:
        response = LLM.invoke([
            SystemMessage(content=(
                "你是 WMS 报表助手。根据 JSON 数据生成结构清晰的中文运营报表，"
                "包括核心指标、异常、风险和行动建议。只使用已有数据。"
            )),
            *state["messages"][-10:],
            SystemMessage(content=f"生成报表可使用的 WMS 数据：\n{json.dumps(context, ensure_ascii=False)}"),
        ])
        analysis = str(response.content)
    report_id = save_report("AI 仓储运营报表", analysis, context)
    answer = f"报表已生成，可以点击下方链接下载 PDF。\n\n{analysis}"
    return {
        "answer": answer,
        "report_id": report_id,
        "messages": [AIMessage(content=answer)],
    }


def route(state: AgentState) -> Literal["rules", "analytics", "report"]:
    return state["route"]  # type: ignore[return-value]


builder = StateGraph(AgentState)
builder.add_node("supervisor", supervisor)
builder.add_node("rules", rules_agent)
builder.add_node("analytics", analytics_agent)
builder.add_node("report", report_agent)
builder.add_edge(START, "supervisor")
builder.add_conditional_edges("supervisor", route)
builder.add_edge("rules", END)
builder.add_edge("analytics", END)
builder.add_edge("report", END)
graph = builder.compile(checkpointer=MemorySaver())

app = FastAPI(title="Smart WMS Multi-Agent Service", version="1.0.0")


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "modelConfigured": LLM is not None}


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    try:
        result = graph.invoke(
            {
                "messages": [HumanMessage(content=request.message)],
                "warehouse_id": request.warehouse_id,
                "locale": request.locale,
                "route": "",
                "answer": "",
                "report_id": None,
            },
            config={"configurable": {"thread_id": request.session_id}},
        )
        report_id = result.get("report_id")
        return ChatResponse(
            answer=result["answer"],
            agent=result["route"],
            reportId=report_id,
            downloadUrl=f"/api/ai/reports/{report_id}/pdf" if report_id else None,
        )
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"WMS backend unavailable: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
