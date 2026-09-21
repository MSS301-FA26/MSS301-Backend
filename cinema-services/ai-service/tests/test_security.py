import pytest
from fastapi.testclient import TestClient
import os

# Test environment settings
os.environ["SERVER_PORT"] = "8000"
os.environ["INTERNAL_GATEWAY_SECRET"] = "valid-test-gateway-secret-12345"
os.environ["INTERNAL_SERVICE_SECRET"] = "valid-test-internal-secret-67890"
os.environ["DB_HOST"] = "localhost"
os.environ["DB_PORT"] = "5435"
os.environ["DB_NAME"] = "recommendation_db"
os.environ["DB_USER"] = "rec_user"
os.environ["DB_PASSWORD"] = "rec_pass_123"
os.environ["RABBITMQ_HOST"] = "localhost"

from app.main import app

client = TestClient(app, raise_server_exceptions=False)


def test_healthcheck_whitelisted_without_secret():
    """Verify that /health is whitelisted and returns 200 without requiring secret header."""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP", "service": "ai-service"}


def test_direct_access_rejected_with_403_forbidden():
    """Verify that direct request without X-Gateway-Secret is rejected with 403 Forbidden."""
    response = client.get("/api/v1/recommendations/user/1")
    assert response.status_code == 403
    data = response.json()
    assert data["success"] is False
    assert data["message"] == "Trusted service access required"
    assert "timestamp" in data


def test_invalid_secret_rejected_with_403_forbidden():
    """Verify that supplying an invalid secret returns 403 Forbidden."""
    response = client.get(
        "/api/v1/recommendations/user/1",
        headers={"X-Gateway-Secret": "wrong-secret-token"}
    )
    assert response.status_code == 403
    assert response.json()["message"] == "Trusted service access required"


def test_valid_secret_passes_security_middleware():
    """Verify that supplying the correct X-Gateway-Secret successfully passes security middleware."""
    response = client.get(
        "/api/v1/recommendations/user/1",
        headers={"X-Gateway-Secret": "valid-test-gateway-secret-12345"}
    )
    assert response.status_code in [200, 500]
