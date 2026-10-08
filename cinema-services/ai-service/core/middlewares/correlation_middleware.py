import uuid
import logging
from contextvars import ContextVar
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

# ContextVar storing correlation_id throughout the async context of a request
correlation_id_ctx: ContextVar[str] = ContextVar("correlation_id", default="system")


def get_correlation_id() -> str:
    return correlation_id_ctx.get()


class CorrelationIdLogFilter(logging.Filter):
    """Logging filter attaching [correlation_id] to all log records for distributed tracing."""
    def filter(self, record):
        record.correlation_id = get_correlation_id()
        return True


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    """
    Distributed Tracing Middleware.
    Extracts incoming X-Correlation-Id header from Gateway or generates a new UUIDv4.
    """
    HEADER_NAME = "X-Correlation-Id"

    async def dispatch(self, request: Request, call_next):
        correlation_id = request.headers.get(self.HEADER_NAME)
        if not correlation_id:
            correlation_id = str(uuid.uuid4())

        token = correlation_id_ctx.set(correlation_id)
        try:
            response = await call_next(request)
            response.headers[self.HEADER_NAME] = correlation_id
            return response
        finally:
            correlation_id_ctx.reset(token)
