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
    """Execute SQL schema migrations in alphabetical order if not already applied."""
    migrations_dir = os.path.join(os.path.dirname(__file__), "..", "db", "migration")
    if not os.path.exists(migrations_dir):
        return

    sql_files = sorted([f for f in os.listdir(migrations_dir) if f.endswith(".sql")])
    if not sql_files:
        return

    with get_db_connection() as conn:
        with conn.cursor() as cursor:
            # Create schema_migrations tracking table if not exists
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version VARCHAR(128) PRIMARY KEY,
                    applied_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
            """)
            cursor.execute("SELECT version FROM schema_migrations;")
            applied = {row[0] for row in cursor.fetchall()}

            for sql_file in sql_files:
                if sql_file in applied:
                    continue

                full_path = os.path.join(migrations_dir, sql_file)
                logger.info(f"Applying database migration: {sql_file}")
                with open(full_path, "r", encoding="utf-8") as f:
                    sql_content = f.read()
                    cursor.execute(sql_content)

                cursor.execute(
                    "INSERT INTO schema_migrations (version) VALUES (%s);",
                    (sql_file,)
                )
                logger.info(f"Successfully applied migration: {sql_file}")
        conn.commit()
