import numpy as np
import psycopg2.extras
from service.db import get_connection

TOP_K = 10
TOP_USERS = 20


def get_user_ratings(user_id: int) -> dict:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT movie_id, rating FROM reviews WHERE user_id = %s
        """, (user_id,))
        return {r["movie_id"]: r["rating"] for r in cursor.fetchall()}
    finally:
        cursor.close()
        conn.close()


def get_watched_ids(user_id: int) -> set:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT DISTINCT s.movie_id
            FROM bookings b
            JOIN showtimes s ON s.id = b.showtime_id
            WHERE b.user_id = %s AND b.status = 'PAID'
        """, (user_id,))
        return {r["movie_id"] for r in cursor.fetchall()}
    finally:
        cursor.close()
        conn.close()


def get_all_ratings() -> dict:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("SELECT user_id, movie_id, rating FROM reviews")
        result: dict[int, dict[int, float]] = {}
        for r in cursor.fetchall():
            uid = r["user_id"]
            if uid not in result:
                result[uid] = {}
            result[uid][r["movie_id"]] = r["rating"]
        return result
    finally:
        cursor.close()
        conn.close()


def get_movie_info(movie_ids: list) -> dict:
    if not movie_ids:
        return {}
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute(
            "SELECT id, title, poster_url FROM movies WHERE id = ANY(%s)",
            (movie_ids,)
        )
        return {r["id"]: dict(r) for r in cursor.fetchall()}
    finally:
        cursor.close()
        conn.close()


def get_movie_title(movie_id: int) -> str | None:
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("SELECT title FROM movies WHERE id = %s", (movie_id,))
        row = cursor.fetchone()
        return row["title"] if row else None
    finally:
        cursor.close()
        conn.close()


def cosine_similarity_dicts(a: dict, b: dict) -> float:
    common = set(a.keys()) & set(b.keys())
    if not common:
        return 0.0
    dot = sum(a[k] * b[k] for k in common)
    norm_a = np.sqrt(sum(v ** 2 for v in a.values()))
    norm_b = np.sqrt(sum(v ** 2 for v in b.values()))
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return dot / (norm_a * norm_b)


def recommend_collaborative(user_id: int) -> list:
    user_vec = get_user_ratings(user_id)
    seen = get_watched_ids(user_id) | set(user_vec.keys())

    all_ratings = get_all_ratings()

    if not user_vec:
        return _fallback(seen)

    similar_users = sorted(
        [(uid, cosine_similarity_dicts(user_vec, vec))
         for uid, vec in all_ratings.items() if uid != user_id],
        key=lambda x: x[1], reverse=True
    )[:TOP_USERS]

    # Predicted rating = sum(sim * rating) / sum(sim), normalized to 0..1 by /5
    weighted: dict[int, float] = {}
    sim_sums: dict[int, float] = {}
    contrib_counts: dict[int, int] = {}
    for uid, sim in similar_users:
        if sim <= 0:
            continue
        for mid, rating in all_ratings[uid].items():
            if mid in seen:
                continue
            weighted[mid] = weighted.get(mid, 0) + sim * rating
            sim_sums[mid] = sim_sums.get(mid, 0) + sim
            contrib_counts[mid] = contrib_counts.get(mid, 0) + 1

    movie_scores = {
        mid: min(weighted[mid] / sim_sums[mid] / 5.0, 1.0)
        for mid in weighted if sim_sums[mid] > 0
    }
    if not movie_scores:
        return _fallback(seen)

    top_ids = sorted(movie_scores, key=movie_scores.get, reverse=True)[:TOP_K]
    movies = get_movie_info(top_ids)

    anchor_title = None
    try:
        anchor_title = get_movie_title(max(user_vec, key=user_vec.get))
    except Exception:
        pass

    results = []
    for mid in top_ids:
        if mid not in movies:
            continue
        predicted = round(weighted[mid] / sim_sums[mid], 1)
        neighbor_count = contrib_counts.get(mid, 0)
        reason = f"{neighbor_count} khán giả có gu giống bạn chấm trung bình {predicted}/5"
        if anchor_title:
            reason += f" · vì bạn thích {anchor_title}"
        results.append({
            "movieId": mid,
            "title": movies[mid]["title"],
            "posterUrl": movies[mid].get("poster_url", ""),
            "similarity": round(movie_scores[mid], 4),
            "source": "collaborative",
            "predictedRating": predicted,
            "neighborCount": neighbor_count,
            "anchorTitle": anchor_title,
            "reason": reason,
        })
    return results


def _fallback(seen: set) -> list:
    """Tier 1: top movies by average review rating. Tier 2: popularity by PAID bookings."""
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    try:
        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, AVG(r.rating) as avg_score,
                   COUNT(r.id) as rating_count
            FROM movies m
            JOIN reviews r ON r.movie_id = m.id
            GROUP BY m.id, m.title, m.poster_url
            ORDER BY avg_score DESC
            LIMIT %s
        """, (TOP_K + len(seen),))
        results = []
        for row in cursor.fetchall():
            if row["id"] in seen:
                continue
            avg_rating = round(float(row["avg_score"]), 1)
            rating_count = row["rating_count"]
            results.append({
                "movieId": row["id"],
                "title": row["title"],
                "posterUrl": row.get("poster_url", ""),
                "similarity": round(float(row["avg_score"]) / 5.0, 4),
                "source": "fallback_rating",
                "avgRating": avg_rating,
                "ratingCount": rating_count,
                "reason": f"Điểm trung bình {avg_rating}/5 từ {rating_count} đánh giá thực",
            })
            if len(results) >= TOP_K:
                break
        if results:
            return results

        cursor.execute("""
            SELECT m.id, m.title, m.poster_url, COUNT(b.id) as booking_count
            FROM movies m
            JOIN showtimes s ON s.movie_id = m.id
            JOIN bookings b ON b.showtime_id = s.id AND b.status = 'PAID'
            GROUP BY m.id, m.title, m.poster_url
            ORDER BY booking_count DESC
            LIMIT %s
        """, (TOP_K + len(seen),))
        rows = [r for r in cursor.fetchall() if r["id"] not in seen]
        if not rows:
            return []
        max_count = max(r["booking_count"] for r in rows)
        return [
            {
                "movieId": row["id"],
                "title": row["title"],
                "posterUrl": row.get("poster_url", ""),
                "similarity": round(row["booking_count"] / max_count, 4),
                "source": "fallback_popularity",
                "bookingCount": row["booking_count"],
                "reason": f"Top đặt vé — {row['booking_count']} vé đã bán",
            }
            for row in rows[:TOP_K]
        ]
    finally:
        cursor.close()
        conn.close()
