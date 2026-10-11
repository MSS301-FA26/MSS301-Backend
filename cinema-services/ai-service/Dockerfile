FROM python:3.12-slim

# Install Astral uv binary
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Install OS runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install dependencies using uv sync
COPY pyproject.toml uv.lock ./
RUN --mount=type=cache,target=/root/.cache/uv \
    UV_HTTP_TIMEOUT=300 UV_CONCURRENT_DOWNLOADS=4 UV_RETRIES=10 uv sync --frozen --no-dev --no-install-project

# Copy application code
COPY . .

# Place virtual environment in PATH
ENV PATH="/app/.venv/bin:$PATH"
ENV PYTHONPATH=/app
ENV SERVER_PORT=8000

# Pre-download SentenceTransformer model to local image cache
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8000/health || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
