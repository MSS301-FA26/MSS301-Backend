import json
from service.prompt_builder import get_movie_context, build_system_prompt
from service.tool_executor import execute_tool
from service.conversation_store import (
    new_conversation_id, get_history, append,
    get_last_movie_id, set_last_movie_id,
)
from service.ollama_client import chat_completion
from service.router import (
    detect_tool, detect_genre, extract_person_name,
    DIRECTOR_TRIGGER_KEYWORDS, ACTOR_TRIGGER_KEYWORDS,
    is_out_of_scope, OUT_OF_SCOPE_REPLY, is_blocked, BLOCKED_REPLY,
)

MOVIE_LIST_TOOLS = {
    "search_movies", "filter_by_genre",
    "recommend_similar_movies", "recommend_for_user",
    "search_by_director", "search_by_actor",
    "get_trending", "get_most_booked", "get_now_showing",
}
DATA_LIMIT = 5

CHAT_REC_LIMIT = 4


def build_response_data(tool_name: str | None, result) -> dict | None:
    if tool_name in MOVIE_LIST_TOOLS and isinstance(result, list) and result:
        # get_now_showing hiển thị tất cả, các tool khác giới hạn DATA_LIMIT
        limit = len(result) if tool_name == "get_now_showing" else DATA_LIMIT
        movies = [
            {
                "id": m.get("movieId"),
                "title": m.get("title", ""),
                "poster": m.get("posterUrl", ""),
                "similarity": m.get("similarity"),
                "year": m.get("year"),
                "rating": m.get("rating"),
                "status": m.get("status"),
                "overview": m.get("overview", ""),
                "trailerUrl": m.get("trailerUrl", ""),
            }
            for m in result[:limit]
        ]
        return {"movies": movies}

    if tool_name == "get_showtimes" and isinstance(result, list) and result:
        return {"showtimes": result[:DATA_LIMIT]}

    return None


def _safe_execute_tool(name: str, args: dict):
    """Không để 1 tool lỗi (DB down, args thiếu field) làm sập cả chat."""
    try:
        return execute_tool(name, args)
    except Exception as e:
        return {"error": f"Tool '{name}' thất bại: {e}"}


EMPTY_RESULT_MESSAGES = {
    "search_movies":            "Không tìm thấy phim phù hợp trong hệ thống.",
    "filter_by_genre":          "Không tìm thấy phim thuộc thể loại này trong hệ thống.",
    "recommend_similar_movies": "Không tìm thấy phim tương tự trong hệ thống.",
    "recommend_for_user":       "Chưa có đủ dữ liệu để gợi ý phim riêng cho bạn.",
    "get_showtimes":            "Phim này hiện chưa có suất chiếu nào được mở.",
    "get_ticket_price":         "Hiện chưa có suất chiếu nào để tham khảo giá vé.",
    "search_by_director":       "Không tìm thấy phim nào của đạo diễn này trong hệ thống.",
    "search_by_actor":          "Không tìm thấy phim nào có diễn viên này trong hệ thống.",
    "get_trending":             "Hiện chưa có dữ liệu phim nổi bật.",
    "get_most_booked":          "Hiện chưa có dữ liệu đặt vé để xếp hạng.",
    "get_now_showing":          "Hiện tại chưa có phim nào đang chiếu trong hệ thống.",
}


def _is_empty_result(result) -> bool:
    if isinstance(result, dict) and "error" in result:
        return True
    if isinstance(result, list) and not result:
        return True
    return False


