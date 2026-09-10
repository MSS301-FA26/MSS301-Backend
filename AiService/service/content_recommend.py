import numpy as np
import psycopg2.extras
from service.db import get_connection, read_lob
from service.embedding_service import get_embedding_service

TOP_K = 10
TOP_K_BUFFER = 20  # lấy dư để bù cho các movieId không còn trong DB


def get_movie_data(movie_id: int) -> dict | None:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.description, m.director, m.main_actors, m.poster_url
            FROM movies m
            WHERE m.id = %s
        """, (movie_id,))
        movie = cursor.fetchone()
        if not movie:
            return None

        movie = dict(movie)
        movie["description"] = read_lob(conn, movie.get("description"))

        cursor.execute("""
            SELECT g.name FROM genres g
            JOIN movie_genres mg ON mg.genre_id = g.id
            WHERE mg.movie_id = %s
        """, (movie_id,))
        genres = [r["name"] for r in cursor.fetchall()]

        cursor.execute("""
            SELECT a.name FROM actors a
            JOIN movie_actors ma ON ma.actor_id = a.id
            WHERE ma.movie_id = %s
        """, (movie_id,))
        actors = [r["name"] for r in cursor.fetchall()]

        movie["genres"] = genres
        movie["actors"] = actors
        return movie
    finally:
        cursor.close()
        conn.close()


def build_text(movie: dict) -> str:
    genres = " ".join(movie.get("genres") or [])
    actors = " ".join(movie.get("actors") or [])
    return f"{movie.get('description') or ''} {movie.get('director') or ''} {actors} {genres}"


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    norm_a, norm_b = np.linalg.norm(a), np.linalg.norm(b)
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return float(np.dot(a, b) / (norm_a * norm_b))


def get_movies_info(movie_ids: list) -> dict:
    if not movie_ids:
        return {}
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, m.release_date, m.status,
                   m.description, m.trailer_url,
                   AVG(r.rating) AS avg_rating
            FROM movies m
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            WHERE m.id = ANY(%s)
            GROUP BY m.id, m.title, m.poster_url, m.release_date, m.status,
                     m.description, m.trailer_url
        """, (movie_ids,))
        rows = {r["id"]: dict(r) for r in cursor.fetchall()}
        for row in rows.values():
            row["description"] = read_lob(conn, row.get("description"))
        return rows
    finally:
        cursor.close()
        conn.close()


def _movie_summary(info: dict) -> dict:
    year = info["release_date"].year if info.get("release_date") else None
    rating = round(float(info["avg_rating"]), 1) if info.get("avg_rating") else None
    return {
        "title": info["title"],
        "posterUrl": info.get("poster_url", ""),
        "year": year,
        "rating": rating,
        "status": info.get("status"),
        "overview": info.get("description") or "",
        "trailerUrl": info.get("trailer_url") or "",
    }


def recommend_content(movie_id: int) -> list:
    movie = get_movie_data(movie_id)
    if not movie:
        return []

    svc = get_embedding_service()
    query_vec = svc.encode(build_text(movie))
    embeddings = svc.get_embeddings()

    scores = []
    for mid, data in embeddings.items():
        if mid == movie_id:
            continue
        sim = cosine_similarity(query_vec, data["embedding"])
        scores.append({"movieId": mid, "similarity": round(float(sim), 4)})

    scores.sort(key=lambda x: x["similarity"], reverse=True)
    candidates = scores[:TOP_K_BUFFER]

    db_info = get_movies_info([s["movieId"] for s in candidates])
    result = [
        {**_movie_summary(db_info[s["movieId"]]), "movieId": s["movieId"], "similarity": s["similarity"]}
        for s in candidates if s["movieId"] in db_info
    ]
    return result[:TOP_K]


def filter_movies_by_genre(genre_name: str, limit: int = 10) -> list:
    """Lọc phim theo thể loại CHÍNH XÁC qua bảng genres (không phải semantic search).
    Ưu tiên phim đang chiếu (NOW_SHOWING), rồi tới điểm đánh giá cao."""
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, m.release_date, m.status,
                   m.description, m.trailer_url,
                   AVG(r.rating) AS avg_rating
            FROM movies m
            JOIN movie_genres mg ON mg.movie_id = m.id
            JOIN genres g ON g.id = mg.genre_id
            LEFT JOIN reviews r ON r.movie_id = m.id AND r.status = 'VISIBLE'
            WHERE g.name ILIKE %s
            GROUP BY m.id, m.title, m.poster_url, m.release_date, m.status,
                     m.description, m.trailer_url
            ORDER BY (m.status = 'NOW_SHOWING') DESC, avg_rating DESC NULLS LAST
            LIMIT %s
        """, (genre_name, limit))
        rows = [dict(r) for r in cursor.fetchall()]
        for row in rows:
            row["description"] = read_lob(conn, row.get("description"))
        return [
            {**_movie_summary(row), "movieId": row["id"], "similarity": None}
            for row in rows
        ]
    finally:
        cursor.close()
        conn.close()


SEARCH_SIMILARITY_THRESHOLD = 0.25  # dưới ngưỡng này coi là không liên quan

def search_movies(query: str, limit: int = 5) -> list:
    """Tìm phim theo mô tả tự nhiên (VD: 'phim hành động kiểu mind-bending').
    Khác recommend_content: không cần movie_id gốc, encode thẳng câu query."""
    svc = get_embedding_service()
    query_vec = svc.encode(query)
    embeddings = svc.get_embeddings()

    scores = []
    for mid, data in embeddings.items():
        sim = cosine_similarity(query_vec, data["embedding"])
        scores.append({"movieId": mid, "similarity": round(float(sim), 4)})

    scores.sort(key=lambda x: x["similarity"], reverse=True)

    # Lọc theo threshold — tránh trả về phim không liên quan chỉ vì DB ít phim
    above_threshold = [s for s in scores if s["similarity"] >= SEARCH_SIMILARITY_THRESHOLD]
    candidates = (above_threshold if above_threshold else scores[:2])[:limit]

    db_info = get_movies_info([s["movieId"] for s in candidates])
    return [
        {**_movie_summary(db_info[s["movieId"]]), "movieId": s["movieId"], "similarity": s["similarity"]}
        for s in candidates if s["movieId"] in db_info
    ]
