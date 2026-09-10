#!/usr/bin/env python3
"""
Download and prepare the official MovieLens 1M dataset.

The script downloads ml-1m.zip directly from GroupLens, extracts it locally,
converts the .dat files to UTF-8 CSV, and creates a per-user temporal
80/10/10 train-validation-test split.

Run:
    python download_and_prepare_movielens_1m.py --output ./movielens_1m_ready

No third-party Python packages are required.
"""

from __future__ import annotations

import argparse
import csv
import shutil
import urllib.request
import zipfile
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

DATASET_URL = "https://files.grouplens.org/datasets/movielens/ml-1m.zip"

AGE_DESC = {
    "1": "Under 18",
    "18": "18-24",
    "25": "25-34",
    "35": "35-44",
    "45": "45-49",
    "50": "50-55",
    "56": "56+",
}
OCC_DESC = {
    "0": "other_or_unspecified",
    "1": "academic_or_educator",
    "2": "artist",
    "3": "clerical_or_admin",
    "4": "college_or_grad_student",
    "5": "customer_service",
    "6": "doctor_or_health_care",
    "7": "executive_or_managerial",
    "8": "farmer",
    "9": "homemaker",
    "10": "K-12_student",
    "11": "lawyer",
    "12": "programmer",
    "13": "retired",
    "14": "sales_or_marketing",
    "15": "scientist",
    "16": "self_employed",
    "17": "technician_or_engineer",
    "18": "tradesman_or_craftsman",
    "19": "unemployed",
    "20": "writer",
}

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="./movielens_1m_ready")
    parser.add_argument("--keep-zip", action="store_true")
    return parser.parse_args()

def download(url: str, destination: Path) -> None:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "CinemaAI-Research-Dataset-Preparation/1.0"},
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        with destination.open("wb") as output:
            shutil.copyfileobj(response, output)

def write_csv(path: Path, fieldnames: list[str], rows: list[dict]) -> None:
    with path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

def main() -> None:
    args = parse_args()
    output = Path(args.output).resolve()
    raw_dir = output / "raw"
    output.mkdir(parents=True, exist_ok=True)
    raw_dir.mkdir(parents=True, exist_ok=True)

    archive = output / "ml-1m.zip"
    print(f"Downloading official dataset to {archive}")
    download(DATASET_URL, archive)

    with zipfile.ZipFile(archive) as zip_file:
        zip_file.extractall(raw_dir)

    source = raw_dir / "ml-1m"

    movies = []
    with (source / "movies.dat").open("r", encoding="latin-1") as file:
        for line in file:
            movie_id, title, genres = line.rstrip("\n").split("::")
            year = ""
            if title.endswith(")") and "(" in title[-7:]:
                candidate = title[-5:-1]
                if candidate.isdigit():
                    year = candidate
            movies.append({
                "movie_id": int(movie_id),
                "title": title,
                "release_year": year,
                "genres": genres,
            })

    users = []
    with (source / "users.dat").open("r", encoding="latin-1") as file:
        for line in file:
            user_id, gender, age, occupation, zipcode = line.rstrip("\n").split("::")
            users.append({
                "user_id": int(user_id),
                "gender": gender,
                "age_code": int(age),
                "age_group": AGE_DESC.get(age, "unknown"),
                "occupation_code": int(occupation),
                "occupation": OCC_DESC.get(occupation, "unknown"),
                "zipcode": zipcode,
            })

    ratings = []
    grouped = defaultdict(list)
    with (source / "ratings.dat").open("r", encoding="latin-1") as file:
        for line in file:
            user_id, movie_id, rating, timestamp = line.rstrip("\n").split("::")
            timestamp_int = int(timestamp)
            row = {
                "user_id": int(user_id),
                "movie_id": int(movie_id),
                "rating": int(rating),
                "timestamp": timestamp_int,
                "datetime_utc": datetime.fromtimestamp(
                    timestamp_int, tz=timezone.utc
                ).isoformat().replace("+00:00", "Z"),
            }
            ratings.append(row)
            grouped[int(user_id)].append(row)

    rating_fields = [
        "user_id", "movie_id", "rating", "timestamp", "datetime_utc"
    ]
    write_csv(
        output / "movies.csv",
        ["movie_id", "title", "release_year", "genres"],
        movies,
    )
    write_csv(
        output / "users.csv",
        [
            "user_id", "gender", "age_code", "age_group",
            "occupation_code", "occupation", "zipcode",
        ],
        users,
    )
    write_csv(output / "ratings.csv", rating_fields, ratings)

    train, validation, test = [], [], []
    for rows in grouped.values():
        rows.sort(key=lambda row: row["timestamp"])
        n = len(rows)
        train_end = max(1, int(n * 0.80))
        validation_end = max(train_end + 1, int(n * 0.90))
        validation_end = min(validation_end, n - 1)
        train.extend(rows[:train_end])
        validation.extend(rows[train_end:validation_end])
        test.extend(rows[validation_end:])

    write_csv(output / "train.csv", rating_fields, train)
    write_csv(output / "validation.csv", rating_fields, validation)
    write_csv(output / "test.csv", rating_fields, test)

    if not args.keep_zip:
        archive.unlink(missing_ok=True)

    print(
        f"Done: {len(users):,} users, {len(movies):,} movies, "
        f"{len(ratings):,} ratings."
    )
    print(f"Output: {output}")

if __name__ == "__main__":
    main()
