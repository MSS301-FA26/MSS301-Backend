from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from functools import lru_cache
from typing import Optional


class Settings(BaseSettings):
    """
    Application settings configuration.
    Loads and validates required infrastructure and security environment variables.
    """
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    # Server Configuration
    SERVER_PORT: int = Field(..., description="Server port")

    # Security (Zero-Trust tokens)
    INTERNAL_GATEWAY_SECRET: str = Field(
        ...,
        description="Secret key injected by API Gateway in X-Gateway-Secret header"
    )
    INTERNAL_SERVICE_SECRET: str = Field(
        ...,
        description="Secret key for service-to-service calls in X-Internal-Service-Secret header"
    )

    # PostgreSQL Database
    DB_HOST: str = Field(..., description="Postgres host")
    DB_PORT: int = Field(..., description="Postgres port")
    DB_NAME: str = Field(..., description="Postgres database name")
    DB_USER: str = Field(..., description="Postgres user")
    DB_PASSWORD: str = Field(..., description="Postgres password")

    # RabbitMQ Event Broker
    RABBITMQ_HOST: str = Field(..., description="RabbitMQ host")
    RABBITMQ_PORT: int = Field(..., description="RabbitMQ port")
    RABBITMQ_USER: str = Field(..., description="RabbitMQ username")
    RABBITMQ_PASS: str = Field(..., description="RabbitMQ password")

    # OpenAI-Compatible LLM for Chatbot
    OPENAI_API_KEY: Optional[str] = Field(default="", description="OpenAI API key")
    OPENAI_BASE_URL: str = Field(default="https://api.openai.com/v1", description="OpenAI Base URL")
    OPENAI_MODEL: str = Field(default="gpt-4o-mini", description="Model name for query rewriting and chat")


@lru_cache()
def get_settings() -> Settings:
    return Settings()
