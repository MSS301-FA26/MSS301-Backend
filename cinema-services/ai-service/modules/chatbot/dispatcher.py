import logging
from typing import Tuple, Optional, Any
from modules.search.interfaces import ISearchService
from modules.recommendation.interfaces import IRecommendationService

logger = logging.getLogger(__name__)

REC_KEYWORDS = [
    "gợi ý cho tôi", "đề xuất cho tôi", "phù hợp với tôi", "gợi ý riêng",
    "tôi nên xem", "nên xem gì", "xem gì bây giờ", "theo sở thích", "hợp với tôi"
]

SEARCH_KEYWORDS = [
    "tìm phim", "phim gì", "phim nào", "có phim", "phim về", "nói về",
    "đạo diễn", "diễn viên", "thể loại", "chiếu lúc", "vũ trụ", "kinh dị", "hành động"
]


class SubsystemDispatcher:
    """
    In-Memory Subsystem Dispatcher:
    Dispatches directly to ISearchService or IRecommendationService within the same process RAM,
    completely eliminating HTTP network hops.
    """

    def __init__(self, search_service: ISearchService, rec_service: IRecommendationService):
        self.search_service = search_service
        self.rec_service = rec_service

    def dispatch(
        self,
        query: str,
        user_id: Optional[int] = None
    ) -> Tuple[str, Any]:
        """
        Determines target subsystem and executes retrieval.
        Returns: (subsystem_name, data_result)
        """
        q_lower = query.lower()

        # 1. Check for personalized recommendation intent
        if any(k in q_lower for k in REC_KEYWORDS):
            effective_uid = user_id or 1
            logger.info(f"[Dispatcher] Dispatching to RECOMMENDATION SUBSYSTEM (User {effective_uid})")
            rec_result = self.rec_service.get_user_recommendations(user_id=effective_uid, limit=5)
            return "RECOMMEND", rec_result

        # 2. Check for movie search intent
        if any(k in q_lower for k in SEARCH_KEYWORDS) or len(query.split()) >= 2:
            logger.info(f"[Dispatcher] Dispatching to SEARCH SUBSYSTEM with query: '{query}'")
            search_result = self.search_service.search(query=query, limit=5)
            return "SEARCH", search_result

        # 3. Direct conversational fallback
        logger.info("[Dispatcher] Dispatching to DIRECT CONVERSATION")
        return "DIRECT", None
