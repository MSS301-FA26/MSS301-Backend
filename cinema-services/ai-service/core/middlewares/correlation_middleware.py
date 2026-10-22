import uuid
import time
import logging
from contextvars import ContextVar
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

# ContextVar storing correlation_id throughout the async context of a request
correlation_id_ctx: ContextVar[str] = ContextVar("correlation_id", default="system")
access_logger = logging.getLogger("ai_service.access")


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
    Logs HTTP request and response status in real-time.
    """
    HEADER_NAME = "X-Correlation-Id"

    async def dispatch(self, request: Request, call_next):
        correlation_id = request.headers.get(self.HEADER_NAME)
        if not correlation_id:
            correlation_id = str(uuid.uuid4())

        token = correlation_id_ctx.set(correlation_id)
        start_time = time.time()
        is_health = request.url.path == "/health"
        try:
            response = await call_next(request)
            duration_ms = (time.time() - start_time) * 1000
            if not is_health:
                access_logger.info(f"{request.method} {request.url.path} -> {response.status_code} ({duration_ms:.1f}ms)")
            response.headers[self.HEADER_NAME] = correlation_id
            return response
        except Exception as e:
            duration_ms = (time.time() - start_time) * 1000
            access_logger.error(f"{request.method} {request.url.path} -> ERROR: {e} ({duration_ms:.1f}ms)")
            raise
        finally:
            correlation_id_ctx.reset(token)