def _build_intro(tool_name: str, result: list, args: dict, message: str) -> str:
    """Tạo câu giới thiệu có lý do cụ thể — chỉ dùng data thật, không gọi LLM."""
    count = min(len(result), 5)

    if tool_name == "search_movies":
        # Lý do: phân tích ngữ nghĩa từ query gốc + similarity score cao nhất
        top_sim = result[0].get("similarity") if result else None
        sim_note = f" (độ tương đồng cao nhất: {round(top_sim * 100)}%)" if top_sim else ""
        low_note = " — lưu ý hệ thống hiện có ít phim, kết quả có thể chưa đầy đủ." if count < 3 else "."
        return (
            f"Tìm được {count} phim có nội dung gần nhất với \"{message.strip()}\"{sim_note}{low_note}\n"
            f"Lý do chọn: hệ thống phân tích ngữ nghĩa câu hỏi và so khớp với mô tả, thể loại, diễn viên của từng phim trong cơ sở dữ liệu."
        )

    if tool_name == "filter_by_genre":
        genre = args.get("genre", "")
        top_rated = [m for m in result[:5] if m.get("rating")]
        avg = round(sum(m["rating"] for m in top_rated) / len(top_rated), 1) if top_rated else None
        rating_note = f" Rating trung bình: {avg}/5." if avg else ""
        return (
            f"Đây là {count} phim thể loại **{genre}** trong hệ thống, sắp xếp ưu tiên phim đang chiếu rồi đến rating cao nhất.{rating_note}\n"
            f"Lý do chọn: khớp chính xác tên thể loại \"{genre}\" trong cơ sở dữ liệu."
        )

    if tool_name == "recommend_similar_movies":
        top_sim = result[0].get("similarity") if result else None
        sim_note = f" Phim tương tự nhất đạt {round(top_sim * 100)}% độ giống." if top_sim else ""
        return (
            f"Dựa trên phim bạn đang xem, đây là {count} gợi ý tương tự nhất.{sim_note}\n"
            f"Lý do chọn: so sánh embedding (mô tả + thể loại + diễn viên) của phim hiện tại với toàn bộ phim trong hệ thống."
        )

    if tool_name == "recommend_for_user":
        return (
            f"Dựa trên lịch sử xem của bạn, đây là {count} phim bạn có thể thích.\n"
            f"Lý do chọn: collaborative filtering — tìm những user có sở thích tương tự và gợi ý phim họ đã xem mà bạn chưa xem."
        )

    if tool_name == "search_by_director":
        director_name = args.get("name", "")
        return (
            f"Tìm được {count} phim của đạo diễn **{director_name}** trong hệ thống, sắp xếp theo rating.\n"
            f"Lý do chọn: tìm kiếm tên đạo diễn \"{director_name}\" trực tiếp trong cơ sở dữ liệu phim."
        )

    if tool_name == "search_by_actor":
        actor_name = args.get("name", "")
        return (
            f"Tìm được {count} phim có diễn viên **{actor_name}** tham gia, sắp xếp theo rating.\n"
            f"Lý do chọn: tìm kiếm tên diễn viên \"{actor_name}\" trong danh sách diễn viên của từng phim."
        )

    if tool_name == "get_trending":
        top_reviews = result[0].get("reviewCount", 0) if result else 0
        return (
            f"Đây là {count} phim đang hot nhất hiện tại.\n"
            f"Lý do chọn: ưu tiên phim đang chiếu (NOW_SHOWING), sau đó sắp xếp theo số lượt đánh giá và điểm rating — phim có {top_reviews} lượt review dẫn đầu."
        )

    if tool_name == "get_most_booked":
        top_tickets = result[0].get("ticketCount", 0) if result else 0
        return (
            f"Đây là {count} phim được đặt vé nhiều nhất.\n"
            f"Lý do chọn: đếm số vé đã bán qua hệ thống đặt chỗ — phim dẫn đầu có {top_tickets} vé đã được đặt."
        )

    if tool_name == "get_now_showing":
        total = len(result)
        return f"Hiện tại có {total} phim đang chiếu tại CinePremier, sắp xếp theo đánh giá từ cao đến thấp."

    return f"Đây là {count} phim phù hợp:"


