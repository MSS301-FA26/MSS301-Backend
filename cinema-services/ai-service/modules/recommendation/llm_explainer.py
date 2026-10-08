import logging
from typing import List, Optional
from core.openai_client import OpenAIClient
from modules.prompt.service_impl import get_prompt_service

logger = logging.getLogger(__name__)


class LLMRecommendationExplainer:
    """
    Grounded explanation synthesizer for personalized movie recommendations.
    Leverages dynamic prompt templates with factual constraint guardrails to prevent hallucinations,
    with automatic graceful fallback to template rationale.
    """

    def __init__(self, openai_client: Optional[OpenAIClient] = None):
        self.openai_client = openai_client
        self.prompt_service = get_prompt_service()

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

        try:
            sys_prompt, user_prompt, model_name, temp, max_tok = self.prompt_service.render_prompt(
                "RECOMMENDATION_EXPLAINER",
                {
                    "movie_title": movie_title,
                    "genres_str": genres_str,
                    "recent_str": recent_str,
                    "source": source
                }
            )

            messages = [
                {"role": "system", "content": sys_prompt},
                {"role": "user", "content": user_prompt}
            ]

            explanation = self.openai_client.chat_completion(
                messages,
                temperature=temp,
                max_tokens=max_tok,
                model=model_name
            )
            if explanation and len(explanation) > 5:
                return explanation.strip().strip('"')
            return base_reason
        except Exception as e:
            logger.debug(f"LLM explanation fallback due to: {e}")
            return base_reason
