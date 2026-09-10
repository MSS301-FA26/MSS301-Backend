import psycopg2.extras
from service.db import get_connection, read_lob


def get_movie_context(movie_id: int) -> str:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.title, m.description, m.director, m.main_actors,
                   m.duration_minutes, m.release_date, m.language, m.status,
                   AVG(r.rating) AS avg_rating
            FROM movies m
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            WHERE m.id = %s
            GROUP BY m.id, m.title, m.description, m.director, m.main_actors,
                     m.duration_minutes, m.release_date, m.language, m.status
        """, (movie_id,))
        movie = cursor.fetchone()
        if not movie:
            return ""
        movie = dict(movie)
        movie["description"] = read_lob(conn, movie.get("description"))

        cursor.execute("""
            SELECT g.name FROM genres g
            JOIN movie_genres mg ON mg.genre_id = g.id
            WHERE mg.movie_id = %s
        """, (movie_id,))
        genres = ", ".join(r["name"] for r in cursor.fetchall())

        cursor.execute("""
            SELECT s.start_time, c.name AS cinema_name, r.name AS room_name
            FROM showtimes s
            JOIN rooms r ON r.id = s.room_id
            JOIN cinemas c ON c.id = r.cinema_id
            WHERE s.movie_id = %s AND s.status = 'OPEN' AND s.start_time >= NOW()
            ORDER BY s.start_time
            LIMIT 5
        """, (movie_id,))
        showtimes = cursor.fetchall()

        lines = [
            f"Phim: {movie['title']}",
            f"Thể loại: {genres}",
            f"Mô tả: {movie['description'] or 'Chưa có'}",
            f"Đạo diễn: {movie['director'] or 'Chưa có'}",
            f"Diễn viên: {movie['main_actors'] or 'Chưa có'}",
            f"Thời lượng: {movie['duration_minutes']} phút" if movie['duration_minutes'] else "",
            f"Ngôn ngữ: {movie['language'] or 'Chưa có'}",
            f"Trạng thái: {movie['status']}",
            f"Điểm đánh giá: {round(float(movie['avg_rating']), 1)}/5" if movie['avg_rating'] else "",
        ]

        if showtimes:
            lines.append("Lịch chiếu sắp tới:")
            for s in showtimes:
                lines.append(f"  - {s['start_time']:%d/%m %H:%M} | {s['cinema_name']} - {s['room_name']}")

        return "\n".join(l for l in lines if l)
    finally:
        cursor.close()
        conn.close()


def build_system_prompt(movie_context: str) -> str:
    if movie_context:
        return (
            "Bạn là chatbot của rạp chiếu phim CinePremier.\n"
            "QUY TẮC BẮT BUỘC:\n"
            "1. Bạn đang có \"Dữ liệu phim\" bên dưới — hãy TỰ DO nói về phim đó: "
            "   tên phim, diễn viên, đạo diễn, nội dung, lịch chiếu... đều được phép.\n"
            "2. CHỈ dùng thông tin từ \"Dữ liệu phim\". Không thêm, suy diễn, hoặc nhắc "
            "   phim KHÁC không có trong dữ liệu.\n"
            "3. Nếu người dùng hỏi thứ gì ngoài dữ liệu phim đã cung cấp: "
            "   trả lời \"Tôi chưa có thông tin đó về phim này.\"\n"
            "4. Trả lời ngắn gọn, tự nhiên, bằng tiếng Việt.\n"
            f"\nDữ liệu phim người dùng đang xem:\n{movie_context}"
        )
    return (
        "Bạn là chatbot của rạp chiếu phim CinePremier.\n"
        "QUY TẮC BẮT BUỘC — vi phạm bất kỳ quy tắc nào là sai:\n"
        "1. Không có \"Dữ liệu phim\" nào được cung cấp trong lần này.\n"
        "2. TUYỆT ĐỐI KHÔNG tự nghĩ ra, liệt kê hay gợi ý tên phim, diễn viên, đạo diễn "
        "   nào từ trí nhớ — dù người dùng yêu cầu. Hallucinate tên phim là sai nghiêm trọng.\n"
        "3. Nếu người dùng hỏi về phim cụ thể: trả lời \"Tôi chưa có thông tin về phim này. "
        "   Bạn có thể dùng chức năng tìm kiếm hoặc cho tôi biết tên phim cụ thể hơn nhé.\"\n"
        "4. Nếu người dùng hỏi danh sách / gợi ý phim: hướng dẫn dùng chức năng tìm kiếm, "
        "   KHÔNG tự liệt kê.\n"
        "5. Trả lời ngắn gọn, tự nhiên, bằng tiếng Việt."
    )
