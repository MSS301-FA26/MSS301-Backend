from fastapi import APIRouter
from dtos.common import ApiResponse
from dtos.chat_dtos import ChatMessageRequest, ChatMessageResponse
from modules.chatbot.service_impl import get_chat_service

router = APIRouter(prefix="/api/v1/chat", tags=["Chatbot"])


@router.post("/message", response_model=ApiResponse[ChatMessageResponse])
def send_chat_message(request: ChatMessageRequest):
    """
    Multi-turn conversational assistant endpoint with context resolution, query rewriting, and grounding.
    """
    service = get_chat_service()
    data = service.chat(request=request)
    return ApiResponse(
        success=True,
        message="Chat message processed successfully",
        data=data
    )
