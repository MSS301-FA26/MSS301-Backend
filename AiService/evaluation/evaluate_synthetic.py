"""
Offline evaluation of the CinemaAI recommender algorithms on the synthetic
research dataset (CinemaAI_Research_Dataset_Pack).

Models mirror the production code in service/:
  1. Popularity baseline      -> collaborative_recommend._fallback (tier 2)
  2. User-based CF (k=20)     -> collaborative_recommend.recommend_collaborative
  3. Content-based SBERT      -> content_recommend + embedding_service
     (adapted item-to-item -> per-user profile for offline ranking evaluation)

Protocol: fit on train.csv only; candidates = all movies minus the user's
train items; Top-K=10. Relevance: liked==1 (primary) and rating>=3.5 (variant).
Metrics: Precision@10, Recall@10, NDCG@10, MAP@10, catalog coverage@10;
RMSE/MAE for CF rating prediction vs global-mean and item-mean baselines.

Usage:
  python evaluate_synthetic.py --data <path to synthetic_cinemaai folder>

NOTE: dataset is fully synthetic (synthetic_flag=true). Results validate the
pipeline/algorithms only — they must not be reported as real-world audience
behavior (see the dataset pack README).
"""
import argparse
import json
import math
import os
import time

import numpy as np
import pandas as pd

TOP_K = 10
TOP_USERS = 20  # same k as service/collaborative_recommend.py
LIKE_RATING_THRESHOLD = 3.5


# ---------------------------------------------------------------- metrics ---

def precision_at_k(rec: list, relevant: set, k: int) -> float:
    return len([m for m in rec[:k] if m in relevant]) / k


def recall_at_k(rec: list, relevant: set, k: int) -> float:
    return len([m for m in rec[:k] if m in relevant]) / len(relevant)


def ndcg_at_k(rec: list, relevant: set, k: int) -> float:
    dcg = sum(1.0 / math.log2(i + 2) for i, m in enumerate(rec[:k]) if m in relevant)
    ideal = sum(1.0 / math.log2(i + 2) for i in range(min(len(relevant), k)))
    return dcg / ideal if ideal > 0 else 0.0


def ap_at_k(rec: list, relevant: set, k: int) -> float:
    hits, score = 0, 0.0
    for i, m in enumerate(rec[:k]):
        if m in relevant:
            hits += 1
            score += hits / (i + 1)
    return score / min(len(relevant), k)


# ----------------------------------------------------------------- models ---

def build_popularity_ranking(train: pd.DataFrame) -> list:
    counts = train.groupby("movie_id").size().sort_values(ascending=False)
    return counts.index.tolist()


def cosine_similarity_dicts(a: dict, b: dict) -> float:
    """Verbatim logic from service/collaborative_recommend.py."""
    common = set(a.keys()) & set(b.keys())
    if not common:
        return 0.0
    dot = sum(a[k] * b[k] for k in common)
    norm_a = math.sqrt(sum(v ** 2 for v in a.values()))
    norm_b = math.sqrt(sum(v ** 2 for v in b.values()))
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return dot / (norm_a * norm_b)


def build_cf_scores(train: pd.DataFrame) -> dict:
    """user_id -> {movie_id: predicted rating} for unseen movies,
    using the production formula: top-20 similar users, sum(sim*r)/sum(sim)."""
    ratings_by_user: dict[int, dict[int, float]] = {
        uid: dict(zip(g["movie_id"], g["rating"]))
        for uid, g in train.groupby("user_id")
    }
    predictions: dict[int, dict[int, float]] = {}
    for uid, user_vec in ratings_by_user.items():
        sims = sorted(
            ((vid, cosine_similarity_dicts(user_vec, vec))
             for vid, vec in ratings_by_user.items() if vid != uid),
            key=lambda x: x[1], reverse=True,
        )[:TOP_USERS]
        weighted: dict[int, float] = {}
        sim_sums: dict[int, float] = {}
        for vid, sim in sims:
            if sim <= 0:
                continue
            for mid, rating in ratings_by_user[vid].items():
                if mid in user_vec:
                    continue
                weighted[mid] = weighted.get(mid, 0.0) + sim * rating
                sim_sums[mid] = sim_sums.get(mid, 0.0) + sim
        predictions[uid] = {
            mid: weighted[mid] / sim_sums[mid]
            for mid in weighted if sim_sums[mid] > 0
        }
    return predictions


