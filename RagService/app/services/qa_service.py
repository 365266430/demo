from fastapi import HTTPException

from app.core.generator import generator
from app.core.vector_store import vector_store
from app.schemas import AskRequest, AskResponse


class QaService:
    def ask(self, request: AskRequest) -> AskResponse:
        if not request.question or not request.question.strip():
            raise HTTPException(status_code=400, detail="question must not be empty")

        sources = vector_store.search(
            document_id=request.document_id,
            question=request.question,
            top_k=request.top_k,
        )
        answer = generator.answer(request.question, sources)
        return AskResponse(
            documentId=request.document_id,
            question=request.question,
            answer=answer,
            sources=sources,
        )


qa_service = QaService()

