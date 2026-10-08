import json
import logging
import math
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple
import numpy as np
import psycopg2.extras
from core.db import get_db_connection

logger = logging.getLogger(__name__)


def parse_vector(raw_vec) -> Optional[List[float]]:
    """Parse dense vector from PostgreSQL/pgvector format (list, array or json/string)."""
    if raw_vec is None:
        return None
    if isinstance(raw_vec, (list, tuple)):
        return [float(x) for x in raw_vec]
    if isinstance(raw_vec, np.ndarray):
        return raw_vec.astype(float).tolist()
    if isinstance(raw_vec, str):
        raw_vec = raw_vec.strip()
        if raw_vec.startswith("[") and raw_vec.endswith("]"):
            try:
                return [float(x.strip()) for x in raw_vec[1:-1].split(",") if x.strip()]
            except Exception:
                pass
            try:
                return json.loads(raw_vec)
            except Exception:
                pass
    return None


def cosine_similarity(vec_a: Optional[List[float]], vec_b: Optional[List[float]]) -> float:
    """Calculate Cosine Similarity between two dense embedding vectors."""
    if not vec_a or not vec_b:
        return 0.0
    a = np.array(vec_a, dtype=np.float32)
    b = np.array(vec_b, dtype=np.float32)
    norm_a = np.linalg.norm(a)
    norm_b = np.linalg.norm(b)
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return float(np.dot(a, b) / (norm_a * norm_b))


class ContentBasedFilter:
    """
    Content-Based Filtering Engine:
    - Dual user profile representation: Positive vector (likes, high ratings, bookings)
      and Negative vector (dislikes, low ratings).
    - Exponential Time Decay applied to all historical interactions.
    - Content scoring with negative penalty to suppress disliked patterns.
    """

    def get_movie_vector(self, movie_id: int) -> Optional[List[float]]:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("SELECT dense_vector FROM movie_embeddings WHERE movie_id = %s", (movie_id,))
                row = cur.fetchone()
                return parse_vector(row["dense_vector"]) if row and row.get("dense_vector") else None

    def find_similar_movies(self, target_movie_id: int, top_n: int = 6) -> List[Tuple[int, float]]:
        """Find top N most similar movies to target_movie_id by cosine similarity."""
        target_vec = self.get_movie_vector(target_movie_id)
        if not target_vec:
            return []

        similarities: List[Tuple[int, float]] = []
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, dense_vector
                    FROM movie_embeddings
                    WHERE movie_id != %s AND dense_vector IS NOT NULL
                """, (target_movie_id,))
                for row in cur.fetchall():
                    mid = row["movie_id"]
                    vec = parse_vector(row["dense_vector"])
                    if vec:
                        sim = cosine_similarity(target_vec, vec)
                        similarities.append((mid, sim))

        similarities.sort(key=lambda x: x[1], reverse=True)
        return similarities[:top_n]

    def build_dual_user_profile_vectors(
        self,
        interactions: List[dict],
        half_life_days: float = 30.0
    ) -> Tuple[Optional[List[float]], Optional[List[float]]]:
        """
        Construct dual (Positive, Negative) normalized preference vectors from user interaction history.
        Each interaction is decayed exponentially: decay = exp(-delta_days / half_life).
        - Positive signals: 4-5 stars, BOOKING_PAID, TICKET_USED, MOVIE_LIKED.
        - Negative signals: 1-2 stars, MOVIE_DISLIKED, raw_feedback_score < 0.
        """
        if not interactions:
            return None, None

        movie_ids = list({inter["movie_id"] for inter in interactions})
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, dense_vector
                    FROM movie_embeddings
                    WHERE movie_id = ANY(%s) AND dense_vector IS NOT NULL
                """, (movie_ids,))
                vectors_map = {r["movie_id"]: parse_vector(r["dense_vector"]) for r in cur.fetchall()}

        valid_vectors = {mid: vec for mid, vec in vectors_map.items() if vec is not None}
        if not valid_vectors:
            return None, None

        sample_dim = len(next(iter(valid_vectors.values())))
        pos_vec = np.zeros(sample_dim, dtype=np.float32)
        neg_vec = np.zeros(sample_dim, dtype=np.float32)
        pos_weight_total = 0.0
        neg_weight_total = 0.0

        now = datetime.now(timezone.utc)

        for inter in interactions:
            mid = inter.get("movie_id")
            if mid not in valid_vectors:
                continue

            vec = np.array(valid_vectors[mid], dtype=np.float32)
            updated_at = inter.get("updated_at")
            if updated_at:
                if isinstance(updated_at, str):
                    try:
                        updated_at = datetime.fromisoformat(updated_at.replace("Z", "+00:00"))
                    except Exception:
                        updated_at = None
                if updated_at and updated_at.tzinfo is None:
                    updated_at = updated_at.replace(tzinfo=timezone.utc)
                delta_days = max(0.0, (now - updated_at).total_seconds() / 86400.0) if updated_at else 0.0
            else:
                delta_days = 0.0

            decay = math.exp(-delta_days / half_life_days)
            rating = float(inter.get("rating") or 3.0)
            is_disliked = bool(inter.get("is_disliked"))
            interaction_type = str(inter.get("interaction_type") or "")
            raw_feedback = float(inter.get("raw_feedback_score") or 0.0)

            # Negative signal: Disliked or Rating <= 2 or raw_feedback < 0
            if is_disliked or rating <= 2.0 or raw_feedback < 0:
                penalty_weight = 1.0 if is_disliked else ((3.0 - rating) / 2.0)
                eff_weight = penalty_weight * decay
                neg_vec += vec * eff_weight
                neg_weight_total += eff_weight

            # Positive signal: Rating >= 4, or engagement signals (booking, ticket used, like)
            elif rating >= 4.0 or interaction_type in ("BOOKING_PAID", "TICKET_USED", "MOVIE_LIKED") or raw_feedback > 0:
                score_weight = (rating / 5.0) if rating >= 4.0 else 1.0
                eff_weight = score_weight * decay
                pos_vec += vec * eff_weight
                pos_weight_total += eff_weight

        pos_result = None
        if pos_weight_total > 0:
            norm_pos = np.linalg.norm(pos_vec)
            if norm_pos > 0:
                pos_result = (pos_vec / norm_pos).tolist()

        neg_result = None
        if neg_weight_total > 0:
            norm_neg = np.linalg.norm(neg_vec)
            if norm_neg > 0:
                neg_result = (neg_vec / norm_neg).tolist()

        return pos_result, neg_result

    def calculate_content_score(
        self,
        movie_vector: Optional[List[float]],
        pos_vec: Optional[List[float]],
        neg_vec: Optional[List[float]],
        lambda_neg: float = 0.5
    ) -> float:
        """
        Calculate Content-Based similarity with Negative Penalty:
        Score = max(0.0, Cosine(V_pos, V_movie) - lambda_neg * Cosine(V_neg, V_movie))
        """
        if not movie_vector or not pos_vec:
            return 0.0

        pos_sim = cosine_similarity(pos_vec, movie_vector)
        neg_sim = cosine_similarity(neg_vec, movie_vector) if neg_vec else 0.0

        score = pos_sim - (lambda_neg * neg_sim)
        return float(min(1.0, max(0.0, score)))

    def build_user_profile_vector(self, user_ratings: Dict[int, float]) -> Optional[List[float]]:
        """Legacy helper for backward compatibility."""
        interactions = [
            {"movie_id": mid, "rating": r, "is_disliked": (r <= 1.0)}
            for mid, r in user_ratings.items()
        ]
        pos_vec, _ = self.build_dual_user_profile_vectors(interactions)
        return pos_vec
