import json
import logging
from core.db import get_db_connection

logger = logging.getLogger(__name__)


def handle_payment_succeeded_event(body: dict):
    """
    Handle payment.succeeded event from cinema.payment.events exchange.
    Records strong interaction signal (weight=5.0) for user on the purchased movie.
    Ensures idempotent execution using ON CONFLICT DO UPDATE.
    """
    try:
        payload = body.get("payload", {})
        user_id = payload.get("userId") or body.get("userId")
        movie_id = payload.get("movieId") or payload.get("movie_id")
        booking_id = payload.get("bookingId") or body.get("aggregateId")

        if not user_id or not movie_id:
            logger.info(
                f"[RabbitMQ] Received payment.succeeded for bookingId={booking_id}, "
                f"missing userId or movieId in payload. Skipping interaction update."
            )
            return

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute("""
                    INSERT INTO user_interactions (user_id, movie_id, rating, interaction_type, weight, updated_at)
                    VALUES (%s, %s, 5.0, 'BOOKING_PAID', 5.0, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, movie_id)
                    DO UPDATE SET
                        rating = EXCLUDED.rating,
                        weight = user_interactions.weight + 5.0,
                        updated_at = CURRENT_TIMESTAMP
                """, (int(user_id), int(movie_id)))
            conn.commit()

        logger.info(
            f"[RabbitMQ] Updated real-time interaction: User {user_id} -> Movie {movie_id} (Booking {booking_id})"
        )
    except Exception as e:
        logger.error(f"[RabbitMQ] Error handling payment.succeeded event: {e}", exc_info=True)
