from deepagents import create_deep_agent
from deepagents.backends import FilesystemBackend

from app.agents.analytics.prompt import SYSTEM_PROMPT
from app.agents.analytics.skills.load_warehouse_context import load_warehouse_context
from app.core.memory import analytics_checkpointer
from app.core.model import llm


def create_analytics_agent():
    if llm is None:
        raise RuntimeError("Gemini is not configured")
    return create_deep_agent(
        name="analytics-agent",
        model=llm,
        system_prompt=SYSTEM_PROMPT,
        tools=[load_warehouse_context],
        backend=FilesystemBackend(
            root_dir="/app/app/agents/analytics",
            virtual_mode=True,
        ),
        memory=["memory/AGENTS.md"],
        skills=["skills/"],
        checkpointer=analytics_checkpointer,
    )
