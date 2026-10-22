import inspect
from typing import Callable, Optional, Dict, Any, get_type_hints, get_origin, get_args, Union


def _map_type_to_json_schema(py_type: Any) -> Dict[str, Any]:
    """Map python type annotations to OpenAPI JSON schema types."""
    origin = get_origin(py_type)
    if origin is Union:
        args = [arg for arg in get_args(py_type) if arg is not type(None)]
        if len(args) == 1:
            return _map_type_to_json_schema(args[0])

    if py_type in (str, Optional[str]):
        return {"type": "string"}
    elif py_type in (int, Optional[int]):
        return {"type": "integer"}
    elif py_type in (float, Optional[float]):
        return {"type": "number"}
    elif py_type in (bool, Optional[bool]):
        return {"type": "boolean"}
    elif py_type in (dict, Dict, Optional[dict]):
        return {"type": "object"}
    elif py_type in (list, list, Optional[list]):
        return {"type": "array", "items": {"type": "string"}}
    return {"type": "string"}


class Tool:
    """Represents an agent tool with dynamic schema derived directly from docstring and type hints."""

    def __init__(self, func: Callable, name: Optional[str] = None, description: Optional[str] = None):
        self.func = func
        self.name = name or func.__name__
        self.description = (description or inspect.getdoc(func) or "").strip()
        self.schema = self._build_openai_tool_schema()

    def _build_openai_tool_schema(self) -> Dict[str, Any]:
        sig = inspect.signature(self.func)
        type_hints = get_type_hints(self.func)

        properties: Dict[str, Any] = {}
        required_params = []

        # Parse docstring for parameter descriptions if available
        param_docs: Dict[str, str] = {}
        if self.description:
            lines = self.description.split("\n")
            in_args = False
            for line in lines:
                s = line.strip()
                if s.lower().startswith("args:") or s.lower().startswith("parameters:"):
                    in_args = True
                    continue
                if in_args:
                    if s.startswith("return") or s.startswith("returns:"):
                        break
                    if ":" in s:
                        p_name, p_desc = s.split(":", 1)
                        param_docs[p_name.strip()] = p_desc.strip()

        for param_name, param in sig.parameters.items():
            if param_name in ("self", "cls"):
                continue

            py_type = type_hints.get(param_name, str)
            schema_type = _map_type_to_json_schema(py_type)

            p_desc = param_docs.get(param_name) or f"Parameter {param_name}"
            schema_type["description"] = p_desc

            properties[param_name] = schema_type

            # Check if parameter has no default value (required)
            if param.default == inspect.Parameter.empty:
                required_params.append(param_name)

        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": {
                    "type": "object",
                    "properties": properties,
                    "required": required_params
                }
            }
        }

    def __call__(self, *args, **kwargs):
        return self.func(*args, **kwargs)


def tool(name: Optional[str] = None, description: Optional[str] = None):
    """Decorator to register a python function as a dynamic docstring-driven Tool."""
    def decorator(func: Callable) -> Tool:
        return Tool(func, name=name, description=description)
    return decorator
