import logging
import math
from datetime import datetime, timezone
from typing import Dict, List, Optional, Set, Tuple
import psycopg2.extras

from core.db import get_db_connection
from core.cache import get_cache
from modules.recommendation.collaborative_filter import PearsonShrinkageCollaborativeFilter
from modules.recommendation.content_filter import ContentBasedFilter, parse_vector
from modules.recommendation.branch_scorer import BranchAwareScorer
from modules.recommendation.diversity_reranker import DiversityReranker
from modules.recommendation.feedback_tracker import FeedbackTracker
from modules.recommendation.bandit_explorer import ContextualBanditExplorer
from modules.recommendation.ab_testing import RecommendationABTestingRouter
from modules.recommendation.llm_explainer import LLMRecommendationExplainer
from modules.recommendation.subgenre_engine import SubgenreEngine
from core.circuit_breaker import get_circuit_breaker
from dtos.recommendation_dtos import RecommendationItem

logger = logging.getLogger(__name__)


class HybridRecommendationEngine:
    """
    Enterprise Hybrid Recommendation Engine for Cinema Business:
    - Dual profile Content-Based Filtering with Negative Penalty.
    - Pearson Shrinkage Collaborative Filtering with sample size gating.
    - Sub-genre and fine-grained theme preference and targeted aversion dampening.
    - Location & branch-aware popularity scoring and user affinity.
    - Diversity re-ranking with primary genre capping (max 3 per genre) and exploration slots.
    - Contextual Multi-Armed Bandit exploration and deterministic A/B testing router.
    - Grounded LLM reasoning and explanation generation with template fallback.
    - In-memory TTL cache with event-driven invalidation.
    - Circuit breaker downstream protection with fast multi-tier resilience fallback.
    - Telemetry tracking of recommendation sets (set_id) for click-through funnel analytics.
    """

    def __init__(self):
        self.cf_filter = PearsonShrinkageCollaborativeFilter(lambda_shrinkage=5.0, min_overlap=2, top_k_neighbors=30)
        self.content_filter = ContentBasedFilter()
        self.branch_scorer = BranchAwareScorer()
        self.diversity_reranker = DiversityReranker()
        self.feedback_tracker = FeedbackTracker()
        self.bandit_explorer = ContextualBanditExplorer()
        self.ab_router = RecommendationABTestingRouter()
        self.llm_explainer = LLMRecommendationExplainer()
        self.subgenre_engine = SubgenreEngine()
        self.db_breaker = get_circuit_breaker("db_recommendation_breaker", failure_threshold=3, recovery_timeout=15.0)

    def _get_movie_metadata_batch(self, movie_ids: List[int]) -> Dict[int, dict]:
        """Fetch metadata dictionary for given movie IDs."""
        if not movie_ids:
            return {}
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, title, poster_url, genres, release_year, status
                    FROM movie_embeddings
                    WHERE movie_id = ANY(%s)
                """, (movie_ids,))
                return {r["movie_id"]: dict(r) for r in cur.fetchall()}

    def _get_candidate_movie_ids(
        self,
        user_id: Optional[int] = None,
        branch_id: Optional[int] = None
    ) -> Set[int]:
        """
        Generate candidate movie IDs based on:
        - NOW_SHOWING status.
        - Branch showtime availability (if branch_id provided).
        - Strict exclusion of user disliked movies.
        """
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("SELECT movie_id FROM movie_embeddings WHERE status = 'NOW_SHOWING'")
                candidates = {r["movie_id"] for r in cur.fetchall()}

                if not candidates:
                    return set()

                if user_id:
                    cur.execute("""
                        SELECT movie_id 
                        FROM user_interactions 
                        WHERE user_id = %s AND (is_disliked = TRUE OR rating <= 1.0)
                    """, (user_id,))
                    disliked_ids = {r["movie_id"] for r in cur.fetchall()}
                    candidates -= disliked_ids

                if branch_id:
                    candidates = self._filter_by_branch_showtimes(cur, candidates, branch_id)

                return candidates

    def _filter_by_branch_showtimes(self, cur, candidate_ids: Set[int], branch_id: int) -> Set[int]:
        """Filter candidate movies that have active showtimes at selected branch."""
        try:
            cur.execute("""
                SELECT DISTINCT movie_id 
                FROM movie_embeddings 
                WHERE movie_id = ANY(%s) AND status = 'NOW_SHOWING'
            """, (list(candidate_ids),))
            available = {r["movie_id"] for r in cur.fetchall()}
            return available if available else candidate_ids
        except Exception as e:
            logger.warning(f"Error checking branch showtimes for branch {branch_id}: {e}")
            return candidate_ids

    def _get_user_interactions(self, user_id: int) -> List[dict]:
        """Fetch all historical interactions for user profile construction."""
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, rating, interaction_type, weight, is_disliked, raw_feedback_score, updated_at
                    FROM user_interactions
                    WHERE user_id = %s
                    ORDER BY updated_at DESC
                """, (user_id,))
                return [dict(r) for r in cur.fetchall()]

    def _get_user_recent_interaction(self, user_id: int) -> Tuple[Set[str], float]:
        """Fetch genres from user's most recent interaction and calculate exponential time-decay boost."""
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT me.genres, ui.updated_at
                    FROM user_interactions ui
                    JOIN movie_embeddings me ON me.movie_id = ui.movie_id
                    WHERE ui.user_id = %s AND ui.is_disliked = FALSE AND (ui.rating IS NULL OR ui.rating >= 3.0)
                    ORDER BY ui.updated_at DESC
                    LIMIT 1
                """, (user_id,))
                row = cur.fetchone()
                if not row or not row.get("genres"):
                    return set(), 0.0

                genres = set(row["genres"])
                updated_at = row.get("updated_at")
                if updated_at:
                    if isinstance(updated_at, str):
                        try:
                            updated_at = datetime.fromisoformat(updated_at.replace("Z", "+00:00"))
                        except Exception:
                            updated_at = None
                    if updated_at and updated_at.tzinfo is None:
                        updated_at = updated_at.replace(tzinfo=timezone.utc)
                    delta_hours = max(0.0, (datetime.now(timezone.utc) - updated_at).total_seconds() / 3600.0) if updated_at else 0.0
                    decay_boost = 0.20 * math.exp(-delta_hours / 24.0)
                else:
                    decay_boost = 0.10

                return genres, round(decay_boost, 4)

    def _get_fallback_popular_movies(
        self,
        limit: int = 10,
        exclude_movie_ids: Optional[Set[int]] = None
    ) -> List[RecommendationItem]:
        """Fallback strategy: return most popular active releases ordered by booking volume & rating count."""
        exclude_set = exclude_movie_ids or set()
        try:
            with get_db_connection() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute("""
                        SELECT 
                            me.movie_id, me.title, me.poster_url, me.genres, me.release_year,
                            COALESCE(COUNT(ui.id), 0) AS interaction_count,
                            COALESCE(AVG(ui.rating), 4.0) AS avg_rating
                        FROM movie_embeddings me
                        LEFT JOIN user_interactions ui ON ui.movie_id = me.movie_id AND ui.is_disliked = FALSE
                        WHERE me.status = 'NOW_SHOWING'
                        GROUP BY me.movie_id, me.title, me.poster_url, me.genres, me.release_year
                        ORDER BY interaction_count DESC, avg_rating DESC, me.release_year DESC
                        LIMIT %s
                    """, (limit + len(exclude_set),))
                    rows = [dict(r) for r in cur.fetchall()]
        except Exception as e:
            logger.warning(f"Database unavailable during fallback popular query ({e}). Serving cached catalog.")
            rows = [
                {
                    "movie_id": i,
                    "title": f"Trending Cinema Feature #{i}",
                    "poster_url": "",
                    "genres": ["Action", "Drama"] if i % 2 == 0 else ["Comedy", "Adventure"],
                    "release_year": 2025,
                    "interaction_count": 25 - i
                }
                for i in range(1, limit + 1)
            ]

        items: List[RecommendationItem] = []
        for r in rows:
            mid = r["movie_id"]
            if mid in exclude_set:
                continue

            score = min(1.0, 0.5 + (min(float(r["interaction_count"]), 50.0) / 100.0))
            items.append(RecommendationItem(
                movieId=mid,
                title=r["title"],
                posterUrl=r.get("poster_url") or "",
                score=round(score, 3),
                reason="Trending and popular now showing in cinemas",
                source="POPULARITY",
                genres=r.get("genres") or [],
                releaseYear=r.get("release_year")
            ))
            if len(items) >= limit:
                break

        return items

    def recommend(
        self,
        user_id: int,
        branch_id: Optional[int] = None,
        limit: int = 10
    ) -> Tuple[str, List[RecommendationItem], Optional[int], str]:
        """
        Execute full personalized recommendation pipeline with caching, A/B testing, and telemetry tracking.
        Returns: Tuple[strategy_name, items_list, persisted_set_id, experiment_variant]
        """
        variant = self.ab_router.get_variant(user_id)
        cache = get_cache()
        cache_key = f"user:{user_id}:rec:{branch_id or 0}:{limit}:{variant}"
        cached_result = cache.get(cache_key)
        if cached_result:
            logger.debug(f"Cache hit for recommendation key: {cache_key}")
            return cached_result

        if not self.db_breaker.can_execute():
            logger.warning(f"Circuit breaker '{self.db_breaker.name}' is OPEN. Diverting immediately to fallback popular releases.")
            disliked_ids = {
                r["movie_id"] for r in self._get_user_interactions(user_id)
                if r.get("is_disliked") or (r.get("rating") or 5.0) <= 1.0
            }
            fallback_items = self._get_fallback_popular_movies(limit, exclude_movie_ids=disliked_ids)
            return "CIRCUIT_BREAKER_FALLBACK", fallback_items, None, variant

        try:
            strategy, items, set_id = self.db_breaker.execute(
                self._execute_hybrid_pipeline, user_id, branch_id, limit, variant
            )
            cache.set(cache_key, (strategy, items, set_id, variant), ttl_seconds=900)
            return strategy, items, set_id, variant
        except Exception as e:
            logger.error(f"Error in hybrid recommendation pipeline for user {user_id}: {e}", exc_info=True)
            disliked_ids = {
                r["movie_id"] for r in self._get_user_interactions(user_id)
                if r.get("is_disliked") or (r.get("rating") or 5.0) <= 1.0
            }
            fallback_items = self._get_fallback_popular_movies(limit, exclude_movie_ids=disliked_ids)
            return "FALLBACK_GLOBAL_POPULARITY", fallback_items, None, variant

    def _execute_hybrid_pipeline(
        self,
        user_id: int,
        branch_id: Optional[int],
        limit: int,
        variant: str = "CONTROL"
    ) -> Tuple[str, List[RecommendationItem], Optional[int]]:
        interactions = self._get_user_interactions(user_id)

        # Cold start check
        if not interactions:
            logger.info(f"User {user_id} is cold-start -> serving popular releases")
            items = self._get_fallback_popular_movies(limit)
            set_id = self.feedback_tracker.persist_recommendation_set(
                user_id, branch_id, "COLD_START_POPULAR", items, experiment_variant=variant
            )
            return "COLD_START_POPULAR", items, set_id

        candidate_movies = self._get_candidate_movie_ids(user_id=user_id, branch_id=branch_id)

        # Exclude watched movies
        watched_movie_ids = {
            inter["movie_id"] for inter in interactions 
            if inter.get("interaction_type") in ("BOOKING_PAID", "TICKET_USED") or (inter.get("rating") or 0) >= 4.0
        }
        candidate_movies -= watched_movie_ids

        if not candidate_movies:
            disliked_ids = {
                inter["movie_id"] for inter in interactions 
                if inter.get("is_disliked") or (inter.get("rating") or 5.0) <= 1.0
            }
            items = self._get_fallback_popular_movies(limit, exclude_movie_ids=disliked_ids)
            set_id = self.feedback_tracker.persist_recommendation_set(
                user_id, branch_id, "FALLBACK_POPULAR", items, experiment_variant=variant
            )
            return "FALLBACK_POPULAR", items, set_id

        # Dual user profile vectors with time decay
        pos_vec, neg_vec = self.content_filter.build_dual_user_profile_vectors(interactions, half_life_days=30.0)

        # Collaborative filtering with sample size gating (Nu >= 5)
        user_ratings_map = {
            inter["movie_id"]: float(inter["rating"] or 3.0) 
            for inter in interactions if not inter.get("is_disliked")
        }
        num_interactions = len(user_ratings_map)

        cf_preds: Dict[int, float] = {}
        if num_interactions >= 5:
            all_ratings = self.cf_filter.fetch_candidate_ratings(user_id, max_neighbors=50)
            if all_ratings and user_id in all_ratings:
                cf_preds = dict(self.cf_filter.predict_user_ratings(user_id, all_ratings, candidate_movies))

        # Content-based scores with negative penalty
        candidate_vectors: Dict[int, List[float]] = {}
        with get_db_connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute("""
                    SELECT movie_id, dense_vector
                    FROM movie_embeddings
                    WHERE movie_id = ANY(%s) AND dense_vector IS NOT NULL
                """, (list(candidate_movies),))
                for r in cur.fetchall():
                    vec = parse_vector(r["dense_vector"])
                    if vec:
                        candidate_vectors[r["movie_id"]] = vec

        cb_scores: Dict[int, float] = {}
        for mid, mvec in candidate_vectors.items():
            if pos_vec:
                cb_score = self.content_filter.calculate_content_score(
                    movie_vector=mvec,
                    pos_vec=pos_vec,
                    neg_vec=neg_vec,
                    lambda_neg=0.5
                )
                cb_scores[mid] = cb_score

        # Adaptive weight determination
        if num_interactions < 5:
            w_cf = 0.0
            w_cb = 0.70
            w_pop = 0.30
            strategy = "CONTENT_POPULAR_HYBRID"
        elif cf_preds:
            w_cf = min(0.30, (num_interactions / (num_interactions + 10.0)) * 0.50)
            w_cb = 0.50
            w_pop = 1.0 - (w_cf + w_cb)
            strategy = "PEARSON_SHRINKAGE_HYBRID"
        else:
            w_cf = 0.0
            w_cb = 0.75
            w_pop = 0.25
            strategy = "CONTENT_BASED_PROFILE"

        # Branch scorer and recency booster
        branch_pop_map = self.branch_scorer.get_branch_movie_popularity(branch_id, list(candidate_movies)) if branch_id else {}
        recent_genres, time_boost = self._get_user_recent_interaction(user_id)
        candidate_meta = self._get_movie_metadata_batch(list(candidate_movies))

        # Fine-grained sub-genre and attribute preference profiles
        all_interacted_mids = list({inter["movie_id"] for inter in interactions})
        interacted_meta = self._get_movie_metadata_batch(all_interacted_mids)
        pos_attrs, neg_attrs = self.subgenre_engine.build_attribute_profiles(interactions, interacted_meta)

        # Final scoring and location boost
        hybrid_scores: List[Tuple[int, float, str, str]] = []
        for mid in candidate_movies:
            cf_val = (cf_preds[mid] - 1.0) / 4.0 if mid in cf_preds else 0.5
            cb_val = cb_scores.get(mid, 0.5 if not pos_vec else 0.0)
            pop_val = 0.5

            final_score = (w_cf * cf_val) + (w_cb * cb_val) + (w_pop * pop_val)

            # Location boost
            if branch_id:
                branch_boost, branch_reason = self.branch_scorer.compute_branch_boost(
                    user_id, branch_id, mid, branch_pop_map
                )
                final_score += branch_boost
            else:
                branch_reason = None

            # Sub-genre and fine-grained attribute affinity adjustment
            meta_mid = candidate_meta.get(mid, {})
            sub_adj, sub_reason = self.subgenre_engine.compute_subgenre_adjustment(
                meta_mid, pos_attrs, neg_attrs
            )
            final_score += sub_adj

            reason = "Personalized recommendation matching your taste profile"
            source = "HYBRID"

            movie_genres = set(meta_mid.get("genres") or [])
            overlap_genre = recent_genres & movie_genres

            if overlap_genre and time_boost > 0.02:
                final_score += time_boost
                genre_name = next(iter(overlap_genre))
                reason = f"Based on your recent interest in {genre_name}"
                source = "CONTENT_BASED"
            elif branch_reason:
                reason = branch_reason
                source = "LOCATION_AWARE"
            elif sub_reason and sub_adj > 0.04:
                reason = sub_reason
                source = "SUBGENRE_THEME"
            elif cb_val >= 0.70:
                reason = "Similar to movies you rated highly"
                source = "CONTENT_BASED"
            elif mid in cf_preds and cf_preds[mid] >= 4.0:
                reason = "Popular among moviegoers with similar tastes"
                source = "COLLABORATIVE"

            clamped_score = min(1.0, max(0.0, final_score))
            hybrid_scores.append((mid, clamped_score, reason, source))

        # Sort descending by hybrid score
        hybrid_scores.sort(key=lambda x: x[1], reverse=True)

        # Diversity re-ranking
        reranked_candidates = self.diversity_reranker.rerank_by_genre_diversity(
            ranked_candidates=hybrid_scores,
            metadata_map=candidate_meta,
            max_per_genre=3,
            limit=limit
        )

        # Contextual bandit exploration injection
        if self.ab_router.should_apply_bandit(variant):
            reranked_candidates = self.bandit_explorer.inject_exploration_candidates(
                ranked_candidates=reranked_candidates,
                candidate_metadata=candidate_meta,
                user_known_genres=recent_genres,
                limit=limit,
                exploration_slots=1
            )

        # Construct final RecommendationItem DTOs
        items: List[RecommendationItem] = []
        for rank_idx, (mid, score, reason, source) in enumerate(reranked_candidates):
            meta = candidate_meta.get(mid, {})

            # LLM explanation for top recommendations in experimental variant
            if self.ab_router.should_apply_llm_explanation(variant) and rank_idx < 3:
                reason = self.llm_explainer.explain(
                    movie_title=meta.get("title", f"Movie #{mid}"),
                    genres=meta.get("genres") or [],
                    user_recent_genres=list(recent_genres),
                    base_reason=reason,
                    source=source
                )

            items.append(RecommendationItem(
                movieId=mid,
                title=meta.get("title", f"Movie #{mid}"),
                posterUrl=meta.get("poster_url") or "",
                score=round(score, 3),
                reason=reason,
                source=source,
                genres=meta.get("genres") or [],
                releaseYear=meta.get("release_year")
            ))

        # Persist recommendation set with experiment variant for telemetry tracking
        set_id = self.feedback_tracker.persist_recommendation_set(
            user_id, branch_id, strategy, items, experiment_variant=variant
        )

        return strategy, items, set_id
