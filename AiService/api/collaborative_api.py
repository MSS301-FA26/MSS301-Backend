from fastapi import APIRouter
from typing import List
from api.recommend_models import RecommendMovieResponse
from service.collaborative_recommend import recommend_collaborative

router = APIRouter()


@router.get("/recommend/collaborative/{user_id}", response_model=List[RecommendMovieResponse])
def collaborative(user_id: int):
    return recommend_collaborative(user_id)
