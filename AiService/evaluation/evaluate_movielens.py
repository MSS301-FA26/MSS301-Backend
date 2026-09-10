"""
Offline evaluation of the CinemaAI recommender algorithms on MovieLens 1M.

Same three models and the same mathematical formulas as evaluate_synthetic.py
(which mirrors the production code in service/), but implemented with
vectorized numpy because ML-1M has 6,040 users:

  1. Popularity baseline      (train interaction count)
  2. User-based CF (k=20)     cosine over rating vectors, pred = sum(sim*r)/sum(sim)
  3. Content-based SBERT      all-MiniLM-L6-v2 over "title + genres";
                              user profile = rating-weighted mean of liked items

Protocol: fit on train.csv only; candidates = all catalog movies minus the
user's train items; Top-K=10. Relevance: test items with rating >= 4
(standard practice for ML-1M explicit feedback).
Metrics: Precision@10, Recall@10, NDCG@10, MAP@10, catalog coverage@10;
RMSE/MAE for rating prediction vs global-mean and item-mean baselines.

Usage:
  python evaluate_movielens.py --data <path to movielens_1m_ready folder>
"""
import argparse
import json
import os
import time

import numpy as np
import pandas as pd

from evaluate_synthetic import (
    precision_at_k, recall_at_k, ndcg_at_k, ap_at_k, md_table,
)

TOP_K = 10
TOP_USERS = 20          # same k as service/collaborative_recommend.py
RELEVANT_RATING = 4.0   # standard "liked" threshold for ML-1M


def build_matrix(train: pd.DataFrame, user_index: dict, movie_index: dict) -> np.ndarray:
    R = np.zeros((len(user_index), len(movie_index)), dtype=np.float32)
    R[train["user_id"].map(user_index), train["movie_id"].map(movie_index)] = \
        train["rating"].astype(np.float32)
    return R


