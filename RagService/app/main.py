from fastapi import FastAPI

from app.api.health_api import router as health_router
from app.api.rag_api import router as rag_router

app = FastAPI(title="Legal Document RAG Service")

app.include_router(health_router)
app.include_router(rag_router, prefix="/api/rag", tags=["rag"])

