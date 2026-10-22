import logging
from typing import Dict, Any
from fastapi import APIRouter
from dtos.common import ApiResponse
from modules.sync.service_impl import get_catalog_sync_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/internal/v1/sync", tags=["Internal Catalog Sync"])


@router.post("/catalog", response_model=ApiResponse[Dict[str, Any]])
def sync_catalog():
    """
    On-Demand Catalog Sync:
    Pulls active movies from Catalog Service via REST, computes embeddings, and updates local pgvector.
    Requires X-Internal-Service-Secret header.
    """
    logger.info("[SyncAPI] Manual catalog sync requested via /internal/v1/sync/catalog")
    sync_service = get_catalog_sync_service()
    result = sync_service.sync_catalog()
    return ApiResponse(
        success=(result.get("status") == "SUCCESS"),
        message=f"Đồng bộ danh mục hoàn tất: {result.get('syncedCount', 0)} phim đã cập nhật vector",
        data=result
    )


@router.post("/movies/{movie_id}", response_model=ApiResponse[Dict[str, Any]])
def sync_movie(movie_id: int):
    """
    Sync a specific single movie by ID from Catalog Service.
    Requires X-Internal-Service-Secret header.
    """
    logger.info(f"[SyncAPI] Single movie sync requested for movie_id={movie_id}")
    sync_service = get_catalog_sync_service()
    success = sync_service.sync_single_movie(movie_id)
    return ApiResponse(
        success=success,
        message=f"Đồng bộ phim #{movie_id} {'thành công' if success else 'thất bại'}",
        data={"movieId": movie_id, "updated": success}
    )


@router.get("/status", response_model=ApiResponse[Dict[str, Any]])
def get_sync_status():
    """
    Query current count and status of movie embeddings in local AI database.
    Requires X-Internal-Service-Secret header.
    """
    sync_service = get_catalog_sync_service()
    count = sync_service.get_embedding_count()
    return ApiResponse(
        success=True,
        message="Trạng thái lưu trữ vector embeddings",
        data={"totalEmbeddedMovies": count}
    )
