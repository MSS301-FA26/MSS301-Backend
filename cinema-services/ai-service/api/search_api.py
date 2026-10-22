from fastapi import APIRouter
from dtos.common import ApiResponse
from dtos.search_dtos import AdaptiveSearchRequest, AdaptiveSearchResponse
from modules.search.service_impl import get_search_service

router = APIRouter(prefix="/api/v1/search", tags=["Search"])


@router.post("/adaptive", response_model=ApiResponse[AdaptiveSearchResponse])
def adaptive_movie_search(request: AdaptiveSearchRequest):
    """
    Adaptive multi-tier movie search using Soft Utility Supervision and Entropy Fallback.
    """
    service = get_search_service()
    data = service.search(query=request.query, limit=request.limit)
    return ApiResponse(
        success=True,
        message="Movie search completed successfully",
        data=data
    )
