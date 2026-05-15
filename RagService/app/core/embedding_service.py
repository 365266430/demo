from functools import lru_cache

from sentence_transformers import SentenceTransformer

from app.config import settings


class EmbeddingService:
    @lru_cache(maxsize=1)
    def _model(self) -> SentenceTransformer:
        return SentenceTransformer(settings.embedding_model)

    def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        vectors = self._model().encode(texts, normalize_embeddings=True)
        return vectors.tolist()


embedding_service = EmbeddingService()

