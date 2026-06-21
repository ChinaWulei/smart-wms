from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    session_id: str = Field(min_length=1, max_length=120)
    message: str = Field(min_length=1, max_length=4000)
    warehouse_id: int | None = None
    locale: str = "zh"


class ChatResponse(BaseModel):
    answer: str
    agent: str
    reportId: int | None = None
    downloadUrl: str | None = None
