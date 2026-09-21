import logging
from typing import Dict, List, Any, Optional
from .tool import Tool

logger = logging.getLogger(__name__)


class ToolRegistry:
    """Registry managing agent tools, schema compilation, and execution dispatching."""

    def __init__(self):
        self._tools: Dict[str, Tool] = {}

    def register(self, tool_instance: Tool) -> None:
        """Register a tool instance into the registry."""
        self._tools[tool_instance.name] = tool_instance
        logger.debug(f"[ToolRegistry] Registered tool '{tool_instance.name}'")

    def get_tool(self, name: str) -> Optional[Tool]:
        """Retrieve tool by name."""
        return self._tools.get(name)

    def get_tools_schema(self) -> List[Dict[str, Any]]:
        """Return compiled OpenAPI tool schemas for all registered tools."""
        return [t.schema for t in self._tools.values()]

    def execute(self, tool_name: str, **kwargs) -> Any:
        """Dispatch execution to the registered tool by name."""
        tool_instance = self.get_tool(tool_name)
        if not tool_instance:
            raise ValueError(f"Tool '{tool_name}' not found in ToolRegistry.")
        return tool_instance(**kwargs)
