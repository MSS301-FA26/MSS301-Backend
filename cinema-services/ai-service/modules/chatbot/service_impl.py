import uuid
import json
import logging
from typing import List, Dict, Optional, Tuple, Any
import psycopg2.extras
from dtos.chat_dtos import ChatMessageRequest, ChatMessageResponse
from modules.chatbot.interfaces import IChatService
from modules.search.service_impl import get_search_service
from modules.recommendation.service_impl import get_recommendation_service
from core.openai_client import get_openai_client
from core.db import get_db_connection
from modules.chatbot.agent.tools import build_default_registry
from modules.chatbot.agent.executor import SingleHopTagExecutor

logger = logging.getLogger(__name__)


class ChatServiceImpl:
    """Implementation of IChatService using SingleHopTagExecutor."""

    def __init__(self, search_service=None, rec_service=None, openai_client=None, executor=None):
        self.search_service = search_service if search_service is not None else get_search_service()
        self.rec_service = rec_service if rec_service is not None else get_recommendation_service()
        self.openai_client = openai_client if openai_client is not None else get_openai_client()

        if executor is not None:
            self.executor = executor
        else:
            registry = build_default_registry(self.search_service, self.rec_service)
            self.executor = SingleHopTagExecutor(registry, self.openai_client)

    def _get_or_create_session(
        self, conversation_id: Optional[str], user_id: Optional[int]
    ) -> Tuple[str, List[Dict[str, str]], Optional[int]]:
        conv_id = conversation_id or str(uuid.uuid4())
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT conversation_id, history, last_movie_id
                    FROM chat_sessions
                    WHERE conversation_id = %s
                """, (conv_id,))
                row = cur.fetchone()
                if row:
                    history = row["history"] if isinstance(row["history"], list) else []
                    return conv_id, history, row.get("last_movie_id")
                else:
                    cur.execute("""
                        INSERT INTO chat_sessions (conversation_id, user_id, history, last_active)
                        VALUES (%s, %s, '[]'::jsonb, CURRENT_TIMESTAMP)
                    """, (conv_id, user_id))
                    conn.commit()
                    return conv_id, [], None

    def _save_session(self, conv_id: str, history: List[Dict[str, str]], last_movie_id: Optional[int]):
        with get_db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("""
                    UPDATE chat_sessions
                    SET history = %s, last_movie_id = COALESCE(%s, last_movie_id), last_active = CURRENT_TIMESTAMP
                    WHERE conversation_id = %s
                """, (json.dumps(history, ensure_ascii=False), last_movie_id, conv_id))
            conn.commit()

    def chat(self, request: ChatMessageRequest) -> ChatMessageResponse:
        conv_id, history, last_movie_id = self._get_or_create_session(request.conversationId, request.userId)

        # Execute Single-Hop Agent Runtime (Docstring-driven, Bounded Latency)
        agent_result = self.executor.execute(
            message=request.message,
            history=history,
            user_id=request.userId
        )

        # Update Session History
        history.append({"role": "user", "content": request.message})
        history.append({"role": "assistant", "content": agent_result.reply})

        new_last_mid = last_movie_id
        serialized_data = None
        if agent_result.movies:
            new_last_mid = agent_result.movies[0].get("movieId", last_movie_id)
            serialized_data = {"movies": agent_result.movies}

        self._save_session(conv_id, history, new_last_mid)

        return ChatMessageResponse(
            conversationId=conv_id,
            reply=agent_result.reply,
            rewrittenQuery=agent_result.rewritten_query,
            subsystemInvoked=agent_result.subsystem,
            data=serialized_data
        )


_chat_service_instance = None


def get_chat_service() -> IChatService:
    global _chat_service_instance
    if _chat_service_instance is None:
        _chat_service_instance = ChatServiceImpl()
    return _chat_service_instance
