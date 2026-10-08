import logging
from typing import Optional, Dict, Any, List
from .tool import tool
from .registry import ToolRegistry
from modules.search.interfaces import ISearchService
from modules.recommendation.interfaces import IRecommendationService

logger = logging.getLogger(__name__)


def create_cinema_tools(search_service: ISearchService, rec_service: IRecommendationService) -> List[Any]:
    """Create and bind cinema tools with clean docstrings to the underlying services."""

    @tool()
    def search_movies(query: str) -> Dict[str, Any]:
        """Tra cứu phim trong danh mục rạp CinePremier theo tiêu đề, diễn viên, đạo diễn, thể loại hoặc cốt truyện.
        Tự động sửa lỗi chính tả, dịch tiếng lóng hoặc tên nhân vật (ví dụ: 'anh thon' -> 'Thor', 'fim ma' -> 'phim ma') thành câu truy vấn chuẩn xác.

        Args:
            query: Câu truy vấn tìm kiếm độc lập đã làm rõ đầy đủ đại từ thay thế từ lịch sử trò chuyện.
        """
        logger.info(f"[Agent Tool] search_movies invoked with query='{query}'")
        res = search_service.search(query=query, limit=5)
        movies = [m.model_dump() for m in res.results]
        summary = (
            f"Tìm thấy {len(movies)} phim phù hợp: " +
            ", ".join([f"'{m['title']}' (Thể loại: {', '.join(m.get('genres', []))})" for m in movies[:4]])
            if movies else "Hiện rạp CinePremier không có phim nào khớp với yêu cầu này."
        )
        return {
            "movies": movies,
            "summary": summary,
            "subsystem": "SEARCH",
            "rewritten_query": query
        }

    @tool()
    def recommend_movies(user_id: int = 1, mood_or_topic: Optional[str] = None) -> Dict[str, Any]:
        """Gợi ý phim cá nhân hóa theo gu người dùng, tâm trạng cảm xúc hoặc hoàn cảnh buổi xem phim (ví dụ: buồn, vui, xả stress, hẹn hò với bạn gái, cuối tuần...).

        Args:
            user_id: Mã định danh của người dùng cần nhận gợi ý.
            mood_or_topic: Chủ đề, tâm trạng hoặc ngữ cảnh buổi xem phim của khách hàng.
        """
        logger.info(f"[Agent Tool] recommend_movies invoked for user_id={user_id}, mood_or_topic='{mood_or_topic}'")
        res = rec_service.get_user_recommendations(user_id=user_id, limit=5)
        movies = [m.model_dump() for m in res.recommendations]
        summary = (
            f"Đề xuất {len(movies)} phim theo sở thích: " +
            ", ".join([f"'{m['title']}' ({m.get('reason', '')})" for m in movies[:4]])
            if movies else "Danh sách các phim thịnh hành đang chiếu rạp CinePremier."
        )
        return {
            "movies": movies,
            "summary": summary,
            "subsystem": "RECOMMEND",
            "rewritten_query": mood_or_topic
        }

    return [search_movies, recommend_movies]


def build_default_registry(search_service: ISearchService, rec_service: IRecommendationService) -> ToolRegistry:
    """Factory helper initializing a ToolRegistry with default cinema tools."""
    registry = ToolRegistry()
    tools = create_cinema_tools(search_service, rec_service)
    for t in tools:
        registry.register(t)
    return registry
