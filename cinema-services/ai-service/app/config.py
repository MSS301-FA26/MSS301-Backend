from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from functools import lru_cache
from typing import Optional


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    # Server Configuration
    SERVER_PORT: int = Field(default=8000, description="Server port")

    # Security
    INTERNAL_GATEWAY_SECRET: str = Field(
        default="cinema-gateway-secret-key-change-in-production",
        description="Secret key injected by API Gateway in X-Gateway-Secret header"
    )
    INTERNAL_SERVICE_SECRET: str = Field(
        default="cinema-internal-service-secret-key-change-in-production",
        description="Secret key for service-to-service calls in X-Internal-Service-Secret header"
    )

    # PostgreSQL Database
    DB_HOST: str = Field(default="localhost", description="Postgres host")
    DB_PORT: int = Field(default=5435, description="Postgres port")
    DB_NAME: str = Field(default="recommendation_db", description="Postgres database name")
    DB_USER: str = Field(default="rec_user", description="Postgres user")
    DB_PASSWORD: str = Field(default="rec_pass_123", description="Postgres password")

    # RabbitMQ Event Broker
    RABBITMQ_HOST: str = Field(default="localhost", description="RabbitMQ host")
    RABBITMQ_PORT: int = Field(default=5672, description="RabbitMQ port")
    RABBITMQ_USER: str = Field(default="guest", description="RabbitMQ username")
    RABBITMQ_PASS: str = Field(default="guest", description="RabbitMQ password")

    # OpenAI-Compatible LLM for Chatbot
    OPENAI_API_KEY: Optional[str] = Field(default="", description="OpenAI API key")
    OPENAI_BASE_URL: str = Field(default="https://api.openai.com/v1", description="OpenAI Base URL")
    OPENAI_MODEL: str = Field(default="gpt-4o-mini", description="Model name for query rewriting and chat")


@lru_cache()
def get_settings() -> Settings:
    return Settings()
