from pathlib import Path

from fastapi import APIRouter, HTTPException, Request

from app.schemas import AskRequest, AskResponse, IngestRequest, IngestResponse
from app.services.ingest_service import ingest_service
from app.services.qa_service import qa_service
from app.utils.file_reader import read_upload_text

router = APIRouter()


@router.post("/ingest", response_model=IngestResponse)
async def ingest(request: Request) -> IngestResponse:
    content_type = request.headers.get("content-type", "")
    if content_type.startswith("multipart/form-data"):
        return await _ingest_upload(request)

    payload = await request.json()
    return ingest_service.ingest(IngestRequest.model_validate(payload))


@router.post("/ask", response_model=AskResponse)
def ask(request: AskRequest) -> AskResponse:
    return qa_service.ask(request)


async def _ingest_upload(request: Request) -> IngestResponse:
    form = await request.form()
    file = form.get("file")
    if not file or not hasattr(file, "filename") or not hasattr(file, "read"):
        raise HTTPException(status_code=400, detail="file is required")

    content = await read_upload_text(file)
    document_id = str(form.get("documentId") or form.get("document_id") or "").strip()
    if not document_id:
        document_id = Path(file.filename or "uploaded-document").stem

    ingest_request = IngestRequest(
        documentId=document_id,
        filename=file.filename,
        content=content,
    )
    return ingest_service.ingest(ingest_request)
