from typing import Protocol
from dtos.search_dtos import AdaptiveSearchResponse


class ISearchService(Protocol):
    """Interface defining operations for the Adaptive Search Subsystem."""

    def search(self, query: str, limit: int = 10) -> AdaptiveSearchResponse:
        """Perform adaptive multi-tier movie search with soft utility routing."""
        ...
