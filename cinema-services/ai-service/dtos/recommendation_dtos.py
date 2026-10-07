from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional


class RecommendationItem(BaseModel):
    """Recommended movie item DTO."""
    model_config = ConfigDict(frozen=True)

    movieId: int = Field(..., description="Movie logical ID")
    title: str = Field(..., description="Movie title")
    posterUrl: Optional[str] = Field(default="", description="Movie poster URL")
    score: float = Field(..., description="Normalized recommendation score [0, 1]")
    reason: Optional[str] = Field(default="", description="Recommendation rationale")
    source: str = Field(default="HYBRID", description="Source mechanism (e.g. CONTENT_BASED, COLLABORATIVE, POPULARITY, HYBRID)")
    genres: List[str] = Field(default_factory=list, description="Movie genres")
    releaseYear: Optional[int] = Field(default=None, description="Release year")


class RecommendationResponse(BaseModel):
    """Personalized user recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    userId: int = Field(..., description="User logical ID")
    strategy: str = Field(..., description="Applied algorithm strategy")
    branchId: Optional[int] = Field(default=None, description="Applied branch filter ID if specified")
    recommendations: List[RecommendationItem] = Field(default_factory=list)


class ContentRecommendationResponse(BaseModel):
    """Similar movie content recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    movieId: int = Field(..., description="Target movie ID")
    recommendations: List[RecommendationItem] = Field(default_factory=list)


class FeedbackRequest(BaseModel):
    """User interaction and feedback submission DTO (Immutable)."""
    model_config = ConfigDict(frozen=True)

    userId: int = Field(..., description="User logical ID")
    movieId: int = Field(..., description="Movie logical ID")
    interactionType: str = Field(..., description="Interaction type (e.g. MOVIE_RATED, MOVIE_LIKED, MOVIE_DISLIKED)")
    rating: Optional[float] = Field(default=None, ge=1.0, le=5.0, description="Optional rating value [1.0, 5.0]")
    comment: Optional[str] = Field(default=None, description="Optional user review text")


class FeedbackResponseData(BaseModel):
    """Feedback submission response acknowledgement DTO (Immutable)."""
    model_config = ConfigDict(frozen=True)

    recorded: bool = Field(..., description="Whether feedback was successfully processed")
    userId: int = Field(..., description="User logical ID")
    movieId: int = Field(..., description="Movie logical ID")


class TrendingRecommendationResponse(BaseModel):
    """Trending and popular recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    branchId: Optional[int] = Field(default=None, description="Filtered branch ID")
    strategy: str = Field(default="GLOBAL_POPULARITY", description="Strategy used")
    recommendations: List[RecommendationItem] = Field(default_factory=list)
