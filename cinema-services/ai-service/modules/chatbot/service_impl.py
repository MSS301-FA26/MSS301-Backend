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

logger = logging.getLogger(__name__)

CHATBOT_SYSTEM_PROMPT = (
    "You are PopBot, the intelligent cinema AI assistant for CinePremier theater. "
    "You help moviegoers discover movies, explore genres, check showtimes, and get personalized recommendations. "
    "Always be friendly, polite, and helpful in Vietnamese. "
    "When the user is looking for a movie or asking questions about movies, use 'search_movies' with a fully resolved standalone query. "
    "When the user asks for recommendations or mentions feelings/moods/situations (e.g. relaxing, date night, happy, sad), use 'recommend_movies'. "
    "If the user is just greeting or chatting socially, reply directly without calling tools. "
    "CRITICAL: When presenting movie results, strictly mention only the movies provided in the tool output. Never hallucinate unlisted movies."
)

CINEMA_TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "search_movies",
            "description": "Search for movies in CinePremier theater catalog by title, genre, actor, director, or plot themes.",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The resolved standalone search query. Resolve all pronouns and ellipses from conversation history."
                    }
                },
                "required": ["query"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "recommend_movies",
            "description": "Get personalized movie recommendations based on user profile, mood, or preference.",
            "parameters": {
                "type": "object",
                "properties": {
                    "mood_or_topic": {
                        "type": "string",
                        "description": "Optional mood or genre topic expressed by user."
                    }
                }
            }
        }
    }
]


class ChatServiceImpl:
    """Agentic Implementation of IChatService using dynamic Tool Calling."""

    def __init__(self, search_service=None, rec_service=None, openai_client=None):
        self.search_service = search_service if search_service is not None else get_search_service()
        self.rec_service = rec_service if rec_service is not None else get_recommendation_service()
        self.openai_client = openai_client if openai_client is not None else get_openai_client()

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

        # Build message context for LLM
        messages: List[Dict[str, Any]] = [{"role": "system", "content": CHATBOT_SYSTEM_PROMPT}]
        recent_history = history[-6:] if len(history) > 6 else history
        for turn in recent_history:
            messages.append({"role": turn["role"], "content": turn["content"]})
        messages.append({"role": "user", "content": request.message})

        # Dynamic LLM Tool Calling (No regex, no hardcoded keywords)
        response_msg = self.openai_client.chat_completion_with_tools(
            messages=messages,
            tools=CINEMA_TOOLS,
            temperature=0.1
        )

        subsystem = "DIRECT"
        rewritten_query = None
        serialized_data = None
        new_last_mid = last_movie_id

        if response_msg.tool_calls:
            tool_call = response_msg.tool_calls[0]
            func_name = tool_call.function.name
            raw_args = tool_call.function.arguments
            try:
                args = json.loads(raw_args) if isinstance(raw_args, str) else (raw_args or {})
            except Exception:
                args = {}

            if func_name == "search_movies":
                subsystem = "SEARCH"
                rewritten_query = args.get("query", request.message)
                logger.info(f"[PopBot ToolCall] Dynamic Search query: '{rewritten_query}'")
                search_data = self.search_service.search(query=rewritten_query, limit=5)
                serialized_data = {"movies": [m.model_dump() for m in search_data.results]}
                if search_data.results:
                    new_last_mid = search_data.results[0].movieId

                tool_result_summary = (
                    f"Found {len(search_data.results)} movies: " +
                    ", ".join([f"'{m.title}' (Thể loại: {', '.join(m.genres)})" for m in search_data.results[:4]])
                    if search_data.results else "No matching movies currently showing in catalog."
                )

            elif func_name == "recommend_movies":
                subsystem = "RECOMMEND"
                effective_uid = request.userId or 1
                logger.info(f"[PopBot ToolCall] Dynamic Recommendation for User {effective_uid}")
                rec_data = self.rec_service.get_user_recommendations(user_id=effective_uid, limit=5)
                serialized_data = {"movies": [m.model_dump() for m in rec_data.recommendations]}
                if rec_data.recommendations:
                    new_last_mid = rec_data.recommendations[0].movieId

                tool_result_summary = (
                    f"Recommended {len(rec_data.recommendations)} movies: " +
                    ", ".join([f"'{m.title}' ({m.reason})" for m in rec_data.recommendations[:4]])
                    if rec_data.recommendations else "Popular trending movies returned."
                )
            else:
                tool_result_summary = "No tool result available."

            # Follow-up turn to generate grounded natural language reply
            follow_up_messages = list(messages)
            follow_up_messages.append({
                "role": "assistant",
                "content": f"I have executed the tool {func_name}."
            })
            follow_up_messages.append({
                "role": "user",
                "content": f"Catalog data from CinePremier system: {tool_result_summary}. "
                           f"Formulate a warm, helpful response answering the user message: \"{request.message}\". "
                           "Strictly only mention movies from the provided catalog data above."
            })

            reply_text = self.openai_client.chat_completion(
                messages=follow_up_messages,
                temperature=0.2,
                max_tokens=300
            ) or "Dưới đây là các phim phù hợp tại CinePremier mà tôi tìm thấy cho bạn."

        else:
            # Direct chit-chat / conversational answer
            subsystem = "DIRECT"
            reply_text = response_msg.content or "Xin chào! Tôi có thể giúp gì cho bạn về phim ảnh tại CinePremier hôm nay?"

        # Update Session History
        history.append({"role": "user", "content": request.message})
        history.append({"role": "assistant", "content": reply_text})
        self._save_session(conv_id, history, new_last_mid)

        return ChatMessageResponse(
            conversationId=conv_id,
            reply=reply_text,
            rewrittenQuery=rewritten_query,
            subsystemInvoked=subsystem,
            data=serialized_data
        )


_chat_service_instance = None


def get_chat_service() -> IChatService:
    global _chat_service_instance
    if _chat_service_instance is None:
        _chat_service_instance = ChatServiceImpl()
    return _chat_service_instance
