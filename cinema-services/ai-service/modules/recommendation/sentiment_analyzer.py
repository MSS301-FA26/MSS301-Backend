import json
import logging
from dataclasses import dataclass
from typing import Dict, Optional, Tuple
import psycopg2.extras

from core.db import get_db_connection
from core.openai_client import OpenAIClient, get_openai_client

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class SentimentAnalysisResult:
    sentiment_label: str
    sentiment_score: float
    feedback_consistency: str
    confidence_score: float
    aspect_sentiment: Dict[str, float]


class AspectSentimentAnalyzer:
    """
    Aspect-based sentiment analyzer leveraging LLM semantic reasoning.
    Analyzes review text, evaluates rating-comment consistency, detects sarcasm and noise,
    and extracts fine-grained aspect dimensions (plot, acting, visuals, audio).
    """

    def __init__(self, openai_client: Optional[OpenAIClient] = None):
        self.openai_client = openai_client or get_openai_client()

    def analyze(self, comment: str, rating: float) -> SentimentAnalysisResult:
        clean_text = (comment or "").strip()
        normalized_rating = (rating - 3.0) / 2.0

        if not clean_text:
            label = "POSITIVE" if rating >= 4.0 else ("NEGATIVE" if rating <= 2.0 else "NEUTRAL")
            return SentimentAnalysisResult(
                sentiment_label=label,
                sentiment_score=round(normalized_rating, 3),
                feedback_consistency="CONSISTENT",
                confidence_score=0.85,
                aspect_sentiment={}
            )

        # Attempt semantic sentiment analysis via LLM
        prompt = (
            f"User review for a cinema movie:\n"
            f"- Star rating given by user: {rating} / 5.0\n"
            f"- Review comment: \"{clean_text}\"\n\n"
            "Analyze the review and return a valid JSON object with the following fields:\n"
            "- \"sentiment_score\": float between -1.0 (most negative) and 1.0 (most positive)\n"
            "- \"sentiment_label\": string, either \"POSITIVE\", \"NEGATIVE\", or \"NEUTRAL\"\n"
            "- \"feedback_consistency\": string, \"CONSISTENT\" if rating matches comment tone, or \"INCONSISTENT\" if contradictory (e.g. 5 stars but harsh complaints, 1 star but glowing praise, sarcasm, spam, or off-topic)\n"
            "- \"confidence_score\": float between 0.0 and 1.0 (lower <= 0.35 if sarcastic, contradictory, or noisy; high >= 0.85 if clear and consistent)\n"
            "- \"aspect_sentiment\": object with keys \"plot\", \"acting\", \"visuals\", \"audio\" containing score between -1.0 and 1.0 for aspects mentioned in comment\n\n"
            "Return ONLY the raw JSON object without markdown formatting, code block, or explanation."
        )

        messages = [
            {
                "role": "system",
                "content": "You are an expert NLP cinema sentiment and aspect analyzer. Output strictly valid JSON."
            },
            {
                "role": "user",
                "content": prompt
            }
        ]

        try:
            raw_reply = self.openai_client.chat_completion(messages, temperature=0.0, max_tokens=250)
            if raw_reply:
                # Strip markdown code blocks if wrapped
                cleaned_json = raw_reply.strip()
                if cleaned_json.startswith("```"):
                    cleaned_json = cleaned_json.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
                parsed = json.loads(cleaned_json)

                score = float(parsed.get("sentiment_score", normalized_rating))
                label = str(parsed.get("sentiment_label", "NEUTRAL")).upper()
                if label not in ("POSITIVE", "NEGATIVE", "NEUTRAL"):
                    label = "POSITIVE" if score >= 0.2 else ("NEGATIVE" if score <= -0.2 else "NEUTRAL")

                consistency = str(parsed.get("feedback_consistency", "CONSISTENT")).upper()
                if consistency not in ("CONSISTENT", "INCONSISTENT"):
                    consistency = "CONSISTENT"

                confidence = max(0.0, min(1.0, float(parsed.get("confidence_score", 0.85))))
                if consistency == "INCONSISTENT":
                    confidence = min(0.35, confidence * 0.4)
                aspects = parsed.get("aspect_sentiment") or {}
                aspect_map = {k: round(float(v), 2) for k, v in aspects.items() if isinstance(v, (int, float))}

                return SentimentAnalysisResult(
                    sentiment_label=label,
                    sentiment_score=round(score, 3),
                    feedback_consistency=consistency,
                    confidence_score=round(confidence, 3),
                    aspect_sentiment=aspect_map
                )
        except Exception as e:
            logger.debug(f"LLM sentiment analysis fallback to rating baseline: {e}")

        # Mathematical fallback when LLM is unavailable
        label = "POSITIVE" if rating >= 4.0 else ("NEGATIVE" if rating <= 2.0 else "NEUTRAL")
        return SentimentAnalysisResult(
            sentiment_label=label,
            sentiment_score=round(normalized_rating, 3),
            feedback_consistency="CONSISTENT",
            confidence_score=0.75,
            aspect_sentiment={}
        )

    def persist_review(
        self,
        user_id: int,
        movie_id: int,
        rating: float,
        comment: str,
        result: SentimentAnalysisResult,
        event_id: Optional[str] = None
    ) -> None:
        """Persist structured review in database and update user interaction signal."""
        with get_db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    INSERT INTO movie_reviews (
                        user_id, movie_id, rating, comment,
                        sentiment_label, sentiment_score, feedback_consistency,
                        confidence_score, aspect_sentiment, event_id, updated_at
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, movie_id) DO UPDATE SET
                        rating = EXCLUDED.rating,
                        comment = EXCLUDED.comment,
                        sentiment_label = EXCLUDED.sentiment_label,
                        sentiment_score = EXCLUDED.sentiment_score,
                        feedback_consistency = EXCLUDED.feedback_consistency,
                        confidence_score = EXCLUDED.confidence_score,
                        aspect_sentiment = EXCLUDED.aspect_sentiment,
                        updated_at = CURRENT_TIMESTAMP
                """, (
                    user_id,
                    movie_id,
                    rating,
                    comment,
                    result.sentiment_label,
                    result.sentiment_score,
                    result.feedback_consistency,
                    result.confidence_score,
                    json.dumps(result.aspect_sentiment),
                    event_id
                ))

                # Update interaction profile: scale signal by confidence score to prevent distorted profiles
                weighted_feedback = result.sentiment_score * result.confidence_score
                is_disliked = (rating <= 2.0 and result.feedback_consistency == "CONSISTENT") or (result.sentiment_score <= -0.5 and result.confidence_score >= 0.8)

                cur.execute("""
                    INSERT INTO user_interactions (
                        user_id, movie_id, interaction_type, rating,
                        raw_feedback_score, is_disliked, updated_at
                    ) VALUES (%s, %s, 'MOVIE_REVIEWED', %s, %s, %s, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, movie_id) DO UPDATE SET
                        rating = EXCLUDED.rating,
                        raw_feedback_score = EXCLUDED.raw_feedback_score,
                        is_disliked = EXCLUDED.is_disliked,
                        updated_at = CURRENT_TIMESTAMP
                """, (user_id, movie_id, rating, round(weighted_feedback, 3), is_disliked))

            conn.commit()
