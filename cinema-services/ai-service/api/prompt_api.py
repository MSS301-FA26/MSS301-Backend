from typing import List, Optional
from fastapi import APIRouter, Query, Path, Body
from dtos.common import ApiResponse
from dtos.prompt_dtos import (
    PromptTemplateResponse,
    PromptCreateRequest,
    PromptUpdateRequest,
    PromptTestRenderRequest,
    PromptTestRenderResponse
)
from modules.prompt.service_impl import get_prompt_service
from core.exceptions import NotFoundException

router = APIRouter(prefix="/api/v1/prompts", tags=["Prompts"])


@router.get("", response_model=ApiResponse[List[PromptTemplateResponse]])
@router.get("/", response_model=ApiResponse[List[PromptTemplateResponse]], include_in_schema=False)
def list_prompts(
    active_only: bool = Query(default=False, description="Filter only active prompt templates")
):
    """
    List all LLM prompt templates in the system.
    Provides visibility into system instructions, parameters, and variable definitions.
    """
    service = get_prompt_service()
    data = service.list_prompts(active_only=active_only)
    return ApiResponse(
        success=True,
        message="Prompt templates retrieved successfully",
        data=data
    )


@router.get("/{prompt_code}", response_model=ApiResponse[PromptTemplateResponse])
def get_prompt_by_code(
    prompt_code: str = Path(..., description="Unique prompt template identifier code (e.g. QUERY_REWRITE)")
):
    """
    Retrieve details of a single prompt template by uppercase identifier code.
    """
    service = get_prompt_service()
    data = service.get_prompt_by_code(prompt_code)
    if not data:
        raise NotFoundException(f"Prompt template '{prompt_code}' not found")
    return ApiResponse(
        success=True,
        message=f"Prompt template '{prompt_code}' retrieved successfully",
        data=data
    )


@router.post("", response_model=ApiResponse[PromptTemplateResponse])
def create_prompt(
    request: PromptCreateRequest = Body(..., description="Prompt template creation payload")
):
    """
    Create a new dynamic prompt template.
    Enables zero-downtime additions of specialized LLM prompts.
    """
    service = get_prompt_service()
    data = service.create_prompt(request)
    return ApiResponse(
        success=True,
        message=f"Prompt template '{data.promptCode}' created successfully",
        data=data
    )


@router.put("/{prompt_code}", response_model=ApiResponse[PromptTemplateResponse])
def update_prompt(
    prompt_code: str = Path(..., description="Unique prompt template identifier code to update"),
    request: PromptUpdateRequest = Body(..., description="Prompt fields to modify")
):
    """
    Update an existing prompt template and immediately bust in-memory cache (hot-reload).
    Changes take effect on the very next LLM invocation without restarting the server.
    """
    service = get_prompt_service()
    data = service.update_prompt(prompt_code, request)
    return ApiResponse(
        success=True,
        message=f"Prompt template '{prompt_code}' updated successfully",
        data=data
    )


@router.delete("/{prompt_code}", response_model=ApiResponse[dict])
def delete_prompt(
    prompt_code: str = Path(..., description="Unique prompt template identifier code to deactivate")
):
    """
    Deactivate a prompt template (soft delete).
    When deactivated, LLM subsystems will revert to default code fallback templates.
    """
    service = get_prompt_service()
    success = service.delete_prompt(prompt_code)
    return ApiResponse(
        success=success,
        message=f"Prompt template '{prompt_code}' deactivated successfully",
        data={"promptCode": prompt_code, "deactivated": success}
    )


@router.post("/{prompt_code}/test-render", response_model=ApiResponse[PromptTestRenderResponse])
def test_render_prompt(
    prompt_code: str = Path(..., description="Unique prompt template identifier code to test"),
    request: PromptTestRenderRequest = Body(..., description="Mock variables and execution options")
):
    """
    Dry-run test template variable substitution and optionally execute test call to OpenAI LLM.
    Allows administrators to safely test and inspect prompt output before going to production.
    """
    service = get_prompt_service()
    data = service.test_render(prompt_code, request)
    return ApiResponse(
        success=True,
        message=f"Prompt template '{prompt_code}' rendered successfully",
        data=data
    )
