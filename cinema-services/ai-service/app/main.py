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

# Configure root logger with correlation_id
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter("[%(correlation_id)s] %(asctime)s [%(levelname)s] %(name)s: %(message)s"))
handler.addFilter(CorrelationIdLogFilter())

root_logger = logging.getLogger()
root_logger.setLevel(logging.INFO)
root_logger.handlers = [handler]
logger = logging.getLogger("ai_service")


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

# Global Exception Handlers
register_exception_handlers(app)

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


if __name__ == "__main__":
    import uvicorn
    settings = get_settings()
    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.SERVER_PORT, reload=False)
