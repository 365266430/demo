from fastapi import APIRouter

from app.schemas import AskRequest, AskResponse, IngestRequest, IngestResponse
from app.services.ingest_service import ingest_service
from app.services.qa_service import qa_service

router = APIRouter()


@router.post("/ingest", response_model=IngestResponse)
def ingest(request: IngestRequest) -> IngestResponse:
    return ingest_service.ingest(request)


@router.post("/ask", response_model=AskResponse)
def ask(request: AskRequest) -> AskResponse:
    return qa_service.ask(request)

