import time
from collections.abc import Callable
from typing import Any
from uuid import UUID

from langchain_core.callbacks import BaseCallbackHandler


AGENT_NAMES = {
    "rules-agent": "rules",
    "analytics-agent": "analytics",
    "sql-agent": "sql",
    "report-agent": "report",
    "wms-supervisor": "supervisor",
}

TOOL_LABELS = {
    "task": "选择并调用专业 Agent",
    "load_warehouse_context": "读取仓库业务上下文",
    "list_wms_tables": "读取可查询业务表",
    "get_wms_schema": "读取数据库表结构",
    "execute_readonly_sql": "执行只读 SQL",
    "load_wms_rules": "读取仓储规则",
    "save_report": "保存 AI 报表",
}


def _text(value: Any, limit: int = 180) -> str:
    text = str(value).replace("\n", " ").strip()
    return text if len(text) <= limit else f"{text[:limit]}…"


def _agent_from_text(value: Any, fallback: str = "supervisor") -> str:
    text = str(value).lower()
    for name, agent in AGENT_NAMES.items():
        if name in text:
            return agent
    return fallback


class AgentTraceHandler(BaseCallbackHandler):
    def __init__(self, emit: Callable[[dict], None]):
        self.emit = emit
        self.started_at = time.monotonic()
        self.current_agent = "supervisor"
        self.model_runs: dict[UUID, tuple[str, float]] = {}
        self.tool_runs: dict[UUID, tuple[str, str, float]] = {}

    def event(
        self,
        event_type: str,
        status: str,
        agent: str,
        label: str,
        detail: str = "",
        duration_ms: int | None = None,
    ) -> None:
        payload = {
            "type": event_type,
            "status": status,
            "agent": agent,
            "label": label,
            "detail": detail,
            "elapsedMs": round((time.monotonic() - self.started_at) * 1000),
        }
        if duration_ms is not None:
            payload["durationMs"] = duration_ms
        self.emit(payload)

    def on_chat_model_start(
        self,
        serialized: dict[str, Any],
        messages: list[list[Any]],
        *,
        run_id: UUID,
        **kwargs: Any,
    ) -> None:
        agent = _agent_from_text(
            [serialized, kwargs.get("name"), kwargs.get("tags"), kwargs.get("metadata")],
            self.current_agent,
        )
        self.model_runs[run_id] = (agent, time.monotonic())
        self.event("model", "running", agent, "模型正在推理")

    def on_llm_end(self, response: Any, *, run_id: UUID, **kwargs: Any) -> None:
        run = self.model_runs.pop(run_id, None)
        if not run:
            return
        agent, started = run
        self.event(
            "model",
            "completed",
            agent,
            "模型推理完成",
            duration_ms=round((time.monotonic() - started) * 1000),
        )

    def on_llm_error(self, error: BaseException, *, run_id: UUID, **kwargs: Any) -> None:
        run = self.model_runs.pop(run_id, (self.current_agent, time.monotonic()))
        agent, started = run
        self.event(
            "model",
            "failed",
            agent,
            "模型调用失败",
            _text(error),
            round((time.monotonic() - started) * 1000),
        )

    def on_tool_start(
        self,
        serialized: dict[str, Any],
        input_str: str,
        *,
        run_id: UUID,
        **kwargs: Any,
    ) -> None:
        tool = str(serialized.get("name") or kwargs.get("name") or "tool")
        agent = self.current_agent
        if tool == "task":
            agent = _agent_from_text([input_str, kwargs.get("inputs")], agent)
            if agent != "supervisor":
                self.current_agent = agent
        else:
            agent = _agent_from_text([kwargs.get("tags"), kwargs.get("metadata")], agent)
        self.tool_runs[run_id] = (tool, agent, time.monotonic())
        self.event(
            "agent" if tool == "task" else "tool",
            "running",
            agent,
            TOOL_LABELS.get(tool, f"调用工具：{tool}"),
            _text(input_str),
        )

    def on_tool_end(self, output: Any, *, run_id: UUID, **kwargs: Any) -> None:
        run = self.tool_runs.pop(run_id, None)
        if not run:
            return
        tool, agent, started = run
        self.event(
            "agent" if tool == "task" else "tool",
            "completed",
            agent,
            "专业 Agent 已返回" if tool == "task" else f"{TOOL_LABELS.get(tool, tool)}完成",
            duration_ms=round((time.monotonic() - started) * 1000),
        )
        if tool == "task":
            self.current_agent = "supervisor"

    def on_tool_error(self, error: BaseException, *, run_id: UUID, **kwargs: Any) -> None:
        run = self.tool_runs.pop(run_id, ("tool", self.current_agent, time.monotonic()))
        tool, agent, started = run
        self.event(
            "agent" if tool == "task" else "tool",
            "failed",
            agent,
            "专业 Agent 调用失败" if tool == "task" else f"{TOOL_LABELS.get(tool, tool)}失败",
            _text(error),
            round((time.monotonic() - started) * 1000),
        )
