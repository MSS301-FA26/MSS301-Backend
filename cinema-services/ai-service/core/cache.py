import fnmatch
import logging
import threading
import time
from typing import Any, Dict, Optional, Tuple

logger = logging.getLogger(__name__)


class TTLMemoryCache:
    """Thread-safe in-memory cache supporting TTL expiration and wildcard key invalidation."""

    def __init__(self):
        self._cache: Dict[str, Tuple[Any, float]] = {}
        self._lock = threading.Lock()

    def get(self, key: str) -> Optional[Any]:
        """Retrieve cached value if present and not expired."""
        with self._lock:
            entry = self._cache.get(key)
            if entry is None:
                return None
            value, expires_at = entry
            if time.time() > expires_at:
                del self._cache[key]
                return None
            return value

    def set(self, key: str, value: Any, ttl_seconds: int = 900) -> None:
        """Store value with specified time-to-live in seconds."""
        expires_at = time.time() + ttl_seconds
        with self._lock:
            self._cache[key] = (value, expires_at)

    def delete(self, key: str) -> bool:
        """Remove a single key from cache."""
        with self._lock:
            if key in self._cache:
                del self._cache[key]
                return True
            return False

    def delete_pattern(self, pattern: str) -> int:
        """Remove all keys matching pattern (e.g. 'user:10:rec:*')."""
        removed_count = 0
        with self._lock:
            matching_keys = [k for k in self._cache if fnmatch.fnmatch(k, pattern)]
            for k in matching_keys:
                del self._cache[k]
                removed_count += 1
        return removed_count

    def clear(self) -> None:
        """Clear all cached entries."""
        with self._lock:
            self._cache.clear()


# Global cache instance
_cache_instance: Optional[TTLMemoryCache] = None


def get_cache() -> TTLMemoryCache:
    """Retrieve singleton TTL memory cache instance."""
    global _cache_instance
    if _cache_instance is None:
        _cache_instance = TTLMemoryCache()
    return _cache_instance
