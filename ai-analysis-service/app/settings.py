from typing import Literal

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

LlmBackend = Literal["heuristic", "ollama", "groq", "openai"]


def _openai_compatible_base(url: str) -> str:
    """AsyncOpenAI expects base URL ending in /v1 (Ollama, Groq, OpenAI-compatible)."""
    base = url.strip().rstrip("/")
    return base if base.endswith("/v1") else f"{base}/v1"


class AiSettings(BaseSettings):
    """Runtime knobs for evidence gathering + LLM (Groq free tier or local Ollama)."""

    model_config = SettingsConfigDict(env_prefix="", env_file=".env", env_file_encoding="utf-8", extra="ignore")

    ingestion_base_url: str = "http://localhost:8090"
    elasticsearch_url: str = "http://localhost:9200"
    elasticsearch_index: str = "gateway-events-active"

    # Compose stack defaults LLM_BACKEND=groq (+ GROQ_API_KEY in .env). Local dev defaults to Ollama on localhost.
    llm_backend: LlmBackend = Field(default="ollama", description="heuristic | ollama | groq | openai")

    ollama_openai_base_url: str = Field(default="http://localhost:11434/v1")
    ollama_model: str = Field(default="llama3.2:1b")

    groq_openai_base_url: str = Field(default="https://api.groq.com/openai/v1")
    groq_api_key: str | None = None
    groq_model: str = Field(default="llama-3.1-8b-instant")

    openai_api_key: str | None = None
    openai_model: str = Field(default="gpt-4o-mini")

    def llm_http_config(self) -> tuple[str, str, str] | None:
        """Returns (base_url, api_key, model) for AsyncOpenAI, or None to use heuristic only."""
        b = self.llm_backend
        if b == "heuristic":
            return None
        if b == "ollama":
            return (_openai_compatible_base(self.ollama_openai_base_url), "ollama", self.ollama_model)
        if b == "groq":
            key = (self.groq_api_key or "").strip()
            if not key:
                return None
            return (_openai_compatible_base(self.groq_openai_base_url), key, self.groq_model)
        if b == "openai":
            key = (self.openai_api_key or "").strip()
            if not key:
                return None
            return ("https://api.openai.com/v1", key, self.openai_model)
        return None


settings = AiSettings()
