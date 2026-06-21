from deepagents import create_deep_agent
from deepagents.backends import FilesystemBackend

from app.agents.rules.prompt import system_prompt
from app.core.memory import rules_checkpointer
from app.core.model import llm


def create_rules_agent():
    if llm is None:
        raise RuntimeError("Gemini is not configured")
    return create_deep_agent(
        name="rules-agent",
        model=llm,
        system_prompt=system_prompt(),
        tools=[],
        backend=FilesystemBackend(
            root_dir="/app/app/agents/rules",
            virtual_mode=True,
        ),
        memory=["memory/AGENTS.md"],
        skills=["skills/"],
        checkpointer=rules_checkpointer,
    )
