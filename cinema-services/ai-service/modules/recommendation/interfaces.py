from typing import Optional, Protocol
from dtos.recommendation_dtos import (
    RecommendationResponse,
    ContentRecommendationResponse,
    TrendingRecommendationResponse,
    FeedbackRequest,
    FeedbackClickRequest,
    MovieReviewRequest,
    MovieReviewResponseData,
    RecommendationMetricsResponse
)


class IRecommendationService(Protocol):
    """Interface defining operations for the Recommendation Subsystem."""

    def get_user_recommendations(
        self,
        user_id: int,
        branch_id: Optional[int] = None,
        limit: int = 10
    ) -> RecommendationResponse:
        """Generate personalized recommendations for a target user with optional branch filtering."""
        ...

    def get_content_recommendations(
        self,
        movie_id: int,
        limit: int = 6
    ) -> ContentRecommendationResponse:
        """Find similar movies based on semantic metadata embeddings."""
        ...

    def get_trending_recommendations(
        self,
        branch_id: Optional[int] = None,
        limit: int = 10
    ) -> TrendingRecommendationResponse:
        """Retrieve trending popular releases for guests or fallback."""
        ...

    def record_feedback(self, feedback: FeedbackRequest) -> bool:
        """Record real-time user feedback (rating, like, dislike)."""
        ...

    def record_click_telemetry(self, click_data: FeedbackClickRequest) -> bool:
        """Record recommendation item click event for CTR analytics."""
        ...

    def submit_movie_review(self, review_data: MovieReviewRequest) -> MovieReviewResponseData:
        """Submit a user movie review and perform sentiment and consistency analysis."""
        ...

    def get_recommendation_metrics(
        self,
        branch_id: Optional[int] = None
    ) -> RecommendationMetricsResponse:
        """Retrieve aggregated full-funnel conversion and A/B test uplift telemetry metrics."""
        ...

    def purge_user_data(self, user_id: int) -> bool:
        """Purge all user preference records, reviews, recommendation history, and invalidate cache for GDPR compliance."""
        ...



