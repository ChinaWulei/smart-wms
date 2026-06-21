from deepagents import create_deep_agent
from deepagents.backends import FilesystemBackend

from app.agents.report.prompt import SYSTEM_PROMPT
from app.agents.report.skills.load_report_data import load_report_data
from app.agents.report.skills.persist_report import persist_report
from app.core.memory import report_checkpointer
from app.core.model import llm


def create_report_agent():
    if llm is None:
        raise RuntimeError("Gemini is not configured")
    return create_deep_agent(
        name="report-agent",
        model=llm,
        system_prompt=SYSTEM_PROMPT,
        tools=[load_report_data, persist_report],
        backend=FilesystemBackend(
            root_dir="/app/app/agents/report",
            virtual_mode=True,
        ),
        memory=["memory/AGENTS.md"],
        skills=["skills/"],
        checkpointer=report_checkpointer,
    )
