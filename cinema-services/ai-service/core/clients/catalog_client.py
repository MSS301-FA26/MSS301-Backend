import abc
import logging
import uuid
from typing import List, Dict, Any, Optional
import httpx

from app.config import get_settings
from core.middlewares.correlation_middleware import get_correlation_id

logger = logging.getLogger(__name__)


class ICatalogClient(abc.ABC):
    """Interface defining contracts for communication with Catalog Service."""

    @abc.abstractmethod
    def fetch_all_movies(self, status: Optional[str] = None) -> List[Dict[str, Any]]:
        """Fetch all movies from Catalog Service."""
        pass

    @abc.abstractmethod
    def fetch_movie_by_id(self, movie_id: int) -> Optional[Dict[str, Any]]:
        """Fetch a single movie detail by logical movie ID."""
        pass

    @abc.abstractmethod
    def fetch_available_showtimes(self, movie_id: int, date: Optional[str] = None) -> List[Dict[str, Any]]:
        """Fetch available showtimes for a specific movie."""
        pass

    @abc.abstractmethod
    def fetch_cinemas(self) -> List[Dict[str, Any]]:
        """Fetch all cinema theaters."""
        pass


class CatalogClientImpl(ICatalogClient):
    """
    Enterprise implementation of ICatalogClient using httpx with resilience and security headers.
    Adheres to Zero-Trust architecture with X-Internal-Service-Secret and distributed tracing.
    """

    def __init__(self, base_url: Optional[str] = None, timeout_seconds: float = 10.0):
        settings = get_settings()
        self.base_url = (base_url or settings.CATALOG_SERVICE_URL).rstrip("/")
        self.timeout = timeout_seconds

    def _get_headers(self) -> Dict[str, str]:
        settings = get_settings()
        cid = get_correlation_id() or str(uuid.uuid4())
        return {
            "X-Internal-Service-Secret": settings.INTERNAL_SERVICE_SECRET,
            "X-Gateway-Secret": settings.INTERNAL_GATEWAY_SECRET,
            "X-Correlation-Id": cid,
            "Accept": "application/json"
        }

    def fetch_all_movies(self, status: Optional[str] = None) -> List[Dict[str, Any]]:
        """
        Fetch all movies across pages from /api/v1/movies.
        """
        all_movies: List[Dict[str, Any]] = []
        page = 0
        size = 100
        headers = self._get_headers()

        with httpx.Client(timeout=self.timeout) as client:
            while True:
                url = f"{self.base_url}/api/v1/movies"
                params = {"page": page, "size": size}
                if status:
                    params["status"] = status

                try:
                    logger.info(f"[CatalogClient] Fetching movies from {url} (page={page}, size={size})")
                    resp = client.get(url, params=params, headers=headers)
                    if resp.status_code != 200:
                        logger.error(f"[CatalogClient] Failed to fetch movies. HTTP {resp.status_code}: {resp.text}")
                        break

                    body = resp.json()
                    data = body.get("data", {})
                    # Spring Boot PageResponse envelope: data.content
                    content = data.get("content", []) if isinstance(data, dict) else (data if isinstance(data, list) else [])
                    if not content:
                        break

                    all_movies.extend(content)
                    total_pages = data.get("totalPages", 1) if isinstance(data, dict) else 1
                    is_last = data.get("last", True) if isinstance(data, dict) else True

                    page += 1
                    if is_last or page >= total_pages:
                        break
                except Exception as ex:
                    logger.error(f"[CatalogClient] Network exception fetching movies: {ex}", exc_info=True)
                    break

        logger.info(f"[CatalogClient] Successfully retrieved {len(all_movies)} movies from Catalog Service.")
        return all_movies

    def fetch_movie_by_id(self, movie_id: int) -> Optional[Dict[str, Any]]:
        url = f"{self.base_url}/api/v1/movies/{movie_id}"
        headers = self._get_headers()

        try:
            with httpx.Client(timeout=self.timeout) as client:
                resp = client.get(url, headers=headers)
                if resp.status_code == 200:
                    body = resp.json()
                    return body.get("data")
                elif resp.status_code == 404:
                    logger.warning(f"[CatalogClient] Movie ID {movie_id} not found.")
                    return None
                else:
                    logger.error(f"[CatalogClient] Failed to fetch movie {movie_id}. HTTP {resp.status_code}: {resp.text}")
                    return None
        except Exception as ex:
            logger.error(f"[CatalogClient] Error fetching movie {movie_id}: {ex}")
            return None

    def fetch_available_showtimes(self, movie_id: int, date: Optional[str] = None) -> List[Dict[str, Any]]:
        url = f"{self.base_url}/api/v1/movies/{movie_id}/available-showtimes"
        params = {}
        if date:
            params["date"] = date
        headers = self._get_headers()

        try:
            with httpx.Client(timeout=self.timeout) as client:
                resp = client.get(url, params=params, headers=headers)
                if resp.status_code == 200:
                    body = resp.json()
                    return body.get("data", []) or []
                logger.warning(f"[CatalogClient] Failed to fetch showtimes for movie {movie_id}. HTTP {resp.status_code}")
                return []
        except Exception as ex:
            logger.error(f"[CatalogClient] Error fetching showtimes for movie {movie_id}: {ex}")
            return []

    def fetch_cinemas(self) -> List[Dict[str, Any]]:
        url = f"{self.base_url}/api/v1/cinemas"
        headers = self._get_headers()

        try:
            with httpx.Client(timeout=self.timeout) as client:
                resp = client.get(url, headers=headers)
                if resp.status_code == 200:
                    body = resp.json()
                    data = body.get("data", [])
                    return data if isinstance(data, list) else data.get("content", [])
                logger.warning(f"[CatalogClient] Failed to fetch cinemas. HTTP {resp.status_code}")
                return []
        except Exception as ex:
            logger.error(f"[CatalogClient] Error fetching cinemas: {ex}")
            return []


_catalog_client_instance: Optional[ICatalogClient] = None


def get_catalog_client() -> ICatalogClient:
    """Singleton factory provider for ICatalogClient."""
    global _catalog_client_instance
    if _catalog_client_instance is None:
        _catalog_client_instance = CatalogClientImpl()
    return _catalog_client_instance
