from pydantic import BaseModel
from typing import List, Optional


class RecommendMovieResponse(BaseModel):
    movieId: int
    title: str
    posterUrl: str
    similarity: float
    # Explanation metadata (additive, optional để tương thích ngược)
    source: Optional[str] = None            # collaborative | content | fallback_rating | fallback_popularity
    reason: Optional[str] = None            # lý do gợi ý, tiếng Việt, hiển thị trực tiếp trên UI
    predictedRating: Optional[float] = None  # CF: điểm dự đoán thang /5
    neighborCount: Optional[int] = None      # CF: số khán giả tương tự đã chấm phim này
    anchorTitle: Optional[str] = None        # CF: phim user chấm cao nhất
    matchedGenres: Optional[List[str]] = None    # content: thể loại trùng
    matchedActors: Optional[List[str]] = None    # content: diễn viên trùng
    sameDirector: Optional[bool] = None
    directorName: Optional[str] = None
    avgRating: Optional[float] = None        # fallback_rating
    ratingCount: Optional[int] = None        # fallback_rating
    bookingCount: Optional[int] = None       # fallback_popularity
