import logging
from typing import Dict, List, Optional, Set, Tuple
import numpy as np
from modules.recommendation.content_filter import cosine_similarity

logger = logging.getLogger(__name__)


class DiversityReranker:
    """
    Reranks recommendation candidates to balance Exploitation vs Exploration:
    - Genre Capping: Ensures no single genre dominates the top recommendation list (max 3 per genre).
    - Intra-List Diversity (ILD): Measures pairwise dissimilarities among recommended items.
    - Exploration Injection: Reserves bottom slots for high-quality items outside current preference clusters.
    """

    def rerank_by_genre_diversity(
        self,
        ranked_candidates: List[Tuple[int, float, str, str]],
        metadata_map: Dict[int, dict],
        max_per_genre: int = 3,
        limit: int = 10
    ) -> List[Tuple[int, float, str, str]]:
        """
        Greedy genre-capping reranker.
        Ensures at most max_per_genre movies with the same primary genre in top recommendations.
        """
        if not ranked_candidates:
            return []

        selected: List[Tuple[int, float, str, str]] = []
        deferred: List[Tuple[int, float, str, str]] = []
        genre_counts: Dict[str, int] = {}

        for item in ranked_candidates:
            mid = item[0]
            meta = metadata_map.get(mid, {})
            genres = meta.get("genres") or []
            primary_genre = genres[0] if genres else "General"

            current_count = genre_counts.get(primary_genre, 0)
            if current_count < max_per_genre:
                selected.append(item)
                genre_counts[primary_genre] = current_count + 1
            else:
                deferred.append(item)

            if len(selected) >= limit:
                break

        # If list has remaining slots, fill from deferred candidates
        if len(selected) < limit and deferred:
            remaining_slots = limit - len(selected)
            selected.extend(deferred[:remaining_slots])

        return selected

    def calculate_intra_list_diversity(self, vectors: List[List[float]]) -> float:
        """
        Compute Intra-List Diversity (ILD) using average pairwise cosine distance:
        ILD = 2 / (N * (N - 1)) * sum_{i < j} (1 - Cosine(v_i, v_j))
        Values range from 0.0 (identical items) to 2.0 (orthogonal/opposite items).
        """
        n = len(vectors)
        if n < 2:
            return 0.0

        pairwise_distances: List[float] = []
        for i in range(n):
            for j in range(i + 1, n):
                sim = cosine_similarity(vectors[i], vectors[j])
                dist = max(0.0, 1.0 - sim)
                pairwise_distances.append(dist)

        return round(float(np.mean(pairwise_distances)), 4) if pairwise_distances else 0.0

    def inject_exploration_slots(
        self,
        primary_list: List[Tuple[int, float, str, str]],
        exploration_candidates: List[Tuple[int, float, str, str]],
        slots: int = 1
    ) -> List[Tuple[int, float, str, str]]:
        """
        Replace bottom slots with high-quality exploration items to combat filter bubbles.
        """
        if not exploration_candidates or slots <= 0 or len(primary_list) <= slots:
            return primary_list

        existing_ids = {item[0] for item in primary_list}
        explorers: List[Tuple[int, float, str, str]] = []

        for item in exploration_candidates:
            if item[0] not in existing_ids:
                # Mark with exploration reason
                explorers.append((
                    item[0],
                    item[1],
                    "Featured discovery outside your usual favorites",
                    "EXPLORATION"
                ))
            if len(explorers) >= slots:
                break

        if not explorers:
            return primary_list

        result = list(primary_list[:-len(explorers)]) + explorers
        return result
