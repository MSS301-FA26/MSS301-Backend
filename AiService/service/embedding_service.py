import pickle
import os
from sentence_transformers import SentenceTransformer
import numpy as np

MODEL_NAME = "all-MiniLM-L6-v2"
EMBEDDINGS_PATH = os.path.join(os.path.dirname(__file__), "../model/movie_embeddings.pkl")

_instance = None


class EmbeddingService:
    def __init__(self):
        self.model = SentenceTransformer(MODEL_NAME)
        self._embeddings: dict = {}
        self._load()

    def _load(self):
        if os.path.exists(EMBEDDINGS_PATH):
            with open(EMBEDDINGS_PATH, "rb") as f:
                self._embeddings = pickle.load(f)

    def save(self, data: dict):
        os.makedirs(os.path.dirname(EMBEDDINGS_PATH), exist_ok=True)
        with open(EMBEDDINGS_PATH, "wb") as f:
            pickle.dump(data, f)
        self._embeddings = data

    def encode(self, text: str) -> np.ndarray:
        return self.model.encode(text, convert_to_numpy=True)

    def get_embeddings(self) -> dict:
        return self._embeddings


def get_embedding_service() -> EmbeddingService:
    global _instance
    if _instance is None:
        _instance = EmbeddingService()
    return _instance
