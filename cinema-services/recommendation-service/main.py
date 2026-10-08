import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass


GATEWAY_SECRET = os.getenv("INTERNAL_GATEWAY_SECRET", "8F78D52690EED1A48867F89272F07391B8FBC8968F187BB5C53C60E20243D7AD")

app = FastAPI(
    title="CinemaAI - Recommendation AI Service API",
    description="Hệ thống gợi ý phim thông minh (Content-Based + Collaborative Filtering) cho CinemaAI.",
    version="1.0.0",
    docs_url=None,       # Disable direct Swagger UI on port 8000
    redoc_url=None,      # Disable direct ReDoc on port 8000
    openapi_url="/openapi.json",
    servers=[
        {"url": "http://localhost:8080", "description": "API Gateway (All requests must go through Gateway)"}
    ]
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from starlette.requests import Request
from starlette.responses import JSONResponse

@app.middleware("http")
async def verify_gateway_secret(request: Request, call_next):
    # Whitelisted endpoints: Docker/K8s health check
    if request.url.path == "/health":
        return await call_next(request)

    supplied = request.headers.get("X-Gateway-Secret")
    if not supplied or supplied != GATEWAY_SECRET:
        return JSONResponse(
            status_code=403,
            content={
                "success": False,
                "code": "DIRECT_ACCESS_FORBIDDEN",
                "message": "Direct access to microservice is blocked. Requests must be routed through API Gateway (Port 8080)."
            }
        )
    return await call_next(request)

class MovieRecommendation(BaseModel):
    movieId: int
    title: Optional[str] = "Phim Gợi Ý"
    posterUrl: Optional[str] = None
    similarity: float = 0.95
    source: str = "ai_model"
    reason: Optional[str] = "Gợi ý thông minh phù hợp sở thích của bạn"
    avgRating: Optional[float] = 8.5

class RecommendationItem(BaseModel):
    movieId: int
    score: float
    reason: Optional[str] = "Popular in your favorite genres"

class RecommendationResponse(BaseModel):
    userId: int
    recommendations: List[RecommendationItem]

class StatsResponse(BaseModel):
    reviewCount: int = 150
    reviewerCount: int = 65
    paidBookingCount: int = 100
    embeddedMovieCount: int = 20

@app.get("/health")
def health_check():
    return {"status": "UP", "service": "recommendation-service"}

@app.get("/api/v1/recommendations/user/{user_id}", response_model=RecommendationResponse)
def get_user_recommendations(user_id: int):
    return RecommendationResponse(
        userId=user_id,
        recommendations=[
            RecommendationItem(movieId=1, score=0.95, reason="Trending Now"),
            RecommendationItem(movieId=2, score=0.88, reason="Based on your past bookings"),
            RecommendationItem(movieId=3, score=0.82, reason="Top Rated in Action"),
        ]
    )

@app.get("/api/v1/recommendation/collaborative/{user_id}", response_model=List[MovieRecommendation])
def get_collaborative_recommendations(user_id: int):
    return [
        MovieRecommendation(movieId=1, title="Dune: Part Two", similarity=0.98, reason="Dựa trên các phim bạn đã xem gần đây", avgRating=9.0),
        MovieRecommendation(movieId=2, title="Mai", similarity=0.92, reason="Phim chiếu rạp có lượng đặt vé cao nhất tuần", avgRating=8.6),
        MovieRecommendation(movieId=3, title="Kung Fu Panda 4", similarity=0.87, reason="Phù hợp với thể loại phim hài, hoạt hình bạn yêu thích", avgRating=8.2),
        MovieRecommendation(movieId=4, title="Exhuma: Quật Mộ Trùng Ma", similarity=0.81, reason="Khán giả có cùng sở thích cũng xem phim này", avgRating=8.5),
    ]

@app.get("/api/v1/recommendation/content/{movie_id}", response_model=List[MovieRecommendation])
def get_content_recommendations(movie_id: int):
    return [
        MovieRecommendation(movieId=1 if movie_id != 1 else 2, title="Phim tương tự A", similarity=0.94, reason="Cùng thể loại và đạo diễn", avgRating=8.8),
        MovieRecommendation(movieId=3 if movie_id != 3 else 4, title="Phim tương tự B", similarity=0.89, reason="Nội dung kịch tính tương đồng", avgRating=8.4),
        MovieRecommendation(movieId=5 if movie_id != 5 else 6, title="Phim tương tự C", similarity=0.83, reason="Được đánh giá cao bởi cùng nhóm khán giả", avgRating=8.1),
    ]

@app.get("/api/v1/recommendation/stats", response_model=StatsResponse)
def get_stats():
    return StatsResponse()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

