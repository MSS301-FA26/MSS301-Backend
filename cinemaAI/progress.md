# CinemaAI Progress

## Session update - 2026-07-23 recoverable standalone concession checkout

Scope:

- Added a 15-minute backend-owned payment deadline for standalone food orders and synchronized it with VNPay `vnp_ExpireDate`.
- Updated PostgreSQL `food_orders_status_check` so the new `EXPIRED` lifecycle state is accepted on existing databases.
- Added customer food-order listing and explicit cancellation APIs, plus automatic `EXPIRED` cleanup.
- Preserved pending food orders after a failed/cancelled VNPay attempt so customers can retry without creating another order.
- Added a separate "Đơn bắp nước của tôi" UI with countdown, retry, cancellation, paid pickup code and duplicate-checkout prevention.
- Updated the payment callback to return standalone concession customers to their food orders instead of movie tickets.
- Consolidated customer history under "Đơn của tôi" with separate movie-ticket and standalone-concession tabs; the concessions storefront now shows only an active pending checkout.

Verification:

- `mvnw.cmd -DskipTests package`: passed.
- `PaymentIntegrationTests`: passed (5 tests), including standalone create/pay and list/cancel coverage.
- Frontend `npm run build`: passed (2799 modules; existing bundle-size warning only).

## Session update - 2026-07-23 standalone concessions checkout

Scope:

- Added authenticated standalone food orders that do not require a movie booking.
- Made booking references optional for standalone food-order items and payments while retaining the booking-linked add-on flow.
- Added direct VNPay/mock-payment ownership checks through the food-order customer.
- Updated the concessions page so the header flow selects food immediately; a booking is linked only when entering from My Tickets.
- Added PostgreSQL migration `V13__standalone_food_orders.sql` and included paid standalone orders in concession revenue aggregation.
- Removed the redundant standalone-order explanation card from the bill and added current/estimated CinePoints information.
- Food orders now earn CinePoints exactly once after successful payment; point redemption remains intentionally disabled until abandoned-order restoration is supported.
- Added a PostgreSQL startup compatibility step because Flyway is currently disabled and Hibernate does not relax legacy `booking_id NOT NULL` constraints automatically.
- Refined the concessions bill empty/error/loading states with an actionable empty state, inline payment recovery and accessible quantity controls.

Verification:

- `mvnw.cmd -DskipTests package`: passed.
- `PaymentIntegrationTests#shouldCreateAndPayStandaloneFoodOrderWithoutBooking`: passed (1 test), including the post-payment loyalty balance assertion.
- `PaymentIntegrationTests`: standalone create/VNPay/mock flow passed; the full run exposed an old-payment lookup bug, and the focused retry/supersede scenario passed after changing the lookup to return the newest payment.
- Frontend `npm run build`: passed (2799 modules; existing bundle-size warning only).
- Local PostgreSQL metadata check: standalone-order booking references are nullable and `food_orders.customer_id` remains required after startup compatibility ran.

## Session update - 2026-07-23 single active checkout per showtime

Scope:

- Enforced one active `HOLDING`/`PENDING_PAYMENT` booking per customer and showtime.
- Serialized hold creation with a pessimistic showtime lock so concurrent tabs cannot bypass the rule.
- Added an integration test covering duplicate rejection, manual cancellation and successful rebooking.
- Aligned the documented hold duration with the implemented three-minute session.

Verification:

- `BookingIntegrationTests#shouldAllowOnlyOneActiveCheckoutPerCustomerAndShowtime` passed independently (1 test).
- Full `BookingIntegrationTests` passed after making its date and late-night pricing fixture deterministic (6 tests).

## Session update - 2026-06-27 configuration externalization

Scope:

- Scanned Java source, application/test properties, Maven metadata, Postman/docs examples and provider/config adapters for deployment-specific values.
- Externalized CORS origins, payment redirect URL, VNPay client IP fallback, Google token-info base URL, Cloudinary upload folder prefix/default folder, local seed account values, mail sender flags, scheduler flags/delays, recommendation weights, upload limits and provider settings through `application.properties` placeholders backed by `.env`.
- Kept domain/business constants in code, including booking/showtime/review state machines, role/enums, upload validation limits in service logic and recommendation scoring semantics.

Files changed:

- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- `.env.example`
- `src/main/java/com/sba301/cinemaai/config/*Properties.java`, `CloudinaryConfig`, `CorsConfig`, `VNPayConfig`, `VnpayProperties`
- `PaymentController`, `GoogleTokenVerifierImpl`, `MailServiceImpl`, `StorageUploadServiceImpl`, `VNPayServiceImpl`
- `AdminAccountSeeder`, `StaffAccountSeeder`
- `StorageUploadServiceTest`
- `api/CinemaAI_Phase0-6_Auto_Test.postman_collection.json`
- `api/phase-2-auth-user-security/README.md`
- `api/phase-6-payment/README.md`

Local-only note:

- Added missing `SEED_*` keys to local `.env` without reading or changing existing values, so current local seed-account behavior is preserved while committed Java/config no longer contains those passwords.

