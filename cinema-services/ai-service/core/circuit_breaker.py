from enum import Enum
import logging
import threading
import time
from typing import Any, Callable, Dict, Optional

logger = logging.getLogger(__name__)


class CircuitState(str, Enum):
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class CircuitBreakerOpenException(Exception):
    """Raised when an operation is attempted while the circuit breaker is in the OPEN state."""
    pass


class CircuitBreaker:
    """
    Thread-safe implementation of the Circuit Breaker pattern.
    Transitions between CLOSED, OPEN, and HALF_OPEN states to shield downstream
    subsystems (database, pgvector, external LLM) from cascading overload.
    """

    def __init__(
        self,
        name: str = "default",
        failure_threshold: int = 5,
        recovery_timeout: float = 30.0,
        half_open_success_threshold: int = 1
    ):
        self.name = name
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.half_open_success_threshold = half_open_success_threshold

        self._state = CircuitState.CLOSED
        self._consecutive_failures = 0
        self._successful_probes = 0
        self._last_state_change = time.time()
        self._lock = threading.Lock()

    @property
    def state(self) -> CircuitState:
        with self._lock:
            self._evaluate_state_transition_locked()
            return self._state

    def _evaluate_state_transition_locked(self) -> None:
        """Internal helper to check if OPEN timeout expired and transition to HALF_OPEN."""
        if self._state == CircuitState.OPEN:
            elapsed = time.time() - self._last_state_change
            if elapsed >= self.recovery_timeout:
                logger.info(
                    f"CircuitBreaker '{self.name}': Recovery timeout of {self.recovery_timeout}s elapsed. "
                    "Transitioning from OPEN to HALF_OPEN for canary probe."
                )
                self._state = CircuitState.HALF_OPEN
                self._successful_probes = 0
                self._last_state_change = time.time()

    def can_execute(self) -> bool:
        """Determine whether the next request should be permitted to execute."""
        with self._lock:
            self._evaluate_state_transition_locked()
            return self._state in (CircuitState.CLOSED, CircuitState.HALF_OPEN)

    def record_success(self) -> None:
        """Record successful invocation and update circuit state."""
        with self._lock:
            if self._state == CircuitState.HALF_OPEN:
                self._successful_probes += 1
                if self._successful_probes >= self.half_open_success_threshold:
                    logger.info(
                        f"CircuitBreaker '{self.name}': Canary probe succeeded. "
                        "Resetting circuit from HALF_OPEN to CLOSED."
                    )
                    self._state = CircuitState.CLOSED
                    self._consecutive_failures = 0
                    self._successful_probes = 0
                    self._last_state_change = time.time()
            elif self._state == CircuitState.CLOSED:
                self._consecutive_failures = 0

    def record_failure(self, error: Optional[Exception] = None) -> None:
        """Record failure and trip circuit to OPEN if threshold reached."""
        with self._lock:
            self._consecutive_failures += 1
            logger.warning(
                f"CircuitBreaker '{self.name}': Failure recorded (count: {self._consecutive_failures}). "
                f"Caused by: {error or 'Unknown exception'}"
            )
            if self._state == CircuitState.HALF_OPEN:
                logger.warning(
                    f"CircuitBreaker '{self.name}': Canary probe failed in HALF_OPEN. "
                    "Tripping circuit back to OPEN state."
                )
                self._state = CircuitState.OPEN
                self._last_state_change = time.time()
            elif self._state == CircuitState.CLOSED and self._consecutive_failures >= self.failure_threshold:
                logger.error(
                    f"CircuitBreaker '{self.name}': Consecutive failure threshold ({self.failure_threshold}) exceeded. "
                    "Tripping circuit from CLOSED to OPEN."
                )
                self._state = CircuitState.OPEN
                self._last_state_change = time.time()

    def execute(self, func: Callable, *args, fallback: Optional[Callable] = None, **kwargs) -> Any:
        """
        Execute function within circuit breaker protection boundary.
        If circuit is OPEN or execution fails, falls back gracefully to fallback callable.
        """
        if not self.can_execute():
            logger.warning(f"CircuitBreaker '{self.name}' is OPEN. Diverting to fallback without invoking primary target.")
            if fallback is not None:
                return fallback(*args, **kwargs)
            raise CircuitBreakerOpenException(f"Circuit breaker '{self.name}' is open")

        try:
            result = func(*args, **kwargs)
            self.record_success()
            return result
        except Exception as ex:
            self.record_failure(ex)
            if fallback is not None:
                logger.info(f"CircuitBreaker '{self.name}': Invoking fallback after primary execution failure.")
                return fallback(*args, **kwargs)
            raise

    def reset(self) -> None:
        """Manually reset circuit breaker to pristine CLOSED state."""
        with self._lock:
            self._state = CircuitState.CLOSED
            self._consecutive_failures = 0
            self._successful_probes = 0
            self._last_state_change = time.time()


# Circuit breaker registry
_registry_lock = threading.Lock()
_circuit_breakers: Dict[str, CircuitBreaker] = {}


def get_circuit_breaker(
    name: str,
    failure_threshold: int = 5,
    recovery_timeout: float = 30.0
) -> CircuitBreaker:
    """Retrieve or initialize named circuit breaker instance."""
    with _registry_lock:
        if name not in _circuit_breakers:
            _circuit_breakers[name] = CircuitBreaker(
                name=name,
                failure_threshold=failure_threshold,
                recovery_timeout=recovery_timeout
            )
        return _circuit_breakers[name]
