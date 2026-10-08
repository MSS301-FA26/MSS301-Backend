from abc import ABC, abstractmethod
from typing import List, Optional, Tuple, Dict, Any
from dtos.prompt_dtos import (
    PromptTemplateResponse,
    PromptCreateRequest,
    PromptUpdateRequest,
    PromptTestRenderRequest,
    PromptTestRenderResponse
)


class IPromptService(ABC):
    """Interface for dynamic prompt template management and rendering."""

    @abstractmethod
    def list_prompts(self, active_only: bool = False) -> List[PromptTemplateResponse]:
        """List all prompt templates, optionally filtered by active status."""
        pass

    @abstractmethod
    def get_prompt_by_code(self, code: str) -> Optional[PromptTemplateResponse]:
        """Retrieve a specific prompt template by unique uppercase code."""
        pass

    @abstractmethod
    def create_prompt(self, request: PromptCreateRequest) -> PromptTemplateResponse:
        """Create a new prompt template."""
        pass

    @abstractmethod
    def update_prompt(self, code: str, request: PromptUpdateRequest) -> PromptTemplateResponse:
        """Update an existing prompt template and invalidate cache."""
        pass

    @abstractmethod
    def delete_prompt(self, code: str) -> bool:
        """Deactivate or remove a prompt template."""
        pass

    @abstractmethod
    def render_prompt(
        self,
        code: str,
        variables: Dict[str, Any]
    ) -> Tuple[str, str, str, float, int]:
        """
        Render system and user prompt for LLM consumption.
        Returns tuple of (system_prompt, rendered_user_prompt, model_name, temperature, max_tokens).
        """
        pass

    @abstractmethod
    def test_render(
        self,
        code: str,
        request: PromptTestRenderRequest
    ) -> PromptTestRenderResponse:
        """Dry-run test template rendering with provided variables."""
        pass
