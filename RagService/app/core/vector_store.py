from functools import lru_cache
import os
from pathlib import Path

os.environ.setdefault("ANONYMIZED_TELEMETRY", "False")
os.environ.setdefault("CHROMA_PRODUCT_TELEMETRY_IMPL", "app.utils.chroma_telemetry.NoOpProductTelemetryClient")
os.environ.setdefault("CHROMA_TELEMETRY_IMPL", "app.utils.chroma_telemetry.NoOpProductTelemetryClient")

from app.config import settings
from app.core.embedding_service import embedding_service
from app.schemas import SourceChunk


class VectorStore:
    def __init__(self) -> None:
        import chromadb
        from chromadb.config import Settings as ChromaSettings

        Path(settings.vector_store_path).mkdir(parents=True, exist_ok=True)
        self.client = chromadb.PersistentClient(
            path=settings.vector_store_path,
            settings=ChromaSettings(
                anonymized_telemetry=False,
                chroma_product_telemetry_impl="app.utils.chroma_telemetry.NoOpProductTelemetryClient",
                chroma_telemetry_impl="app.utils.chroma_telemetry.NoOpProductTelemetryClient",
            ),
        )
        self.collection = self.client.get_or_create_collection(name="legal_documents")

    def upsert_document(self, document_id: str, filename: str | None, chunks: list[str]) -> int:
        self.delete_document(document_id)
        if not chunks:
            return 0

        ids = [f"{document_id}:{index}" for index in range(len(chunks))]
        metadatas = [
            {
                "document_id": document_id,
                "filename": filename or "",
                "chunk_index": index,
            }
            for index in range(len(chunks))
        ]
        embeddings = embedding_service.embed(chunks)

        self.collection.upsert(
            ids=ids,
            documents=chunks,
            embeddings=embeddings,
            metadatas=metadatas,
        )
        return len(chunks)

    def search(self, document_id: str, question: str, top_k: int) -> list[SourceChunk]:
        query_embedding = embedding_service.embed([question])[0]
        result = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=max(top_k, 1),
            where={"document_id": document_id},
        )

        ids = result.get("ids", [[]])[0]
        documents = result.get("documents", [[]])[0]
        metadatas = result.get("metadatas", [[]])[0]
        distances = result.get("distances", [[]])[0]

        sources: list[SourceChunk] = []
        for chunk_id, content, metadata, distance in zip(ids, documents, metadatas, distances):
            sources.append(
                SourceChunk(
                    documentId=document_id,
                    chunkId=chunk_id,
                    chunkIndex=int(metadata.get("chunk_index", 0)),
                    content=content,
                    score=1 - float(distance) if distance is not None else None,
                )
            )
        return sources

    def delete_document(self, document_id: str) -> None:
        self.collection.delete(where={"document_id": document_id})


@lru_cache(maxsize=1)
def get_vector_store() -> VectorStore:
    return VectorStore()
