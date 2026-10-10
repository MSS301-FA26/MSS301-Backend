import pytest
from unittest.mock import MagicMock
from modules.chatbot.agent.tools import create_cinema_tools
from modules.search.interfaces import ISearchService
from modules.recommendation.interfaces import IRecommendationService
from core.clients.catalog_client import ICatalogClient


def test_cinema_tools_showtimes_and_cinemas():
    mock_search = MagicMock(spec=ISearchService)
    mock_rec = MagicMock(spec=IRecommendationService)
    mock_catalog = MagicMock(spec=ICatalogClient)

    mock_catalog.fetch_available_showtimes.return_value = [
        {"showtimeId": 1, "startTime": "2026-10-10T14:00:00", "roomName": "Room 1"}
    ]
    mock_catalog.fetch_cinemas.return_value = [
        {"id": 1, "name": "CinePremier Landmark 81", "address": "720A Dien Bien Phu"}
    ]

    tools = create_cinema_tools(mock_search, mock_rec, mock_catalog)
    tool_names = [t.name for t in tools]
    assert "search_movies" in tool_names
    assert "recommend_movies" in tool_names
    assert "get_available_showtimes" in tool_names
    assert "get_cinemas" in tool_names

    # Test get_available_showtimes execution
    showtimes_tool = next(t for t in tools if t.name == "get_available_showtimes")
    result = showtimes_tool(movie_id=99, date="2026-10-10")
    assert result["movie_id"] == 99
    assert len(result["showtimes"]) == 1
    assert "Tìm thấy 1 cụm suất chiếu khả dụng" in result["summary"]

    # Test get_cinemas execution
    cinemas_tool = next(t for t in tools if t.name == "get_cinemas")
    cinemas_res = cinemas_tool()
    assert len(cinemas_res["cinemas"]) == 1
    assert "CinePremier Landmark 81" in cinemas_res["summary"]
