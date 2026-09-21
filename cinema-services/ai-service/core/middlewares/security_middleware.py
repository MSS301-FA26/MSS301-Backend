import hmac
import logging
from datetime import datetime, timezone
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse
from app.config import get_settings
from dtos.common import ErrorResponse

logger = logging.getLogger("security.audit")

WHITELIST_PATHS = {
    "/health",
    "/docs",
    "/redoc",
    "/openapi.json"
}


class GatewaySecretMiddleware(BaseHTTPMiddleware):
    """
    Zero-Trust Security & Direct Access Prevention Middleware.
    Validates that incoming requests originate from a trusted Gateway or internal service:
      - /api/v1/**: Requires valid X-Gateway-Secret header.
      - /internal/**: Requires valid X-Internal-Service-Secret header.
    Uses hmac.compare_digest to prevent timing attacks.
    """

    async def dispatch(self, request: Request, call_next):
        path = request.url.path

        # Whitelist public health check and docs endpoints
        if path in WHITELIST_PATHS:
            return await call_next(request)

        settings = get_settings()
        is_internal_path = path.startswith("/internal/")
        header_name = "X-Internal-Service-Secret" if is_internal_path else "X-Gateway-Secret"
        expected_secret = (
            settings.INTERNAL_SERVICE_SECRET if is_internal_path
            else settings.INTERNAL_GATEWAY_SECRET
        )

        supplied_secret = request.headers.get(header_name)

        # Constant-time comparison against timing attacks
        is_valid = False
        if supplied_secret and expected_secret:
            is_valid = hmac.compare_digest(
                supplied_secret.encode("utf-8"),
                expected_secret.encode("utf-8")
            )

        if not is_valid:
            client_ip = request.client.host if request.client else "unknown"
            logger.warning(
                f"Security Warning: Unauthorized direct access attempt to [{path}] from IP [{client_ip}] - Missing or invalid {header_name}"
            )

            error_payload = ErrorResponse(
                success=False,
                message="Trusted service access required",
                path=path,
                errors=[],
                timestamp=datetime.now(timezone.utc).isoformat()
            )
            return JSONResponse(
                status_code=403,
                content=error_payload.model_dump()
            )

        return await call_next(request)
