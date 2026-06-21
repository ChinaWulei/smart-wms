import json

from langchain_core.tools import tool

from app.core.backend_client import get_context


@tool
def load_warehouse_context(warehouse_id: int | None = None) -> str:
    """Fetch current structured WMS data for one warehouse. Read-only."""
    return json.dumps(get_context(warehouse_id), ensure_ascii=False)
