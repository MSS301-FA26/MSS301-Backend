import uuid

_store: dict[str, list[dict]] = {}
_last_movie: dict[str, int] = {}
MAX_TURNS = 20  # giữ tối đa 20 cặp user/assistant gần nhất


def new_conversation_id() -> str:
    return str(uuid.uuid4())


def get_history(conversation_id: str) -> list:
    return list(_store.get(conversation_id, []))


def append(conversation_id: str, role: str, content: str):
    history = _store.setdefault(conversation_id, [])
    history.append({"role": role, "content": content})
    if len(history) > MAX_TURNS * 2:
        _store[conversation_id] = history[-MAX_TURNS * 2:]


def get_last_movie_id(conversation_id: str) -> int | None:
    return _last_movie.get(conversation_id)


def set_last_movie_id(conversation_id: str, movie_id: int):
    _last_movie[conversation_id] = movie_id
