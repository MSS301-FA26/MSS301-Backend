from typing import Protocol
from dtos.recommendation_dtos import RecommendationResponse, ContentRecommendationResponse


class IRecommendationService(Protocol):
    """Interface defining operations for the Recommendation Subsystem."""

    def get_user_recommendations(self, user_id: int, limit: int = 10) -> RecommendationResponse:
        """Generate personalized recommendations for a target user."""
        ...

    def get_content_recommendations(self, movie_id: int, limit: int = 6) -> ContentRecommendationResponse:
        """Find similar movies based on semantic metadata embeddings."""
        ...
