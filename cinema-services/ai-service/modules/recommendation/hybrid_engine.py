import logging
from typing import Dict, List, Set, Tuple
import psycopg2.extras
from core.db import get_db_connection
from modules.recommendation.collaborative_filter import PearsonShrinkageCollaborativeFilter
from modules.recommendation.content_filter import ContentBasedFilter, cosine_similarity
from dtos.recommendation_dtos import RecommendationItem

logger = logging.getLogger(__name__)


class HybridRecommendationEngine:
    """
    Adaptive Hybrid Recommendation Engine:
    - Combines Pearson Shrinkage CF and Content-based Cosine Similarity.
    - Real-time Booster: Increases scores for movies matching genres from recent booking payments.
    - Cold-start Fallback: Automatically serves trending/now-showing releases if no user history exists.
    """

    def __init__(self):
        self.cf_filter = PearsonShrinkageCollaborativeFilter(lambda_shrinkage=5.0, min_overlap=2)
        self.content_filter = ContentBasedFilter()

    def _get_movie_metadata_batch(self, movie_ids: List[int]) -> Dict[int, dict]:
        if not movie_ids:
            return {}
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, title, poster_url, genres, release_year
                    FROM movie_embeddings
                    WHERE movie_id = ANY(%s)
                """, (movie_ids,))
                return {r["movie_id"]: dict(r) for r in cur.fetchall()}

    def _get_all_candidate_movie_ids(self) -> Set[int]:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("SELECT movie_id FROM movie_embeddings WHERE status = 'NOW_SHOWING'")
                return {r["movie_id"] for r in cur.fetchall()}

    def _get_user_recent_interaction(self, user_id: int) -> Tuple[Set[str], float]:
        """Fetch genres from user's most recent interaction and calculate exponential time-decay boost."""
        import math
        from datetime import datetime, timezone

        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT me.genres, ui.updated_at
                    FROM user_interactions ui
                    JOIN movie_embeddings me ON me.movie_id = ui.movie_id
                    WHERE ui.user_id = %s
                    ORDER BY ui.updated_at DESC
                    LIMIT 1
                """, (user_id,))
                row = cur.fetchone()
                if not row or not row.get("genres"):
                    return set(), 0.0

                genres = set(row["genres"])
                updated_at = row.get("updated_at")
                if updated_at:
                    if updated_at.tzinfo is None:
                        updated_at = updated_at.replace(tzinfo=timezone.utc)
                    delta_hours = max(0.0, (datetime.now(timezone.utc) - updated_at).total_seconds() / 3600.0)
                    # Exponential decay with half-life of ~24 hours
                    decay_boost = 0.20 * math.exp(-delta_hours / 24.0)
                else:
                    decay_boost = 0.10

                return genres, round(decay_boost, 4)

    def recommend(self, user_id: int, limit: int = 10) -> Tuple[str, List[RecommendationItem]]:
        # Fetch ratings using candidate neighbor pruning (avoid full-table scan)
        all_ratings = self.cf_filter.fetch_candidate_ratings(user_id, max_neighbors=50)
        user_ratings = all_ratings.get(user_id, {})
        candidate_movies = self._get_all_candidate_movie_ids()

        # 1. Cold Start: User has no recorded interaction history
        if not user_ratings:
            logger.info(f"User {user_id} is cold-start -> Falling back to popular trending movies")
            return "COLD_START_POPULAR", self._get_fallback_popular_movies(limit)

        # 2. Collaborative Filtering scores
        cf_preds = dict(self.cf_filter.predict_user_ratings(user_id, all_ratings, candidate_movies))

        # 3. Content-Based Profile scores
        user_profile_vec = self.content_filter.build_user_profile_vector(user_ratings)
        cb_scores: Dict[int, float] = {}
        if user_profile_vec:
            with get_db_connection() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute("""
                        SELECT movie_id, dense_vector
                        FROM movie_embeddings
                        WHERE movie_id = ANY(%s) AND dense_vector IS NOT NULL
                    """, (list(candidate_movies),))
                    for r in cur.fetchall():
                        mid = r["movie_id"]
                        if mid not in user_ratings:
                            vec = r["dense_vector"]
                            if isinstance(vec, str):
                                try:
                                    import json
                                    vec = json.loads(vec)
                                except Exception:
                                    vec = [float(x.strip()) for x in vec.strip("[]").split(",") if x.strip()]
                            sim = cosine_similarity(user_profile_vec, vec)
                            cb_scores[mid] = sim

        # 4. Adaptive Confidence Weighting based on user interaction count N_u
        n_u = len(user_ratings)
        w_cf = n_u / (n_u + 5.0)  # Low history -> low CF weight; High history -> high CF weight
        w_cb = 1.0 - w_cf

        # 5. Real-time booster with exponential time-decay
        recent_genres, time_boost = self._get_user_recent_interaction(user_id)
        candidate_meta = self._get_movie_metadata_batch(list(candidate_movies))

        # 6. Hybrid scoring combination with dynamic weights
        hybrid_scores: List[Tuple[int, float, str]] = []
        for mid in candidate_movies:
            if mid in user_ratings:
                continue

            cf_score = (cf_preds.get(mid, 3.0) - 1.0) / 4.0 if mid in cf_preds else 0.5
            cb_score = cb_scores.get(mid, 0.5)

            combined = w_cf * cf_score + w_cb * cb_score

            movie_genres = set(candidate_meta.get(mid, {}).get("genres") or [])
            overlap_genre = recent_genres & movie_genres
            reason = "Personalized recommendation matching your taste profile"

            if overlap_genre and time_boost > 0.02:
                combined += time_boost
                genre_name = next(iter(overlap_genre))
                reason = f"Based on your recent interest in {genre_name}"

            clamped_score = min(1.0, max(0.0, combined))
            hybrid_scores.append((mid, clamped_score, reason))

        hybrid_scores.sort(key=lambda x: x[1], reverse=True)
        top_candidates = hybrid_scores[:limit]

        items: List[RecommendationItem] = []
        for mid, score, reason in top_candidates:
            meta = candidate_meta.get(mid, {})
            items.append(RecommendationItem(
                movieId=mid,
                title=meta.get("title", f"Movie #{mid}"),
                posterUrl=meta.get("poster_url") or "",
                score=round(score, 3),
                reason=reason,
                genres=meta.get("genres") or [],
                releaseYear=meta.get("release_year")
            ))

        strategy = "PEARSON_SHRINKAGE_HYBRID" if cf_preds else "CONTENT_BASED_PROFILE"
        return strategy, items
