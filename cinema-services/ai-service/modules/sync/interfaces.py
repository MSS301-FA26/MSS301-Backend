import abc
from typing import Dict, Any


class ICatalogSyncService(abc.ABC):
    """Interface defining catalog data synchronization from Catalog Service to AI pgvector."""

    @abc.abstractmethod
    def sync_catalog(self) -> Dict[str, Any]:
        """
        Pulls all movies from catalog-service via REST,
        computes dense vector embeddings, and upserts into local movie_embeddings table.
        """
        pass

    @abc.abstractmethod
    def sync_single_movie(self, movie_id: int) -> bool:
        """
        Pulls a single movie from catalog-service by ID and updates its local embedding.
        """
        pass

    @abc.abstractmethod
    def get_embedding_count(self) -> int:
        """
        Returns the total number of embedded movies in local movie_embeddings table.
        """
        pass
