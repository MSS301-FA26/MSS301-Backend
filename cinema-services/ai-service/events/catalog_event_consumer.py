import logging
from core.db import get_db_connection
from core.shared_embeddings import get_embedding_service

logger = logging.getLogger(__name__)


def handle_movie_published_event(body: dict):
    """
    Handle movie.published / movie.updated event from cinema.catalog.events exchange.
    Generates dense vector embeddings and updates movie_embeddings table.
    """
    try:
        payload = body.get("payload", body)
        movie_id = payload.get("id") or payload.get("movieId")
        title = payload.get("title", "")
        description = payload.get("description", "") or ""
        director = payload.get("director", "") or ""
        genres = payload.get("genres", [])
        if isinstance(genres, str):
            genres = [g.strip() for g in genres.split(",")]
        actors = payload.get("actors", []) or payload.get("mainActors", [])
        if isinstance(actors, str):
            actors = [a.strip() for a in actors.split(",")]
        poster_url = payload.get("posterUrl", "")
        release_year = payload.get("releaseYear")
        status = payload.get("status", "NOW_SHOWING")

        if not movie_id or not title:
            logger.warning("[RabbitMQ] Skipping movie event due to missing movieId or title")
            return

        # Concatenate text features for dense representation
        genres_str = " ".join(genres)
        actors_str = " ".join(actors)
        summary_text = f"{title} {description} {director} {genres_str} {actors_str}".strip()

        embedding_svc = get_embedding_service()
        dense_vector = embedding_svc.encode(summary_text)
        vector_repr = f"[{','.join(str(x) for x in dense_vector)}]"

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute("""
                    INSERT INTO movie_embeddings
                        (movie_id, title, genres, director, actors, description, poster_url, release_year, status, dense_vector, updated_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, CURRENT_TIMESTAMP)
                    ON CONFLICT (movie_id)
                    DO UPDATE SET
                        title = EXCLUDED.title,
                        genres = EXCLUDED.genres,
                        director = EXCLUDED.director,
                        actors = EXCLUDED.actors,
                        description = EXCLUDED.description,
                        poster_url = EXCLUDED.poster_url,
                        release_year = EXCLUDED.release_year,
                        status = EXCLUDED.status,
                        dense_vector = EXCLUDED.dense_vector,
                        updated_at = CURRENT_TIMESTAMP
                """, (
                    int(movie_id), title, genres, director, actors,
                    description, poster_url, release_year, status, vector_repr
                ))
            conn.commit()

        logger.info(f"[RabbitMQ] Updated movie embedding for Movie {movie_id} - '{title}'")
    except Exception as e:
        logger.error(f"[RabbitMQ] Error handling movie event: {e}", exc_info=True)
