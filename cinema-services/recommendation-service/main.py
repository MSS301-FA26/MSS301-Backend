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

class RecommendationItem(BaseModel):
    movieId: int
    score: float
    reason: Optional[str] = "Popular in your favorite genres"

class RecommendationResponse(BaseModel):
    userId: int
    recommendations: List[RecommendationItem]

@app.get("/health")
def health_check():
    return {"status": "UP", "service": "recommendation-service"}

@app.get("/api/v1/recommendations/user/{user_id}", response_model=RecommendationResponse)
def get_user_recommendations(user_id: int):
    # Baseline popularity/content-based placeholder
    return RecommendationResponse(
        userId=user_id,
        recommendations=[
            RecommendationItem(movieId=1, score=0.95, reason="Trending Now"),
            RecommendationItem(movieId=2, score=0.88, reason="Based on your past bookings"),
            RecommendationItem(movieId=3, score=0.82, reason="Top Rated in Action"),
        ]
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
