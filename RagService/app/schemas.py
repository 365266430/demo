from pydantic import BaseModel, Field


class IngestRequest(BaseModel):
    document_id: str = Field(alias="documentId")
    filename: str | None = None
    content: str

    model_config = {"populate_by_name": True}


class IngestResponse(BaseModel):
    document_id: str = Field(alias="documentId")
    chunk_count: int = Field(alias="chunkCount")

    model_config = {"populate_by_name": True}


class AskRequest(BaseModel):
    document_id: str = Field(alias="documentId")
    question: str
    top_k: int = Field(default=5, alias="topK")

    model_config = {"populate_by_name": True}


class SourceChunk(BaseModel):
    document_id: str = Field(alias="documentId")
    chunk_id: str = Field(alias="chunkId")
    chunk_index: int = Field(alias="chunkIndex")
    content: str
    score: float | None = None

    model_config = {"populate_by_name": True}


class AskResponse(BaseModel):
    document_id: str = Field(alias="documentId")
    question: str
    answer: str
    sources: list[SourceChunk]

    model_config = {"populate_by_name": True}

