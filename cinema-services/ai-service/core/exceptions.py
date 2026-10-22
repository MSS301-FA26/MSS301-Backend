from fastapi import Request, FastAPI
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from dtos.common import ErrorResponse
from datetime import datetime, timezone
import logging

logger = logging.getLogger(__name__)


class CinemaApiException(Exception):
    def __init__(self, message: str, status_code: int = 400, errors: list = None):
        super().__init__(message)
        self.message = message
        self.status_code = status_code
        self.errors = errors or []


class BadRequestException(CinemaApiException):
    def __init__(self, message: str = "Invalid request", errors: list = None):
        super().__init__(message=message, status_code=400, errors=errors)


class UnauthorizedException(CinemaApiException):
    def __init__(self, message: str = "Unauthorized", errors: list = None):
        super().__init__(message=message, status_code=401, errors=errors)


class ForbiddenException(CinemaApiException):
    def __init__(self, message: str = "Trusted service access required", errors: list = None):
        super().__init__(message=message, status_code=403, errors=errors)


class NotFoundException(CinemaApiException):
    def __init__(self, message: str = "Resource not found", errors: list = None):
        super().__init__(message=message, status_code=404, errors=errors)


class ConflictException(CinemaApiException):
    def __init__(self, message: str = "Data conflict", errors: list = None):
        super().__init__(message=message, status_code=409, errors=errors)


def register_exception_handlers(app: FastAPI):
    @app.exception_handler(CinemaApiException)
    async def cinema_api_exception_handler(request: Request, exc: CinemaApiException):
        error_body = ErrorResponse(
            success=False,
            message=exc.message,
            path=request.url.path,
            errors=exc.errors,
            timestamp=datetime.now(timezone.utc).isoformat()
        )
        return JSONResponse(status_code=exc.status_code, content=error_body.model_dump())

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        errors = [f"{err['loc'][-1]}: {err['msg']}" for err in exc.errors()]
        error_body = ErrorResponse(
            success=False,
            message="Validation error",
            path=request.url.path,
            errors=errors,
            timestamp=datetime.now(timezone.utc).isoformat()
        )
        return JSONResponse(status_code=400, content=error_body.model_dump())

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        logger.error(f"Unhandled server error at [{request.url.path}]: {str(exc)}", exc_info=True)
        error_body = ErrorResponse(
            success=False,
            message="Internal server error",
            path=request.url.path,
            errors=[],
            timestamp=datetime.now(timezone.utc).isoformat()
        )
        return JSONResponse(status_code=500, content=error_body.model_dump())
