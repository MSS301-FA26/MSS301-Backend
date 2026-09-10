#!/usr/bin/env python3
"""
Regenerate a smaller CinemaAI synthetic dataset for testing.

This utility intentionally creates fictional users, movies, and interactions.
It must not be described as observed real-world data in a publication.
"""

import argparse
import csv
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path

GENRES = [
    "Action", "Adventure", "Animation", "Children", "Comedy", "Crime",
    "Documentary", "Drama", "Fantasy", "Film-Noir", "Horror", "Musical",
    "Mystery", "Romance", "Sci-Fi", "Thriller", "War", "Western"
]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="./synthetic_cinemaai_small")
    parser.add_argument("--seed", type=int, default=20260717)
    parser.add_argument("--users", type=int, default=100)
    parser.add_argument("--movies", type=int, default=300)
    parser.add_argument("--ratings-per-user", type=int, default=50)
    args = parser.parse_args()

    rng = random.Random(args.seed)
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)

    movies = []
    movie_genres = {}
    for movie_id in range(1, args.movies + 1):
        genres = rng.sample(GENRES, rng.choice([1, 2, 2, 3]))
        movie_genres[movie_id] = genres
        movies.append({
            "movie_id": movie_id,
            "title": f"Synthetic Movie {movie_id:04d}",
            "release_year": rng.randint(1980, 2025),
            "genres": "|".join(genres),
            "synthetic_flag": True,
        })

    users = []
    preferences = {}
    for user_id in range(1, args.users + 1):
        preferred = rng.sample(GENRES, 4)
        preferences[user_id] = set(preferred)
        users.append({
            "user_id": user_id,
            "preferred_genres": "|".join(preferred),
            "synthetic_flag": True,
        })

    start = datetime(2021, 1, 1, tzinfo=timezone.utc)
    rows = []
    for user_id in range(1, args.users + 1):
        selected = rng.sample(
            range(1, args.movies + 1),
            min(args.ratings_per_user, args.movies),
        )
        for movie_id in selected:
            overlap = len(preferences[user_id].intersection(movie_genres[movie_id]))
            rating = max(0.5, min(5.0, round((2.5 + 0.8 * overlap + rng.gauss(0, 0.7)) * 2) / 2))
            timestamp = start + timedelta(days=rng.randint(0, 1825))
            rows.append({
                "user_id": user_id,
                "movie_id": movie_id,
                "rating": rating,
                "timestamp": int(timestamp.timestamp()),
                "synthetic_flag": True,
            })

    def write_csv(name, fieldnames, data):
        with (output / name).open("w", newline="", encoding="utf-8") as file:
            writer = csv.DictWriter(file, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(data)

    write_csv("movies.csv", movies[0].keys(), movies)
    write_csv("users.csv", users[0].keys(), users)
    write_csv("ratings.csv", rows[0].keys(), rows)
    print(f"Created {len(movies)} movies, {len(users)} users, {len(rows)} ratings.")

if __name__ == "__main__":
    main()
