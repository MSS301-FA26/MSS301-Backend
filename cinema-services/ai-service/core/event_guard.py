import logging
from typing import Optional

logger = logging.getLogger(__name__)


def is_event_processed(cursor, event_id: str) -> bool:
    """
    Check if event_id has already been processed and recorded in processed_events.
    """
    if not event_id:
        return False
    cursor.execute("SELECT 1 FROM processed_events WHERE event_id = %s;", (str(event_id),))
    return cursor.fetchone() is not None


def mark_event_processed(
    cursor,
    event_id: str,
    event_type: str,
    source: str = "RABBITMQ",
    payload_hash: Optional[str] = None
):
    """
    Record event_id in processed_events table to enforce message idempotency.
    """
    if not event_id:
        return
    cursor.execute("""
        INSERT INTO processed_events (event_id, event_type, source_service, payload_hash, processed_at)
        VALUES (%s, %s, %s, %s, CURRENT_TIMESTAMP)
        ON CONFLICT (event_id) DO NOTHING;
    """, (str(event_id), event_type, source, payload_hash))
