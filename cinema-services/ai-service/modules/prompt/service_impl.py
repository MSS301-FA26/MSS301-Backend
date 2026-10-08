import json
import logging
import re
from typing import List, Optional, Tuple, Dict, Any
from datetime import datetime, timezone
import psycopg2.extras

from core.db import get_db_connection
from core.cache import get_cache
from core.openai_client import get_openai_client
from core.exceptions import NotFoundException, ConflictException, CinemaApiException
from dtos.prompt_dtos import (
    PromptTemplateResponse,
    PromptCreateRequest,
    PromptUpdateRequest,
    PromptTestRenderRequest,
    PromptTestRenderResponse
)
from modules.prompt.interfaces import IPromptService

logger = logging.getLogger(__name__)

DEFAULT_FALLBACK_PROMPTS: Dict[str, Dict[str, Any]] = {
    "QUERY_REWRITE": {
        "system_prompt": "You are an expert at resolving conversational ellipsis and rewriting follow-up queries into clear standalone search queries.",
        "user_template": (
            "Given the conversational dialogue between a User and a Cinema AI Assistant:\n"
            "{formatted_history}\n\n"
            "Latest user message: \"{current_message}\"\n"
            "Most recently discussed movie: \"{last_movie_title}\"\n\n"
            "Task: Rewrite the user's latest message into a fully resolved, standalone search query "
            "preserving all implicit context and entity mentions from previous turns. "
            "Return ONLY the rewritten query text without explanation or greeting."
        ),
        "model_name": "gpt-4o-mini",
        "temperature": 0.0,
        "max_tokens": 100,
        "required_variables": ["formatted_history", "current_message", "last_movie_title"]
    },
    "CHATBOT_GROUNDED_REPLY": {
        "system_prompt": "You are PopBot, AI assistant for CinePremier cinema. Ground all responses strictly in the provided movies.",
        "user_template": (
            "Catalog data retrieved from CinePremier: {titles_str}.\n"
            "User request: \"{user_message}\".\n"
            "Write 1-2 natural, polite sentences introducing these movies. "
            "ONLY mention movies present in the retrieved list above. Do NOT hallucinate unlisted movies."
        ),
        "model_name": "gpt-4o-mini",
        "temperature": 0.20,
        "max_tokens": 150,
        "required_variables": ["titles_str", "user_message"]
    },
    "SENTIMENT_ASPECT_ANALYSIS": {
        "system_prompt": "You are an expert NLP cinema sentiment and aspect analyzer. Output strictly valid JSON.",
        "user_template": (
            "User review for a cinema movie:\n"
            "- Star rating given by user: {rating} / 5.0\n"
            "- Review comment: \"{clean_text}\"\n\n"
            "Analyze the review and return a valid JSON object with the following fields:\n"
            "- \"sentiment_score\": float between -1.0 (most negative) and 1.0 (most positive)\n"
            "- \"sentiment_label\": string, either \"POSITIVE\", \"NEGATIVE\", or \"NEUTRAL\"\n"
            "- \"feedback_consistency\": string, \"CONSISTENT\" if rating matches comment tone, or \"INCONSISTENT\" if contradictory\n"
            "- \"confidence_score\": float between 0.0 and 1.0\n"
            "- \"aspect_sentiment\": object with keys \"plot\", \"acting\", \"visuals\", \"audio\" containing score between -1.0 and 1.0\n\n"
            "Return ONLY the raw JSON object without markdown formatting, code block, or explanation."
        ),
        "model_name": "gpt-4o-mini",
        "temperature": 0.0,
        "max_tokens": 250,
        "required_variables": ["rating", "clean_text"]
    },
    "RECOMMENDATION_EXPLAINER": {
        "system_prompt": "Bạn là chuyên viên đề xuất phim của CinePremier. Giải thích chính xác, súc tích và có căn cứ thực tế.",
        "user_template": (
            "Phim được đề xuất: '{movie_title}' (Thể loại: {genres_str}).\n"
            "Lịch sử xem gần đây của người dùng: Thích các phim thể loại {recent_str}.\n"
            "Nguồn gợi ý thuật toán: {source}.\n\n"
            "Nhiệm vụ: Viết đúng 1 câu tiếng Việt ngắn gọn (dưới 25 từ), tự nhiên và lịch sự để giải thích "
            "tại sao hệ thống rạp CinePremier đề xuất phim này cho người dùng.\n"
            "YÊU CẦU BẮT BUỘC: CHỈ ĐƯỢC dựa vào các thông tin thể loại và lịch sử cung cấp ở trên. "
            "Tuyệt đối không bịa đặt nội dung phim hoặc diễn viên không có trong dữ liệu."
        ),
        "model_name": "gpt-4o-mini",
        "temperature": 0.15,
        "max_tokens": 80,
        "required_variables": ["movie_title", "genres_str", "recent_str", "source"]
    }
}


