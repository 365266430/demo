from fastapi import HTTPException

from app.core.text_splitter import text_splitter
from app.core.vector_store import vector_store
from app.schemas import IngestRequest, IngestResponse


class IngestService:
    def ingest(self, request: IngestRequest) -> IngestResponse:
        if not request.content or not request.content.strip():
            raise HTTPException(status_code=400, detail="content must not be empty")

        chunks = text_splitter.split(request.content)
        chunk_count = vector_store.upsert_document(
            document_id=request.document_id,
            filename=request.filename,
            chunks=chunks,
        )
        return IngestResponse(documentId=request.document_id, chunkCount=chunk_count)


ingest_service = IngestService()

