import logging
import numpy as np
from typing import List, Dict, Tuple
import psycopg2.extras
from core.db import get_db_connection
from core.shared_embeddings import get_embedding_service
from modules.recommendation.content_filter import cosine_similarity
from dtos.search_dtos import SearchResultItem

logger = logging.getLogger(__name__)


class SearchRoutes:
    """
    Search routes R0, R1, and R2:
    - R0: First-stage retriever (hybrid keyword + dense vector).
    - R1: Lightweight neural re-ranker (cross-scoring candidate re-ranking).
    - R2: Late-interaction re-ranker (token-level MaxSim interaction).
    """

    def __init__(self):
        self.embedding_svc = get_embedding_service()

    def route_r0_first_stage(self, query: str, limit: int = 10) -> List[SearchResultItem]:
        """Route R0: Fast first-stage retriever with status filtering and dense cosine similarity."""
        q_vec = self.embedding_svc.encode(query)
        q_words = set(query.lower().split())

        candidates: List[SearchResultItem] = []
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                # Query only NOW_SHOWING movies to optimize scanning
                cur.execute("""
                    SELECT movie_id, title, description, genres, director, actors,
                           poster_url, release_year, dense_vector
                    FROM movie_embeddings
                    WHERE status = 'NOW_SHOWING'
                """)
                for row in cur.fetchall():
                    mid = row["movie_id"]
                    title = row["title"]
                    vec = row["dense_vector"]
                    if isinstance(vec, str):
                        try:
                            # If returned as pgvector string format '[0.1, 0.2, ...]'
                            import json
                            vec = json.loads(vec)
                        except Exception:
                            vec = [float(x.strip()) for x in vec.strip("[]").split(",") if x.strip()]

                    # Cosine similarity on dense representation
                    cos_sim = cosine_similarity(q_vec, vec) if vec else 0.0

                    # Keyword overlap bonus
                    text_corpus = (f"{title} {row.get('description') or ''} {row.get('director') or ''} "
                                   f"{' '.join(row.get('genres') or [])}").lower()
                    overlap = sum(1 for w in q_words if w in text_corpus)
                    kw_score = overlap / max(1, len(q_words))

                    final_r0 = 0.6 * cos_sim + 0.4 * kw_score

                    candidates.append(SearchResultItem(
                        movieId=mid,
                        title=title,
                        posterUrl=row.get("poster_url") or "",
                        score=round(float(final_r0), 4),
                        genres=row.get("genres") or [],
                        overview=row.get("description") or "",
                        releaseYear=row.get("release_year")
                    ))

        candidates.sort(key=lambda x: x.score, reverse=True)
        return candidates[:limit]

    def route_r1_lightweight_reranker(
        self,
        query: str,
        candidates: List[SearchResultItem] = None,
        limit: int = 10
    ) -> List[SearchResultItem]:
        """Route R1: Cross-scoring re-ranker reusing first-stage candidates."""
        r0_candidates = candidates if candidates is not None else self.route_r0_first_stage(query, limit=limit * 2)
        if not r0_candidates:
            return []

        q_lower = query.lower()
        reranked: List[SearchResultItem] = []
        for item in r0_candidates:
            bonus = 0.0
            if item.title.lower() in q_lower or q_lower in item.title.lower():
                bonus += 0.25
            if any(g.lower() in q_lower for g in item.genres):
                bonus += 0.15

            new_score = min(1.0, item.score * 0.8 + bonus)
            reranked.append(SearchResultItem(
                movieId=item.movieId,
                title=item.title,
                posterUrl=item.posterUrl,
                score=round(float(new_score), 4),
                genres=item.genres,
                overview=item.overview,
                releaseYear=item.releaseYear
            ))

        reranked.sort(key=lambda x: x.score, reverse=True)
        return reranked[:limit]

    def route_r2_heavy_adap_colbert(
        self,
        query: str,
        candidates: List[SearchResultItem] = None,
        limit: int = 10
    ) -> List[SearchResultItem]:
        """Route R2: Late-interaction token-level MaxSim re-ranker reusing first-stage candidates."""
        r0_candidates = candidates if candidates is not None else self.route_r0_first_stage(query, limit=limit * 3)
        if not r0_candidates:
            return []

        q_tokens = query.lower().split()
        reranked: List[SearchResultItem] = []

        for item in r0_candidates:
            doc_text = f"{item.title} {item.overview} {' '.join(item.genres)}".lower()
            doc_tokens = doc_text.split()

            # MaxSim token-level late interaction
            max_sim_scores = []
            for qt in q_tokens:
                token_sims = [1.0 if qt == dt else (0.6 if qt in dt or dt in qt else 0.0) for dt in doc_tokens]
                max_sim_scores.append(max(token_sims) if token_sims else 0.0)

            late_interaction_score = sum(max_sim_scores) / max(1, len(q_tokens))
            combined_r2 = 0.5 * item.score + 0.5 * late_interaction_score

            reranked.append(SearchResultItem(
                movieId=item.movieId,
                title=item.title,
                posterUrl=item.posterUrl,
                score=round(float(combined_r2), 4),
                genres=item.genres,
                overview=item.overview,
                releaseYear=item.releaseYear
            ))

        reranked.sort(key=lambda x: x.score, reverse=True)
        return reranked[:limit]
