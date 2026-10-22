import logging
import math
from datetime import datetime, timezone
from typing import Dict, List, Optional, Set, Tuple

logger = logging.getLogger(__name__)


class SubgenreEngine:
    """
    Fine-grained preference and aversion engine for granular movie attributes:
    - Analyzes sub-genres, themes, keywords, directors, and actors.
    - Separates positive affinity from targeted aversion.
    - Prevents aversion leakage: penalizes specific unwanted sub-themes (e.g. Gore/Slasher)
      without purging the broader parent genre (e.g. Horror or Psychological Mystery).
    """

    def __init__(self, half_life_days: float = 30.0, aversion_penalty_weight: float = 0.35):
        self.half_life_days = half_life_days
        self.aversion_penalty_weight = aversion_penalty_weight

    def extract_attributes(self, movie_meta: dict) -> Set[str]:
        """
        Extract normalized tokens representing sub-genres, themes, director, and actors.
        Preserves case-insensitive semantic tags.
        """
        attributes: Set[str] = set()

        # Extract parent genres
        raw_genres = movie_meta.get("genres") or []
        for g in raw_genres:
            if g and isinstance(g, str):
                attributes.add(f"genre:{g.strip().lower()}")

        # Extract director
        director = movie_meta.get("director")
        if director and isinstance(director, str) and director.strip():
            attributes.add(f"director:{director.strip().lower()}")

        # Extract actors
        actors = movie_meta.get("actors")
        if actors:
            if isinstance(actors, list):
                for a in actors:
                    if a and isinstance(a, str):
                        attributes.add(f"actor:{a.strip().lower()}")
            elif isinstance(actors, str):
                for a in actors.split(","):
                    if a.strip():
                        attributes.add(f"actor:{a.strip().lower()}")

        # Extract explicit subgenres, tags, and keywords from dynamic movie metadata
        for field_name, prefix in [("subgenres", "subgenre"), ("tags", "tag"), ("keywords", "theme")]:
            field_val = movie_meta.get(field_name)
            if field_val:
                items = field_val if isinstance(field_val, list) else [x.strip() for x in str(field_val).split(",")]
                for item in items:
                    if item and isinstance(item, str) and item.strip():
                        attributes.add(f"{prefix}:{item.strip().lower()}")

        # Dynamically decompose compound genres into granular sub-attributes (e.g. 'Psychological Thriller' -> 'psychological', 'thriller')
        for g in raw_genres:
            if g and isinstance(g, str):
                parts = [p.strip().lower() for p in g.replace("/", " ").replace("-", " ").split() if len(p.strip()) >= 3]
                if len(parts) > 1:
                    for part in parts:
                        attributes.add(f"subgenre:{part}")

        return attributes


    def build_attribute_profiles(
        self,
        interactions: List[dict],
        movies_meta: Dict[int, dict]
    ) -> Tuple[Dict[str, float], Dict[str, float]]:
        """
        Construct time-decayed positive affinity and negative aversion profiles across fine attributes.
        """
        positive_profile: Dict[str, float] = {}
        negative_profile: Dict[str, float] = {}

        now = datetime.now(timezone.utc)

        for inter in interactions:
            movie_id = inter.get("movie_id")
            meta = movies_meta.get(movie_id)
            if not meta:
                continue

            attrs = self.extract_attributes(meta)
            if not attrs:
                continue

            updated_at = inter.get("updated_at")
            if updated_at:
                if isinstance(updated_at, str):
                    try:
                        updated_at = datetime.fromisoformat(updated_at.replace("Z", "+00:00"))
                    except Exception:
                        updated_at = None
                if updated_at and updated_at.tzinfo is None:
                    updated_at = updated_at.replace(tzinfo=timezone.utc)
                delta_days = max(0.0, (now - updated_at).total_seconds() / 86400.0) if updated_at else 0.0
            else:
                delta_days = 0.0

            decay = math.exp(-delta_days / self.half_life_days)
            rating = float(inter.get("rating") or 3.0)
            is_disliked = bool(inter.get("is_disliked"))
            interaction_type = str(inter.get("interaction_type") or "")
            raw_feedback = float(inter.get("raw_feedback_score") or 0.0)

            # Negative interaction creates targeted aversion for that movie's specific attributes
            if is_disliked or rating <= 2.0 or raw_feedback < 0:
                penalty_weight = 1.0 if is_disliked else ((3.0 - rating) / 2.0)
                eff_weight = penalty_weight * decay
                for attr in attrs:
                    negative_profile[attr] = negative_profile.get(attr, 0.0) + eff_weight

            # Positive interaction reinforces affinity for that movie's attributes
            elif rating >= 4.0 or interaction_type in ("BOOKING_PAID", "TICKET_USED", "MOVIE_LIKED") or raw_feedback > 0:
                score_weight = (rating / 5.0) if rating >= 4.0 else 1.0
                eff_weight = score_weight * decay
                for attr in attrs:
                    positive_profile[attr] = positive_profile.get(attr, 0.0) + eff_weight

        return positive_profile, negative_profile

    def compute_subgenre_adjustment(
        self,
        movie_meta: dict,
        positive_profile: Dict[str, float],
        negative_profile: Dict[str, float]
    ) -> Tuple[float, Optional[str]]:
        """
        Calculate net attribute score modifier with targeted aversion penalty.
        Returns:
            adjustment: float in [-0.4, +0.3]
            explanation: optional string describing the primary attribute match
        """
        movie_attrs = self.extract_attributes(movie_meta)
        if not movie_attrs:
            return 0.0, None

        pos_score = sum(positive_profile.get(a, 0.0) for a in movie_attrs)
        neg_score = sum(negative_profile.get(a, 0.0) for a in movie_attrs)

        # Normalize relative to attribute count
        norm_pos = min(0.25, (pos_score / (len(movie_attrs) + 2.0)) * 0.15)
        norm_neg = min(0.40, (neg_score / (len(movie_attrs) + 2.0)) * self.aversion_penalty_weight)

        net_adjustment = norm_pos - norm_neg

        # Identify prominent matched attribute dynamically for explanation
        explanation = None
        if norm_pos > 0.05:
            matched_themes = [a.replace("theme:", "").title() for a in movie_attrs if a in positive_profile and a.startswith("theme:")]
            matched_subgenres = [a.replace("subgenre:", "").title() for a in movie_attrs if a in positive_profile and a.startswith("subgenre:")]
            matched_directors = [a.replace("director:", "").title() for a in movie_attrs if a in positive_profile and a.startswith("director:")]
            matched_actors = [a.replace("actor:", "").title() for a in movie_attrs if a in positive_profile and a.startswith("actor:")]

            if matched_subgenres:
                explanation = f"Matches your interest in {matched_subgenres[0]} films"
            elif matched_themes:
                explanation = f"Matches your interest in {matched_themes[0]} themes"
            elif matched_directors:
                explanation = f"Directed by {matched_directors[0]} whose films you enjoyed"
            elif matched_actors:
                explanation = f"Featuring {matched_actors[0]} whose performances you liked"

        return round(float(net_adjustment), 3), explanation