def cf_scores_matrix(R: np.ndarray) -> np.ndarray:
    """Predicted-rating matrix using the production formula, vectorized.

    sim(u,v) = cosine of the full rating vectors (zeros for unrated items give
    exactly the sum-over-common-items dot product used in the dict version).
    pred(u,m) = sum_{v in top-20, sim>0} sim*r(v,m) / sum sim  (over raters of m).
    Unpredictable entries stay at 0 and rank last (all real preds are >= 1).
    """
    norms = np.linalg.norm(R, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    Rn = R / norms
    S = Rn @ Rn.T
    np.fill_diagonal(S, -1.0)

    n_users = R.shape[0]
    preds = np.zeros_like(R)
    rated = (R > 0).astype(np.float32)
    top = np.argpartition(S, -TOP_USERS, axis=1)[:, -TOP_USERS:]
    for u in range(n_users):
        nb = top[u]
        sims = S[u, nb]
        keep = sims > 0
        nb, sims = nb[keep], sims[keep]
        if len(nb) == 0:
            continue
        weighted = sims @ R[nb]
        sim_sums = sims @ rated[nb]
        mask = sim_sums > 0
        preds[u, mask] = weighted[mask] / sim_sums[mask]
    return preds


def content_scores_matrix(train: pd.DataFrame, movies: pd.DataFrame,
                          user_index: dict, movie_index: dict) -> np.ndarray:
    from sentence_transformers import SentenceTransformer

    soup = (movies["title"].fillna("") + " " +
            movies["genres"].fillna("").str.replace("|", " ", regex=False))
    model = SentenceTransformer("all-MiniLM-L6-v2")
    t0 = time.time()
    emb = model.encode(soup.tolist(), convert_to_numpy=True, batch_size=128,
                       show_progress_bar=False)
    print(f"  encoded {len(movies)} movies in {time.time() - t0:.1f}s")
    emb = emb / np.linalg.norm(emb, axis=1, keepdims=True)

    profiles = np.zeros((len(user_index), emb.shape[1]), dtype=np.float32)
    for uid, g in train.groupby("user_id"):
        liked = g[g["rating"] >= RELEVANT_RATING]
        basis = liked if len(liked) else g
        rows = basis["movie_id"].map(movie_index).to_numpy()
        weights = basis["rating"].to_numpy(dtype=np.float32)
        profile = (emb[rows] * weights[:, None]).sum(axis=0) / weights.sum()
        norm = np.linalg.norm(profile)
        if norm > 0:
            profiles[user_index[uid]] = profile / norm
    return profiles @ emb.T


def evaluate_ranking(name: str, score_matrix: np.ndarray, seen_mask: np.ndarray,
                     relevant_by_row: dict, movie_ids: np.ndarray) -> dict:
    scores = score_matrix.copy()
    scores[seen_mask] = -np.inf
    p, r, n, m = [], [], [], []
    recommended_pool = set()
    for row, rel in relevant_by_row.items():
        top = np.argpartition(scores[row], -TOP_K)[-TOP_K:]
        top = top[np.argsort(scores[row, top])[::-1]]
        rec = movie_ids[top].tolist()
        recommended_pool.update(rec)
        p.append(precision_at_k(rec, rel, TOP_K))
        r.append(recall_at_k(rec, rel, TOP_K))
        n.append(ndcg_at_k(rec, rel, TOP_K))
        m.append(ap_at_k(rec, rel, TOP_K))
    out = {
        "model": name,
        "users_evaluated": len(relevant_by_row),
        f"precision@{TOP_K}": round(float(np.mean(p)), 4),
        f"recall@{TOP_K}": round(float(np.mean(r)), 4),
        f"ndcg@{TOP_K}": round(float(np.mean(n)), 4),
        f"map@{TOP_K}": round(float(np.mean(m)), 4),
        f"coverage@{TOP_K}": round(len(recommended_pool) / len(movie_ids), 4),
    }
    for key, val in out.items():
        if isinstance(val, float):
            assert 0.0 <= val <= 1.0, f"{name}: {key}={val} out of [0,1]"
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True,
                        help="Path to the movielens_1m_ready folder")
    args = parser.parse_args()

    train = pd.read_csv(os.path.join(args.data, "train.csv"))
    test = pd.read_csv(os.path.join(args.data, "test.csv"))
    movies = pd.read_csv(os.path.join(args.data, "movies.csv"))
    print(f"train={len(train)} test={len(test)} movies={len(movies)} "
          f"users={train['user_id'].nunique()}")

    user_ids = np.sort(train["user_id"].unique())
    movie_ids = movies["movie_id"].to_numpy()
    user_index = {u: i for i, u in enumerate(user_ids)}
    movie_index = {m: i for i, m in enumerate(movie_ids)}
    train = train[train["movie_id"].isin(movie_index)]

    R = build_matrix(train, user_index, movie_index)
    seen_mask = R > 0

    print("Building user-based CF predictions (k=20, vectorized)...")
    t0 = time.time()
    cf_preds = cf_scores_matrix(R)
    print(f"  done in {time.time() - t0:.1f}s")

    print("Building content-based SBERT scores...")
    content = content_scores_matrix(train, movies, user_index, movie_index)

    pop_counts = np.zeros(len(movie_ids), dtype=np.float32)
    counts = train.groupby("movie_id").size()
    pop_counts[[movie_index[m] for m in counts.index]] = counts.to_numpy()
    popularity = np.broadcast_to(pop_counts, R.shape).copy()

    test_known = test[test["user_id"].isin(user_index) &
                      test["movie_id"].isin(movie_index)]
    relevant_by_row = {}
    for uid, g in test_known[test_known["rating"] >= RELEVANT_RATING].groupby("user_id"):
        relevant_by_row[user_index[uid]] = set(g["movie_id"])

    ranking_rows = [
        evaluate_ranking("Popularity", popularity, seen_mask, relevant_by_row, movie_ids),
        evaluate_ranking("User-based CF k=20", cf_preds, seen_mask, relevant_by_row, movie_ids),
        evaluate_ranking("Content-based SBERT", content, seen_mask, relevant_by_row, movie_ids),
    ]
    print(f"\n=== Ranking metrics (relevance: rating>={RELEVANT_RATING:g}) ===")
    print(pd.DataFrame(ranking_rows).to_string(index=False))

    global_mean = train["rating"].mean()
    item_mean = train.groupby("movie_id")["rating"].mean().to_dict()
    u_rows = test_known["user_id"].map(user_index).to_numpy()
    m_rows = test_known["movie_id"].map(movie_index).to_numpy()
    actual = test_known["rating"].to_numpy(dtype=np.float64)
    cf_vals = cf_preds[u_rows, m_rows].astype(np.float64)
    item_fallback = np.array([item_mean.get(m, global_mean)
                              for m in test_known["movie_id"]])
    cf_vals = np.where(cf_vals > 0, cf_vals, item_fallback)

    def rmse_mae(pred):
        err = pred - actual
        return (round(float(np.sqrt(np.mean(err ** 2))), 4),
                round(float(np.mean(np.abs(err))), 4))

    rating_rows = []
    for name, pred in [("Global mean", np.full_like(actual, global_mean)),
                       ("Item mean", item_fallback),
                       ("User-based CF k=20", cf_vals)]:
        rmse, mae = rmse_mae(pred)
        rating_rows.append({"model": name, "rmse": rmse, "mae": mae})
    print("\n=== Rating prediction (test set) ===")
    print(pd.DataFrame(rating_rows).to_string(index=False))

    out_dir = os.path.dirname(os.path.abspath(__file__))
    payload = {
        "dataset": "MovieLens 1M (official GroupLens release)",
        "protocol": {
            "fit_on": "train.csv only (per-user temporal 80/10/10 split)",
            "top_k": TOP_K,
            "cf_neighbors": TOP_USERS,
            "relevance": f"test rating >= {RELEVANT_RATING:g}",
            "candidates": "full catalog minus user's train items",
        },
        "ranking": ranking_rows,
        "rating_prediction": rating_rows,
    }
    with open(os.path.join(out_dir, "results_movielens.json"), "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)
    md = ["# Offline evaluation — MovieLens 1M", "",
          f"Relevance: test rating >= {RELEVANT_RATING:g}. "
          "Per-user temporal 80/10/10 split, fit on train only.", "",
          "## Ranking metrics", "", md_table(ranking_rows), "",
          "## Rating prediction (RMSE / MAE)", "", md_table(rating_rows), ""]
    with open(os.path.join(out_dir, "results_movielens.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(md))
    print(f"\nSaved results_movielens.json / results_movielens.md in {out_dir}")


if __name__ == "__main__":
    main()
