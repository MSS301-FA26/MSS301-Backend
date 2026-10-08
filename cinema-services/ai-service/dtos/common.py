from pydantic import BaseModel, Field, ConfigDict
from typing import Generic, TypeVar, Optional, List, Any
from datetime import datetime, timezone

T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    """ApiResponse envelope for service responses."""
    model_config = ConfigDict(frozen=True)

    success: bool = Field(default=True, description="Execution status")
    message: str = Field(default="Success", description="Response message")
    data: Optional[T] = Field(default=None, description="Response payload")
    timestamp: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat(),
        description="ISO-8601 UTC timestamp"
    )


class PageResponse(BaseModel, Generic[T]):
    """Pagination envelope."""
    model_config = ConfigDict(frozen=True)

    items: List[T] = Field(default_factory=list, description="Paginated items")
    page: int = Field(default=0, description="Page index (0-indexed)")
    size: int = Field(default=10, description="Page size")
    totalElements: int = Field(default=0, description="Total elements")
    totalPages: int = Field(default=0, description="Total pages")
    isLast: bool = Field(default=True, description="Whether this is the last page")


class ErrorResponse(BaseModel):
    """Standard error response envelope."""
    model_config = ConfigDict(frozen=True)

    success: bool = Field(default=False, description="Always false on error")
    message: str = Field(..., description="Error message")
    path: str = Field(default="", description="Request path causing error")
    errors: List[str] = Field(default_factory=list, description="Detailed validation errors")
    timestamp: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat(),
        description="ISO-8601 UTC timestamp"
    )
