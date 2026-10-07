from typing import Optional
from fastapi import APIRouter, Query, Path, Body, HTTPException, status
from dtos.common import ApiResponse
from dtos.recommendation_dtos import (
    RecommendationResponse,
    ContentRecommendationResponse,
    TrendingRecommendationResponse,
    FeedbackRequest,
    FeedbackResponseData,
    FeedbackClickRequest,
    MovieReviewRequest,
    MovieReviewResponseData,
    RecommendationMetricsResponse
)
from modules.recommendation.service_impl import get_recommendation_service

router = APIRouter(prefix="/api/v1/recommendations", tags=["Recommendations"])


@router.get("/users/{user_id}", response_model=ApiResponse[RecommendationResponse])
@router.get("/user/{user_id}", response_model=ApiResponse[RecommendationResponse], include_in_schema=False)
def get_user_recommendations(
    user_id: int = Path(..., description="User ID (Logical ID)"),
    branch_id: Optional[int] = Query(default=None, description="Optional Cinema Branch ID for availability filtering"),
    limit: int = Query(default=10, ge=1, le=50, description="Maximum number of recommendations")
):
    """
    Get personalized movie recommendations for a user.
    Integrates dual profile content-based filtering, Pearson CF, availability, branch scoring and diversity.
    """
    service = get_recommendation_service()
    data = service.get_user_recommendations(user_id=user_id, branch_id=branch_id, limit=limit)
    return ApiResponse(
        success=True,
        message="User recommendations retrieved successfully",
        data=data
    )


@router.get("/movies/{movie_id}/similar", response_model=ApiResponse[ContentRecommendationResponse])
@router.get("/content/{movie_id}", response_model=ApiResponse[ContentRecommendationResponse], include_in_schema=False)
def get_content_recommendations(
    movie_id: int = Path(..., description="Target movie ID (Logical ID)"),
    limit: int = Query(default=6, ge=1, le=20, description="Maximum number of similar movies")
):
    """
    Get similar movies based on semantic content and metadata embedding similarity.
    """
    service = get_recommendation_service()
    data = service.get_content_recommendations(movie_id=movie_id, limit=limit)
    return ApiResponse(
        success=True,
        message="Similar movies retrieved successfully",
        data=data
    )


@router.get("/trending", response_model=ApiResponse[TrendingRecommendationResponse])
def get_trending_recommendations(
    branch_id: Optional[int] = Query(default=None, description="Optional Cinema Branch ID filter"),
    limit: int = Query(default=10, ge=1, le=50, description="Maximum number of trending recommendations")
):
    """
    Get trending and popular movies currently showing.
    Ideal for guest users or cold-start fallback.
    """
    service = get_recommendation_service()
    data = service.get_trending_recommendations(branch_id=branch_id, limit=limit)
    return ApiResponse(
        success=True,
        message="Trending recommendations retrieved successfully",
        data=data
    )


@router.post("/feedbacks", response_model=ApiResponse[FeedbackResponseData])
@router.post("/feedback", response_model=ApiResponse[FeedbackResponseData], include_in_schema=False)
def record_recommendation_feedback(
    feedback: FeedbackRequest = Body(..., description="User rating, like, or dislike feedback payload")
):
    """
    Submit user feedback or explicit rating/dislike signal.
    Immediately updates user preference profile and invalidates cache.
    """
    service = get_recommendation_service()
    success = service.record_feedback(feedback)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to record recommendation feedback"
        )
    return ApiResponse(
        success=True,
        message="Feedback recorded successfully",
        data=FeedbackResponseData(
            recorded=True,
            userId=feedback.userId,
            movieId=feedback.movieId
        )
    )


@router.post("/clicks", response_model=ApiResponse[dict])
def record_recommendation_click(
    click_data: FeedbackClickRequest = Body(..., description="Recommendation click event telemetry payload")
):
    """
    Record recommendation item click event for CTR analytics and conversion tracking.
    """
    service = get_recommendation_service()
    service.record_click_telemetry(click_data)
    return ApiResponse(
        success=True,
        message="Click telemetry recorded successfully",
        data={"recorded": True, "setId": click_data.setId, "movieId": click_data.movieId}
    )


@router.post("/reviews", response_model=ApiResponse[MovieReviewResponseData])
def submit_movie_review(
    review_data: MovieReviewRequest = Body(..., description="User movie review with comment and rating")
):
    """
    Submit user movie review with comment and rating.
    Performs aspect-based sentiment analysis, detects consistency/sarcasm, and persists review.
    """
    service = get_recommendation_service()
    data = service.submit_movie_review(review_data)
    return ApiResponse(
        success=True,
        message="Movie review processed and recorded successfully",
        data=data
    )


@router.get("/metrics", response_model=ApiResponse[RecommendationMetricsResponse])
def get_recommendation_metrics(
    branch_id: Optional[int] = Query(default=None, description="Optional branch ID filter for location-specific telemetry")
):
    """
    Get full-funnel conversion telemetry and A/B test uplift analytics.
    Tracks impression, click, detail view, booking, and ticket redemption conversion rates.
    """
    service = get_recommendation_service()
    data = service.get_recommendation_metrics(branch_id=branch_id)
    return ApiResponse(
        success=True,
        message="Recommendation funnel telemetry metrics retrieved successfully",
        data=data
    )


