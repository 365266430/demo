from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    rag_service_host: str = "0.0.0.0"
    rag_service_port: int = 8000
    vector_store_path: str = "./data/vector_store"
    embedding_model: str = "BAAI/bge-small-zh-v1.5"
    llm_api_key: str | None = None
    llm_base_url: str = "https://api.deepseek.com"
    llm_model: str = "deepseek-chat"
    llm_temperature: float = 0.2

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()

