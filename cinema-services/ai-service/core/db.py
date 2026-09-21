import os
import logging
from contextlib import contextmanager
import psycopg2
import psycopg2.extras
import psycopg2.pool
from app.config import get_settings

logger = logging.getLogger(__name__)

_connection_pool: psycopg2.pool.ThreadedConnectionPool = None


def init_db_pool():
    global _connection_pool
    if _connection_pool is None:
        settings = get_settings()
        logger.info(f"Initializing connection pool to PostgreSQL at {settings.DB_HOST}:{settings.DB_PORT}/{settings.DB_NAME}")
        try:
            _connection_pool = psycopg2.pool.ThreadedConnectionPool(
                minconn=2,
                maxconn=20,
                host=settings.DB_HOST,
                port=settings.DB_PORT,
                dbname=settings.DB_NAME,
                user=settings.DB_USER,
                password=settings.DB_PASSWORD,
            )
            run_migrations()
        except Exception as e:
            logger.error(f"Database connection error for {settings.DB_NAME}: {e}")
            raise e


def close_db_pool():
    global _connection_pool
    if _connection_pool is not None:
        _connection_pool.closeall()
        _connection_pool = None
        logger.info("Closed database connection pool.")


@contextmanager
def get_db_connection():
    global _connection_pool
    if _connection_pool is None:
        init_db_pool()
    conn = _connection_pool.getconn()
    try:
        yield conn
    finally:
        _connection_pool.putconn(conn)


def run_migrations():
    """Execute initial SQL schema migration if tables do not exist."""
    migration_file = os.path.join(
        os.path.dirname(__file__), "..", "db", "migration", "V1__init_ai_db.sql"
    )
    if os.path.exists(migration_file):
        with get_db_connection() as conn:
            with conn.cursor() as cursor:
                with open(migration_file, "r", encoding="utf-8") as f:
                    sql = f.read()
                    cursor.execute(sql)
            conn.commit()
            logger.info("Executed migration V1__init_ai_db.sql successfully.")
