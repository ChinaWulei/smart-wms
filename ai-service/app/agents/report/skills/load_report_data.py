import json

from langchain_core.tools import tool

from app.core.backend_client import get_context


@tool
def load_report_data(warehouse_id: int | None = None) -> str:
    """Load structured WMS data used to generate a warehouse report."""
    return json.dumps(get_context(warehouse_id), ensure_ascii=False)
