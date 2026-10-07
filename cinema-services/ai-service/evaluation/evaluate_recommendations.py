import math
import logging
from typing import Dict, List, Set, Tuple
import numpy as np

from modules.recommendation.content_filter import cosine_similarity

logger = logging.getLogger(__name__)


def precision_at_k(recommended: List[int], relevant: Set[int], k: int = 10) -> float:
    """Calculate Precision@K: proportion of recommended items in top K that are relevant."""
    if k <= 0 or not recommended:
        return 0.0
    top_k = recommended[:k]
    hits = len(set(top_k) & set(relevant))
    return float(hits / k)


def recall_at_k(recommended: List[int], relevant: Set[int], k: int = 10) -> float:
    """Calculate Recall@K: proportion of relevant items captured in top K recommendations."""
    if not relevant or not recommended or k <= 0:
        return 0.0
    top_k = recommended[:k]
    hits = len(set(top_k) & set(relevant))
    return float(hits / len(relevant))


def ndcg_at_k(
    recommended: List[int],
    relevance_scores: Dict[int, float],
    k: int = 10
) -> float:
    """
    Calculate Normalized Discounted Cumulative Gain (NDCG@K).
    Uses logarithmic position discount: rank position discount factor = log2(rank + 1).
    """
    if k <= 0 or not recommended or not relevance_scores:
        return 0.0

    top_k = recommended[:k]
    dcg = 0.0
    for idx, item in enumerate(top_k):
        rel = relevance_scores.get(item, 0.0)
        if rel > 0:
            dcg += (2.0 ** rel - 1.0) / math.log2(idx + 2)

    # Ideal DCG: sort relevance scores descending
    ideal_scores = sorted(relevance_scores.values(), reverse=True)[:k]
    idcg = 0.0
    for idx, rel in enumerate(ideal_scores):
        if rel > 0:
            idcg += (2.0 ** rel - 1.0) / math.log2(idx + 2)

    if idcg == 0.0:
        return 0.0

    return float(dcg / idcg)


def catalog_coverage(
    all_recommendations: List[List[int]],
    catalog_movie_ids: Set[int]
) -> float:
    """
    Calculate catalog coverage: percentage of unique catalog items recommended across all users.
    """
    if not catalog_movie_ids:
        return 0.0

    recommended_unique = set()
    for rec_list in all_recommendations:
        recommended_unique.update(rec_list)

    covered = len(recommended_unique & catalog_movie_ids)
    return float((covered / len(catalog_movie_ids)) * 100.0)


def intra_list_diversity(vectors: List[List[float]]) -> float:
    """
    Calculate Intra-List Diversity (ILD) as mean pairwise cosine distance:
    ILD = 2 / (N * (N - 1)) * sum_{i < j} (1 - Cosine(v_i, v_j)).
    """
    n = len(vectors)
    if n < 2:
        return 0.0

    distances = []
    for i in range(n):
        for j in range(i + 1, n):
            sim = cosine_similarity(vectors[i], vectors[j])
            distances.append(max(0.0, 1.0 - sim))

    return round(float(np.mean(distances)), 4) if distances else 0.0


def run_benchmark():
    """Run simulated offline evaluation metrics comparison."""
    # Synthetic evaluation scenario: 10 test users, 100 movie catalog
    catalog = set(range(1, 101))
    
    # Ground truth: relevant items for 5 test users
    ground_truth = {
        1: {10, 12, 15, 20},
        2: {5, 6, 7, 8, 9},
        3: {50, 51, 52},
        4: {30, 31, 32, 33, 34},
        5: {80, 81}
    }

    relevance_scores = {
        1: {10: 5.0, 12: 4.0, 15: 4.5, 20: 3.0},
        2: {5: 5.0, 6: 4.0, 7: 4.0, 8: 3.5, 9: 5.0},
        3: {50: 4.0, 51: 5.0, 52: 3.0},
        4: {30: 5.0, 31: 4.0, 32: 4.0, 33: 5.0, 34: 4.5},
        5: {80: 5.0, 81: 4.0}
    }

    # Model predictions
    predictions = {
        1: [10, 12, 15, 99, 98, 97, 20, 96, 95, 94],
        2: [5, 6, 7, 8, 99, 98, 97, 96, 95, 9],
        3: [50, 99, 51, 98, 97, 52, 96, 95, 94, 93],
        4: [30, 31, 32, 33, 34, 99, 98, 97, 96, 95],
        5: [80, 81, 99, 98, 97, 96, 95, 94, 93, 92]
    }

    precisions_5 = [precision_at_k(predictions[u], ground_truth[u], k=5) for u in ground_truth]
    recalls_5 = [recall_at_k(predictions[u], ground_truth[u], k=5) for u in ground_truth]
    ndcgs_5 = [ndcg_at_k(predictions[u], relevance_scores[u], k=5) for u in ground_truth]

    precisions_10 = [precision_at_k(predictions[u], ground_truth[u], k=10) for u in ground_truth]
    recalls_10 = [recall_at_k(predictions[u], ground_truth[u], k=10) for u in ground_truth]
    ndcgs_10 = [ndcg_at_k(predictions[u], relevance_scores[u], k=10) for u in ground_truth]

    coverage = catalog_coverage(list(predictions.values()), catalog)

    report = {
        "mean_precision_at_5": round(float(np.mean(precisions_5)), 4),
        "mean_recall_at_5": round(float(np.mean(recalls_5)), 4),
        "mean_ndcg_at_5": round(float(np.mean(ndcgs_5)), 4),
        "mean_precision_at_10": round(float(np.mean(precisions_10)), 4),
        "mean_recall_at_10": round(float(np.mean(recalls_10)), 4),
        "mean_ndcg_at_10": round(float(np.mean(ndcgs_10)), 4),
        "catalog_coverage_pct": round(coverage, 2)
    }
    return report


if __name__ == "__main__":
    results = run_benchmark()
    logger.info(f"Evaluation benchmark results: {results}")
