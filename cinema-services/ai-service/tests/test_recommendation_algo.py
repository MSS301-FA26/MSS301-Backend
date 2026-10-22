import pytest
from modules.recommendation.collaborative_filter import PearsonShrinkageCollaborativeFilter


def test_pearson_similarity_with_shrinkage():
    filter_engine = PearsonShrinkageCollaborativeFilter(lambda_shrinkage=5.0, min_overlap=2)

    # User 1 and User 2 share similar relative preferences: both prefer 101 over 102
    u_ratings = {101: 5.0, 102: 2.0}
    v_ratings = {101: 4.5, 102: 1.5, 103: 5.0}

    sim = filter_engine.calculate_pearson_similarity(u_ratings, v_ratings)
    assert sim > 0.0
    assert sim <= 1.0


def test_pearson_insufficient_overlap_returns_zero():
    filter_engine = PearsonShrinkageCollaborativeFilter(lambda_shrinkage=5.0, min_overlap=3)

    # Only 2 common items while min_overlap = 3 -> must return 0.0 to prevent spurious correlations
    u_ratings = {101: 5.0, 102: 4.0}
    v_ratings = {101: 5.0, 102: 4.0, 103: 1.0}

    sim = filter_engine.calculate_pearson_similarity(u_ratings, v_ratings)
    assert sim == 0.0


def test_rating_deviation_prediction():
    filter_engine = PearsonShrinkageCollaborativeFilter(lambda_shrinkage=1.0, min_overlap=1)

    all_ratings = {
        1: {101: 5.0, 102: 4.0},           # Target user: mean = 4.5
        2: {101: 4.0, 102: 3.0, 103: 5.0}  # Neighbor: mean = 4.0; movie 103 rated 5.0 (+1.0 deviation)
    }

    preds = filter_engine.predict_user_ratings(
        target_user_id=1,
        all_ratings=all_ratings,
        candidate_movie_ids={103}
    )

    assert len(preds) == 1
    movie_id, score = preds[0]
    assert movie_id == 103
    # Target mean is 4.5; neighbor has positive deviation (+1.0), so predicted score >= 4.5
    assert score >= 4.5
