import pytest
from unittest.mock import patch, MagicMock
from core.clients.catalog_client import CatalogClientImpl


def test_catalog_client_headers():
    client = CatalogClientImpl(base_url="http://mock-catalog:8082")
    headers = client._get_headers()
    assert "X-Internal-Service-Secret" in headers
    assert "X-Gateway-Secret" in headers
    assert "X-Correlation-Id" in headers
    assert headers["Accept"] == "application/json"


@patch("httpx.Client.get")
def test_catalog_client_fetch_all_movies(mock_get):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "success": True,
        "data": {
            "content": [
                {
                    "id": 101,
                    "title": "Dune: Part Two",
                    "description": "Epic Sci-fi adventure",
                    "director": "Denis Villeneuve",
                    "genres": [{"id": 1, "name": "Sci-Fi"}],
                    "actors": [{"id": 1, "name": "Timothee Chalamet"}],
                    "status": "NOW_SHOWING"
                }
            ],
            "totalPages": 1,
            "last": True
        }
    }
    mock_get.return_value = mock_response

    client = CatalogClientImpl(base_url="http://mock-catalog:8082")
    movies = client.fetch_all_movies()
    assert len(movies) == 1
    assert movies[0]["id"] == 101
    assert movies[0]["title"] == "Dune: Part Two"


@patch("httpx.Client.get")
def test_catalog_client_fetch_showtimes(mock_get):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "success": True,
        "data": [
            {
                "showtimeId": 501,
                "startTime": "2026-10-10T19:30:00",
                "roomName": "Screen 01 - IMAX"
            }
        ]
    }
    mock_get.return_value = mock_response

    client = CatalogClientImpl(base_url="http://mock-catalog:8082")
    slots = client.fetch_available_showtimes(movie_id=101, date="2026-10-10")
    assert len(slots) == 1
    assert slots[0]["showtimeId"] == 501
