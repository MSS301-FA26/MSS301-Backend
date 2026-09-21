import pytest
from dtos.search_dtos import SearchResultItem
from modules.search.soft_utility_router import SoftUtilityRouter


def test_soft_utility_router_selection_without_fallback():
    router = SoftUtilityRouter()

    items = [
        SearchResultItem(movieId=1, title="Interstellar", score=0.85, genres=["Sci-Fi"]),
        SearchResultItem(movieId=2, title="Inception", score=0.84, genres=["Sci-Fi"]),
    ]

    route, entropy_h, was_fallback = router.predict_route(
        query="a complex space exploration journey directed by Christopher Nolan",
        r0_items=items
    )

    assert route in ["R0_FIRST_STAGE", "R1_LIGHTWEIGHT", "R2_ADAP_COLBERT"]
    assert entropy_h >= 0.0
    assert was_fallback is False
