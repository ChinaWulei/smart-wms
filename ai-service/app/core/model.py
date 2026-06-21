from langchain_google_genai import ChatGoogleGenerativeAI

from app.core.config import GEMINI_API_KEY, GEMINI_MODEL


def create_model() -> ChatGoogleGenerativeAI | None:
    if not GEMINI_API_KEY:
        return None
    return ChatGoogleGenerativeAI(
        model=GEMINI_MODEL,
        google_api_key=GEMINI_API_KEY,
        temperature=0,
        max_retries=2,
    )


llm = create_model()


def model_configured() -> bool:
    return llm is not None