def _fallback_template(tool_name: str, result, args: dict = None, message: str = "") -> str:
    if isinstance(result, list) and result:
        if tool_name in MOVIE_LIST_TOOLS:
            return _build_intro(tool_name, result, args or {}, message)
        if tool_name == "get_showtimes":
            return "Đây là các suất chiếu sắp tới:"
        if tool_name == "get_ticket_price":
            def fmt(val):
                if val is None:
                    return None
                return f"{int(float(val)):,}đ".replace(",", ".")
            r = result[0]
            lines = ["Bảng giá vé tham khảo:"]
            std = [x for x in [
                f"Người lớn: {fmt(r.get('adult_standard_price'))}",
                f"Sinh viên: {fmt(r.get('student_standard_price'))}",
                f"Trẻ em: {fmt(r.get('child_standard_price'))}",
            ] if "None" not in x]
            vip = [x for x in [
                f"Người lớn: {fmt(r.get('adult_vip_price'))}",
                f"Sinh viên: {fmt(r.get('student_vip_price'))}",
                f"Trẻ em: {fmt(r.get('child_vip_price'))}",
            ] if "None" not in x]
            couple = [x for x in [
                f"Người lớn: {fmt(r.get('adult_couple_price'))}",
                f"Sinh viên: {fmt(r.get('student_couple_price'))}",
                f"Trẻ em: {fmt(r.get('child_couple_price'))}",
            ] if "None" not in x]
            if std:
                lines.append("• Ghế thường — " + ", ".join(std))
            if vip:
                lines.append("• Ghế VIP — " + ", ".join(vip))
            if couple:
                lines.append("• Ghế đôi — " + ", ".join(couple))
            return "\n".join(lines)
    return "Không tìm thấy thông tin phù hợp trong hệ thống."


def _extract_real_titles(result) -> list[str]:
    if isinstance(result, list):
        return [m.get("title", "") for m in result if isinstance(m, dict) and m.get("title")]
    return []


def _is_grounded(text: str, real_titles: list[str]) -> bool:
    """Kiểm tra câu trả lời của LLM có thực sự nhắc tới ít nhất 1 phim thật trong data không.
    Nếu model bịa toàn phim khác (không match title nào), coi như output không đáng tin."""
    if not real_titles:
        return True  # không có title để so (VD: showtimes/ticket_price) -> bỏ qua check này
    text_lower = text.lower()
    return any(title.lower() in text_lower for title in real_titles)


def _format_with_llm(user_message: str, result) -> str | None:
    """LLM CHỈ viết lại data thật thành câu trả lời ngắn — không quyết định logic, không tool.
    Cố tình KHÔNG đưa movie_context vào đây: nếu có, model dễ lấy thông tin phim đang xem
    ra trả lời thay vì bám vào đúng data tool trả về (đã từng gây nhầm lẫn khi test)."""
    data_json = json.dumps(result, ensure_ascii=False)[:3000]
    prompt = (
        "Dữ liệu thật lấy từ hệ thống (JSON):\n"
        f"{data_json}\n\n"
        f"Câu hỏi của người dùng: {user_message}\n\n"
        "Viết lại thành 1-2 câu giới thiệu ngắn gọn, tự nhiên, bằng tiếng Việt. "
        "CHỈ dùng tên phim/thông tin có trong JSON trên. "
        "KHÔNG thêm phim hay chi tiết nào khác ngoài JSON. "
        "KHÔNG liệt kê lại từng field chi tiết (đã hiển thị sẵn trên giao diện)."
    )
    messages = [
        {"role": "system", "content": (
            "Bạn viết lại dữ liệu JSON thành câu trả lời tiếng Việt ngắn gọn. "
            "Chỉ dùng thông tin có trong JSON, không thêm gì khác."
        )},
        {"role": "user", "content": prompt},
    ]
    try:
        reply = chat_completion(messages, temperature=0.1)
        text = (reply.get("content") or "").strip()
        if not text:
            return None
        # Validate: model có thực sự bám vào data thật không, hay đang tự bịa
        if not _is_grounded(text, _extract_real_titles(result)):
            return None
        return text
    except Exception:
        return None


