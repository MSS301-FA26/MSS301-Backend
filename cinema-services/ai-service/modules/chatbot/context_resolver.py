import re
from typing import List, Dict

DEPENDENT_PATTERNS = [
    r"bộ (này|đó|kia|thứ|nào)",
    r"cái (thứ|này|đó)",
    r"phim (này|đó|kia|vừa|trên)",
    r"trong số (đó|các phim|này)",
    r"những phim (vừa|trên|đó|này)",
    r"(còn|thế còn) (phim|đạo diễn|diễn viên|thể loại|suất)",
    r"bao nhiêu tiền",
    r"mấy giờ chiếu",
    r"ai đóng",
    r"do ai làm",
    r"nội dung là gì",
    r"nói về cái gì",
    r"chiếu lúc nào",
]


class ContextResolver:
    """
    Conditional Context Resolution c_t in {0, 1}:
    Determines whether the incoming user turn is context-dependent on previous conversation history.
    Avoids redundant LLM rewrite calls on already standalone queries, reducing latency and cost.
    """

    def is_context_dependent(self, message: str, history: List[Dict[str, str]]) -> bool:
        if not history:
            return False

        msg_lower = message.strip().lower()

        # Short follow-up utterance heuristic (<= 3 words with follow-up markers)
        if len(msg_lower.split()) <= 3 and any(w in msg_lower for w in ["còn", "thế", "nào", "sao", "tiếp"]):
            return True

        # Match contextual dependent regex patterns
        for pattern in DEPENDENT_PATTERNS:
            if re.search(pattern, msg_lower):
                return True

        return False
