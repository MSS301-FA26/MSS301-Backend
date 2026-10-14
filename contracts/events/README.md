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

### 4. `CustomerActivatedEvent`

- **Schema**: [customer-activated.schema.json](customer-activated.schema.json)
- **Exchange / routing key**: `cinema.identity.events` / `customer.activated`
- **Publisher**: Identity transactional outbox, after verified ACTIVE CUSTOMER activation.
- **Consumer / queue**: Payment / `payment.customer-activated.queue`.
- **Dead-letter exchange / queue**: existing `cinema.dlx` / `payment.customer-activated.dlq`.
- **Dead-letter routing key**: `dlq.payment.customer-activated`.
- **Wire convention**: flat JSON DTO serialized to a JSON String through the existing
  `Jackson2JsonMessageConverter` convention; String listener parses the inner JSON.
  This event does not use the illustrative nested envelope shown above.
- **Delivery**: at least once; Payment uses unique `cine_wallets.user_id` and atomic
  `ON CONFLICT DO NOTHING`. Replay never resets an existing balance.
- **Payload**: only `eventId`, positive internal `userId`, `occurredAt` (Java local
  date-time), and safe `correlationId`. No email, password or tokens.
- **Operations / existing-user backfill**:
  [wallet provisioning decision and runbook](../../docs/architecture/WALLET-PROVISION-01.md).
