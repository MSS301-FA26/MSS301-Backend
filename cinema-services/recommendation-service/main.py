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


app = FastAPI(
    title="CinemaAI Recommendation Service",
    description="Adaptive Hybrid Recommendation Engine (Content-Based + Collaborative Filtering)",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
