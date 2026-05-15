# RAG Service

Python FastAPI service for legal document RAG. The Spring Boot app sends parsed document text to this service through HTTP.

## Run

```bash
cd RagService
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## APIs

```text
GET  /health
POST /api/rag/ingest
POST /api/rag/ask
```

