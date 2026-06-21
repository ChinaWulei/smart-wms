import json

import httpx

from app.core.config import BACKEND_URL, INTERNAL_TOKEN


def _headers() -> dict[str, str]:
    return {"X-AI-Internal-Token": INTERNAL_TOKEN}


def get_context(warehouse_id: int | None) -> dict:
    params = {"warehouseId": warehouse_id} if warehouse_id is not None else {}
    with httpx.Client(timeout=30) as client:
        response = client.get(
            f"{BACKEND_URL}/api/internal/ai/context",
            params=params,
            headers=_headers(),
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
            headers=_headers(),
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
