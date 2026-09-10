from fastapi import APIRouter, HTTPException
from typing import List
from api.recommend_models import RecommendMovieResponse
from service.content_recommend import recommend_content

router = APIRouter()


@router.get("/recommend/content/{movie_id}", response_model=List[RecommendMovieResponse])
def content(movie_id: int):
    result = recommend_content(movie_id)
    if result is None:
        raise HTTPException(status_code=404, detail="Movie not found")
    return result
