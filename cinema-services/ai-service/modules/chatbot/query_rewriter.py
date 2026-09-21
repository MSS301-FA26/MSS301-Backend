import logging
from typing import List, Dict
from core.openai_client import get_openai_client

logger = logging.getLogger(__name__)


class QueryRewriter:
    """
    History-Aware Query Rewriter:
    Rewrites conversational multi-turn follow-up queries into standalone search queries.
    """

    def __init__(self):
        self.openai_client = get_openai_client()

    def rewrite_query(
        self,
        current_message: str,
        history: List[Dict[str, str]],
        last_movie_title: str = None
    ) -> str:
        # 1. Attempt LLM-based query rewrite if client is configured
        recent_history = history[-4:] if len(history) > 4 else history
        formatted_history = "\n".join([f"{turn['role'].capitalize()}: {turn['content']}" for turn in recent_history])

        prompt = (
            "Given the conversational dialogue between a User and a Cinema AI Assistant:\n"
            f"{formatted_history}\n\n"
            f"Latest user message: \"{current_message}\"\n"
            f"Most recently discussed movie: \"{last_movie_title or 'None'}\"\n\n"
            "Task: Rewrite the user's latest message into a fully resolved, standalone search query "
            "preserving all implicit context and entity mentions from previous turns. "
            "Return ONLY the rewritten query text without explanation or greeting."
        )

        messages = [
            {"role": "system", "content": "You are an expert at resolving conversational ellipsis and rewriting follow-up queries into clear standalone search queries."},
            {"role": "user", "content": prompt}
        ]

        rewritten = self.openai_client.chat_completion(messages, temperature=0.0, max_tokens=100)
        if rewritten:
            logger.info(f"[Query Rewriter] Rewrote '{current_message}' -> '{rewritten}'")
            return rewritten

        # 2. Rule-based heuristic fallback when LLM is unconfigured or unavailable
        fallback_query = current_message
        if last_movie_title:
            if any(w in current_message.lower() for w in ["này", "đó", "thứ hai", "bộ đó", "this", "that"]):
                fallback_query = f"{last_movie_title} {current_message}"
        logger.info(f"[Query Rewriter Fallback] '{current_message}' -> '{fallback_query}'")
        return fallback_query
