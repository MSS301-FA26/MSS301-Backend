@echo off
echo === Start AI Service ===

call venv\Scripts\activate.bat

if not exist "model\movie_embeddings.pkl" (
    echo [INFO] Chua co embeddings, dang tao...
    python create_embedding.py
)

echo [INFO] Khoi dong FastAPI tren port 8000...
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
