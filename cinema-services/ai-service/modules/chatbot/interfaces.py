from typing import Protocol
from dtos.chat_dtos import ChatMessageRequest, ChatMessageResponse


class IChatService(Protocol):
    """Interface defining operations for the Conversational Assistant Subsystem."""

    def chat(self, request: ChatMessageRequest) -> ChatMessageResponse:
        """Process multi-turn conversation turn with context resolution and grounded generation."""
        ...
