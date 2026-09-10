import unicodedata as _ud

def _norm(s: str) -> str:
    return _ud.normalize("NFC", s).lower()

PRICE_KEYWORDS = [_norm(k) for k in [
    "giá vé", "giá tiền", "bao nhiêu tiền", "vé giá", "giá ghế", "giá combo",
    "mua vé", "đặt vé", "vé bao nhiêu", "tiền vé", "học sinh giá", "vip giá",
]]
# get_now_showing: liệt kê phim đang chiếu — KHÔNG cần movie_id
NOW_SHOWING_KEYWORDS = [_norm(k) for k in [
    "phim nào đang chiếu", "đang chiếu", "phim đang chiếu", "phim nào đang",
    "phim chiếu hiện tại", "đang phát chiếu", "phim hiện đang",
    "tất cả phim đang", "danh sách phim đang",
]]

# get_showtimes: lịch giờ chiếu của 1 phim cụ thể — cần movie_id
SHOWTIME_KEYWORDS = [_norm(k) for k in [
    "suất chiếu", "giờ chiếu", "lịch chiếu", "chiếu lúc", "còn suất", "suất nào",
    "chiếu mấy giờ", "còn chiếu không", "lịch phim",
    "xem lúc nào", "chiếu khi nào", "mấy giờ chiếu",
]]
SIMILAR_KEYWORDS = [_norm(k) for k in [
    "giống phim này", "giống cái này", "tương tự phim này", "phim như thế này",
    "gợi ý tương tự", "phim như vậy", "phim kiểu này", "phim dạng này",
    "xem tiếp", "phim khác tương tự",
]]
PERSONAL_KEYWORDS = [_norm(k) for k in [
    "gợi ý cho tôi", "đề xuất cho tôi", "phù hợp với tôi", "gợi ý riêng",
    "gợi ý phim cho tôi", "tôi nên xem", "nên xem gì", "xem gì bây giờ",
    "phim nào hay cho tôi", "theo sở thích", "dựa trên sở thích",
    "tôi thích xem", "phim hợp với tôi",
]]
SEARCH_KEYWORDS = [_norm(k) for k in [
    "giống", "tương tự", "tìm phim", "phim nào", "phim gì", "gợi ý phim",
    "phim hay", "phim về", "có phim", "phim nói về", "phim kể về",
    "tìm kiếm", "phim mới", "phim cũ", "phim nổi tiếng",
]]

# Ánh xạ từ khóa tiếng Việt -> tên genre thật trong bảng genres (khớp chính xác qua SQL,
# không phải semantic search mờ). Xếp theo thứ tự để cụm dài match trước cụm ngắn trùng.
GENRE_ALIASES = [
    ("khoa học viễn tưởng", "Sci-Fi"),
    ("viễn tưởng", "Sci-Fi"),
    ("hành động", "Action"),
    ("phiêu lưu", "Adventure"),
    ("hoạt hình", "Animation"),
    ("hài hước", "Comedy"),
    ("hài", "Comedy"),
    ("tội phạm", "Crime"),
    ("hình sự", "Crime"),
    ("tâm lý", "Drama"),
    ("chính kịch", "Drama"),
    ("gia đình", "Family"),
    ("nhạc kịch", "Musical"),
    ("âm nhạc", "Musical"),
    ("tình cảm", "Romance"),
    ("lãng mạn", "Romance"),
    ("giật gân", "Thriller"),
    ("kinh dị", "Thriller"),
]


def detect_genre(message: str) -> str | None:
    msg = _norm(message)
    for keyword, genre_name in GENRE_ALIASES:
        if keyword in msg:
            return genre_name
    return None


# Từ khóa xác nhận câu hỏi TRONG phạm vi hệ thống (phim / rạp / vé).
# Nếu message không khớp tool nào VÀ không chứa bất kỳ từ nào dưới đây
# → coi là ngoài phạm vi, trả message từ chối thay vì gọi Ollama.
# Từ khóa bị chặn hoàn toàn — trả về message từ chối ngay, không xử lý tiếp.
BLOCKED_KEYWORDS = [_norm(k) for k in [
    "khiêu dâm", "sex", "porn", "18+", "người lớn", "nude", "khỏa thân",
    "nội dung nhạy cảm", "phim cấp", "phim đen",
]]

BLOCKED_REPLY = (
    "Xin lỗi, tôi không thể hỗ trợ yêu cầu này. "
    "Vui lòng hỏi về phim, lịch chiếu hoặc giá vé nhé! 🍿"
)


def is_blocked(message: str) -> bool:
    msg = _norm(message)
    return any(k in msg for k in BLOCKED_KEYWORDS)


