import math
import logging
from typing import Dict, List, Tuple, Set
import psycopg2.extras
from core.db import get_db_connection

logger = logging.getLogger(__name__)


class PearsonShrinkageCollaborativeFilter:
    """
    User-User Collaborative Filtering implementation:
    - Pearson correlation with mean centering strictly over common rating overlap I_uv.
    - Overlap shrinkage: s'_{uv} = s_{uv} * |I_uv| / (|I_uv| + λ) with minimum overlap constraint |I_uv| >= m.
    - Prediction from rating deviation: r̂_ui = r̄_u + ∑ s'_{uv}(r_vi - r̄_v) / ∑ |s'_{uv}|.
    """

    def __init__(self, lambda_shrinkage: float = 5.0, min_overlap: int = 2, top_k_neighbors: int = 20):
        self.lambda_shrinkage = lambda_shrinkage
        self.min_overlap = min_overlap
        self.top_k_neighbors = top_k_neighbors

    def fetch_candidate_ratings(self, target_user_id: int, max_neighbors: int = 50) -> Dict[int, Dict[int, float]]:
        """
        Fetch ratings only for the target user and candidate neighbors who share rated movies.
        Avoids full-table scan on large user_interactions tables.
        """
        ratings: Dict[int, Dict[int, float]] = {}
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                # 1. Fetch movies rated by the target user
                cur.execute("SELECT movie_id FROM user_interactions WHERE user_id = %s", (target_user_id,))
                user_mids = [r["movie_id"] for r in cur.fetchall()]
                if not user_mids:
                    return {}

                # 2. Fetch candidate neighbors who have rated at least one common movie
                cur.execute("""
                    SELECT user_id, movie_id, COALESCE(rating, weight, 5.0) AS score
                    FROM user_interactions
                    WHERE user_id = %s
                       OR user_id IN (
                           SELECT DISTINCT user_id
                           FROM user_interactions
                           WHERE movie_id = ANY(%s) AND user_id != %s
                           LIMIT %s
                       )
                """, (target_user_id, user_mids, target_user_id, max_neighbors))

                for row in cur.fetchall():
                    uid = row["user_id"]
                    mid = row["movie_id"]
                    score = float(row["score"])
                    if uid not in ratings:
                        ratings[uid] = {}
                    ratings[uid][mid] = score
        return ratings

    def fetch_all_ratings(self) -> Dict[int, Dict[int, float]]:
        """Fetch user-item interaction scores with safe limit."""
        ratings: Dict[int, Dict[int, float]] = {}
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT user_id, movie_id, COALESCE(rating, weight, 5.0) AS score
                    FROM user_interactions
                    ORDER BY updated_at DESC
                    LIMIT 2000
                """)
                for row in cur.fetchall():
                    uid = row["user_id"]
                    mid = row["movie_id"]
                    score = float(row["score"])
                    if uid not in ratings:
                        ratings[uid] = {}
                    ratings[uid][mid] = score
        return ratings

    def calculate_pearson_similarity(
        self,
        u_ratings: Dict[int, float],
        v_ratings: Dict[int, float]
    ) -> float:
        """Compute Pearson correlation coefficient with mean-centering and overlap shrinkage."""
        common_items = set(u_ratings.keys()) & set(v_ratings.keys())
        overlap_size = len(common_items)

        if overlap_size < self.min_overlap:
            return 0.0

        mean_u = sum(u_ratings.values()) / len(u_ratings)
        mean_v = sum(v_ratings.values()) / len(v_ratings)

        numerator = sum((u_ratings[i] - mean_u) * (v_ratings[i] - mean_v) for i in common_items)
        denom_u = sum((u_ratings[i] - mean_u) ** 2 for i in common_items)
        denom_v = sum((v_ratings[i] - mean_v) ** 2 for i in common_items)

        if denom_u == 0 or denom_v == 0:
            return 0.0

        denominator = math.sqrt(denom_u) * math.sqrt(denom_v)
        s_uv = numerator / denominator

        # Overlap shrinkage adjustment
        shrinkage = overlap_size / (overlap_size + self.lambda_shrinkage)
        s_prime_uv = s_uv * shrinkage
        return float(s_prime_uv)

    def predict_user_ratings(
        self,
        target_user_id: int,
        all_ratings: Dict[int, Dict[int, float]],
        candidate_movie_ids: Set[int]
    ) -> List[Tuple[int, float]]:
        """Predict preference scores based on neighbor rating deviations from their personal averages."""
        target_ratings = all_ratings.get(target_user_id, {})
        if not target_ratings:
            return []

        target_mean = sum(target_ratings.values()) / len(target_ratings)

        neighbors: List[Tuple[int, float, float]] = []
        for v_id, v_ratings in all_ratings.items():
            if v_id == target_user_id:
                continue
            sim = self.calculate_pearson_similarity(target_ratings, v_ratings)
            if sim > 0:
                mean_v = sum(v_ratings.values()) / len(v_ratings)
                neighbors.append((v_id, sim, mean_v))

        neighbors.sort(key=lambda x: x[1], reverse=True)
        top_neighbors = neighbors[:self.top_k_neighbors]

        if not top_neighbors:
            return []

        predictions: List[Tuple[int, float]] = []
        for movie_id in candidate_movie_ids:
            if movie_id in target_ratings:
                continue

            weighted_deviation_sum = 0.0
            similarity_abs_sum = 0.0

            for v_id, sim, mean_v in top_neighbors:
                v_ratings = all_ratings[v_id]
                if movie_id in v_ratings:
                    deviation = v_ratings[movie_id] - mean_v
                    weighted_deviation_sum += sim * deviation
                    similarity_abs_sum += abs(sim)

            if similarity_abs_sum > 0:
                predicted_score = target_mean + (weighted_deviation_sum / similarity_abs_sum)
                clamped_score = max(1.0, min(5.0, predicted_score))
                predictions.append((movie_id, clamped_score))

        predictions.sort(key=lambda x: x[1], reverse=True)
        return predictions
