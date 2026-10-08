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
    source: str = Field(default="HYBRID", description="Source mechanism (e.g. CONTENT_BASED, COLLABORATIVE, POPULARITY, HYBRID, EXPLORATION)")
    genres: List[str] = Field(default_factory=list, description="Movie genres")
    releaseYear: Optional[int] = Field(default=None, description="Release year")


class RecommendationResponse(BaseModel):
    """Personalized user recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    userId: int = Field(..., description="User logical ID")
    strategy: str = Field(..., description="Applied algorithm strategy")
    branchId: Optional[int] = Field(default=None, description="Applied branch filter ID if specified")
    setId: Optional[int] = Field(default=None, description="Persisted recommendation set ID for CTR tracking")
    experimentVariant: str = Field(default="CONTROL", description="A/B experiment variant assignment")
    recommendations: List[RecommendationItem] = Field(default_factory=list)


class ContentRecommendationResponse(BaseModel):
    """Similar movie content recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    movieId: int = Field(..., description="Target movie ID")
    recommendations: List[RecommendationItem] = Field(default_factory=list)


class FeedbackRequest(BaseModel):
    """User interaction and feedback submission DTO."""
    model_config = ConfigDict(frozen=True)

    userId: int = Field(..., description="User logical ID")
    movieId: int = Field(..., description="Movie logical ID")
    interactionType: str = Field(..., description="Interaction type (e.g. MOVIE_RATED, MOVIE_LIKED, MOVIE_DISLIKED)")
    rating: Optional[float] = Field(default=None, ge=1.0, le=5.0, description="Optional rating value [1.0, 5.0]")
    comment: Optional[str] = Field(default=None, description="Optional user review text")


class FeedbackResponseData(BaseModel):
    """Feedback submission response acknowledgement DTO."""
    model_config = ConfigDict(frozen=True)

    recorded: bool = Field(..., description="Whether feedback was successfully processed")
    userId: int = Field(..., description="User logical ID")
    movieId: int = Field(..., description="Movie logical ID")


class FeedbackClickRequest(BaseModel):
    """Telemetry tracking DTO for recommendation item clicks."""
    model_config = ConfigDict(frozen=True)

    setId: int = Field(..., description="Recommendation set ID")
    movieId: int = Field(..., description="Clicked movie ID")
    userId: int = Field(..., description="User logical ID")


class TrendingRecommendationResponse(BaseModel):
    """Trending and popular recommendation response DTO."""
    model_config = ConfigDict(frozen=True)

    branchId: Optional[int] = Field(default=None, description="Filtered branch ID")
    strategy: str = Field(default="GLOBAL_POPULARITY", description="Strategy used")
    recommendations: List[RecommendationItem] = Field(default_factory=list)


class MovieReviewRequest(BaseModel):
    """User review submission with comment and rating DTO."""
    model_config = ConfigDict(frozen=True)

    userId: int = Field(..., description="User logical ID")
    movieId: int = Field(..., description="Movie logical ID")
    rating: float = Field(..., ge=1.0, le=5.0, description="Star rating [1.0, 5.0]")
    comment: str = Field(..., min_length=1, description="Review text comment")


class MovieReviewResponseData(BaseModel):
    """Processed review response with sentiment and consistency DTO."""
    model_config = ConfigDict(frozen=True)

    userId: int = Field(..., description="User logical ID")
    movieId: int = Field(..., description="Movie logical ID")
    sentimentLabel: str = Field(..., description="Sentiment classification (POSITIVE, NEGATIVE, NEUTRAL)")
    sentimentScore: float = Field(..., description="Sentiment polar score [-1.0, 1.0]")
    feedbackConsistency: str = Field(..., description="Consistency status (CONSISTENT, INCONSISTENT)")
    confidenceScore: float = Field(..., description="Confidence score [0.0, 1.0]")
    aspectSentiment: dict = Field(default_factory=dict, description="Aspect sentiment breakdowns")


class VariantFunnelMetrics(BaseModel):
    """Telemetry metrics per A/B experiment variant."""
    model_config = ConfigDict(frozen=True)

    variant: str = Field(..., description="Variant identifier (e.g. CONTROL, VARIANT_B)")
    impressions: int = Field(default=0, description="Total recommendation sets shown")
    clicks: int = Field(default=0, description="Total recommendation items clicked")
    ctr: float = Field(default=0.0, description="Click-through rate (clicks / impressions)")
    detailViews: int = Field(default=0, description="Total movie detail views")
    detailViewRate: float = Field(default=0.0, description="Detail view rate (detailViews / clicks)")
    bookings: int = Field(default=0, description="Total bookings completed from recommendations")
    bookingConversionRate: float = Field(default=0.0, description="Booking conversion rate (bookings / impressions)")
    ticketsUsed: int = Field(default=0, description="Total tickets redeemed at cinema")
    ticketUsedRate: float = Field(default=0.0, description="Ticket used conversion rate (ticketsUsed / bookings)")


class FunnelTotals(BaseModel):
    """Aggregated full-funnel telemetry across all variants."""
    model_config = ConfigDict(frozen=True)

    totalImpressions: int = Field(default=0, description="Overall recommendation sets delivered")
    totalClicks: int = Field(default=0, description="Overall recommendation clicks recorded")
    overallCtr: float = Field(default=0.0, description="Aggregated click-through rate")
    totalDetailViews: int = Field(default=0, description="Overall detail views recorded")
    totalBookings: int = Field(default=0, description="Overall booking conversions recorded")
    overallBookingRate: float = Field(default=0.0, description="Aggregated booking conversion rate")
    totalTicketsUsed: int = Field(default=0, description="Overall tickets validated at cinema")


class RecommendationMetricsResponse(BaseModel):
    """Full-funnel telemetry and conversion metrics dashboard response DTO."""
    model_config = ConfigDict(frozen=True)

    overall: FunnelTotals = Field(..., description="Aggregated full-funnel metrics")
    variants: List[VariantFunnelMetrics] = Field(default_factory=list, description="Metrics per experiment variant")
    ctrUpliftPercent: Optional[float] = Field(default=None, description="CTR relative uplift of Variant B over Control (%)")
    conversionUpliftPercent: Optional[float] = Field(default=None, description="Booking conversion relative uplift (%)")
    measuredPeriod: str = Field(default="All Time", description="Measurement interval description")

