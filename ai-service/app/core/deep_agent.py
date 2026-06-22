import re

from deepagents import CompiledSubAgent, create_deep_agent
from deepagents.backends import FilesystemBackend

from app.agents.analytics.agent import create_analytics_agent
from app.agents.report.agent import create_report_agent
from app.agents.rules.agent import create_rules_agent
from app.agents.text_to_sql.agent import create_text_to_sql_agent
from app.agents.supervisor.prompt import SYSTEM_PROMPT
from app.core.memory import supervisor_checkpointer
from app.core.model import llm


backend = FilesystemBackend(
    root_dir="/app/app/agents/supervisor",
    virtual_mode=True,
)


def build_deep_agent():
    if llm is None:
        return None

    subagents = [
        CompiledSubAgent(
            name="rules-agent",
            description=(
                "Answer WMS process, inventory, outbound, inbound, Shipping Job, "
                "and permission rule questions."
            ),
            runnable=create_rules_agent(),
        ),
        CompiledSubAgent(
            name="analytics-agent",
            description=(
                "Read current warehouse data through approved tools and answer "
                "inventory, order, trend, quantity, SKU, and dashboard questions."
            ),
            runnable=create_analytics_agent(),
        ),
        CompiledSubAgent(
            name="sql-agent",
            description=(
                "Answer detailed WMS data questions by inspecting PostgreSQL "
                "schemas, generating read-only SQL, and querying warehouse data."
            ),
            runnable=create_text_to_sql_agent(),
        ),
        CompiledSubAgent(
            name="report-agent",
            description=(
                "Generate a warehouse operations report, persist it, and return "
                "the PDF report identifier."
            ),
            runnable=create_report_agent(),
        ),
    ]

    return create_deep_agent(
        name="wms-supervisor",
        model=llm,
        system_prompt=SYSTEM_PROMPT,
        subagents=subagents,
        backend=backend,
        memory=["memory/AGENTS.md"],
        skills=["skills/"],
        checkpointer=supervisor_checkpointer,
    )


deep_agent = build_deep_agent()


def final_text(result: dict) -> str:
    messages = result.get("messages", [])
    if not messages:
        return "AI assistant returned no message."
    message = messages[-1]
    text = getattr(message, "text", None)
    if text:
        return text
    content = getattr(message, "content", "")
    return content if isinstance(content, str) else str(content)


def response_metadata(answer: str) -> tuple[str, int | None, str]:
    agent_match = re.search(r"AGENT=(rules|analytics|sql|report)", answer)
    report_match = re.search(r"REPORT_ID=(\d+)", answer)
    agent = agent_match.group(1) if agent_match else "supervisor"
    report_id = int(report_match.group(1)) if report_match else None
    cleaned = re.sub(r"\n?AGENT=(rules|analytics|sql|report)", "", answer)
    cleaned = re.sub(r"\n?REPORT_ID=\d+", "", cleaned).strip()
    return agent, report_id, cleaned
