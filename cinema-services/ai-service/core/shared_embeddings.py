import logging
import numpy as np
from typing import List

logger = logging.getLogger(__name__)

_model_instance = None


class SharedEmbeddingService:
    """
    Singleton service managing the in-memory Sentence-Transformers model instance.
    Avoids duplicate model loading into RAM across search, recommendation, and chatbot subsystems.
    """
    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        self.model_name = model_name
        self.dim = 384
        self.model = None
        self._load_model()

    def _load_model(self):
        try:
            from sentence_transformers import SentenceTransformer
            logger.info(f"Loading SentenceTransformer model '{self.model_name}' into RAM...")
            self.model = SentenceTransformer(self.model_name)
            logger.info("SentenceTransformer model loaded successfully.")
        except Exception as e:
            logger.error(f"Failed to load SentenceTransformer model '{self.model_name}': {e}")
            raise RuntimeError(f"Failed to load SentenceTransformer model '{self.model_name}': {e}") from e

    def encode(self, text: str) -> List[float]:
        if not text:
            return [0.0] * self.dim
        if self.model is None:
            raise RuntimeError("SentenceTransformer model is not initialized.")
        try:
            vector = self.model.encode(text, convert_to_numpy=True)
            norm = np.linalg.norm(vector)
            if norm > 0:
                vector = vector / norm
            return vector.tolist()
        except Exception as e:
            logger.error(f"Error encoding text via model: {e}")
            raise RuntimeError(f"Error encoding text via SentenceTransformer: {e}") from e


def get_embedding_service() -> SharedEmbeddingService:
    global _model_instance
    if _model_instance is None:
        _model_instance = SharedEmbeddingService()
    return _model_instance
