import logging
from typing import List, Optional
import psycopg2.extras

from core.db import get_db_connection
from core.cache import get_cache
from dtos.recommendation_dtos import (
    RecommendationResponse,
    ContentRecommendationResponse,
    TrendingRecommendationResponse,
    RecommendationItem,
    FeedbackRequest,
    FeedbackClickRequest
)
from modules.recommendation.interfaces import IRecommendationService
from modules.recommendation.hybrid_engine import HybridRecommendationEngine
from modules.recommendation.content_filter import ContentBasedFilter
from events.interaction_event_consumer import (
    handle_movie_rated_event,
    handle_movie_disliked_event
)

logger = logging.getLogger(__name__)


class RecommendationServiceImpl:
    """Enterprise implementation of IRecommendationService."""

    def __init__(self):
        self.hybrid_engine = HybridRecommendationEngine()
        self.content_filter = ContentBasedFilter()

    def get_user_recommendations(
        self,
        user_id: int,
        branch_id: Optional[int] = None,
        limit: int = 10
    ) -> RecommendationResponse:
        strategy, items, set_id = self.hybrid_engine.recommend(
            user_id=user_id,
            branch_id=branch_id,
            limit=limit
        )
        return RecommendationResponse(
            userId=user_id,
            strategy=strategy,
            branchId=branch_id,
            setId=set_id,
            recommendations=items
        )

    def get_content_recommendations(
        self,
        movie_id: int,
        limit: int = 6
    ) -> ContentRecommendationResponse:
        similar_tuples = self.content_filter.find_similar_movies(target_movie_id=movie_id, top_n=limit)
        if not similar_tuples:
            return ContentRecommendationResponse(movieId=movie_id, recommendations=[])

        movie_ids = [m[0] for m in similar_tuples]
        scores_map = {m[0]: m[1] for m in similar_tuples}

        items: List[RecommendationItem] = []
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, title, poster_url, genres, release_year
                    FROM movie_embeddings
                    WHERE movie_id = ANY(%s)
                """, (movie_ids,))
                rows = {r["movie_id"]: dict(r) for r in cur.fetchall()}

        for mid in movie_ids:
            if mid in rows:
                r = rows[mid]
                items.append(RecommendationItem(
                    movieId=mid,
                    title=r["title"],
                    posterUrl=r.get("poster_url") or "",
                    score=round(float(scores_map[mid]), 3),
                    reason="High semantic similarity in content, genre, and themes",
                    source="CONTENT_BASED",
                    genres=r.get("genres") or [],
                    releaseYear=r.get("release_year")
                ))

        return ContentRecommendationResponse(
            movieId=movie_id,
            recommendations=items
        )

    def get_trending_recommendations(
        self,
        branch_id: Optional[int] = None,
        limit: int = 10
    ) -> TrendingRecommendationResponse:
        items = self.hybrid_engine._get_fallback_popular_movies(limit=limit)
        return TrendingRecommendationResponse(
            branchId=branch_id,
            strategy="GLOBAL_POPULARITY",
            recommendations=items
        )

    def record_feedback(self, feedback: FeedbackRequest) -> bool:
        try:
            interaction_type = feedback.interactionType.upper()
            body = {
                "userId": feedback.userId,
                "movieId": feedback.movieId,
                "payload": {
                    "userId": feedback.userId,
                    "movieId": feedback.movieId,
                    "rating": feedback.rating,
                    "comment": feedback.comment
                }
            }

            if interaction_type in ("MOVIE_DISLIKED", "DISLIKE"):
                handle_movie_disliked_event(body)
            elif interaction_type in ("MOVIE_RATED", "RATING"):
                handle_movie_rated_event(body)
            else:
                handle_movie_rated_event(body)

            # Invalidate cached recommendations for this user
            get_cache().delete_pattern(f"user:{feedback.userId}:rec:*")
            return True
        except Exception as e:
            logger.error(f"Error recording user feedback: {e}", exc_info=True)
            return False

    def record_click_telemetry(self, click_data: FeedbackClickRequest) -> bool:
        """Record recommendation item click for CTR telemetry analytics."""
        try:
            with get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("""
                        INSERT INTO ai_metrics_log (correlation_id, query_type, route_selected, latency_ms)
                        VALUES (%s, 'RECOMMENDATION_CLICK', 'DIRECT_CLICK', 0.0);
                    """, (f"click_set_{click_data.setId}_m_{click_data.movieId}",))
                conn.commit()
            return True
        except Exception as e:
            logger.warning(f"Error recording click telemetry: {e}")
            return False


_rec_service_instance = None


def get_recommendation_service() -> IRecommendationService:
    global _rec_service_instance
    if _rec_service_instance is None:
        _rec_service_instance = RecommendationServiceImpl()
    return _rec_service_instance
