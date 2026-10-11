import time
import logging
from typing import Dict, Any, List, Optional

from core.db import get_db_connection
from core.shared_embeddings import get_embedding_service
from core.cache import get_cache
from core.clients.catalog_client import ICatalogClient, get_catalog_client
from modules.sync.interfaces import ICatalogSyncService

logger = logging.getLogger(__name__)


def _extract_string_list(raw_data: Any, key_name: str = "name") -> List[str]:
    """Extract a list of string items from list of objects or comma-delimited strings."""
    if not raw_data:
        return []
    if isinstance(raw_data, str):
        return [s.strip() for s in raw_data.split(",") if s.strip()]
    if isinstance(raw_data, list):
        result = []
        for item in raw_data:
            if isinstance(item, str):
                if item.strip():
                    result.append(item.strip())
            elif isinstance(item, dict) and key_name in item:
                val = str(item[key_name]).strip()
                if val:
                    result.append(val)
        return result
    return []


def _extract_release_year(movie: Dict[str, Any]) -> Optional[int]:
    """Safely parse release year from releaseDate or releaseYear fields."""
    if movie.get("releaseYear"):
        try:
            return int(movie["releaseYear"])
        except (ValueError, TypeError):
            pass
    rel_date = movie.get("releaseDate")
    if rel_date and isinstance(rel_date, str) and len(rel_date) >= 4:
        try:
            return int(rel_date[:4])
        except (ValueError, TypeError):
            pass
    return None


class CatalogSyncServiceImpl(ICatalogSyncService):
    """
    Implementation of ICatalogSyncService.
    Communicates with Catalog Service (Single Source of Truth) to ingest movies and calculate vector embeddings.
    """

    def __init__(self, catalog_client: Optional[ICatalogClient] = None):
        self.catalog_client = catalog_client or get_catalog_client()
        self.embedding_svc = get_embedding_service()

    def get_embedding_count(self) -> int:
        """Query total count of movies in local movie_embeddings table."""
        try:
            with get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT COUNT(*) FROM movie_embeddings;")
                    row = cur.fetchone()
                    return row[0] if row else 0
        except Exception as ex:
            logger.error(f"[CatalogSync] Failed to query movie_embeddings count: {ex}")
            return 0

    def sync_catalog(self) -> Dict[str, Any]:
        """
        Pull all movies from catalog-service, compute vector embeddings, and upsert into movie_embeddings.
        """
        start_time = time.perf_counter()
        logger.info("[CatalogSync] Starting full catalog sync from Catalog Service...")

        movies = self.catalog_client.fetch_all_movies()
        if not movies:
            logger.warning("[CatalogSync] No movies returned from Catalog Service.")
            return {
                "syncedCount": 0,
                "durationMs": round((time.perf_counter() - start_time) * 1000, 2),
                "totalInDb": self.get_embedding_count(),
                "status": "EMPTY_OR_UNREACHABLE"
            }

        synced_count = 0
        failed_count = 0

        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                for movie in movies:
                    try:
                        movie_id = movie.get("id") or movie.get("movieId")
                        title = movie.get("title", "")
                        if not movie_id or not title:
                            continue

                        description = movie.get("description", "") or ""
                        director = movie.get("director", "") or ""
                        genres = _extract_string_list(movie.get("genres"))
                        actors = _extract_string_list(movie.get("actors") or movie.get("mainActors"))
                        poster_url = movie.get("posterUrl", "")
                        release_year = _extract_release_year(movie)
                        status = movie.get("status", "NOW_SHOWING")

                        # Build dense semantic text representation
                        genres_str = " ".join(genres)
                        actors_str = " ".join(actors)
                        summary_text = f"{title} {description} {director} {genres_str} {actors_str}".strip()

                        dense_vector = self.embedding_svc.encode(summary_text)
                        vector_repr = f"[{','.join(str(x) for x in dense_vector)}]"

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
                                updated_at = CURRENT_TIMESTAMP;
                        """, (
                            int(movie_id), title, genres, director, actors,
                            description, poster_url, release_year, status, vector_repr
                        ))
                        synced_count += 1
                    except Exception as err:
                        logger.error(f"[CatalogSync] Error syncing movie {movie.get('id')}: {err}")
                        failed_count += 1

            conn.commit()

        # Invalidate recommendation cache
        try:
            get_cache().delete_pattern("user:*")
        except Exception as cache_ex:
            logger.debug(f"[CatalogSync] Cache invalidation warning: {cache_ex}")

        duration_ms = round((time.perf_counter() - start_time) * 1000, 2)
        total_in_db = self.get_embedding_count()

        logger.info(
            f"[CatalogSync] Catalog sync completed: {synced_count} synced, {failed_count} failed "
            f"in {duration_ms}ms. Total in DB: {total_in_db}."
        )

        return {
            "syncedCount": synced_count,
            "failedCount": failed_count,
            "durationMs": duration_ms,
            "totalInDb": total_in_db,
            "status": "SUCCESS"
        }

    def sync_single_movie(self, movie_id: int) -> bool:
        """Fetch and update embedding for a single movie ID."""
        logger.info(f"[CatalogSync] Syncing single movie ID: {movie_id}")
        movie = self.catalog_client.fetch_movie_by_id(movie_id)
        if not movie:
            logger.warning(f"[CatalogSync] Movie ID {movie_id} could not be retrieved from Catalog Service.")
            return False

        try:
            title = movie.get("title", "")
            description = movie.get("description", "") or ""
            director = movie.get("director", "") or ""
            genres = _extract_string_list(movie.get("genres"))
            actors = _extract_string_list(movie.get("actors") or movie.get("mainActors"))
            poster_url = movie.get("posterUrl", "")
            release_year = _extract_release_year(movie)
            status = movie.get("status", "NOW_SHOWING")

            summary_text = f"{title} {description} {director} {' '.join(genres)} {' '.join(actors)}".strip()
            dense_vector = self.embedding_svc.encode(summary_text)
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
                            updated_at = CURRENT_TIMESTAMP;
                    """, (
                        int(movie_id), title, genres, director, actors,
                        description, poster_url, release_year, status, vector_repr
                    ))
                conn.commit()

            get_cache().delete_pattern("user:*")
            logger.info(f"[CatalogSync] Successfully updated single movie embedding for {movie_id} - '{title}'")
            return True
        except Exception as ex:
            logger.error(f"[CatalogSync] Error syncing single movie {movie_id}: {ex}", exc_info=True)
            return False


_sync_service_instance: Optional[ICatalogSyncService] = None


def get_catalog_sync_service() -> ICatalogSyncService:
    """Factory helper providing singleton ICatalogSyncService."""
    global _sync_service_instance
    if _sync_service_instance is None:
        _sync_service_instance = CatalogSyncServiceImpl()
    return _sync_service_instance