Verification performed:

```powershell
.\mvnw.cmd -DskipTests package
.\mvnw.cmd -Dtest=PaymentIntegrationTests test
.\mvnw.cmd -Dtest=StorageUploadServiceTest test
.\mvnw.cmd test
```

Results:

- Package build: `BUILD SUCCESS`.
- `PaymentIntegrationTests`: 3 tests passed.
- `StorageUploadServiceTest`: 2 tests passed after updating the constructor for `UploadProperties`.
- Full test run still fails in cinema baseline tests:
  - `CinemaShowtimeEndpointInventoryTests`: stale assertion expects `@GetMapping("/api/v1/admin/cinema")` instead of composed class + method mappings.
  - `CinemaShowtimeIntegrationTests`: expected `201` but got `405` on cinema/showtime flow.

Next action:

- Resolve the existing cinema endpoint contract/inventory mismatch separately; no config externalization failures remain in the focused verification.

## Current snapshot

- Reconfirmed: 2026-06-25 (Asia/Ho_Chi_Minh).
- Git HEAD: `2e2287a`.
- Progress source: `phase_mapping_srs_vs_docx_refund_role_updated.md`, updated 2026-06-25.
- Stack: Spring Boot 3.5.13, Java 17 source, Maven Wrapper, PostgreSQL runtime target, H2 tests.
- Controller structure: 33 domain-specific controllers; the previous consolidated-controller snapshot is obsolete.

## Verification performed

```powershell
.\mvnw.cmd -DskipTests package
.\mvnw.cmd test
```

Results:

- Package build: `BUILD SUCCESS`.
- Full test process completed.
- 17/18 reported test classes passed.
- Only `CinemaShowtimeEndpointInventoryTests` failed: the test expects full paths inside method-level annotations, while `AdminCinemaController` correctly composes class-level `@RequestMapping("/api/v1/admin/cinema")` with `@GetMapping`, `@PostMapping`, etc.
- `CinemaShowtimeIntegrationTests` passed.
- Previous failures caused by missing consolidated controller files are no longer applicable.

## Reconfirmed phase status

| Phase | Verified status | Evidence from current repository | Remaining gap |
|---|---|---|---|
| 0 — Shared Foundation | Done | `ApiResponse`, global error handler, JWT security, OpenAPI, correlation/request filters, auditing; foundation tests pass | Improve production observability/OpenAPI error examples |
| 1 — Database Migration | Partial / blocked | `V1`–`V5` and seeders exist | Flyway disabled/missing runtime; migrations use SQL Server dialect, not PostgreSQL; `ddl-auto=update` remains |
| 2 — Auth, Customer & Security | Done with RBAC mismatch | Register/login/Google/refresh/logout/OTP/reset/profile/admin user management; auth tests pass | Role flow says check-in is STAFF-only, but code also permits ADMIN |
| 3 — Movie, Genre & Actor | Done | Public catalog, admin CRUD, relations; movie/actor integration and inventory tests pass | No material backend gap identified |
| 4 — Cinema, Room, Seat & Showtime | Partial | Single-cinema service, room/seat/layout, showtime CRUD/bulk/overlap, seat map and automatic `SCHEDULED -> OPEN -> COMPLETED` scheduler exist; integration test passes | Contract mismatch: mapping says no cinema CREATE/DELETE, code exposes both; one stale inventory assertion |
| 5 — Booking, Seat Locking, F&B & QR | Partial | Hold, booking, ticket pricing, F&B, QR, cleanup scheduler and check-in exist; showtime-level pessimistic locking enforces one active checkout per customer/showtime | No database active-seat unique constraint; QR is not signed; STAFF-only role policy not enforced |
| 6 — Payment & Refund | Partial | VNPay/mock payment, return/IPN, amount/signature/idempotency basics, customer refund request, admin request/mark-refunded, wallet refund on showtime cancellation | No VNPay Refund API; no dedicated refund entity/proof/history/claim; customer refund lacks time-policy check; manual mark-refunded does not update `Payment.status` |
| 7 — Loyalty & Notification | Partial | Earn/redeem/revoke loyalty, notification persistence, unread list, mark one/all read, payment and showtime-cancel notifications exist | No transaction ledger; no complete refund event types/claim notifications; no realtime push |
| 8 — Review | Partial, not “not implemented” | Customer create/update/delete, `USED` eligibility, one review/movie, public list/average and admin moderation are implemented | No dedicated integration suite; current review starts `VISIBLE` rather than requiring approval as stated in mapping |
| 9 — Staff Operations, Audit & Reports | Partial, not “not implemented” | STAFF QR check-in exists; admin revenue/top-movie/occupancy reports exist; audit entity/repository exists | No staff manual booking lookup/combo pickup; audit logging is not wired into operations; report tests missing; ADMIN currently can check in |
| 10 — Storage, Email, WebSocket & Scheduler | Partial | Cloudinary upload, OTP email, hold cleanup and showtime status schedulers exist | No ticket email; no seat/notification WebSocket; no upload delete/management UI/API |
| 11 — Recommendation & AI Analysis | Partial | Hybrid recommendation, signal weights, preference profile, favorite actors and admin debug exist; recommendation test passes | No distinct AI movie-content analysis workflow/model integration found |
| 12 — Integration & QA | Partial | Build passes; auth/catalog/booking/payment/recommendation/foundation tests and Postman collection exist | One inventory test fails; no concurrency, refund incident, refund claim/proof/history, report/review or comprehensive RBAC tests |

