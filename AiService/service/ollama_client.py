import os
import requests

OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "mistral")


def chat_completion(messages: list, tools: list | None = None, temperature: float = 0.7) -> dict:
    """Gọi Ollama /api/chat (non-streaming). Trả về message dict {role, content, tool_calls}."""
    body = {
        "model": OLLAMA_MODEL,
        "messages": messages,
        "stream": False,
        "options": {"temperature": temperature},
    }
    if tools:
        body["tools"] = tools

    resp = requests.post(f"{OLLAMA_URL}/api/chat", json=body, timeout=300)
    resp.raise_for_status()
    return resp.json().get("message", {})
