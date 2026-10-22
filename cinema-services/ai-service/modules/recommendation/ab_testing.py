import hashlib
import logging
from typing import Dict, Any

logger = logging.getLogger(__name__)


class RecommendationABTestingRouter:
    """
    Deterministic A/B testing router for recommendation strategy experimentation.
    Allocates users into hash buckets to evaluate performance across variants:
    - CONTROL (0-49): Adaptive hybrid baseline with diversity re-ranking.
    - BANDIT_EXPLORATION (50-79): Hybrid ranking with contextual multi-armed bandit exploration.
    - LLM_EXPLAINER (80-99): Hybrid ranking with grounded LLM explanation generation.
    """

    VARIANT_CONTROL = "CONTROL"
    VARIANT_BANDIT = "BANDIT_EXPLORATION"
    VARIANT_LLM = "LLM_EXPLAINER"

    def get_variant(self, user_id: int) -> str:
        """Deterministically map user_id to an experiment variant."""
        if not user_id:
            return self.VARIANT_CONTROL

        hash_digest = hashlib.md5(f"experiment_recommendation:{user_id}".encode("utf-8")).hexdigest()
        bucket = int(hash_digest, 16) % 100

        if bucket < 50:
            variant = self.VARIANT_CONTROL
        elif bucket < 80:
            variant = self.VARIANT_BANDIT
        else:
            variant = self.VARIANT_LLM

        logger.debug(f"[A/B Testing] User {user_id} bucketed to {variant} (bucket {bucket})")
        return variant

    def should_apply_bandit(self, variant: str) -> bool:
        return variant == self.VARIANT_BANDIT

    def should_apply_llm_explanation(self, variant: str) -> bool:
        return variant == self.VARIANT_LLM
