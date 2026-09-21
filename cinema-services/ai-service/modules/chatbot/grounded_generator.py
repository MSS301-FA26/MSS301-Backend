import json
import logging
from typing import Optional, Any
from core.openai_client import get_openai_client

logger = logging.getLogger(__name__)


class GroundedGenerator:
    """
    Grounded Response Generator:
    Generates conversational responses strictly grounded in retrieved cinema catalog data
    to eliminate hallucinations.
    """

    def __init__(self):
        self.openai_client = get_openai_client()

    def generate(self, user_message: str, subsystem: str, data: Any) -> str:
        # Direct conversation case without retrieved movie entities
        if subsystem == "DIRECT" or not data:
            return (
                "Xin chào! Tôi là PopBot - Trợ lý AI của rạp chiếu phim CinePremier. "
                "Bạn có thể hỏi tôi về tìm kiếm phim, gợi ý phim theo sở thích hoặc lịch chiếu nhé! 🍿"
            )

        # Extract movie candidate entities
        movies = []
        if subsystem == "SEARCH" and hasattr(data, "results"):
            movies = data.results
        elif subsystem == "RECOMMEND" and hasattr(data, "recommendations"):
            movies = data.recommendations

        if not movies:
            return "Rất tiếc, hệ thống hiện không tìm thấy phim nào phù hợp với yêu cầu của bạn."

        # Prepare summary grounding context
        movie_titles = [m.title for m in movies[:4]]
        titles_str = ", ".join(f"'{t}'" for t in movie_titles)

        # 1. Attempt generation via LLM if client is available
        prompt = (
            f"Catalog data retrieved from CinePremier: {titles_str}.\n"
            f"User request: \"{user_message}\".\n"
            "Write 1-2 natural, polite sentences introducing these movies. "
            "ONLY mention movies present in the retrieved list above. Do NOT hallucinate unlisted movies."
        )

        messages = [
            {"role": "system", "content": "You are PopBot, AI assistant for CinePremier cinema. Ground all responses strictly in the provided movies."},
            {"role": "user", "content": prompt}
        ]

        llm_reply = self.openai_client.chat_completion(messages, temperature=0.2, max_tokens=150)
        if llm_reply:
            return llm_reply

        # 2. Template-based grounded fallback (Zero hallucination guarantee)
        count = len(movies)
        if subsystem == "SEARCH":
            return f"Tôi đã tìm thấy {count} phim phù hợp nhất với yêu cầu của bạn tại CinePremier: {titles_str}."
        else:
            return f"Dựa trên sở thích của bạn, đây là {count} phim được hệ thống đề xuất riêng cho bạn: {titles_str}."
