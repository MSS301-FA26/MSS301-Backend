import logging
from core.db import get_db_connection
from core.event_guard import is_event_processed, mark_event_processed

logger = logging.getLogger(__name__)

def normalize_rating_to_feedback(rating: float) -> float:
    """
    Convert star rating [1.0, 5.0] to a continuous feedback signal in [-1.0, +1.0].
    - 5.0 -> +1.0 (strong positive)
    - 4.0 -> +0.7 (positive)
    - 3.0 ->  0.0 (neutral midpoint)
    - 2.0 -> -0.7 (negative)
    - 1.0 -> -1.0 (strong negative)
    Supports continuous decimal ratings (e.g. 4.5 -> +0.85, 3.5 -> +0.35).
    """
    if rating is None:
        return 0.0
    clamped = min(5.0, max(1.0, float(rating)))
    deviation = clamped - 3.0
    if deviation > 0:
        return round(0.7 * deviation if deviation <= 1.0 else 0.7 + 0.3 * (deviation - 1.0), 3)
    elif deviation < 0:
        return round(0.7 * deviation if deviation >= -1.0 else -0.7 + 0.3 * (deviation + 1.0), 3)
    return 0.0


def handle_movie_rated_event(body: dict):
    """
    Handle user rating submission event (1-5 stars).
    1★ -> raw_feedback = -1.0 (creates strong negative signal)
    5★ -> raw_feedback = +1.0 (creates strong positive signal)
    """
    try:
        payload = body.get("payload", {})
        user_id = payload.get("userId") or body.get("userId")
        movie_id = payload.get("movieId") or payload.get("movie_id")
        rating = float(payload.get("rating", 3.0))
        event_id = body.get("eventId") or body.get("event_id") or payload.get("eventId")

        if not user_id or not movie_id:
            logger.warning("[RabbitMQ] Missing user_id or movie_id in MOVIE_RATED payload")
            return

        clamped_rating = min(5.0, max(1.0, rating))
        raw_feedback = normalize_rating_to_feedback(clamped_rating)
        is_disliked = clamped_rating <= 1.0

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                if event_id and is_event_processed(cursor, event_id):
                    logger.info(f"[RabbitMQ] MOVIE_RATED event {event_id} already processed. Skipping.")
                    return

                cursor.execute("""
                    INSERT INTO user_interactions (
                        user_id, movie_id, rating, interaction_type, weight, event_id, is_disliked, raw_feedback_score, updated_at
                    )
                    VALUES (%s, %s, %s, 'MOVIE_RATED', %s, %s, %s, %s, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, movie_id)
                    DO UPDATE SET
                        rating = EXCLUDED.rating,
                        interaction_type = EXCLUDED.interaction_type,
                        weight = EXCLUDED.weight,
                        event_id = EXCLUDED.event_id,
                        is_disliked = EXCLUDED.is_disliked,
                        raw_feedback_score = EXCLUDED.raw_feedback_score,
                        updated_at = CURRENT_TIMESTAMP;
                """, (
                    int(user_id),
                    int(movie_id),
                    clamped_rating,
                    clamped_rating,
                    str(event_id) if event_id else None,
                    is_disliked,
                    raw_feedback
                ))

                if event_id:
                    mark_event_processed(cursor, event_id, "MOVIE_RATED", source="CATALOG_REVIEW_SERVICE")

            conn.commit()
            logger.info(f"[RabbitMQ] Recorded rating: User {user_id} -> Movie {movie_id} ({clamped_rating} stars)")
    except Exception as e:
        logger.error(f"[RabbitMQ] Error handling MOVIE_RATED event: {e}", exc_info=True)


