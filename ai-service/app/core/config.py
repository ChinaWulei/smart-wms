import os


BACKEND_URL = os.getenv("BACKEND_URL", "http://backend:8080").rstrip("/")
INTERNAL_TOKEN = os.getenv("AI_INTERNAL_TOKEN", "change-me")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
GEMINI_API_KEY = os.getenv("GOOGLE_API_KEY") or os.getenv("GEMINI_API_KEY", "")
