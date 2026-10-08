import math
import logging
from typing import Dict, List, Optional, Set, Tuple

logger = logging.getLogger(__name__)


class ContextualBanditExplorer:
    """
    Contextual Multi-Armed Bandit for recommendation exploration.
    Uses Upper Confidence Bound (UCB) and dynamic epsilon-greedy slot selection
    to balance exploitation of known user taste with exploration of novel content.
    """

    def __init__(self, exploration_constant: float = 0.25):
        self.c = exploration_constant
        self._item_impressions: Dict[int, int] = {}
        self._total_impressions: int = 0

    def record_impression(self, movie_id: int) -> None:
        """Increment display count for candidate movie."""
        self._total_impressions += 1
        self._item_impressions[movie_id] = self._item_impressions.get(movie_id, 0) + 1

    def compute_ucb_bonus(self, movie_id: int) -> float:
        """Calculate UCB uncertainty exploration bonus."""
        n_m = self._item_impressions.get(movie_id, 0)
        total_n = max(self._total_impressions, 1)
        bonus = self.c * math.sqrt(math.log(total_n + 1.0) / (n_m + 1.0))
        return min(0.30, bonus)

    def inject_exploration_candidates(
        self,
        ranked_candidates: List[Tuple[int, float, str, str]],
        candidate_metadata: Dict[int, dict],
        user_known_genres: Set[str],
        limit: int = 10,
        exploration_slots: int = 1
    ) -> List[Tuple[int, float, str, str]]:
        """
        Inject exploration slots into the ranked list to expose diverse, high-potential movies.
        Top positions remain reserved for highest-confidence recommendations.
        """
        if not ranked_candidates or len(ranked_candidates) <= exploration_slots or limit <= 3:
            return ranked_candidates[:limit]

        # Preserve top-half recommendations for pure exploitation
        keep_top_k = max(2, limit - exploration_slots)
        exploited = ranked_candidates[:keep_top_k]
        existing_ids = {item[0] for item in exploited}

        # Identify exploration candidates outside user's primary genres with high UCB bonus
        novelty_pool: List[Tuple[int, float, str, str]] = []
        for mid, score, reason, source in ranked_candidates[keep_top_k:]:
            if mid in existing_ids:
                continue

            movie_genres = set(candidate_metadata.get(mid, {}).get("genres") or [])
            is_novel_genre = bool(movie_genres and not (movie_genres & user_known_genres))
            ucb_val = self.compute_ucb_bonus(mid)
            exploration_score = score + ucb_val + (0.10 if is_novel_genre else 0.0)

            novelty_pool.append((
                mid,
                min(1.0, exploration_score),
                "Discovered for you: A fresh pick outside your familiar genres",
                "EXPLORATION"
            ))

        novelty_pool.sort(key=lambda x: x[1], reverse=True)
        injected_explorations = novelty_pool[:exploration_slots]

        # Combine exploited and exploration picks
        final_list = list(exploited)
        final_list.extend(injected_explorations)

        # Backfill if below required limit
        if len(final_list) < limit:
            added_ids = {x[0] for x in final_list}
            for candidate in ranked_candidates:
                if candidate[0] not in added_ids:
                    final_list.append(candidate)
                    if len(final_list) >= limit:
                        break

        # Record telemetry impressions for bandit learning
        for mid, _, _, _ in final_list:
            self.record_impression(mid)

        return final_list[:limit]