def handle_movie_disliked_event(body: dict):
    """
    Handle explicit user dislike action.
    Sets is_disliked = TRUE and records negative profile signal.
    """
    try:
        payload = body.get("payload", {})
        user_id = payload.get("userId") or body.get("userId")
        movie_id = payload.get("movieId") or payload.get("movie_id")
        event_id = body.get("eventId") or body.get("event_id") or payload.get("eventId")

        if not user_id or not movie_id:
            return

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                if event_id and is_event_processed(cursor, event_id):
                    return

                cursor.execute("""
                    INSERT INTO user_interactions (
                        user_id, movie_id, rating, interaction_type, weight, event_id, is_disliked, raw_feedback_score, updated_at
                    )
                    VALUES (%s, %s, 1.0, 'MOVIE_DISLIKED', 5.0, %s, TRUE, -1.0, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, movie_id)
                    DO UPDATE SET
                        rating = 1.0,
                        interaction_type = 'MOVIE_DISLIKED',
                        weight = 5.0,
                        event_id = EXCLUDED.event_id,
                        is_disliked = TRUE,
                        raw_feedback_score = -1.0,
                        updated_at = CURRENT_TIMESTAMP;
                """, (int(user_id), int(movie_id), str(event_id) if event_id else None))

                if event_id:
                    mark_event_processed(cursor, event_id, "MOVIE_DISLIKED", source="CLIENT_FEEDBACK")

            conn.commit()
            logger.info(f"[RabbitMQ] Recorded dislike: User {user_id} -> Movie {movie_id}")
    except Exception as e:
        logger.error(f"[RabbitMQ] Error handling MOVIE_DISLIKED event: {e}", exc_info=True)


def handle_ticket_used_event(body: dict):
    """
    Handle ticket.used event (customer physically entered the cinema).
    Strongest positive engagement signal.
    """
    try:
        payload = body.get("payload", {})
        user_id = payload.get("userId") or body.get("userId")
        movie_id = payload.get("movieId") or payload.get("movie_id")
        event_id = body.get("eventId") or body.get("event_id") or payload.get("eventId")

        if not user_id or not movie_id:
            return

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                if event_id and is_event_processed(cursor, event_id):
                    return

                cursor.execute("""
                    INSERT INTO user_interactions (
                        user_id, movie_id, rating, interaction_type, weight, event_id, is_disliked, raw_feedback_score, updated_at
                    )
                    VALUES (%s, %s, 5.0, 'TICKET_USED', 5.0, %s, FALSE, 1.0, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id, movie_id)
                    DO UPDATE SET
                        rating = 5.0,
                        interaction_type = 'TICKET_USED',
                        weight = 5.0,
                        event_id = EXCLUDED.event_id,
                        is_disliked = FALSE,
                        raw_feedback_score = 1.0,
                        updated_at = CURRENT_TIMESTAMP;
                """, (int(user_id), int(movie_id), str(event_id) if event_id else None))

                if event_id:
                    mark_event_processed(cursor, event_id, "TICKET_USED", source="TICKET_CHECKIN_SERVICE")

            conn.commit()
            logger.info(f"[RabbitMQ] Recorded ticket checkin: User {user_id} -> Movie {movie_id}")
    except Exception as e:
        logger.error(f"[RabbitMQ] Error handling TICKET_USED event: {e}", exc_info=True)


def handle_movie_reviewed_event(body: dict):
    """
    Handle MOVIE_REVIEWED or MOVIE_COMMENTED event with comment and star rating.
    Performs aspect-based sentiment analysis, verifies rating consistency, and updates interaction signal.
    """
    try:
        from modules.recommendation.sentiment_analyzer import AspectSentimentAnalyzer
        payload = body.get("payload", {})
        user_id = payload.get("userId") or body.get("userId")
        movie_id = payload.get("movieId") or payload.get("movie_id")
        rating = float(payload.get("rating", 3.0))
        comment = payload.get("comment", "")
        event_id = body.get("eventId") or body.get("event_id") or payload.get("eventId")

        if not user_id or not movie_id:
            logger.warning("[RabbitMQ] Missing user_id or movie_id in MOVIE_REVIEWED payload")
            return

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                if event_id and is_event_processed(cursor, event_id):
                    logger.info(f"[RabbitMQ] MOVIE_REVIEWED event {event_id} already processed. Skipping.")
                    return

                analyzer = AspectSentimentAnalyzer()
                result = analyzer.analyze(comment=comment, rating=rating)
                analyzer.persist_review(
                    user_id=int(user_id),
                    movie_id=int(movie_id),
                    rating=rating,
                    comment=comment,
                    result=result,
                    event_id=str(event_id) if event_id else None
                )

                if event_id:
                    mark_event_processed(cursor, event_id, "MOVIE_REVIEWED", source="REVIEW_SERVICE")

            conn.commit()
            logger.info(f"[RabbitMQ] Processed review event: User {user_id} -> Movie {movie_id}")
    except Exception as e:
        logger.error(f"[RabbitMQ] Error handling MOVIE_REVIEWED event: {e}", exc_info=True)

