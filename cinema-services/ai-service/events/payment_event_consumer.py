import json
import logging
from core.db import get_db_connection
from core.event_guard import is_event_processed, mark_event_processed

logger = logging.getLogger(__name__)


def handle_payment_succeeded_event(body: dict):
    """
    Handle payment.succeeded event from cinema.payment.events exchange.
    Records strong interaction signal (weight=5.0) for user on the purchased movie.
    Ensures idempotent execution using event_id and processed_events table.
    """
    try:
        payload = body.get("payload", {})
        user_id = payload.get("userId") or body.get("userId")
        movie_id = payload.get("movieId") or payload.get("movie_id")
        booking_id = payload.get("bookingId") or body.get("aggregateId")
        event_id = body.get("eventId") or body.get("event_id") or payload.get("eventId")
        if not event_id and booking_id:
            event_id = f"payment_booking_{booking_id}"

        if not user_id or not movie_id:
            logger.info(
                f"[RabbitMQ] Received payment.succeeded for bookingId={booking_id}, "
                f"missing userId or movieId in payload. Skipping interaction update."
            )
            return

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                # Idempotency check: Ignore duplicate delivery
                if event_id and is_event_processed(cursor, event_id):
                    logger.info(
                        f"[RabbitMQ] Event {event_id} already processed. Skipping duplicate payment interaction."
                    )
                    return

                cursor.execute("""
                    INSERT INTO user_interactions (
                        user_id, movie_id, rating, interaction_type, weight, event_id, is_disliked, raw_feedback_score, updated_at
                    )
                    VALUES (%s, %s, 5.0, 'BOOKING_PAID', 5.0, %s, FALSE, 1.0, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, movie_id)
                    DO UPDATE SET
                        rating = EXCLUDED.rating,
                        interaction_type = EXCLUDED.interaction_type,
                        weight = 5.0,
                        event_id = EXCLUDED.event_id,
                        is_disliked = FALSE,
                        raw_feedback_score = 1.0,
                        updated_at = CURRENT_TIMESTAMP
                """, (int(user_id), int(movie_id), str(event_id) if event_id else None))

                if event_id:
                    mark_event_processed(cursor, event_id, "BOOKING_PAID", source="PAYMENT_SERVICE")

            conn.commit()

        from core.cache import get_cache
        get_cache().delete_pattern(f"user:{user_id}:*")

        logger.info(
            f"[RabbitMQ] Recorded idempotent interaction: User {user_id} -> Movie {movie_id} (Booking {booking_id}, Event {event_id})"
        )
    except Exception as e:
        logger.error(f"[RabbitMQ] Error handling payment.succeeded event: {e}", exc_info=True)

