from fastapi import FastAPI
from api.content_api import router as content_router
from api.collaborative_api import router as collaborative_router
from api.chat_api import router as chat_router
from api.stats_api import router as stats_router

app = FastAPI(title="CinemaAI Service")

app.include_router(content_router)
app.include_router(collaborative_router)
app.include_router(chat_router)
app.include_router(stats_router)
