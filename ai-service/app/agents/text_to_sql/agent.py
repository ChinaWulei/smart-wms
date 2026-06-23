from pathlib import Path

from deepagents import create_deep_agent
from deepagents.backends import FilesystemBackend

from app.agents.text_to_sql.tools import (
    execute_readonly_sql,
    get_wms_schema,
    list_wms_tables,
)
from app.core.memory import sql_checkpointer
from app.core.model import llm


BASE_DIR = Path(__file__).resolve().parents[1] / "text-to-sql-agent"


def create_text_to_sql_agent():
    if llm is None:
        raise RuntimeError("Gemini is not configured")
    return create_deep_agent(
        name="sql-agent",
        model=llm,
        system_prompt=(
            "你是 Smart WMS 的 Text-to-SQL 数据查询 Agent。"
            "必须通过工具查看表结构并执行只读 SELECT SQL，不能编造数据库中不存在的数据。"
            "调用 execute_readonly_sql 时，sql 参数只传原始 SQL，不要包含 Markdown 代码块、SQL: 前缀或解释文字。"
            "回答要面向仓库业务人员，用中文说明口径、结果和必要的 SQL 依据。"
            "最终回答最后一行必须是 AGENT=sql。"
        ),
        tools=[list_wms_tables, get_wms_schema, execute_readonly_sql],
        backend=FilesystemBackend(root_dir=str(BASE_DIR), virtual_mode=True),
        memory=["AGENTS.md"],
        skills=["skills/"],
        checkpointer=sql_checkpointer,
    )
