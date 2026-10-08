import logging
import numpy as np
import psycopg2.extras
from fastapi import APIRouter
from dtos.common import ApiResponse
from core.db import get_db_connection

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/ai", tags=["Telemetry"])


@router.get("/metrics", response_model=ApiResponse[dict])
def get_ai_metrics():
    """
    Retrieve telemetry and latency metrics (P50, P95, route distribution) for system evaluation.
    """
    metrics = {
        "totalQueries": 0,
        "latencyP50Ms": 0.0,
        "latencyP95Ms": 0.0,
        "latencyMeanMs": 0.0,
        "routes": {"R0_FIRST_STAGE": 0, "R1_LIGHTWEIGHT": 0, "R2_ADAP_COLBERT": 0},
        "fallbackCount": 0
    }

    try:
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("SELECT route_selected, latency_ms, was_fallback FROM ai_metrics_log")
                rows = cur.fetchall()

                if rows:
                    latencies = [r["latency_ms"] for r in rows if r["latency_ms"] is not None]
                    metrics["totalQueries"] = len(rows)
                    if latencies:
                        metrics["latencyMeanMs"] = round(float(np.mean(latencies)), 2)
                        metrics["latencyP50Ms"] = round(float(np.percentile(latencies, 50)), 2)
                        metrics["latencyP95Ms"] = round(float(np.percentile(latencies, 95)), 2)

                    for r in rows:
                        route = r.get("route_selected")
                        if route in metrics["routes"]:
                            metrics["routes"][route] += 1
                        if r.get("was_fallback"):
                            metrics["fallbackCount"] += 1
    except Exception as e:
        logger.warning(f"Error querying ai_metrics_log: {e}")

    return ApiResponse(
        success=True,
        message="AI telemetry metrics retrieved successfully",
        data=metrics
    )
