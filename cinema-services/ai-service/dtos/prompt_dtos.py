from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, ConfigDict


class PromptTemplateResponse(BaseModel):
    """Prompt template response item DTO."""
    model_config = ConfigDict(frozen=True)

    id: int = Field(..., description="Unique record identifier")
    promptCode: str = Field(..., description="Unique uppercase prompt identifier code")
    name: str = Field(..., description="Human-readable template name")
    description: Optional[str] = Field(default="", description="Description of prompt purpose")
    systemPrompt: str = Field(..., description="System role instruction prompt")
    userTemplate: str = Field(..., description="User role prompt template with {var} placeholders")
    modelName: str = Field(default="gpt-4o-mini", description="Target LLM model name")
    temperature: float = Field(default=0.2, description="Generation temperature [0.0, 2.0]")
    maxTokens: int = Field(default=200, description="Maximum token generation limit")
    requiredVariables: List[str] = Field(default_factory=list, description="List of required variable names")
    isActive: bool = Field(default=True, description="Whether template is actively enabled")
    version: int = Field(default=1, description="Template version sequence")
    createdAt: str = Field(..., description="ISO creation timestamp")
    updatedAt: str = Field(..., description="ISO last modified timestamp")


class PromptCreateRequest(BaseModel):
    """Payload to create a new prompt template."""
    model_config = ConfigDict(frozen=True)

    promptCode: str = Field(
        ...,
        min_length=2,
        max_length=64,
        pattern=r"^[A-Z0-9_]+$",
        description="Unique uppercase prompt code (e.g. CUSTOM_SUMMARY)"
    )
    name: str = Field(..., min_length=2, max_length=128, description="Display name")
    description: Optional[str] = Field(default="", description="Usage notes and description")
    systemPrompt: str = Field(..., min_length=1, description="System role prompt")
    userTemplate: str = Field(..., min_length=1, description="User template text with placeholders")
    modelName: str = Field(default="gpt-4o-mini", description="Target OpenAI model")
    temperature: float = Field(default=0.20, ge=0.0, le=2.0, description="Sampling temperature")
    maxTokens: int = Field(default=200, ge=1, le=4096, description="Max response tokens")
    requiredVariables: List[str] = Field(default_factory=list, description="Variables required by template")
    isActive: bool = Field(default=True, description="Active status")


class PromptUpdateRequest(BaseModel):
    """Payload to update an existing prompt template."""
    model_config = ConfigDict(frozen=True)

    name: Optional[str] = Field(default=None, min_length=2, max_length=128)
    description: Optional[str] = Field(default=None)
    systemPrompt: Optional[str] = Field(default=None, min_length=1)
    userTemplate: Optional[str] = Field(default=None, min_length=1)
    modelName: Optional[str] = Field(default=None)
    temperature: Optional[float] = Field(default=None, ge=0.0, le=2.0)
    maxTokens: Optional[int] = Field(default=None, ge=1, le=4096)
    requiredVariables: Optional[List[str]] = Field(default=None)
    isActive: Optional[bool] = Field(default=None)


class PromptTestRenderRequest(BaseModel):
    """Payload to test render a template with mock variables."""
    model_config = ConfigDict(frozen=True)

    variables: Dict[str, Any] = Field(default_factory=dict, description="Key-value mapping of variables")
    callLlm: bool = Field(default=False, description="Whether to invoke LLM dry-run call")


class PromptTestRenderResponse(BaseModel):
    """Result of template render test."""
    model_config = ConfigDict(frozen=True)

    promptCode: str = Field(..., description="Prompt code tested")
    renderedSystemPrompt: str = Field(..., description="Final rendered system prompt")
    renderedUserPrompt: str = Field(..., description="Final rendered user prompt with substituted values")
    missingVariables: List[str] = Field(default_factory=list, description="Any expected variables that were missing")
    llmResponse: Optional[str] = Field(default=None, description="Dry-run LLM response if requested")
