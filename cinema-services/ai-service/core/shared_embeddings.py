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
            import os
            from sentence_transformers import SentenceTransformer

            # Priority 1: Check pre-downloaded local models directory
            base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            local_model_path = os.path.join(base_dir, "models", "all-MiniLM-L6-v2")

            if os.path.isdir(local_model_path):
                logger.info(f"Loading local SentenceTransformer model from '{local_model_path}' (100% offline)...")
                self.model = SentenceTransformer(local_model_path, local_files_only=True)
            else:
                logger.info(f"Loading cached SentenceTransformer model '{self.model_name}'...")
                try:
                    self.model = SentenceTransformer(self.model_name, local_files_only=True)
                except Exception:
                    self.model = SentenceTransformer(self.model_name)
            logger.info("SentenceTransformer model loaded successfully into RAM.")
        except Exception as e:
            logger.error(f"Failed to load SentenceTransformer model: {e}")
            raise RuntimeError(f"Failed to load SentenceTransformer model: {e}") from e

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
