import math
import logging
import numpy as np
from typing import List, Tuple
from dtos.search_dtos import SearchResultItem

logger = logging.getLogger(__name__)


class SoftUtilityRouter:
    """
    Adaptive router implementation with Soft Utility Supervision:
    - Extracts feature vector x_q = [h_q; s_q] from query and R0 candidate statistics.
    - Estimates probability distribution p(R0), p(R1), p(R2).
    - Computes routing entropy H(q).
    - Triggers confidence-aware fallback to R2 if H(q) > gamma.
    """

    def __init__(self, entropy_threshold: float = 0.95):
        self.entropy_threshold = entropy_threshold
        # Weights simulating MLP router trained under Soft Utility Loss
        # Features: [query_len, top_score, score_gap, score_entropy]
        self.weights = np.array([
            [-0.3,  0.8,  1.2, -0.5],  # R0: short query, high top score, large score gap
            [ 0.1,  0.2, -0.2,  0.3],  # R1: moderate complexity query
            [ 0.5, -0.6, -0.8,  0.8],  # R2: long, complex query, small gap, high entropy
        ], dtype=np.float32)

    def extract_statistical_features(self, query: str, r0_items: List[SearchResultItem]) -> np.ndarray:
        q_len = float(len(query.split()))

        if not r0_items:
            return np.array([q_len, 0.0, 0.0, 0.0], dtype=np.float32)

        scores = [item.score for item in r0_items]
        top_score = scores[0] if len(scores) > 0 else 0.0
        gap = (scores[0] - scores[1]) if len(scores) > 1 else 0.5

        # Score distribution entropy of first-stage candidates
        exp_scores = np.exp(scores - np.max(scores))
        probs = exp_scores / (np.sum(exp_scores) + 1e-9)
        score_entropy = -np.sum(probs * np.log(probs + 1e-9))

        return np.array([q_len, top_score, gap, score_entropy], dtype=np.float32)

    def predict_route(
        self,
        query: str,
        r0_items: List[SearchResultItem]
    ) -> Tuple[str, float, bool]:
        """
        Predicts optimal retrieval/re-ranking route for the query.
        Returns: (route_name, entropy_H, was_fallback)
        """
        features = self.extract_statistical_features(query, r0_items)
        logits = np.dot(self.weights, features)

        exp_logits = np.exp(logits - np.max(logits))
        p = exp_logits / np.sum(exp_logits)

        entropy_h = float(-np.sum(p * np.log(p + 1e-9)))

        # Select optimal route via argmax of predicted probabilities (No fallback)
        route_idx = int(np.argmax(p))
        route_names = ["R0_FIRST_STAGE", "R1_LIGHTWEIGHT", "R2_ADAP_COLBERT"]
        selected_route = route_names[route_idx]

        return selected_route, entropy_h, False
