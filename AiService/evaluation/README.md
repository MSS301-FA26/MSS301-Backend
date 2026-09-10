# CinemaAI — Offline Recommender Evaluation

Reproducible offline evaluation of the three production recommendation models
(see `../service/`) on two datasets. This harness produced every figure in
`docs/paper/cinemaai_recommender_paper.{md,tex}`.

| Model | Mirrors production code |
|---|---|
| Popularity baseline | `service/collaborative_recommend.py:_fallback` (tier 2) |
| User-based CF, k=20 | `service/collaborative_recommend.py:recommend_collaborative` (formula ported verbatim; vectorized-equivalent for MovieLens) |
| Content-based SBERT (`all-MiniLM-L6-v2`) | `service/content_recommend.py` + `service/embedding_service.py` (adapted item-to-item → rating-weighted per-user profile for offline ranking) |

## Files

- `evaluate_synthetic.py` — evaluation on the synthetic CinemaAI dataset
- `evaluate_movielens.py` — evaluation on MovieLens 1M (vectorized, same formulas)
- `download_and_prepare_movielens_1m.py` — downloads the official GroupLens
  ml-1m archive and creates the per-user chronological 80/10/10 split
- `generate_synthetic_cinemaai.py` — generates the synthetic dataset
  (default `--seed 20260717`; every record carries `synthetic_flag=true`)
- `dataset_manifest.txt` — SHA-256 + line counts of every CSV used; verify your
  data against it before comparing numbers
- `results_synthetic.{json,md}`, `results_movielens.{json,md}` — generated
  outputs backing the paper tables

## Prerequisites

- Python 3.11 with the AiService virtualenv (`BE/AiService/venv`):
  fastapi/pandas/numpy/sentence-transformers per `../requirements.txt`
- ~100 MB disk for MovieLens 1M
- First run downloads the `all-MiniLM-L6-v2` model from HuggingFace unless
  cached. On this project's dev machine the cache lives on `E:` — set
  `HF_HOME` accordingly (or omit it to use the default `~/.cache`).

## Reproduce

From `BE/AiService/` (Windows Git Bash shown; adapt path separators as needed):

```bash
# 0. Prepare datasets (one-time)
./venv/Scripts/python.exe evaluation/download_and_prepare_movielens_1m.py \
    --output <DATA_DIR>/movielens_1m_ready
./venv/Scripts/python.exe evaluation/generate_synthetic_cinemaai.py \
    --seed 20260717 --output <DATA_DIR>/synthetic_cinemaai
# Verify checksums against evaluation/dataset_manifest.txt

# 1. Synthetic dataset (~1 min)
HF_HOME="E:\piptmp\hf" ./venv/Scripts/python.exe evaluation/evaluate_synthetic.py \
    --data <DATA_DIR>/synthetic_cinemaai

# 2. MovieLens 1M (~1 min)
HF_HOME="E:\piptmp\hf" ./venv/Scripts/python.exe evaluation/evaluate_movielens.py \
    --data <DATA_DIR>/movielens_1m_ready
```

Each script prints the metric tables and rewrites its `results_*.json` /
`results_*.md` next to itself.

## Protocol (fixed across all models)

- Fit on `train.csv` only; the temporal split prevents future-interaction leakage.
- Candidates per user = full catalog minus the user's train items (mirrors the
  production `seen` filter; asserted in-script).
- Top-K = 10; CF neighborhood k = 20 (production values, not tuned).
- Relevance: MovieLens `rating >= 4`; synthetic `liked == 1` (primary) and
  `rating >= 3.5` (sensitivity variant).
- Metrics: Precision@10, Recall@10, NDCG@10, MAP@10, catalog coverage@10;
  RMSE/MAE vs. global-mean and item-mean baselines for rating prediction.

## Determinism

Every step is deterministic: no random sampling, fixed dataset seed (20260717),
deterministic SBERT inference, stable sorts. Verified by running each script
twice and diffing the JSON outputs — byte-identical. To re-verify:

```bash
cp evaluation/results_synthetic.json /tmp/run1.json
HF_HOME="E:\piptmp\hf" ./venv/Scripts/python.exe evaluation/evaluate_synthetic.py --data <DATA_DIR>/synthetic_cinemaai
diff /tmp/run1.json evaluation/results_synthetic.json   # expect: no output
```

## Honesty note

The synthetic dataset is generated (`synthetic_flag=true` on every record) and
is used only to validate the pipeline under the production metadata schema.
Do not present synthetic results as real audience behavior. MovieLens 1M is the
real-world benchmark; cite Harper & Konstan (2015), doi:10.1145/2827872.
