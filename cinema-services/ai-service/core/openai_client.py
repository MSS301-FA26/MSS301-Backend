import logging
from typing import List, Dict, Optional, Any
from app.config import get_settings

logger = logging.getLogger(__name__)


class OpenAIClient:
    """Client for OpenAI-compatible LLM endpoints (OpenAI, Groq, vLLM, DeepSeek)."""
    def __init__(self):
        settings = get_settings()
        self.api_key = settings.OPENAI_API_KEY
        self.base_url = settings.OPENAI_BASE_URL
        self.model = settings.OPENAI_MODEL
        self._client = None
        if self.api_key:
            try:
                from openai import OpenAI
                self._client = OpenAI(api_key=self.api_key, base_url=self.base_url)
                logger.info(f"Initialized OpenAI client connecting to {self.base_url} with model {self.model}")
            except Exception as e:
                logger.warning(f"Failed to initialize OpenAI client: {e}")

    def chat_completion(
        self,
        messages: List[Dict[str, Any]],
        temperature: float = 0.1,
        max_tokens: int = 500
    ) -> Optional[str]:
        """Execute chat completion API request."""
        if not self._client:
            raise RuntimeError("OpenAI API Key is not configured.")

        response = self._client.chat.completions.create(
            model=self.model,
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=15.0
        )
        content = response.choices[0].message.content
        return content.strip() if content else None

    def chat_completion_with_tools(
        self,
        messages: List[Dict[str, Any]],
        tools: List[Dict[str, Any]],
        temperature: float = 0.1,
        max_tokens: int = 500
    ):
        """Execute chat completion with tool/function definitions."""
        if not self._client:
            raise RuntimeError("OpenAI API Key is not configured for Chatbot Agent.")

        response = self._client.chat.completions.create(
            model=self.model,
            messages=messages,
            tools=tools,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=20.0
        )
        return response.choices[0].message


_openai_client_instance = None


def get_openai_client() -> OpenAIClient:
    global _openai_client_instance
    if _openai_client_instance is None:
        _openai_client_instance = OpenAIClient()
    return _openai_client_instance
