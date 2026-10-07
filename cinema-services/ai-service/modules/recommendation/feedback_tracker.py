from datetime import datetime, timezone, timedelta
import logging
from typing import List, Optional
import psycopg2.extras
from core.db import get_db_connection
from dtos.recommendation_dtos import RecommendationItem

logger = logging.getLogger(__name__)


class FeedbackTracker:
    """
    Manages click-to-booking feedback loop persistence and telemetry tracking:
    - Persists generated recommendation sets with unique set_id.
    - Records recommendation items to trace impression-to-conversion funnel.
    """

    def persist_recommendation_set(
        self,
        user_id: int,
        branch_id: Optional[int],
        strategy: str,
        items: List[RecommendationItem],
        experiment_variant: str = "CONTROL",
        ttl_hours: int = 24
    ) -> Optional[int]:
        """
        Store recommendation set and its ranked items in database for CTR and conversion tracking.
        """
        if not items:
            return None

        try:
            expires_at = datetime.now(timezone.utc) + timedelta(hours=ttl_hours)
            with get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("""
                        INSERT INTO recommendation_sets (user_id, branch_id, strategy, experiment_variant, generated_at, expires_at)
                        VALUES (%s, %s, %s, %s, CURRENT_TIMESTAMP, %s)
                        RETURNING set_id;
                    """, (user_id, branch_id, strategy, experiment_variant, expires_at))
                    set_id = cur.fetchone()[0]

                    item_tuples = [
                        (
                            set_id,
                            item.movieId,
                            item.score,
                            idx + 1,
                            item.source,
                            item.reason or ""
                        )
                        for idx, item in enumerate(items)
                    ]

                    psycopg2.extras.execute_values(
                        cur,
                        """
                        INSERT INTO recommendation_items (set_id, movie_id, score, rank, source, reason)
                        VALUES %s;
                        """,
                        item_tuples
                    )
                conn.commit()
            return set_id
        except Exception as e:
            logger.warning(f"Error persisting recommendation set for user {user_id}: {e}")
            return None
