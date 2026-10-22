import pytest
from modules.chatbot.context_resolver import ContextResolver
from modules.chatbot.dispatcher import SubsystemDispatcher
from modules.search.interfaces import ISearchService
from modules.recommendation.interfaces import IRecommendationService
from dtos.search_dtos import AdaptiveSearchResponse
from dtos.recommendation_dtos import RecommendationResponse


class MockSearchService:
    def search(self, query: str, limit: int = 10):
        return AdaptiveSearchResponse(
            query=query,
            routeUsed="R0_FIRST_STAGE",
            routingEntropy=0.1,
            wasFallback=False,
            latencyMs=5.0,
            results=[]
        )


class MockRecommendationService:
    def get_user_recommendations(self, user_id: int, limit: int = 10):
        return RecommendationResponse(
            userId=user_id,
            strategy="MOCK",
            recommendations=[]
        )

    def get_content_recommendations(self, movie_id: int, limit: int = 6):
        return None


def test_context_resolver_independent_query():
    resolver = ContextResolver()
    history = [{"role": "user", "content": "Chào bạn"}, {"role": "assistant", "content": "Xin chào!"}]

    # Standalone query -> c_t = 0 (False)
    assert resolver.is_context_dependent("Phim Interstellar chiếu mấy giờ?", history) is False
    assert resolver.is_context_dependent("Tìm phim hoạt hình Doraemon", history) is False


def test_context_resolver_dependent_query():
    resolver = ContextResolver()
    history = [{"role": "user", "content": "Cho tôi phim của Nolan"}, {"role": "assistant", "content": "Có Interstellar, Inception."}]

    # Context-dependent follow-up query -> c_t = 1 (True)
    assert resolver.is_context_dependent("Thế còn bộ thứ hai?", history) is True
    assert resolver.is_context_dependent("Trong số đó phim nào nói về vũ trụ?", history) is True
    assert resolver.is_context_dependent("Ai đóng phim này?", history) is True


def test_in_memory_dispatcher():
    dispatcher = SubsystemDispatcher(MockSearchService(), MockRecommendationService())

    subsystem, data = dispatcher.dispatch("Gợi ý cho tôi phim hay", user_id=10)
    assert subsystem == "RECOMMEND"

    subsystem, data = dispatcher.dispatch("Tìm phim khoa học viễn tưởng", user_id=10)
    assert subsystem == "SEARCH"


def test_chat_service_agentic_search_tool_call(monkeypatch):
    from unittest.mock import MagicMock
    from modules.chatbot.service_impl import ChatServiceImpl
    from dtos.chat_dtos import ChatMessageRequest

    mock_search = MockSearchService()
    mock_rec = MockRecommendationService()
    mock_openai = MagicMock()

    # Mock OpenAI client response with search_movies tool call
    mock_tool_call = MagicMock()
    mock_tool_call.function.name = "search_movies"
    mock_tool_call.function.arguments = '{"query": "Interstellar"}'

    mock_msg = MagicMock()
    mock_msg.tool_calls = [mock_tool_call]
    mock_msg.content = None

    mock_openai.chat_completion_with_tools = MagicMock(return_value=mock_msg)
    mock_openai.chat_completion = MagicMock(return_value="Dưới đây là thông tin phim Interstellar.")

    service = ChatServiceImpl(search_service=mock_search, rec_service=mock_rec, openai_client=mock_openai)

    # Mock DB session retrieval & save
    monkeypatch.setattr(service, "_get_or_create_session", lambda c_id, u_id: ("test-conv-123", [], None))
    monkeypatch.setattr(service, "_save_session", lambda c_id, hist, last_mid: None)

    req = ChatMessageRequest(conversationId="test-conv-123", message="Tìm phim Interstellar")
    resp = service.chat(req)

    assert resp.subsystemInvoked == "SEARCH"
    assert resp.rewrittenQuery == "Interstellar"
    assert "Interstellar" in resp.reply


def test_tool_decorator_openapi_schema():
    from modules.chatbot.agent.tool import tool

    @tool()
    def sample_tool(title: str, year: int, rating: float = 8.5) -> dict:
        """Tra cứu thông tin phim theo tiêu đề và năm.
        
        Args:
            title: Tên phim cần tra cứu.
            year: Năm phát hành.
            rating: Điểm đánh giá tối thiểu.
        """
        return {"result": f"{title} ({year})"}

    schema = sample_tool.schema
    assert schema["type"] == "function"
    assert schema["function"]["name"] == "sample_tool"
    assert "Tra cứu thông tin phim" in schema["function"]["description"]

    props = schema["function"]["parameters"]["properties"]
    assert props["title"]["type"] == "string"
    assert "Tên phim" in props["title"]["description"]
    assert props["year"]["type"] == "integer"
    assert props["rating"]["type"] == "number"

    # title and year are required, rating has default
    assert "title" in schema["function"]["parameters"]["required"]
    assert "year" in schema["function"]["parameters"]["required"]
    assert "rating" not in schema["function"]["parameters"]["required"]


def test_tool_registry_and_single_hop_direct_path():
    from unittest.mock import MagicMock
    from modules.chatbot.agent.registry import ToolRegistry
    from modules.chatbot.agent.executor import SingleHopTagExecutor
    from modules.chatbot.agent.tool import tool

    registry = ToolRegistry()

    @tool()
    def dummy_tool(x: str) -> dict:
        """Dummy tool."""
        return {"echo": x}

    registry.register(dummy_tool)
    assert registry.get_tool("dummy_tool") is not None
    assert registry.execute("dummy_tool", x="antigravity") == {"echo": "antigravity"}

    # Test direct path (no tool call from LLM)
    mock_openai = MagicMock()
    direct_msg = MagicMock()
    direct_msg.tool_calls = None
    direct_msg.content = "Chào bạn! Tôi có thể giúp gì cho bạn hôm nay? 🍿"
    mock_openai.chat_completion_with_tools = MagicMock(return_value=direct_msg)

    executor = SingleHopTagExecutor(registry, mock_openai)
    result = executor.execute("Xin chào PopBot", history=[], user_id=42)

    assert result.subsystem == "DIRECT"
    assert "Chào bạn" in result.reply
    assert result.movies == []
    # Verify ONLY 1 LLM call was made (bounded latency for chit-chat)
    assert mock_openai.chat_completion_with_tools.call_count == 1
    assert mock_openai.chat_completion.call_count == 0

