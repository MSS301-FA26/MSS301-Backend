import json
import logging
from typing import Optional, Any
from core.openai_client import get_openai_client
from modules.prompt.service_impl import get_prompt_service

logger = logging.getLogger(__name__)


class GroundedGenerator:
    """
    Grounded Response Generator:
    Generates conversational responses strictly grounded in retrieved cinema catalog data
    to eliminate hallucinations using dynamic prompt templates.
    """

    def __init__(self):
        self.openai_client = get_openai_client()
        self.prompt_service = get_prompt_service()

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

        # Attempt generation via dynamic prompt service
        try:
            sys_prompt, user_prompt, model_name, temp, max_tok = self.prompt_service.render_prompt(
                "CHATBOT_GROUNDED_REPLY",
                {
                    "titles_str": titles_str,
                    "user_message": user_message
                }
            )

            messages = [
                {"role": "system", "content": sys_prompt},
                {"role": "user", "content": user_prompt}
            ]

            llm_reply = self.openai_client.chat_completion(
                messages,
                temperature=temp,
                max_tokens=max_tok,
                model=model_name
            )
            if llm_reply:
                return llm_reply
        except Exception as e:
            logger.warning(f"[Grounded Generator] Dynamic prompt execution failed ({e}), using grounded template.")

        # Template-based grounded fallback (Zero hallucination guarantee)
        count = len(movies)
        if subsystem == "SEARCH":
            return f"Tôi đã tìm thấy {count} phim phù hợp nhất với yêu cầu của bạn tại CinePremier: {titles_str}."
        else:
            return f"Dựa trên sở thích của bạn, đây là {count} phim được hệ thống đề xuất riêng cho bạn: {titles_str}."
