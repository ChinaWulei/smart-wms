from functools import lru_cache
from pathlib import Path


@lru_cache(maxsize=1)
def load_rules() -> str:
    path = Path(__file__).resolve().parents[1] / "knowledge" / "wms_rules.md"
    return path.read_text(encoding="utf-8")
