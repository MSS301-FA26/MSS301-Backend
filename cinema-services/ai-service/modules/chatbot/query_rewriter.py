import logging
from typing import List, Dict
from core.openai_client import get_openai_client
from modules.prompt.service_impl import get_prompt_service

logger = logging.getLogger(__name__)


class QueryRewriter:
    """
    History-Aware Query Rewriter:
    Rewrites conversational multi-turn follow-up queries into standalone search queries.
    """

    def __init__(self):
        self.openai_client = get_openai_client()
        self.prompt_service = get_prompt_service()

    def rewrite_query(
        self,
        current_message: str,
        history: List[Dict[str, str]],
        last_movie_title: str = None
    ) -> str:
        # Attempt LLM-based query rewrite using dynamic prompt template
        recent_history = history[-4:] if len(history) > 4 else history
        formatted_history = "\n".join([f"{turn['role'].capitalize()}: {turn['content']}" for turn in recent_history])

        try:
            sys_prompt, user_prompt, model_name, temp, max_tok = self.prompt_service.render_prompt(
                "QUERY_REWRITE",
                {
                    "formatted_history": formatted_history,
                    "current_message": current_message,
                    "last_movie_title": last_movie_title or "None"
                }
            )

            messages = [
                {"role": "system", "content": sys_prompt},
                {"role": "user", "content": user_prompt}
            ]

            rewritten = self.openai_client.chat_completion(
                messages,
                temperature=temp,
                max_tokens=max_tok,
                model=model_name
            )
            if rewritten:
                logger.info(f"[Query Rewriter] Rewrote '{current_message}' -> '{rewritten}'")
                return rewritten
        except Exception as e:
            logger.warning(f"[Query Rewriter] Dynamic prompt execution failed ({e}), falling back to heuristic.")

        # Rule-based heuristic fallback when LLM is unconfigured or unavailable
        fallback_query = current_message
        if last_movie_title:
            if any(w in current_message.lower() for w in ["này", "đó", "thứ hai", "bộ đó", "this", "that"]):
                fallback_query = f"{last_movie_title} {current_message}"
        logger.info(f"[Query Rewriter Fallback] '{current_message}' -> '{fallback_query}'")
        return fallback_query
