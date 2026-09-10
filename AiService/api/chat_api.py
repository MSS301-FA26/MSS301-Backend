from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from api.recommend_models import RecommendMovieResponse
from service.chat_service import chat

router = APIRouter()


class ChatRequest(BaseModel):
    message: str
    movieId: int | None = None
    userId: int | None = None
    conversationId: str | None = None


class ChatResponse(BaseModel):
    message: str
    conversationId: str
    data: dict | None = None


@router.post("/chat", response_model=ChatResponse)
def chat_endpoint(request: ChatRequest):
    if not request.message or not request.message.strip():
        raise HTTPException(status_code=400, detail="message không được để trống")

    reply, conversation_id, data = chat(request.message, request.movieId, request.userId, request.conversationId)
    return ChatResponse(message=reply, conversationId=conversation_id, data=data)
