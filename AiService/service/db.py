import psycopg2
import psycopg2.extras
import os
from dotenv import load_dotenv

load_dotenv()


def get_connection():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", 5432)),
        dbname=os.getenv("DB_NAME", "cinema_ai"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", ""),
    )


def read_lob(conn, oid) -> str:
    """movies.description được Hibernate lưu dạng Postgres 'oid' (Large Object)
    thay vì text thường (do @Lob String không chỉ định JDBC type). Phải dereference
    qua Large Object API, đọc trực tiếp cột sẽ chỉ ra số OID, không phải nội dung."""
    if not oid:
        return ""
    try:
        lo = conn.lobject(int(oid), mode="rb")
        content = lo.read()
        lo.close()
        return content.decode("utf-8", errors="replace")
    except Exception:
        return ""
