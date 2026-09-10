"""
Chạy 1 lần để build embeddings từ PostgreSQL.
Usage: python create_embedding.py
"""
import pickle
import os
import psycopg2
import psycopg2.extras
from dotenv import load_dotenv
from sentence_transformers import SentenceTransformer
from service.db import get_connection, read_lob

load_dotenv()

MODEL_NAME = "all-MiniLM-L6-v2"
OUTPUT_PATH = "model/movie_embeddings.pkl"

conn = get_connection()
cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

cursor.execute("""
    SELECT m.id, m.title, m.description, m.director, m.main_actors, m.poster_url,
           STRING_AGG(DISTINCT g.name, ' ') as genres,
           STRING_AGG(DISTINCT a.name, ' ') as actors
    FROM movies m
    LEFT JOIN movie_genres mg ON mg.movie_id = m.id
    LEFT JOIN genres g ON g.id = mg.genre_id
    LEFT JOIN movie_actors ma ON ma.movie_id = m.id
    LEFT JOIN actors a ON a.id = ma.actor_id
    GROUP BY m.id, m.title, m.description, m.director, m.main_actors, m.poster_url
""")
movies = cursor.fetchall()
for movie in movies:
    movie["description"] = read_lob(conn, movie.get("description"))
cursor.close()
conn.close()

model = SentenceTransformer(MODEL_NAME)
embeddings = {}

for movie in movies:
    text = f"{movie.get('description') or ''} {movie.get('director') or ''} {movie.get('actors') or ''} {movie.get('genres') or ''}"
    vec = model.encode(text, convert_to_numpy=True)
    embeddings[movie["id"]] = {
        "embedding": vec,
        "title": movie["title"],
        "posterUrl": movie.get("poster_url", "") or ""
    }

os.makedirs("model", exist_ok=True)
with open(OUTPUT_PATH, "wb") as f:
    pickle.dump(embeddings, f)

print(f"Done: {len(embeddings)} movies -> {OUTPUT_PATH}")
