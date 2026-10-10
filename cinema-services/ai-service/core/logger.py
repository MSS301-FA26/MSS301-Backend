import os
import logging
from logging.handlers import RotatingFileHandler
from core.middlewares.correlation_middleware import CorrelationIdLogFilter

LOG_FORMAT = "[%(correlation_id)s] %(asctime)s [%(levelname)s] %(name)s: %(message)s"


def setup_logging(
    log_dir: str = "logs",
    log_filename: str = "ai-service.log",
    log_level: int = logging.INFO,
    max_bytes: int = 10 * 1024 * 1024,
    backup_count: int = 5,
) -> logging.Logger:
    """
    Configures application logging with both console and rotating file output.
    Ensures Correlation ID is attached to all log records for distributed tracing.
    """
    os_log_dir = os.getenv("LOG_DIR", log_dir)
    os.makedirs(os_log_dir, exist_ok=True)
    log_file_path = os.getenv("LOG_FILE", os.path.join(os_log_dir, log_filename))

    formatter = logging.Formatter(LOG_FORMAT)
    correlation_filter = CorrelationIdLogFilter()

    # Console Handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    console_handler.addFilter(correlation_filter)

    # Rotating File Handler
    file_handler = RotatingFileHandler(
        log_file_path,
        maxBytes=max_bytes,
        backupCount=backup_count,
        encoding="utf-8"
    )
    file_handler.setFormatter(formatter)
    file_handler.addFilter(correlation_filter)

    # Root Logger Configuration
    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)
    root_logger.handlers = [console_handler, file_handler]

    # Forward uvicorn loggers to file and console as well
    for uvicorn_logger_name in ("uvicorn", "uvicorn.error", "uvicorn.access"):
        u_logger = logging.getLogger(uvicorn_logger_name)
        u_logger.handlers = [console_handler, file_handler]
        u_logger.addFilter(correlation_filter)

    logger = logging.getLogger("ai_service")
    logger.info(f"Logging initialized. Output file: {log_file_path}")
    return logger
