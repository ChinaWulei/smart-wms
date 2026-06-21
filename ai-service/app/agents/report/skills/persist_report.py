import json

from langchain_core.tools import tool

from app.core.backend_client import save_report


@tool
def persist_report(title: str, analysis: str, structured_data_json: str) -> str:
    """Save an AI report and return its report ID. Does not modify WMS operations."""
    context = json.loads(structured_data_json)
    report_id = save_report(title, analysis, context)
    return f"REPORT_ID={report_id}"
