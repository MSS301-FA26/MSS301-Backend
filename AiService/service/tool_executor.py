import psycopg2.extras
from service.db import get_connection
from service.content_recommend import recommend_content, search_movies, filter_movies_by_genre
from service.collaborative_recommend import recommend_collaborative


def get_showtimes(movie_id: int) -> list:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT s.id AS showtime_id, s.start_time, s.end_time,
                   c.name AS cinema_name, r.name AS room_name, r.room_type
            FROM showtimes s
            JOIN rooms r ON r.id = s.room_id
            JOIN cinemas c ON c.id = r.cinema_id
            WHERE s.movie_id = %s AND s.status = 'OPEN' AND s.start_time >= NOW()
            ORDER BY s.start_time
            LIMIT 10
        """, (movie_id,))
        return [dict(r) for r in cursor.fetchall()]
    finally:
        cursor.close()
        conn.close()


def get_ticket_price() -> list:
    """Lấy giá vé từ showtime — ưu tiên suất OPEN sắp chiếu, fallback suất gần nhất bất kỳ."""
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT adult_standard_price, adult_vip_price, adult_couple_price,
                   child_standard_price,  child_vip_price,  child_couple_price,
                   student_standard_price, student_vip_price, student_couple_price,
                   base_price
            FROM showtimes
            WHERE status = 'OPEN' AND start_time >= NOW()
            ORDER BY start_time
            LIMIT 1
        """)
        row = cursor.fetchone()
        if row:
            return [dict(row)]
        # Fallback: không có OPEN → lấy suất gần nhất bất kể status
        cursor.execute("""
            SELECT adult_standard_price, adult_vip_price, adult_couple_price,
                   child_standard_price,  child_vip_price,  child_couple_price,
                   student_standard_price, student_vip_price, student_couple_price,
                   base_price
            FROM showtimes
            ORDER BY start_time DESC
            LIMIT 1
        """)
        row = cursor.fetchone()
        return [dict(row)] if row else []
    finally:
        cursor.close()
        conn.close()


def _movie_rows_to_list(cursor, conn) -> list:
    """Convert raw movie rows (với description là OID) sang list dict chuẩn."""
    from service.db import read_lob
    rows = [dict(r) for r in cursor.fetchall()]
    for row in rows:
        row["description"] = read_lob(conn, row.get("description"))
    return rows


def search_by_director(name: str, limit: int = 10) -> list:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, m.release_date, m.status,
                   m.description, m.trailer_url, m.director,
                   AVG(r.rating) AS avg_rating
            FROM movies m
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            WHERE m.director ILIKE %s
            GROUP BY m.id, m.title, m.poster_url, m.release_date, m.status,
                     m.description, m.trailer_url, m.director
            ORDER BY avg_rating DESC NULLS LAST
            LIMIT %s
        """, (f"%{name}%", limit))
        rows = _movie_rows_to_list(cursor, conn)
        return [_to_summary(row) for row in rows]
    finally:
        cursor.close()
        conn.close()


def search_by_actor(name: str, limit: int = 10) -> list:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        # Tìm trong bảng actors (join) và cả cột main_actors (text)
        cursor.execute("""
            SELECT DISTINCT m.id, m.title, m.poster_url, m.release_date, m.status,
                   m.description, m.trailer_url,
                   AVG(r.rating) OVER (PARTITION BY m.id) AS avg_rating
            FROM movies m
            LEFT JOIN movie_actors ma ON ma.movie_id = m.id
            LEFT JOIN actors a ON a.id = ma.actor_id
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            WHERE a.name ILIKE %s OR m.main_actors ILIKE %s
            ORDER BY avg_rating DESC NULLS LAST
            LIMIT %s
        """, (f"%{name}%", f"%{name}%", limit))
        rows = _movie_rows_to_list(cursor, conn)
        return [_to_summary(row) for row in rows]
    finally:
        cursor.close()
        conn.close()


def get_trending(limit: int = 10) -> list:
    """Phim đang hot: ưu tiên NOW_SHOWING, sắp xếp theo số lượt review gần đây + rating."""
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, m.release_date, m.status,
                   m.description, m.trailer_url,
                   COUNT(r.id) AS review_count,
                   AVG(r.rating) AS avg_rating
            FROM movies m
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            GROUP BY m.id, m.title, m.poster_url, m.release_date, m.status,
                     m.description, m.trailer_url
            ORDER BY (m.status = 'NOW_SHOWING') DESC,
                     review_count DESC,
                     avg_rating DESC NULLS LAST
            LIMIT %s
        """, (limit,))
        rows = _movie_rows_to_list(cursor, conn)
        result = [_to_summary(row) for row in rows]
        # Gắn thêm review_count để dùng trong intro
        for item, row in zip(result, rows):
            item["reviewCount"] = row.get("review_count", 0)
        return result
    finally:
        cursor.close()
        conn.close()


