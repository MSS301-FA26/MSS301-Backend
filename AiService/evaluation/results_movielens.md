# Offline evaluation — MovieLens 1M

Relevance: test rating >= 4. Per-user temporal 80/10/10 split, fit on train only.

## Ranking metrics

| model | users_evaluated | precision@10 | recall@10 | ndcg@10 | map@10 | coverage@10 |
|---|---|---|---|---|---|---|
| Popularity | 5827 | 0.0394 | 0.0446 | 0.0506 | 0.0227 | 0.0301 |
| User-based CF k=20 | 5827 | 0.0067 | 0.0096 | 0.01 | 0.0043 | 0.7005 |
| Content-based SBERT | 5827 | 0.006 | 0.0113 | 0.0093 | 0.004 | 0.2856 |

## Rating prediction (RMSE / MAE)

| model | rmse | mae |
|---|---|---|
| Global mean | 1.1634 | 0.9575 |
| Item mean | 0.9925 | 0.7893 |
| User-based CF k=20 | 1.0807 | 0.8421 |