def chat(message: str, movie_id: int | None = None, user_id: int | None = None,
         conversation_id: str | None = None) -> tuple[str, str, dict | None]:
    if not conversation_id:
        conversation_id = new_conversation_id()

    # Chặn nội dung không phù hợp trước mọi xử lý khác.
    if is_blocked(message):
        append(conversation_id, "user", message)
        append(conversation_id, "assistant", BLOCKED_REPLY)
        return BLOCKED_REPLY, conversation_id, None

    # Ưu tiên movie_id từ request (user đang ở trang chi tiết phim),
    # fallback về phim cuối cùng được nhắc trong hội thoại này.
    effective_movie_id = movie_id or get_last_movie_id(conversation_id)
    movie_context = get_movie_context(effective_movie_id) if effective_movie_id else ""
    tool_name = detect_tool(message)

    if tool_name:
        args: dict = {}

        if tool_name == "search_movies":
            args = {"query": message}

        elif tool_name == "filter_by_genre":
            args = {"genre": detect_genre(message)}

        elif tool_name == "recommend_similar_movies":
            target_movie_id = movie_id or get_last_movie_id(conversation_id)
            if not target_movie_id:
                reply_text = "Bạn đang muốn tìm phim giống phim nào? Hãy cho tôi biết tên phim nhé."
                append(conversation_id, "user", message)
                append(conversation_id, "assistant", reply_text)
                return reply_text, conversation_id, None
            args = {"movie_id": target_movie_id}

        elif tool_name == "get_showtimes":
            target_movie_id = movie_id or get_last_movie_id(conversation_id)
            if not target_movie_id:
                reply_text = "Bạn muốn xem lịch chiếu của phim nào? Hãy cho tôi biết tên phim nhé."
                append(conversation_id, "user", message)
                append(conversation_id, "assistant", reply_text)
                return reply_text, conversation_id, None
            args = {"movie_id": target_movie_id}

        elif tool_name == "recommend_for_user":
            if not user_id:
                reply_text = "Bạn cần đăng nhập để tôi gợi ý phim theo sở thích riêng nhé."
                append(conversation_id, "user", message)
                append(conversation_id, "assistant", reply_text)
                return reply_text, conversation_id, None
            args = {"user_id": user_id}

        elif tool_name == "search_by_director":
            name = extract_person_name(message, DIRECTOR_TRIGGER_KEYWORDS)
            args = {"name": name or message}

        elif tool_name == "search_by_actor":
            name = extract_person_name(message, ACTOR_TRIGGER_KEYWORDS)
            args = {"name": name or message}

        # get_trending, get_most_booked, get_ticket_price không cần args

        result = _safe_execute_tool(tool_name, args)

        if tool_name in MOVIE_LIST_TOOLS and isinstance(result, list) and result:
            first_id = result[0].get("movieId")
            if first_id:
                set_last_movie_id(conversation_id, first_id)
        elif tool_name == "get_showtimes" and args.get("movie_id"):
            set_last_movie_id(conversation_id, args["movie_id"])

        # Tools dùng template trực tiếp — KHÔNG qua Ollama để tránh timeout/hallucination.
        NO_LLM_TOOLS = MOVIE_LIST_TOOLS | {"get_ticket_price", "get_showtimes"}

        if _is_empty_result(result):
            final_reply = EMPTY_RESULT_MESSAGES.get(tool_name, "Không tìm thấy thông tin phù hợp.")
        elif tool_name in NO_LLM_TOOLS:
            final_reply = _fallback_template(tool_name, result, args, message)
        else:
            final_reply = _format_with_llm(message, result) or _fallback_template(tool_name, result, args, message)

        append(conversation_id, "user", message)
        append(conversation_id, "assistant", final_reply)

        data = build_response_data(tool_name, result)
        return final_reply, conversation_id, data

    # Không khớp tool nào — kiểm tra có nằm trong phạm vi hỗ trợ không trước khi gọi LLM.
    # Nếu ngoài scope (hỏi về nấu ăn, thời tiết, lập trình...) → từ chối ngay, không tốn Ollama.
    if is_out_of_scope(message) and not movie_context:
        final_reply = OUT_OF_SCOPE_REPLY
        append(conversation_id, "user", message)
        append(conversation_id, "assistant", final_reply)
        return final_reply, conversation_id, None

    # Hội thoại thường: chào hỏi, hỏi về phim đang xem (có movie_context), hỏi chung về rạp...
    messages = [{"role": "system", "content": build_system_prompt(movie_context)}]
    messages.extend(get_history(conversation_id))
    messages.append({"role": "user", "content": message})

    try:
        reply = chat_completion(messages)
        final_reply = (reply.get("content") or "").strip()
    except Exception:
        final_reply = "Xin lỗi, hệ thống AI đang gặp sự cố. Vui lòng thử lại sau."

    if not final_reply:
        final_reply = "Xin lỗi, tôi chưa có câu trả lời phù hợp."

    append(conversation_id, "user", message)
    append(conversation_id, "assistant", final_reply)

    return final_reply, conversation_id, None
