import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from core.db import init_db_pool, close_db_pool
from core.rabbitmq import start_rabbitmq_consumer, stop_rabbitmq_consumer
from core.exceptions import register_exception_handlers
from core.middlewares.correlation_middleware import CorrelationIdMiddleware, CorrelationIdLogFilter
from core.middlewares.security_middleware import GatewaySecretMiddleware

from api.recommendation_api import router as recommendation_router
from api.search_api import router as search_router
from api.chat_api import router as chat_router
from api.telemetry_api import router as telemetry_router
from api.prompt_api import router as prompt_router
from api.sync_api import router as sync_router

from core.logger import setup_logging

# Configure logging (console + rotating file handler with correlation_id)
logger = setup_logging()


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting CinemaAI ai-service...")
    try:
        init_db_pool()
    except Exception as e:
        logger.warning(f"Database initialization deferred: {e}")

    try:
        start_rabbitmq_consumer()
    except Exception as e:
        logger.warning(f"RabbitMQ consumer initialization deferred: {e}")

    try:
        from core.shared_embeddings import get_embedding_service
        logger.info("Pre-warming SentenceTransformer embedding model into RAM...")
        get_embedding_service()
    except Exception as e:
        logger.warning(f"Embedding model pre-warm deferred: {e}")

    try:
        from modules.sync.service_impl import get_catalog_sync_service
        sync_svc = get_catalog_sync_service()
        current_count = sync_svc.get_embedding_count()
        logger.info(f"Checking local movie embeddings: {current_count} movies found in pgvector.")
        if current_count == 0:
            logger.info("Local movie embeddings table is empty. Triggering background catalog sync from Catalog Service...")
            import threading
            threading.Thread(target=sync_svc.sync_catalog, daemon=True).start()
    except Exception as e:
        logger.warning(f"Auto catalog sync check deferred: {e}")

    yield

    logger.info("Stopping CinemaAI ai-service...")
    stop_rabbitmq_consumer()
    close_db_pool()


app = FastAPI(
    title="CinemaAI ai-service",
    description="CinemaAI Intelligent Services: Adaptive Search, Recommendations, and Conversational Assistant",
    version="1.0.0",
    lifespan=lifespan
)

from fastapi.openapi.utils import get_openapi

# Global Exception Handlers
register_exception_handlers(app)

def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    openapi_schema = get_openapi(
        title=app.title,
        version=app.version,
        description=app.description,
        routes=app.routes,
    )
    openapi_schema["components"]["securitySchemes"] = {
        "GatewaySecret": {
            "type": "apiKey",
            "in": "header",
            "name": "X-Gateway-Secret",
            "description": "Shared Gateway Secret Token (Default: cinema-gateway-secret-key-change-in-production)"
        },
        "InternalServiceSecret": {
            "type": "apiKey",
            "in": "header",
            "name": "X-Internal-Service-Secret",
            "description": "Internal Microservice Secret Token (Default: cinema-internal-service-secret-key-change-in-production)"
        }
    }
    openapi_schema["security"] = [{"GatewaySecret": []}]
    app.openapi_schema = openapi_schema
    return app.openapi_schema

app.openapi = custom_openapi

# Middlewares
app.add_middleware(CorrelationIdMiddleware)
app.add_middleware(GatewaySecretMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Public healthcheck endpoint
@app.get("/health", tags=["Health"])
def health_check():
    return {"status": "UP", "service": "ai-service"}


# API Routers
app.include_router(recommendation_router)
app.include_router(search_router)
app.include_router(chat_router)
app.include_router(telemetry_router)
app.include_router(prompt_router)
app.include_router(sync_router)


if __name__ == "__main__":
    import uvicorn
    settings = get_settings()
    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.SERVER_PORT, reload=False)
