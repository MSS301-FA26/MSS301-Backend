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
    genres: List[str] = Field(default_factory=list, description="Movie genres")
    releaseYear: Optional[int] = Field(default=None, description="Release year")


class RecommendationResponse(BaseModel):
    """Personalized user recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    userId: int = Field(..., description="User logical ID")
    strategy: str = Field(..., description="Applied algorithm strategy")
    recommendations: List[RecommendationItem] = Field(default_factory=list)


class ContentRecommendationResponse(BaseModel):
    """Similar movie content recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    movieId: int = Field(..., description="Target movie ID")
    recommendations: List[RecommendationItem] = Field(default_factory=list)
