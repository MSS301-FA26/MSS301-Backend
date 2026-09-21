import logging
from typing import List
from dtos.recommendation_dtos import (
    RecommendationResponse,
    ContentRecommendationResponse,
    RecommendationItem
)
from modules.recommendation.interfaces import IRecommendationService
from modules.recommendation.hybrid_engine import HybridRecommendationEngine
from modules.recommendation.content_filter import ContentBasedFilter
from core.db import get_db_connection
import psycopg2.extras

logger = logging.getLogger(__name__)


class RecommendationServiceImpl:
    """Implementation of IRecommendationService."""

    def __init__(self):
        self.hybrid_engine = HybridRecommendationEngine()
        self.content_filter = ContentBasedFilter()

    def get_user_recommendations(self, user_id: int, limit: int = 10) -> RecommendationResponse:
        strategy, items = self.hybrid_engine.recommend(user_id=user_id, limit=limit)
        return RecommendationResponse(
            userId=user_id,
            strategy=strategy,
            recommendations=items
        )

    def get_content_recommendations(self, movie_id: int, limit: int = 6) -> ContentRecommendationResponse:
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
                    genres=r.get("genres") or [],
                    releaseYear=r.get("release_year")
                ))

        return ContentRecommendationResponse(
            movieId=movie_id,
            recommendations=items
        )


_rec_service_instance = None


def get_recommendation_service() -> IRecommendationService:
    global _rec_service_instance
    if _rec_service_instance is None:
        _rec_service_instance = RecommendationServiceImpl()
    return _rec_service_instance