def build_content_scores(train: pd.DataFrame, features: pd.DataFrame) -> dict:
    """user_id -> {movie_id: cosine to user profile}. Profile = rating-weighted
    mean of SBERT embeddings of the user's liked train movies (liked==1 or
    rating>=3.5); falls back to all train movies if none liked."""
    from sentence_transformers import SentenceTransformer

    model = SentenceTransformer("all-MiniLM-L6-v2")
    movie_ids = features["movie_id"].tolist()
    t0 = time.time()
    emb = model.encode(features["content_soup"].tolist(),
                       convert_to_numpy=True, batch_size=64,
                       show_progress_bar=False)
    print(f"  encoded {len(movie_ids)} movies in {time.time() - t0:.1f}s")
    emb = emb / np.linalg.norm(emb, axis=1, keepdims=True)
    idx = {mid: i for i, mid in enumerate(movie_ids)}

    scores: dict[int, dict[int, float]] = {}
    for uid, g in train.groupby("user_id"):
        liked = g[(g["liked"] == 1) | (g["rating"] >= LIKE_RATING_THRESHOLD)]
        basis = liked if len(liked) else g
        rows = [idx[m] for m in basis["movie_id"] if m in idx]
        weights = np.array([r for m, r in zip(basis["movie_id"], basis["rating"]) if m in idx])
        profile = (emb[rows] * weights[:, None]).sum(axis=0) / weights.sum()
        profile = profile / np.linalg.norm(profile)
        sims = emb @ profile
        seen = set(g["movie_id"])
        scores[uid] = {mid: float(sims[i]) for mid, i in idx.items() if mid not in seen}
    return scores


# ------------------------------------------------------------- evaluation ---

def rank_topk(scores: dict, candidates: list, k: int) -> list:
    """Rank candidates by score desc; movies without a score go last (stable)."""
    return sorted(candidates, key=lambda m: scores.get(m, float("-inf")), reverse=True)[:k]


def evaluate_ranking(name: str, per_user_topk: dict, relevant_by_user: dict,
                     n_movies: int) -> dict:
    users = [u for u in relevant_by_user if u in per_user_topk]
    p, r, n, m = [], [], [], []
    recommended_pool = set()
    for uid in users:
        rec = per_user_topk[uid]
        rel = relevant_by_user[uid]
        recommended_pool.update(rec)
        p.append(precision_at_k(rec, rel, TOP_K))
        r.append(recall_at_k(rec, rel, TOP_K))
        n.append(ndcg_at_k(rec, rel, TOP_K))
        m.append(ap_at_k(rec, rel, TOP_K))
    out = {
        "model": name,
        "users_evaluated": len(users),
        f"precision@{TOP_K}": round(float(np.mean(p)), 4),
        f"recall@{TOP_K}": round(float(np.mean(r)), 4),
        f"ndcg@{TOP_K}": round(float(np.mean(n)), 4),
        f"map@{TOP_K}": round(float(np.mean(m)), 4),
        f"coverage@{TOP_K}": round(len(recommended_pool) / n_movies, 4),
    }
    for key, val in out.items():
        if isinstance(val, float):
            assert 0.0 <= val <= 1.0, f"{name}: {key}={val} out of [0,1]"
    return out


def evaluate_rating_prediction(train: pd.DataFrame, test: pd.DataFrame,
                               cf_predictions: dict) -> list:
    global_mean = train["rating"].mean()
    item_mean = train.groupby("movie_id")["rating"].mean().to_dict()

    def errors(predict) -> tuple:
        errs = [predict(row.user_id, row.movie_id) - row.rating
                for row in test.itertuples()]
        errs = np.array(errs)
        return (round(float(np.sqrt(np.mean(errs ** 2))), 4),
                round(float(np.mean(np.abs(errs))), 4))

    results = []
    for name, fn in [
        ("Global mean", lambda u, m: global_mean),
        ("Item mean", lambda u, m: item_mean.get(m, global_mean)),
        ("User-based CF k=20",
         lambda u, m: cf_predictions.get(u, {}).get(m, item_mean.get(m, global_mean))),
    ]:
        rmse, mae = errors(fn)
        results.append({"model": name, "rmse": rmse, "mae": mae})
    return results


