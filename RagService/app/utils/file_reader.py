from io import BytesIO
from pathlib import Path

from fastapi import HTTPException, UploadFile


async def read_upload_text(file: UploadFile) -> str:
    filename = file.filename or ""
    suffix = Path(filename).suffix.lower()
    content = await file.read()

    if not content:
        raise HTTPException(status_code=400, detail="uploaded file must not be empty")

    if suffix in {"", ".txt", ".md", ".csv"}:
        return _decode_text(content)
    if suffix == ".docx":
        return _read_docx(content)
    if suffix == ".pdf":
        return _read_pdf(content)

    raise HTTPException(
        status_code=400,
        detail=f"unsupported file type: {suffix or 'unknown'}",
    )


def _decode_text(content: bytes) -> str:
    for encoding in ("utf-8-sig", "utf-8", "gb18030"):
        try:
            return content.decode(encoding)
        except UnicodeDecodeError:
            continue
    raise HTTPException(status_code=400, detail="uploaded text file encoding is not supported")


def _read_docx(content: bytes) -> str:
    from docx import Document

    document = Document(BytesIO(content))
    paragraphs = [paragraph.text.strip() for paragraph in document.paragraphs]
    return "\n".join(paragraph for paragraph in paragraphs if paragraph)


def _read_pdf(content: bytes) -> str:
    from pypdf import PdfReader

    reader = PdfReader(BytesIO(content))
    pages = [page.extract_text() or "" for page in reader.pages]
    return "\n".join(page.strip() for page in pages if page.strip())
