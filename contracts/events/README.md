# Asynchronous Domain Events (RabbitMQ Schemas)

Thư mục lưu trữ JSON Schema cho các sự kiện bất đồng bộ trao đổi qua RabbitMQ:

### 1. `PaymentSucceededEvent`
* **Exchange**: `cinema.payment.events`
* **Routing Key**: `payment.succeeded`
* **Publisher**: `payment-service`
* **Consumer**: `booking-service` (chuyển sang `PAID`, chốt ghế `BOOKED`, sinh QR), `recommendation-service` (cập nhật tín hiệu)
* **Payload**:
  ```json
  {
    "eventId": "uuid",
    "eventType": "PaymentSucceeded",
    "version": 1,
    "occurredAt": "2026-09-15T14:30:00Z",
    "aggregateId": "1001",
    "correlationId": "req-xyz-123",
    "payload": {
      "bookingId": 1001,
      "paymentId": 5001,
      "amount": 225000.00,
      "provider": "VNPAY",
      "transactionNo": "14567890"
    }
  }
  ```

### 2. `PaymentFailedEvent`
* **Exchange**: `cinema.payment.events`
* **Routing Key**: `payment.failed`
* **Publisher**: `payment-service`
* **Consumer**: `booking-service`

### 3. `MoviePublishedEvent`
* **Exchange**: `cinema.catalog.events`
* **Routing Key**: `movie.published`
* **Publisher**: `catalog-service`
* **Consumer**: `recommendation-service`