IN_SCOPE_KEYWORDS = [_norm(k) for k in [
    # Đối tượng cốt lõi
    "phim", "movie", "film",
    # Địa điểm / hạ tầng
    "rạp", "cinema", "phòng chiếu", "màn hình",
    # Giao dịch
    "vé", "ticket", "đặt", "mua", "thanh toán",
    # Lịch
    "chiếu", "lịch", "suất", "giờ",
    # Người trong phim
    "diễn viên", "đạo diễn", "actor", "director",
    # Nội dung phim
    "trailer", "nội dung", "cốt truyện", "thể loại", "genre",
    # Hành động xem phim (cả miền Nam lẫn miền Bắc)
    "xem", "coi", "nên xem", "nên coi", "có nên", "đáng xem", "đáng coi",
    "hay không", "có hay", "review", "đánh giá",
    # Hội thoại thân thiện với chatbot — vẫn cho qua
    "bạn", "bot", "popbot", "xin chào", "hello", "hi", "cảm ơn",
    "bạn có thể", "bạn làm", "giúp tôi", "hỗ trợ",
]]

OUT_OF_SCOPE_REPLY = (
    "Xin lỗi, hệ thống hiện chỉ hỗ trợ các câu hỏi liên quan đến "
    "phim, lịch chiếu và giá vé. "
    "Vui lòng thử lại với câu hỏi về phim nhé! 🍿"
)


def is_out_of_scope(message: str) -> bool:
    """Trả True nếu message không chứa bất kỳ từ khóa nào trong phạm vi hệ thống."""
    msg = _norm(message)
    return not any(k in msg for k in IN_SCOPE_KEYWORDS)


# ── Keywords cho 4 tool mới ────────────────────────────────────────────────

# Phát hiện tên sau keyword: "phim của đạo diễn [NAME]", "đạo diễn [NAME] làm"
DIRECTOR_TRIGGER_KEYWORDS = [_norm(k) for k in [
    "đạo diễn", "do đạo diễn", "phim của đạo diễn", "phim do",
    "director",
]]

ACTOR_TRIGGER_KEYWORDS = [_norm(k) for k in [
    "diễn viên", "phim của diễn viên", "phim có diễn viên",
    "phim có actor", "actor", "phim của actor",
]]

TRENDING_KEYWORDS = [_norm(k) for k in [
    "hot nhất", "đang hot", "trending", "nổi bật nhất",
    "xem nhiều nhất", "nhiều người xem", "phim nổi",
    "phim được xem nhiều", "phim đang nổi",
]]

MOST_BOOKED_KEYWORDS = [_norm(k) for k in [
    "nhiều người mua nhất", "đặt vé nhiều nhất", "bán chạy nhất",
    "vé bán chạy", "nhiều người đặt", "đặt nhiều nhất",
    "mua nhiều nhất", "doanh thu cao nhất",
]]


def extract_person_name(message: str, trigger_keywords: list) -> str | None:
    """Tách tên người (đạo diễn / diễn viên) ra khỏi câu hỏi.
    VD: 'Phim của đạo diễn Christopher Nolan' → 'Christopher Nolan'"""
    msg_lower = _norm(message)
    # Sắp xếp keyword dài trước để match cụm dài hơn trước
    for kw in sorted(trigger_keywords, key=len, reverse=True):
        if kw in msg_lower:
            idx = msg_lower.find(kw) + len(kw)
            # Lấy phần còn lại, bỏ ký tự thừa cuối
            name = message[idx:].strip().rstrip('?.,!').strip()
            # Loại bỏ các suffix không phải tên
            for suffix in [" làm", " đạo diễn", " đóng", " diễn", " thủ vai"]:
                if name.lower().endswith(suffix.strip()):
                    name = name[: -len(suffix)].strip()
            return name if len(name) >= 2 else None
    return None


def detect_tool(message: str) -> str | None:
    """Router deterministic: code quyết định gọi tool nào dựa trên từ khóa,
    KHÔNG để model tự quyết (tránh model bịa/không gọi tool khi cần).
    Trả None nếu không khớp tool nào -> để model trả lời hội thoại thường."""
    msg = _norm(message)

    if any(k in msg for k in PRICE_KEYWORDS):
        return "get_ticket_price"
    if any(k in msg for k in NOW_SHOWING_KEYWORDS):
        return "get_now_showing"
    if any(k in msg for k in SHOWTIME_KEYWORDS):
        return "get_showtimes"
    if any(k in msg for k in MOST_BOOKED_KEYWORDS):
        return "get_most_booked"
    if any(k in msg for k in TRENDING_KEYWORDS):
        return "get_trending"
    if any(k in msg for k in SIMILAR_KEYWORDS):
        return "recommend_similar_movies"
    if any(k in msg for k in PERSONAL_KEYWORDS):
        return "recommend_for_user"
    # Director / actor: phải extract được tên mới route, tránh false positive
    if any(k in msg for k in DIRECTOR_TRIGGER_KEYWORDS):
        if extract_person_name(message, DIRECTOR_TRIGGER_KEYWORDS):
            return "search_by_director"
    if any(k in msg for k in ACTOR_TRIGGER_KEYWORDS):
        if extract_person_name(message, ACTOR_TRIGGER_KEYWORDS):
            return "search_by_actor"
    if detect_genre(msg) is not None:
        return "filter_by_genre"
    if any(k in msg for k in SEARCH_KEYWORDS):
        return "search_movies"
    return None
