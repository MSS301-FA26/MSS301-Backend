from fastapi import APIRouter, Query, Path
from dtos.common import ApiResponse
from dtos.recommendation_dtos import RecommendationResponse, ContentRecommendationResponse
from modules.recommendation.service_impl import get_recommendation_service

router = APIRouter(prefix="/api/v1/recommendations", tags=["Recommendations"])


@router.get("/user/{user_id}", response_model=ApiResponse[RecommendationResponse])
def get_user_recommendations(
    user_id: int = Path(..., description="User ID (Logical ID)"),
    limit: int = Query(default=10, ge=1, le=50, description="Maximum number of recommendations")
):
    """
    Get personalized movie recommendations for a user.
    Uses Pearson Correlation with overlap shrinkage and content-based profile.
    """
    service = get_recommendation_service()
    data = service.get_user_recommendations(user_id=user_id, limit=limit)
    return ApiResponse(
        success=True,
        message="User recommendations retrieved successfully",
        data=data
    )


@router.get("/content/{movie_id}", response_model=ApiResponse[ContentRecommendationResponse])
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
