import time
import logging
from typing import List
from dtos.search_dtos import AdaptiveSearchResponse, SearchResultItem
from modules.search.interfaces import ISearchService
from modules.search.search_routes import SearchRoutes
from modules.search.soft_utility_router import SoftUtilityRouter
from core.middlewares.correlation_middleware import get_correlation_id
from core.db import get_db_connection

logger = logging.getLogger(__name__)


class SearchServiceImpl:
    """Implementation of ISearchService using multi-tier adaptive routing."""

    def __init__(self):
        self.routes = SearchRoutes()
        self.router = SoftUtilityRouter(entropy_threshold=0.95)

    def _log_telemetry(self, route: str, latency_ms: float, was_fallback: bool):
        try:
            cid = get_correlation_id()
            with get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("""
                        INSERT INTO ai_metrics_log (correlation_id, query_type, route_selected, latency_ms, was_fallback)
                        VALUES (%s, 'SEARCH', %s, %s, %s)
                    """, (cid, route, latency_ms, was_fallback))
                conn.commit()
        except Exception as e:
            logger.debug(f"Telemetry logging failed: {e}")

    def search(self, query: str, limit: int = 10) -> AdaptiveSearchResponse:
        start_time = time.perf_counter()

        # Step 1: Initial R0 candidate retrieval once (reused across tiers)
        r0_pool = self.routes.route_r0_first_stage(query, limit=max(limit * 3, 15))

        # Step 2: Soft Utility Router determines execution tier (No fallback)
        route_name, entropy_h, was_fallback = self.router.predict_route(query, r0_pool[:max(limit, 5)])

        # Step 3: Execute selected tier directly using pre-retrieved candidates
        if route_name == "R0_FIRST_STAGE":
            results = r0_pool[:limit]
        elif route_name == "R1_LIGHTWEIGHT":
            results = self.routes.route_r1_lightweight_reranker(query, candidates=r0_pool[:limit * 2], limit=limit)
        else:  # R2_ADAP_COLBERT
            results = self.routes.route_r2_heavy_adap_colbert(query, candidates=r0_pool, limit=limit)

        latency_ms = (time.perf_counter() - start_time) * 1000.0

        self._log_telemetry(route=route_name, latency_ms=latency_ms, was_fallback=was_fallback)

        return AdaptiveSearchResponse(
            query=query,
            routeUsed=route_name,
            routingEntropy=round(entropy_h, 3),
            wasFallback=was_fallback,
            latencyMs=round(latency_ms, 2),
            results=results
        )


_search_service_instance = None


def get_search_service() -> ISearchService:
    global _search_service_instance
    if _search_service_instance is None:
        _search_service_instance = SearchServiceImpl()
    return _search_service_instance