## Refund confirmation

### Existing customer-policy/manual refund

Implemented in a basic form:

```text
PAID or CANCELLED -> REFUND_REQUESTED -> REFUNDED
```

Evidence:

- `POST /api/v1/bookings/{bookingId}/refund-request`
- `POST /api/v1/admin/bookings/{bookingId}/refund-request`
- `POST /api/v1/admin/bookings/{bookingId}/mark-refunded`
- `BookingServiceImpl`

Confirmed gaps against the updated mapping:

- Code accepts both `PAID` and `CANCELLED`; mapping requires customer-policy refund from `PAID`.
- No minimum time-before-showtime policy.
- Ownership is checked for customer request.
- `USED` cannot enter the flow because it fails current status validation.
- Marking booking refunded does not mark the associated `Payment` as `REFUNDED`.
- There is no gateway refund reference or provider refund proof.

### Existing showtime-cancellation compensation

When ADMIN cancels a showtime:

- `HOLDING`/`PENDING_PAYMENT` bookings become `CANCELLED`.
- `PAID`/`REFUND_REQUESTED` bookings become `REFUNDED`.
- Booking amount is credited to the internal wallet.
- Earned loyalty points are revoked.
- A notification is persisted.

This is incident-like compensation, but it is not the requested Cinema Incident Refund module because there is no incident record, refund record, gateway refund, proof, history or claim workflow.

### Cinema Incident Refund

Status: not implemented / blocked by scope decision.

Confirmed absent:

- `CinemaIncident`, `Refund`, `RefundClaim` entities/repositories.
- Incident/refund/claim enums.
- Incident overlap query and APIs.
- Refund gateway adapter or `MockPaymentGatewayRefundService`.
- Refund proof/history APIs.
- Missing-refund claim create/approve/reject APIs.
- Dedicated audit and QA coverage.

Required role decision:

- Updated role flow says STAFF only checks in and ADMIN handles refund.
- Current security also lets ADMIN check in.
- Default for incident refund should therefore be ADMIN-only until role documents and security explicitly authorize STAFF.

## Differences from the mapping document

The following mapping statements are stale relative to current code:

1. Phase 4 says showtime scheduler is missing; it exists in `ShowtimeStatusScheduler`.
2. Phase 4 says cinema has no CREATE/DELETE; current admin API exposes both, with DELETE implemented as deactivation.
3. Phase 7 says mark-all-read is missing; it is implemented.
4. Phase 8 says review is not implemented; backend review flow is implemented.
5. Phase 9 says reports are not implemented; three admin report endpoints are implemented.
6. Phase 10 says showtime scheduler is missing; both showtime and hold-cleanup schedulers exist.
7. Role flow says ADMIN cannot check in; current security/controllers permit ADMIN check-in.

## Prioritized next actions

1. Decide and enforce role ownership for check-in and incident refund.
2. Fix `CinemaShowtimeEndpointInventoryTests` to understand class-level + method-level mappings.
3. Convert migrations to PostgreSQL and enable Flyway with Hibernate `validate`.
4. Add a database-level active-seat constraint and a true parallel-request hold test; service-level showtime locking is now in place.
5. Implement Cinema Incident Refund in isolated phases: schema, incident, gateway refund, claim, proof/history, notification/audit/tests.
6. Add dedicated review, report, scheduler and RBAC integration tests.

## Handoff rule

Future sessions must update:

- selected phase/feature;
- scope and out-of-scope;
- files changed;
- commands actually run;
- pass/fail evidence;
- unresolved blocker and next concrete action.

Do not mark a partial phase `done` merely because its primary controller exists.

## 2026-07-23 — Standalone concession pickup QR

- Added a one-time `CINEAI:FOOD` QR for paid standalone food orders.
- Added staff/admin lookup and pickup-confirmation endpoints with an audited `PAID -> PICKED_UP` transition.
- Added `picked_up_at`, PostgreSQL status-constraint compatibility, and migration `V16`.
- Updated customer order history to render the QR only while it is valid and show completion time afterward.
- Updated the staff scanner to distinguish ticket QR from food QR, display receipt lines, and prevent duplicate handoff.
- Verification passed: `mvnw -DskipTests compile`, `mvnw -Dtest=PaymentIntegrationTests test` (6 tests), and frontend `npm run build`.
- Startup follow-up: clean Spring context test passed and both `FoodOrderServiceImpl`/`QrTicketServiceImpl` are discoverable. The reported missing-bean state was caused by an IntelliJ/DevTools process restarting while `target/classes` was being rebuilt. Legacy `commons-logging` was excluded from Cloudinary because Spring already supplies `spring-jcl`.
