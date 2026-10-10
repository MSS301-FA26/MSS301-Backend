import pytest
from unittest.mock import MagicMock, patch
from modules.sync.service_impl import CatalogSyncServiceImpl
from core.clients.catalog_client import ICatalogClient


@patch("modules.sync.service_impl.get_db_connection")
@patch("modules.sync.service_impl.get_cache")
def test_catalog_sync_service_process(mock_get_cache, mock_get_db):
    mock_catalog = MagicMock(spec=ICatalogClient)
    mock_catalog.fetch_all_movies.return_value = [
        {
            "id": 1,
            "title": "Inception",
            "description": "Mind-bending thriller",
            "director": "Christopher Nolan",
            "genres": [{"name": "Sci-Fi"}, {"name": "Action"}],
            "actors": [{"name": "Leonardo DiCaprio"}],
            "releaseDate": "2010-07-16",
            "status": "NOW_SHOWING"
        }
    ]

    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
    mock_cursor.fetchone.return_value = [1]
    mock_get_db.return_value.__enter__.return_value = mock_conn

    sync_service = CatalogSyncServiceImpl(catalog_client=mock_catalog)
    # Mock embedding encode to avoid heavy model execution during quick test
    sync_service.embedding_svc = MagicMock()
    sync_service.embedding_svc.encode.return_value = [0.1] * 384

    res = sync_service.sync_catalog()

    assert res["status"] == "SUCCESS"
    assert res["syncedCount"] == 1
    assert mock_cursor.execute.call_count >= 1
