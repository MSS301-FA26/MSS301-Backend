import logging
from typing import List, Optional
from core.openai_client import OpenAIClient

logger = logging.getLogger(__name__)


class LLMRecommendationExplainer:
    """
    Grounded explanation synthesizer for personalized movie recommendations.
    Leverages LLM with factual constraint prompt guardrails to prevent hallucinations,
    with automatic graceful fallback to template rationale.
    """

    def __init__(self, openai_client: Optional[OpenAIClient] = None):
        self.openai_client = openai_client

    def explain(
        self,
        movie_title: str,
        genres: List[str],
        user_recent_genres: List[str],
        base_reason: str,
        source: str
    ) -> str:
        """
        Generate natural language explanation grounded strictly in verified catalog facts.
        Falls back to base_reason if LLM is unavailable or encounters an error.
        """
        if not self.openai_client:
            return base_reason

        genres_str = ", ".join(genres) if genres else "Điện ảnh"
        recent_str = ", ".join(user_recent_genres) if user_recent_genres else "các phim chiếu rạp gần đây"

        prompt = (
            f"Phim được đề xuất: '{movie_title}' (Thể loại: {genres_str}).\n"
            f"Lịch sử xem gần đây của người dùng: Thích các phim thể loại {recent_str}.\n"
            f"Nguồn gợi ý thuật toán: {source}.\n\n"
            "Nhiệm vụ: Viết đúng 1 câu tiếng Việt ngắn gọn (dưới 25 từ), tự nhiên và lịch sự để giải thích "
            "tại sao hệ thống rạp CinePremier đề xuất phim này cho người dùng.\n"
            "YÊU CẦU BẮT BUỘC: CHỈ ĐƯỢC dựa vào các thông tin thể loại và lịch sử cung cấp ở trên. "
            "Tuyệt đối không bịa đặt nội dung phim hoặc diễn viên không có trong dữ liệu."
        )

        messages = [
            {
                "role": "system",
                "content": "Bạn là chuyên viên đề xuất phim của CinePremier. Giải thích chính xác, súc tích và có căn cứ thực tế."
            },
            {
                "role": "user",
                "content": prompt
            }
        ]

        try:
            explanation = self.openai_client.chat_completion(messages, temperature=0.15, max_tokens=80)
            if explanation and len(explanation) > 5:
                return explanation.strip().strip('"')
            return base_reason
        except Exception as e:
            logger.debug(f"LLM explanation fallback due to: {e}")
            return base_reason
