# Offline evaluation — synthetic CinemaAI dataset

> Dataset is fully synthetic (`synthetic_flag=true`). Results validate
> the pipeline and algorithms only; they do not represent real audience
> behavior.

## Ranking metrics — relevance: `liked==1`

| model | users_evaluated | precision@10 | recall@10 | ndcg@10 | map@10 | coverage@10 |
|---|---|---|---|---|---|---|
| Popularity | 363 | 0.0036 | 0.0195 | 0.0124 | 0.0084 | 0.0133 |
| User-based CF k=20 | 363 | 0.0039 | 0.0233 | 0.0121 | 0.0067 | 0.5317 |
| Content-based SBERT | 363 | 0.0127 | 0.0787 | 0.0417 | 0.0245 | 0.3942 |

## Ranking metrics — relevance: `rating>=3.5`

| model | users_evaluated | precision@10 | recall@10 | ndcg@10 | map@10 | coverage@10 |
|---|---|---|---|---|---|---|
| Popularity | 524 | 0.0053 | 0.0209 | 0.015 | 0.0087 | 0.0133 |
| User-based CF k=20 | 524 | 0.0053 | 0.0212 | 0.0127 | 0.0063 | 0.5508 |
| Content-based SBERT | 524 | 0.0116 | 0.0523 | 0.0286 | 0.0143 | 0.45 |

## Rating prediction (RMSE / MAE)

| model | rmse | mae |
|---|---|---|
| Global mean | 0.8404 | 0.6795 |
| Item mean | 0.8221 | 0.6641 |
| User-based CF k=20 | 0.9532 | 0.7543 |
