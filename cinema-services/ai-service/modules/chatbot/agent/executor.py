import json
import logging
from dataclasses import dataclass
from typing import List, Dict, Any, Optional
from .registry import ToolRegistry
from core.openai_client import OpenAIClient

logger = logging.getLogger(__name__)

PURE_PERSONA_PROMPT = (
    "Bạn là PopBot - Trợ lý AI thông minh, nhiệt tình và thân thiện của rạp chiếu phim CinePremier. "
    "Nhiệm vụ của bạn là đồng hành cùng khách hàng: tìm phim, gợi ý phim theo gu/cảm xúc, và tư vấn dịch vụ rạp. "
    "Phong cách nói chuyện: Tự nhiên, ấm áp, có cảm xúc, xưng 'tôi' hoặc 'PopBot' và gọi khách là 'bạn'. Có thể dùng emoji tinh tế (🍿, 🎬, ✨). "
    "Nếu khách hỏi câu ngoài lề rạp phim: Khéo léo trả lời vui vẻ ngắn gọn và hướng khách về các bộ phim tại rạp. "
    "QUY TẮC CỐT LÕI: Khi giới thiệu phim từ kết quả hệ thống, CHỈ ĐƯỢC nhắc đến các phim có trong kết quả cung cấp. Tuyệt đối không bịa đặt phim không có trong rạp."
)


@dataclass
class AgentResult:
    reply: str
    subsystem: str
    movies: List[Dict[str, Any]]
    rewritten_query: Optional[str] = None


class SingleHopTagExecutor:
    """
    Deterministic Single-Hop Tool-Augmented Generation (TAG) Executor.
    Bounded latency: Maximum 1 tool hop (1 LLM call for direct chit-chat, 2 LLM calls for tool retrieval).
    Schema is 100% docstring-driven without any hardcoded tool instructions in system prompt.
    """

    def __init__(self, tool_registry: ToolRegistry, openai_client: OpenAIClient):
        self.tool_registry = tool_registry
        self.openai_client = openai_client

    def execute(
        self,
        message: str,
        history: List[Dict[str, str]],
        user_id: Optional[int] = None
    ) -> AgentResult:
        # 1. Build conversational context
        messages: List[Dict[str, Any]] = [{"role": "system", "content": PURE_PERSONA_PROMPT}]
        recent_history = history[-6:] if len(history) > 6 else history
        for turn in recent_history:
            messages.append({"role": turn["role"], "content": turn["content"]})
        messages.append({"role": "user", "content": message})

        # 2. Lượt 1: LLM Tool Classification & Parameter Extraction (Docstring-Driven)
        tools_schema = self.tool_registry.get_tools_schema()
        response_msg = self.openai_client.chat_completion_with_tools(
            messages=messages,
            tools=tools_schema,
            temperature=0.1
        )

        # 3. Handle Tool Calling vs Direct Path
        if response_msg.tool_calls:
            # Lấy tool call đầu tiên (Single-Hop)
            tool_call = response_msg.tool_calls[0]
            func_name = tool_call.function.name
            raw_args = tool_call.function.arguments

            try:
                args = json.loads(raw_args) if isinstance(raw_args, str) else (raw_args or {})
            except Exception:
                args = {}

            # Inject user_id if calling recommend_movies
            if func_name == "recommend_movies" and "user_id" not in args:
                args["user_id"] = user_id or 1

            logger.info(f"[SingleHopExecutor] Invoking tool '{func_name}' with arguments: {args}")

            # Thực thi tool trong RAM (mất ~ 3ms)
            tool_result = self.tool_registry.execute(func_name, **args)

            movies = tool_result.get("movies", [])
            summary = tool_result.get("summary", "")
            subsystem = tool_result.get("subsystem", "SEARCH")
            rewritten_query = tool_result.get("rewritten_query")

            # 4. Lượt 2: LLM Grounded Response Synthesis
            follow_up_messages = list(messages)
            follow_up_messages.append({
                "role": "assistant",
                "content": f"Đã thực thi công cụ {func_name}."
            })
            follow_up_messages.append({
                "role": "user",
                "content": f"Dữ liệu tra cứu từ catalog rạp CinePremier: {summary}. "
                           f"Hãy soạn câu trả lời thân thiện, lịch sự gửi khách cho tin nhắn: \"{message}\". "
                           "CHỈ ĐƯỢC nhắc tới các phim có trong danh sách cung cấp ở trên."
            })

            reply_text = self.openai_client.chat_completion(
                messages=follow_up_messages,
                temperature=0.2,
                max_tokens=300
            ) or f"Dưới đây là các phim phù hợp tại CinePremier: {summary}"

            return AgentResult(
                reply=reply_text,
                subsystem=subsystem,
                movies=movies,
                rewritten_query=rewritten_query
            )

        else:
            # Đường đi trực tiếp (Chit-chat / FAQ rạp) -> Chỉ tốn đúng 1 lượt LLM (~1.0s)
            reply_text = response_msg.content or "Xin chào! PopBot có thể giúp gì cho bạn về phim ảnh tại CinePremier hôm nay? 🍿"
            return AgentResult(
                reply=reply_text,
                subsystem="DIRECT",
                movies=[],
                rewritten_query=None
            )
