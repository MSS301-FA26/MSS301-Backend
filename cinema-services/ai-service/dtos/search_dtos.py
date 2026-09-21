from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional


class SearchResultItem(BaseModel):
    """Search result item representation."""
    model_config = ConfigDict(frozen=True)

    movieId: int = Field(..., description="Movie ID")
    title: str = Field(..., description="Movie title")
    posterUrl: Optional[str] = Field(default="", description="Poster URL")
    score: float = Field(..., description="Relevance or similarity score")
    genres: List[str] = Field(default_factory=list, description="Movie genres")
    overview: Optional[str] = Field(default="", description="Plot overview")
    releaseYear: Optional[int] = Field(default=None, description="Release year")


class AdaptiveSearchRequest(BaseModel):
    """Adaptive multi-tier search request."""
    model_config = ConfigDict(frozen=True)

    query: str = Field(..., min_length=1, max_length=500, description="Search query string")
    limit: int = Field(default=10, ge=1, le=50, description="Maximum number of results to return")


class AdaptiveSearchResponse(BaseModel):
    """Adaptive search response payload."""
    model_config = ConfigDict(frozen=True)

    query: str = Field(..., description="Original search query")
    routeUsed: str = Field(..., description="Selected routing tier (R0_FIRST_STAGE, R1_LIGHTWEIGHT, R2_ADAP_COLBERT)")
    routingEntropy: float = Field(..., description="Routing uncertainty entropy H(q)")
    wasFallback: bool = Field(..., description="Whether confidence fallback to R2 was triggered")
    latencyMs: float = Field(..., description="Execution latency in milliseconds")
    results: List[SearchResultItem] = Field(default_factory=list)