class PromptServiceImpl(IPromptService):
    """Dynamic Prompt Template Service with PostgreSQL persistence and TTL cache invalidation."""

    def __init__(self):
        self.cache = get_cache()
        self.openai_client = get_openai_client()

    def _map_row_to_dto(self, row: dict) -> PromptTemplateResponse:
        created_at = row["created_at"]
        updated_at = row["updated_at"]
        if hasattr(created_at, "isoformat"):
            created_at = created_at.isoformat()
        if hasattr(updated_at, "isoformat"):
            updated_at = updated_at.isoformat()

        req_vars = row.get("required_variables") or []
        if isinstance(req_vars, str):
            try:
                req_vars = json.loads(req_vars)
            except Exception:
                req_vars = []

        return PromptTemplateResponse(
            id=row["id"],
            promptCode=row["prompt_code"],
            name=row["name"],
            description=row.get("description") or "",
            systemPrompt=row["system_prompt"],
            userTemplate=row["user_template"],
            modelName=row.get("model_name") or "gpt-4o-mini",
            temperature=float(row.get("temperature") or 0.20),
            maxTokens=int(row.get("max_tokens") or 200),
            requiredVariables=req_vars,
            isActive=bool(row.get("is_active", True)),
            version=int(row.get("version", 1)),
            createdAt=str(created_at),
            updatedAt=str(updated_at)
        )

    def list_prompts(self, active_only: bool = False) -> List[PromptTemplateResponse]:
        query = "SELECT * FROM prompt_templates"
        if active_only:
            query += " WHERE is_active = TRUE"
        query += " ORDER BY id ASC"

        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute(query)
                rows = cur.fetchall()
                return [self._map_row_to_dto(r) for r in rows]

    def get_prompt_by_code(self, code: str) -> Optional[PromptTemplateResponse]:
        code = code.upper().strip()
        cache_key = f"prompt:dto:{code}"
        cached = self.cache.get(cache_key)
        if cached:
            return cached

        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("SELECT * FROM prompt_templates WHERE prompt_code = %s", (code,))
                row = cur.fetchone()
                if not row:
                    return None
                dto = self._map_row_to_dto(row)
                self.cache.set(cache_key, dto, ttl_seconds=3600)
                return dto

    def create_prompt(self, request: PromptCreateRequest) -> PromptTemplateResponse:
        code = request.promptCode.upper().strip()
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("SELECT id FROM prompt_templates WHERE prompt_code = %s", (code,))
                if cur.fetchone():
                    raise ConflictException(f"Prompt template code '{code}' already exists")

                cur.execute(
                    """
                    INSERT INTO prompt_templates (
                        prompt_code, name, description, system_prompt, user_template,
                        model_name, temperature, max_tokens, required_variables, is_active, version,
                        created_at, updated_at
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    RETURNING *;
                    """,
                    (
                        code, request.name, request.description, request.systemPrompt, request.userTemplate,
                        request.modelName, request.temperature, request.maxTokens,
                        json.dumps(request.requiredVariables), request.isActive
                    )
                )
                row = cur.fetchone()
            conn.commit()

        self.cache.delete(f"prompt:dto:{code}")
        self.cache.delete(f"prompt:render:{code}")
        logger.info(f"Created new prompt template '{code}'")
        return self._map_row_to_dto(row)

    def update_prompt(self, code: str, request: PromptUpdateRequest) -> PromptTemplateResponse:
        code = code.upper().strip()
        existing = self.get_prompt_by_code(code)
        if not existing:
            raise NotFoundException(f"Prompt template '{code}' not found")

        updates = []
        params = []

        if request.name is not None:
            updates.append("name = %s")
            params.append(request.name)
        if request.description is not None:
            updates.append("description = %s")
            params.append(request.description)
        if request.systemPrompt is not None:
            updates.append("system_prompt = %s")
            params.append(request.systemPrompt)
        if request.userTemplate is not None:
            updates.append("user_template = %s")
            params.append(request.userTemplate)
        if request.modelName is not None:
            updates.append("model_name = %s")
            params.append(request.modelName)
        if request.temperature is not None:
            updates.append("temperature = %s")
            params.append(request.temperature)
        if request.maxTokens is not None:
            updates.append("max_tokens = %s")
            params.append(request.maxTokens)
        if request.requiredVariables is not None:
            updates.append("required_variables = %s")
            params.append(json.dumps(request.requiredVariables))
        if request.isActive is not None:
            updates.append("is_active = %s")
            params.append(request.isActive)

        if not updates:
            return existing

        updates.append("version = version + 1")
        updates.append("updated_at = CURRENT_TIMESTAMP")
        set_clause = ", ".join(updates)
        params.append(code)

        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute(f"UPDATE prompt_templates SET {set_clause} WHERE prompt_code = %s RETURNING *;", params)
                row = cur.fetchone()
            conn.commit()

        # Invalidate cache for hot-reload
        self.cache.delete(f"prompt:dto:{code}")
        self.cache.delete(f"prompt:render:{code}")
        logger.info(f"Updated prompt template '{code}' and invalidated cache (hot-reload)")
        return self._map_row_to_dto(row)

    def delete_prompt(self, code: str) -> bool:
        code = code.upper().strip()
        existing = self.get_prompt_by_code(code)
        if not existing:
            raise NotFoundException(f"Prompt template '{code}' not found")

        with get_db_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("UPDATE prompt_templates SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE prompt_code = %s", (code,))
            conn.commit()

        self.cache.delete(f"prompt:dto:{code}")
        self.cache.delete(f"prompt:render:{code}")
        logger.info(f"Deactivated prompt template '{code}' and cleared cache")
        return True

    def render_prompt(
        self,
        code: str,
        variables: Dict[str, Any]
    ) -> Tuple[str, str, str, float, int]:
        code = code.upper().strip()
        template_dto = None

        try:
            template_dto = self.get_prompt_by_code(code)
        except Exception as e:
            logger.warning(f"Error fetching prompt '{code}' from database ({e}). Falling back to hardcoded default.")

        if not template_dto or not template_dto.isActive:
            fallback = DEFAULT_FALLBACK_PROMPTS.get(code)
            if fallback:
                system_prompt = fallback["system_prompt"]
                user_template = fallback["user_template"]
                model_name = fallback["model_name"]
                temperature = fallback["temperature"]
                max_tokens = fallback["max_tokens"]
            else:
                raise NotFoundException(f"Prompt '{code}' not found in database or fallback registry")
        else:
            system_prompt = template_dto.systemPrompt
            user_template = template_dto.userTemplate
            model_name = template_dto.modelName
            temperature = template_dto.temperature
            max_tokens = template_dto.maxTokens

        # Safe template substitution without regex syntax errors
        rendered_user_prompt = user_template
        for k, v in variables.items():
            val_str = str(v) if v is not None else ""
            rendered_user_prompt = rendered_user_prompt.replace(f"{{{k}}}", val_str)

        return system_prompt, rendered_user_prompt, model_name, temperature, max_tokens

    def test_render(
        self,
        code: str,
        request: PromptTestRenderRequest
    ) -> PromptTestRenderResponse:
        code = code.upper().strip()
        prompt_dto = self.get_prompt_by_code(code)
        if not prompt_dto:
            raise NotFoundException(f"Prompt template '{code}' not found")

        # Find expected variables from user template
        expected_vars = re.findall(r"\{([a-zA-Z0-9_]+)\}", prompt_dto.userTemplate)
        missing_vars = [v for v in expected_vars if v not in request.variables]

        system_prompt, rendered_user_prompt, model_name, temperature, max_tokens = self.render_prompt(
            code, request.variables
        )

        llm_reply = None
        if request.callLlm:
            messages = [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": rendered_user_prompt}
            ]
            llm_reply = self.openai_client.chat_completion(
                messages,
                temperature=temperature,
                max_tokens=max_tokens,
                model=model_name
            )

        return PromptTestRenderResponse(
            promptCode=code,
            renderedSystemPrompt=system_prompt,
            renderedUserPrompt=rendered_user_prompt,
            missingVariables=missing_vars,
            llmResponse=llm_reply
        )


_prompt_service_instance: Optional[IPromptService] = None


def get_prompt_service() -> IPromptService:
    global _prompt_service_instance
    if _prompt_service_instance is None:
        _prompt_service_instance = PromptServiceImpl()
    return _prompt_service_instance
