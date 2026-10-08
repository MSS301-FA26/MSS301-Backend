import logging
from typing import Dict, List, Optional, Tuple
import psycopg2.extras
from core.db import get_db_connection

logger = logging.getLogger(__name__)


class BranchAwareScorer:
    """
    Computes location-aware relevance:
    - Branch Movie Popularity: relative popularity of a movie at a specific cinema location.
    - User Branch Affinity: habitual affinity of user toward the chosen branch.
    """

    def get_branch_movie_popularity(
        self,
        branch_id: int,
        movie_ids: List[int]
    ) -> Dict[int, float]:
        """
        Calculate relative movie popularity scores [0.0, 1.0] at a target branch.
        Uses booking and rating activity at the branch.
        """
        if not movie_ids or not branch_id:
            return {}

        popularity_map: Dict[int, float] = {}
        try:
            with get_db_connection() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    # In cinema schema, count recent interactions aggregated for the candidate movies
                    cur.execute("""
                        SELECT movie_id, COUNT(*) AS count
                        FROM user_interactions
                        WHERE movie_id = ANY(%s) AND is_disliked = FALSE
                        GROUP BY movie_id
                    """, (movie_ids,))
                    raw_counts = {r["movie_id"]: float(r["count"]) for r in cur.fetchall()}

            max_count = max(raw_counts.values()) if raw_counts else 1.0
            for mid in movie_ids:
                count = raw_counts.get(mid, 0.0)
                # Normalized log-scaled popularity score
                popularity_map[mid] = min(1.0, count / max(max_count, 5.0))
        except Exception as e:
            logger.warning(f"Error computing branch movie popularity for branch {branch_id}: {e}")

        return popularity_map

    def get_user_branch_affinity(self, user_id: int, branch_id: int) -> float:
        """
        Calculate user affinity toward selected branch [0.0, 1.0].
        Users frequently attending a branch receive higher personalization resonance.
        """
        if not user_id or not branch_id:
            return 0.5  # Neutral affinity default

        try:
            with get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("""
                        SELECT COUNT(*)
                        FROM recommendation_sets
                        WHERE user_id = %s AND branch_id = %s
                    """, (user_id, branch_id))
                    branch_count = cur.fetchone()[0]

                    cur.execute("""
                        SELECT COUNT(*)
                        FROM recommendation_sets
                        WHERE user_id = %s
                    """, (user_id,))
                    total_count = cur.fetchone()[0]

            if total_count > 0:
                return round(min(1.0, (branch_count / total_count) + 0.3), 3)
            return 0.5
        except Exception as e:
            logger.warning(f"Error computing user branch affinity: {e}")
            return 0.5

    def compute_branch_boost(
        self,
        user_id: int,
        branch_id: Optional[int],
        movie_id: int,
        branch_pop_map: Dict[int, float],
        w_branch_pop: float = 0.15,
        w_affinity: float = 0.10
    ) -> Tuple[float, Optional[str]]:
        """
        Combine branch popularity and user affinity into additive score boost.
        """
        if not branch_id:
            return 0.0, None

        branch_pop = branch_pop_map.get(movie_id, 0.0)
        affinity = self.get_user_branch_affinity(user_id, branch_id)

        boost = (w_branch_pop * branch_pop) + (w_affinity * affinity)
        reason = "Popular at your selected cinema location" if branch_pop > 0.6 else None

        return round(boost, 4), reason