def md_table(rows: list) -> str:
    cols = list(rows[0].keys())
    lines = ["| " + " | ".join(cols) + " |",
             "|" + "|".join("---" for _ in cols) + "|"]
    for row in rows:
        lines.append("| " + " | ".join(str(row[c]) for c in cols) + " |")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True,
                        help="Path to the synthetic_cinemaai folder")
    args = parser.parse_args()

    train = pd.read_csv(os.path.join(args.data, "train.csv"))
    test = pd.read_csv(os.path.join(args.data, "test.csv"))
    features = pd.read_csv(os.path.join(args.data, "movie_content_features.csv"))
    all_movies = features["movie_id"].tolist()
    print(f"train={len(train)} test={len(test)} movies={len(all_movies)} "
          f"users={train['user_id'].nunique()}")

    seen_by_user = {uid: set(g["movie_id"]) for uid, g in train.groupby("user_id")}
    candidates_by_user = {uid: [m for m in all_movies if m not in seen]
                          for uid, seen in seen_by_user.items()}

    relevance_defs = {
        "liked==1": {uid: set(g[g["liked"] == 1]["movie_id"])
                     for uid, g in test.groupby("user_id")},
        f"rating>={LIKE_RATING_THRESHOLD}": {
            uid: set(g[g["rating"] >= LIKE_RATING_THRESHOLD]["movie_id"])
            for uid, g in test.groupby("user_id")},
    }
    relevance_defs = {name: {u: r for u, r in rel.items() if r}
                      for name, rel in relevance_defs.items()}

    print("Building popularity ranking...")
    pop_ranking = build_popularity_ranking(train)
    pop_rank_index = {m: i for i, m in enumerate(pop_ranking)}
    print("Building user-based CF predictions (k=20)...")
    t0 = time.time()
    cf_scores = build_cf_scores(train)
    print(f"  done in {time.time() - t0:.1f}s")
    print("Building content-based SBERT scores...")
    content_scores = build_content_scores(train, features)

    topk_by_model = {}
    for name, scorer in [
        ("Popularity", lambda uid: {m: -pop_rank_index.get(m, len(all_movies))
                                    for m in candidates_by_user[uid]}),
        ("User-based CF k=20", lambda uid: cf_scores.get(uid, {})),
        ("Content-based SBERT", lambda uid: content_scores.get(uid, {})),
    ]:
        per_user = {}
        for uid, cands in candidates_by_user.items():
            rec = rank_topk(scorer(uid), cands, TOP_K)
            assert not (set(rec) & seen_by_user[uid]), \
                f"{name}: train item leaked into recs for user {uid}"
            per_user[uid] = rec
        topk_by_model[name] = per_user

    ranking_results = {}
    for rel_name, rel_by_user in relevance_defs.items():
        rows = [evaluate_ranking(model, per_user, rel_by_user, len(all_movies))
                for model, per_user in topk_by_model.items()]
        ranking_results[rel_name] = rows
        print(f"\n=== Ranking metrics (relevance: {rel_name}) ===")
        print(pd.DataFrame(rows).to_string(index=False))

    rating_results = evaluate_rating_prediction(train, test, cf_scores)
    print("\n=== Rating prediction (test set) ===")
    print(pd.DataFrame(rating_results).to_string(index=False))

    out_dir = os.path.dirname(os.path.abspath(__file__))
    payload = {
        "dataset": "CinemaAI Synthetic Research Dataset v1 (synthetic_flag=true)",
        "protocol": {
            "fit_on": "train.csv only",
            "top_k": TOP_K,
            "cf_neighbors": TOP_USERS,
            "candidates": "all movies minus user's train items",
        },
        "ranking": ranking_results,
        "rating_prediction": rating_results,
    }
    with open(os.path.join(out_dir, "results_synthetic.json"), "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)

    md = ["# Offline evaluation — synthetic CinemaAI dataset",
          "",
          "> Dataset is fully synthetic (`synthetic_flag=true`). Results validate",
          "> the pipeline and algorithms only; they do not represent real audience",
          "> behavior.",
          ""]
    for rel_name, rows in ranking_results.items():
        md += [f"## Ranking metrics — relevance: `{rel_name}`", "",
               md_table(rows), ""]
    md += ["## Rating prediction (RMSE / MAE)", "",
           md_table(rating_results), ""]
    with open(os.path.join(out_dir, "results_synthetic.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(md))
    print(f"\nSaved results_synthetic.json / results_synthetic.md in {out_dir}")


if __name__ == "__main__":
    main()