def get_most_booked(limit: int = 10) -> list:
    """Phim được đặt vé nhiều nhất, tính từ bảng booking_details → showtimes → movies."""
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, m.release_date, m.status,
                   m.description, m.trailer_url,
                   COUNT(bd.id) AS ticket_count,
                   AVG(r.rating) AS avg_rating
            FROM movies m
            JOIN showtimes s ON s.movie_id = m.id
            JOIN booking_details bd ON bd.showtime_id = s.id
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            GROUP BY m.id, m.title, m.poster_url, m.release_date, m.status,
                     m.description, m.trailer_url
            ORDER BY ticket_count DESC
            LIMIT %s
        """, (limit,))
        rows = _movie_rows_to_list(cursor, conn)
        result = [_to_summary(row) for row in rows]
        for item, row in zip(result, rows):
            item["ticketCount"] = row.get("ticket_count", 0)
        return result
    finally:
        cursor.close()
        conn.close()


def get_now_showing(limit: int = 50) -> list:
    """Danh sách tất cả phim đang chiếu (status = NOW_SHOWING)."""
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, m.release_date, m.status,
                   m.description, m.trailer_url, m.director,
                   AVG(r.rating) AS avg_rating
            FROM movies m
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            WHERE m.status = 'NOW_SHOWING'
            GROUP BY m.id, m.title, m.poster_url, m.release_date, m.status,
                     m.description, m.trailer_url, m.director
            ORDER BY avg_rating DESC NULLS LAST
            LIMIT %s
        """, (limit,))
        rows = _movie_rows_to_list(cursor, conn)
        return [_to_summary(row) for row in rows]
    finally:
        cursor.close()
        conn.close()


def _to_summary(row: dict) -> dict:
    from datetime import date
    release = row.get("release_date")
    year = release.year if isinstance(release, date) else None
    rating = row.get("avg_rating")
    return {
        "movieId": row["id"],
        "title": row.get("title", ""),
        "posterUrl": row.get("poster_url", ""),
        "year": year,
        "rating": round(float(rating), 1) if rating else None,
        "status": row.get("status"),
        "overview": row.get("description") or "",
        "trailerUrl": row.get("trailer_url") or "",
        "director": row.get("director") or "",
        "similarity": None,
    }


def execute_tool(name: str, arguments: dict) -> list | dict:
    if name == "search_movies":
        return search_movies(arguments["query"])
    if name == "filter_by_genre":
        return filter_movies_by_genre(arguments["genre"])
    if name == "recommend_similar_movies":
        return recommend_content(arguments["movie_id"])
    if name == "recommend_for_user":
        return recommend_collaborative(arguments["user_id"])
    if name == "get_showtimes":
        return get_showtimes(arguments["movie_id"])
    if name == "get_ticket_price":
        return get_ticket_price()
    if name == "search_by_director":
        return search_by_director(arguments["name"])
    if name == "search_by_actor":
        return search_by_actor(arguments["name"])
    if name == "get_trending":
        return get_trending()
    if name == "get_most_booked":
        return get_most_booked()
    if name == "get_now_showing":
        return get_now_showing()
    return {"error": f"Unknown tool: {name}"}
