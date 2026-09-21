import logging
import numpy as np
from typing import Dict, List, Tuple, Optional
import psycopg2.extras
from core.db import get_db_connection

logger = logging.getLogger(__name__)


def cosine_similarity(vec_a: List[float], vec_b: List[float]) -> float:
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
    """Content-based filtering using dense semantic embeddings and metadata cosine similarity."""

    def get_movie_vector(self, movie_id: int) -> Optional[List[float]]:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("SELECT dense_vector FROM movie_embeddings WHERE movie_id = %s", (movie_id,))
                row = cur.fetchone()
                return row["dense_vector"] if row and row.get("dense_vector") else None

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
                    vec = row["dense_vector"]
                    sim = cosine_similarity(target_vec, vec)
                    similarities.append((mid, sim))

        similarities.sort(key=lambda x: x[1], reverse=True)
        return similarities[:top_n]

    def build_user_profile_vector(self, user_ratings: Dict[int, float]) -> Optional[List[float]]:
        """Construct weighted average user preference vector from interacted movie embeddings."""
        if not user_ratings:
            return None

        movie_ids = list(user_ratings.keys())
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, dense_vector
                    FROM movie_embeddings
                    WHERE movie_id = ANY(%s) AND dense_vector IS NOT NULL
                """, (movie_ids,))
                vectors = {r["movie_id"]: r["dense_vector"] for r in cur.fetchall()}

        if not vectors:
            return None

        weighted_vec = np.zeros(len(next(iter(vectors.values()))), dtype=np.float32)
        total_weight = 0.0

        for mid, vec in vectors.items():
            weight = max(1.0, user_ratings.get(mid, 3.0))
            weighted_vec += np.array(vec, dtype=np.float32) * weight
            total_weight += weight

        if total_weight > 0:
            weighted_vec /= total_weight
            norm = np.linalg.norm(weighted_vec)
            if norm > 0:
                weighted_vec /= norm
            return weighted_vec.tolist()
        return None
