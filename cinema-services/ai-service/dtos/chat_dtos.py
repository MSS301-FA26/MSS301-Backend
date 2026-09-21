from pydantic import BaseModel, Field, ConfigDict
from typing import Optional, Dict, Any


class ChatMessageRequest(BaseModel):
    """Chat message request payload for conversational assistant."""
    model_config = ConfigDict(frozen=True)

    message: str = Field(..., min_length=1, max_length=1000, description="User message content")
    conversationId: Optional[str] = Field(default=None, description="Multi-turn conversation session ID")
    userId: Optional[int] = Field(default=None, description="Authenticated user ID if available")
    movieId: Optional[int] = Field(default=None, description="Current movie ID context if available")


class ChatMessageResponse(BaseModel):
    """Chat message response with grounded natural language answer and carousel data."""
    model_config = ConfigDict(frozen=True)

    conversationId: str = Field(..., description="Conversation session ID")
    reply: str = Field(..., description="Grounded natural language response")
    rewrittenQuery: Optional[str] = Field(default=None, description="Rewritten standalone query if context-dependent")
    subsystemInvoked: str = Field(..., description="Invoked subsystem: SEARCH, RECOMMEND, or DIRECT")
    data: Optional[Dict[str, Any]] = Field(default=None, description="Retrieved movie payload for UI carousel")
