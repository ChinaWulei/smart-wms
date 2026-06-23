import os


BACKEND_URL = os.getenv("BACKEND_URL", "http://backend:8080").rstrip("/")
INTERNAL_TOKEN = os.getenv("AI_INTERNAL_TOKEN", "change-me")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
GEMINI_MAX_RETRIES = int(os.getenv("GEMINI_MAX_RETRIES", "5"))
GEMINI_API_KEY = os.getenv("GOOGLE_API_KEY") or os.getenv("GEMINI_API_KEY", "")
WMS_DB_DSN = os.getenv(
    "WMS_DB_DSN",
    "postgresql://postgres:postgres@postgres:5432/smart_wms",
)
