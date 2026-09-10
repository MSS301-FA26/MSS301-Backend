import psycopg2.extras
from fastapi import APIRouter
from pydantic import BaseModel
from service.db import get_connection
from service.embedding_service import get_embedding_service

router = APIRouter()


class RecommendStatsResponse(BaseModel):
    reviewCount: int
    reviewerCount: int
    paidBookingCount: int
    embeddedMovieCount: int


@router.get("/recommend/stats", response_model=RecommendStatsResponse)
def stats():
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("SELECT COUNT(*) AS c, COUNT(DISTINCT user_id) AS u FROM reviews")
        row = cursor.fetchone()
        review_count, reviewer_count = row["c"], row["u"]

        cursor.execute("SELECT COUNT(*) AS c FROM bookings WHERE status = 'PAID'")
        paid_booking_count = cursor.fetchone()["c"]
    finally:
        cursor.close()
        conn.close()

    try:
        embedded_count = len(get_embedding_service().get_embeddings())
    except Exception:
        embedded_count = 0

    return RecommendStatsResponse(
        reviewCount=review_count,
        reviewerCount=reviewer_count,
        paidBookingCount=paid_booking_count,
        embeddedMovieCount=embedded_count,
    )
