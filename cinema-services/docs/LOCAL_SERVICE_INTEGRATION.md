# Task 05.3 — Local Java HTTP integration

Verification date: 2026-10-24. This is a local JVM integration runtime, **not**
IntelliJ verification or a full deployed E2E. AI runtime was not launched.

Task 05.3 results below are historical baseline evidence. The Task 05.3.1 section
at the end records the wallet/availability repairs and their separate verification.
For the final combined review and PR draft, see [INTEGRATION_PR_READINESS.md](INTEGRATION_PR_READINESS.md).

## A. Git and safety

Worktree: `D:\FPTK9\MSS\MSS301-Backend-integration`.
Branch: `feature/microservices-integration-audit`; HEAD `ed62e84`.
Task 05.2–05.2.1 changes were uncommitted and preserved. No commit/push/merge,
reset/clean/stash, external payment, production email, or Neon access was performed.
The existing Compose project `mss301-backend` belongs to the other checkout
`D:\FPTK9\MSS\MSS301-Backend`; its containers/networks/volumes are out of scope.

## B. Simultaneous runtime and ports

```text
HTTP client → Gateway :15800
                 ├→ Identity :15801 → identity_db :57101
                 ├→ Catalog  :15802 → catalog_db  :57102
                 ├→ Booking  :15803 → booking_db  :57103
                 └→ Payment  :15804 → payment_db  :57104
Booking/Payment → dedicated RabbitMQ :57172
Java ↔ Java: loopback HTTP + internal service secret
```

All host bindings use `127.0.0.1`. Four separate PostgreSQL 16 containers,
one RabbitMQ 3.13 container, network `task053-local`, label `task.owner=task053`.
Database data uses container tmpfs, not existing volumes. Credentials/secrets are
generated per run and must not be published. AI target :15805 and disabled VNPay
target :15806 intentionally have no application behind them.

| JVM | PostgreSQL database / user | JDBC ApplicationName | Successful Flyway versions |
|---|---|---|---:|
| Identity | identity_db / identity_verify | task053-identity | 4 |
| Catalog | catalog_db / catalog_verify | task053-catalog | 14 |
| Booking | booking_db / booking_verify | task053-booking | 8 |
| Payment | payment_db / payment_verify | task053-payment | 9 |

These counts come from actual database history. Catalog has 14 migrations despite
its highest version being 15. Runtime JDBC arguments, `pg_stat_activity` application
names/users/databases, Hibernate validation and HTTP health confirm isolation.
Original five JVMs were PID 27476, 28532, 30332, 10664, 21936; controlled restarts
changed PIDs. All five health endpoints returned 200 while the applications coexisted.

## C. Communication matrix (fresh source audit)

Every listed Java internal request sends `X-Internal-Service-Secret` from
`app.internal.secret`. `ApiResponse<T>` envelope is expected unless noted.
“Default” timeout means no explicit client timeout in this source, not a guaranteed bound.

| Caller → target | Property / environment key | Actual paths | Connect/read timeout | Failure behavior |
|---|---|---|---|---|
| Identity → Catalog | catalog.service.url / CATALOG_SERVICE_URL | GET /internal/v1/cinemas/{id} | Default | missing→404; unavailable→400; assignment not persisted |
| Catalog → Identity | identity.service.url / IDENTITY_SERVICE_URL | POST /internal/v1/audit-logs | Default | logs failure, moderation may continue |
| Catalog → Booking | booking.service.url / BOOKING_SERVICE_URL | GET /internal/v1/bookings/showtimes/{id}/occupied-seats; /verify-eligibility; POST /showtimes/{id}/cancel-and-refund | Default | empty seats / empty eligibility / null cancellation on error |
| Booking → Catalog | catalog.service.url / CATALOG_SERVICE_URL | POST /internal/v1/catalog/checkout-quote; /internal/v1/foods/quote; GET /internal/v1/catalog/showtimes/{id}/scope; /reporting | 2000/3000 ms, configurable | quote→400; scope fail-closed; food quote→null |
| Booking → Identity | identity.service.url / IDENTITY_SERVICE_URL | GET /internal/v1/users/{id}/scope; POST /internal/v1/mail/refund-notice | Default | scope→401/403; mail failure logged |
| Booking → Payment | payment.service.url / PAYMENT_SERVICE_URL | GET /internal/v1/payments/bookings/{id}/status; /food-orders/{id}/status; /internal/v1/loyalty/config; POST /internal/v1/wallets/credit; /internal/v1/loyalty/award; /refund | Default | booking status failure→active=true, blocking repricing; food paid=false; config/credit null; award throws; refund logs |
| Payment → Booking | booking.service.url / BOOKING_SERVICE_URL | GET /internal/v1/bookings/{id}; /internal/v1/food-orders/{id}; POST each /{id}/mark-paid | Default | lookup404→not found; other failure throws; mark-paid outcome unknown throws |
| Payment → Identity | identity.service.url / IDENTITY_SERVICE_URL | GET /internal/v1/users/{id}; /{id}/scope | 2000/3000 ms | null on lookup failure; manager authorization fails closed |

All runtime URLs are `http://127.0.0.1:1580N` for the actual target process.
The prior config repairs in root Compose and module examples remain unchanged.
Mail/refund/wallet mutation contracts were audited but not all exercised financially.

## D. Gateway routes

| Target | Representative real route groups | Runtime result |
|---|---|---|
| Identity | auth/login; users/me; admin/users; admin/staff-profiles; staff/profiles; admin/audit-logs | JWT login/profile/admin/scoped staff/audit HTTP verified |
| Catalog | movies; cinemas; showtimes and seat-map; foods/items; admin/genres; admin/reviews | public read and authorized writes verified |
| Booking | bookings/hold; checkout; items; admin/bookings | real quote, persistence, scope and price guard verified |
| Payment | payments/mock; wallet; loyalty/config; admin/loyalty/config and add | routes reach Payment; first-use wallet defect below |
| Internal | /internal/** | Gateway returns404; no public route added |
| AI | recommendations/**; chat rewrite; singular legacy recommendation rewrites; search/adaptive | configuration + HTTP contract-probe tests only |

Fixed the two missing Identity route groups and the confirmed FastAPI search path.
Removed duplicate hardcoded `AddRequestHeader=X-Gateway-Secret` filters: the trusted
filter already sets the configured Java/AI secret. No JWT ownership rule changed.

## E–F. Authentication and internal security

Gateway does not itself validate JWTs. It forwards Bearer tokens; downstream Spring
Security verifies signatures, expiry and authorities. Identity additionally loads
current user/roles from its database. Scope-sensitive Booking/Payment paths obtain
authoritative Identity scope rather than trusting client headers.

Real Gateway HTTP checks covered missing/invalid JWT (401), valid JWT, spoofed
X-User-Id/Roles/Email/Cinema-Id and internal secret (no privilege gain), and internal
path404. Direct internal requests to all four services reject missing/wrong secret403
and accept the dedicated test secret. No security filter was disabled.

## G–K. Business HTTP evidence

- Identity→Catalog: assign staff to active cinema1 succeeds; nonexistent cinema404.
- Catalog→Identity: review moderation invokes AuditClient; audit row checked in Identity DB.
  Genre audit is log-only in current source and is not claimed as cross-service evidence.
- Catalog→Booking: real seat-map requests occupied seats; missing showtime404.
- Booking→Catalog: invalid quote400; valid quote creates persisted hold with price/snapshots;
  checkout and item update map DTOs correctly.
- Booking→Identity: Manager's booking query uses cinema1 even when cinema999 is requested;
  returned rows are checked for cinema1. This list contract coerces scope, not403.
- Booking→Payment: disposable legacy redeemed-points fixture triggers real payment status
  lookup before resetting points; guard reset verified in Booking SQL.
- Payment→Booking: missing lookup404, cross-owner404; existing MOCK payment endpoint on a
  disposable booking invokes real lookup and mark-paid; Booking PAID and Payment SUCCESS
  checked. This is not VNPay integration or event-contract verification.
- Payment→Identity: profile verification for points addition and Manager cinema scope;
  same-cinema read/write succeeds, other cinema403, missing user404.
- Manager staff: both canonical/alias routes allow staff cinema1 and deny staff cinema999,
  including spoofed headers. Admin global routes work.

## L. Failure isolation and confirmed issues

Only owned JVM PIDs are stopped/restarted. Identity down: Loyalty write403, Booking
scope401, unrelated Catalog read200, no extra configuration row. Catalog down:
assignment/quote400; Payment config unaffected. Payment down: Booking refuses legacy
loyalty repricing400 and preserves points. Booking down: Payment lookup500 with no new
payment row. Controlled HTTP-server fault tests separately cover4xx/5xx and Identity
read timeout; these are **not** real downstream application fault responses.

Open issues, intentionally not hidden or fixed by changing unrelated financial logic:

1. `WalletServiceImpl.getWallet` is read-only but calls `getOrCreateWallet`. A new user
   gets500 on PostgreSQL (`cannot execute INSERT in a read-only transaction`). Reproduced
   through Gateway, retained as a failing HTTP assertion. Existing-wallet reads are not
   a substitute for first-use verification.
2. Catalog BookingClient catches unavailable Booking and returns empty occupied seats.
   Seat-map consequently returns200/AVAILABLE rather than unavailable/unknown. Booking
   remains the final hold authority, but availability is misleading during outage.
3. Several clients lack explicit connect/read deadlines. Refused connections were finite;
   black-hole timeout guarantees cannot be claimed for those clients.
4. Some service error envelopes include internal exception/SQL details. No test secret
   leak was observed, but error-response sanitization needs separate review.
5. First cold quote after Catalog restart exceeded Booking's3-second read limit under
   concurrent test load. Error was400/no hold, not false success. The harness explicitly
   warms the quote endpoint after restart; that does not prove cold-start latency is solved.

Therefore failure isolation is PARTIAL and full integration merge readiness is blocked.

## M. AI compatibility (not runtime)

FastAPI source: `api/recommendation_api.py`, `api/search_api.py`, `api/chat_api.py`,
registered by `app/main.py`. Actual paths: `/api/v1/recommendations/users/{id}`,
`/movies/{id}/similar`, `/api/v1/search/adaptive`, `/api/v1/chat/message`.
Gateway legacy rewrites and search now target those paths; contract-probe HTTP tests
verify target selection, rewrites and exactly one configured secret header.

Remaining mismatches: Catalog calls singular `/api/v1/recommendation/collaborative/{id}`
and `/content/{id}` directly to AI, expects bare arrays, while FastAPI returns envelopes
with structured recommendation data. Gateway rewrite does not repair direct Catalog
calls. Catalog's local fallback is not a Gateway fallback. Existing chat wildcard maps
all subpaths to `/message`; only the supported message contract was verified.
Java examples use port8000, AI Compose defaults8032, and root Compose's recommendation
service is a different target. Align hostname/port when introducing a dedicated AI stack.
AI requires pgvector/broker/model/API configuration; none was faked or launched here.
`AI_ROUTE_COMPATIBILITY=PARTIAL`, `AI_HTTP_INTEGRATION=NOT_VERIFIED`.

## N. Reproduction and test categories

From this worktree's `cinema-services`, PowerShell with Java21 and Docker Desktop:

```powershell
.\mvnw.cmd package -DskipTests
.\scripts\Start-Task053Runtime.ps1
# Wait for ALL five loopback /actuator/health endpoints to return200.
.\scripts\Test-Task053Http.ps1
# Current source includes the Task 05.3.1 repairs: expect 112 passing checks.
.\mvnw.cmd test '-Dspring.config.import=optional:file:.env.example[.properties]' '-Dlogging.pattern.level=%5p'
```

Build **before** launch; Windows locks running jars. Never repackage a running module's
jar. Health checks are necessary but not sufficient for cold quote readiness.
The HTTP script uses actual services/DBs/broker and existing MOCK payment only;
fixture SQL is guarded by container ownership labels, and resets only disposable
Booking fixture rows. The CSV under `target` records names/status/expected/pass/time,
not JWTs or secrets. CLI state file contains disposable credentials and must stay private.

`GatewayRouteConfigurationTest`:3 tests, initially3 red, then green.
`ConfiguredGatewayHttpTest`: actual Gateway config with five stub targets; no business
security or AI runtime claim. `IdentityClientHttpFailureTest`:2 bounded HTTP fault tests.
Existing component/MockMvc tests use mocks/H2 where their source specifies. Opt-in
PostgreSQL/Rabbit suites remain distinct from default suite skips. Final counts are
recorded below.

### Recorded verification results

| Verification | Total | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Configured Maven reactor suite | 331 | 296 | 0 | 0 | 35 |
| Booking PostgreSQL migrations (separate run) | 2 | 2 | 0 | 0 | 0 |
| Payment PostgreSQL concurrency (separate run) | 16 | 16 | 0 | 0 | 0 |
| Latest result per unique Maven test case | 331 | 314 | 0 | 0 | 17 |
| Five-app HTTP script, including SQL assertions | 88 | 86 | 2 | 0 | 0 |

Reactor module totals: Gateway7/0 skipped; Identity33/9 skipped; Catalog68/1 skipped;
Booking129/5 skipped; Payment94/20 skipped. The two supplemental runs activate18
previously skipped cases; they are not extra unique tests. Remaining skips: Identity
authorization PostgreSQL8, four per-service startup probes, Rabbit poison-message2,
Payment admin-loyalty PostgreSQL3. None was newly disabled or weakened.

Both HTTP failures are retained: `valid-wallet` (500 instead of200) and
`booking-offline-seat-availability-fails-closed` (paid seat1 reportedAVAILABLE).
The HTTP script exits nonzero on these failures. This is not an all-green E2E result.

Important methods/classes:

- `GatewayRouteConfigurationTest`: `identityAliasesAndAuditLogHaveRealRoutes`,
  `adaptiveSearchRoutesToAiWithoutRewritingPath`, `aiSecretComesOnlyFromTrustedFilter`.
  Red baseline3 failures; post-fix3 pass.
- `ConfiguredGatewayHttpTest.configuredRoutesChooseTargetPreservePathsAndSetOneTrustedSecret`:
  real Gateway HTTP, five local stub targets,9 route/rewrite samples; passed.
- `IdentityClientHttpFailureTest.downstream4xxAnd5xxCannotGrantScopeOrProfile` and
  `readTimeoutFailsClosedWithinBoundedTime`: real client + controlled local HTTP faults;
  both passed. No application-level Identity stub is counted as live integration.
- `BookingSchemaMigrationPostgresTest.cleanInstallationMatchesAllEntityMappings` and
  `upgradingV5PreservesExistingBookingsAndAllowsSnapshots`: actual PostgreSQL/Flyway/
  Hibernate validation; both passed, no mocked database.
- `PaymentPendingConcurrencyPostgresTest`:3 cases with actual unique indexes.
  `PaymentServicePostgresConcurrencyIntegrationTest`:13 cases with Spring/JPA/PostgreSQL;
  BookingClient is mocked in that class, unlike the five-app HTTP script.
- Existing authentication/ownership, wallet, Staff Profile, Manager scope, admin loyalty,
  VNPay idempotency, booking expiration and seat-concurrency classes ran as part of the
  configured reactor suite. VNPay tests use controlled test data, not live VNPay.

Evidence retained locally (gitignored): `task053-regression-configured.log`,
`task053-migration.log`, `task053-payment-postgres.log`, `task053-http-final.log`,
`target/task053-http-results.csv`, `target/task053-test-index.csv`.
The initial plain `mvn test` failed4 Identity smoke-test methods due to missing
`SPRINGDOC_API_DOCS_ENABLED`. Importing `.env.example` explicitly supplies required
placeholders; no assertions or production defaults were removed to obtain green.
An intermediate Gateway repackage failed because Windows locked the running jar;
stopping only that owned JVM and rebuilding resolved it. Its tests had already passed.

Booking migration verification (owned DB must still be running):

```powershell
$state=Import-Clixml target/task053-state.clixml
.\mvnw.cmd -pl booking-service '-Dtest=BookingSchemaMigrationPostgresTest' '-Dlocal.db.verify=true' '-Dlocal.db.booking.url=jdbc:postgresql://127.0.0.1:57103/booking_db' "-Dlocal.db.password=$($state.Password)" test
.\mvnw.cmd -pl payment-service '-Dtest=PaymentPendingConcurrencyPostgresTest,PaymentServicePostgresConcurrencyIntegrationTest' '-Dpayment.concurrency.test.url=jdbc:postgresql://127.0.0.1:57104/payment_db' '-Dpayment.concurrency.test.username=payment_verify' "-Dpayment.concurrency.test.password=$($state.Password)" '-Dpayment.service.test.jdbc-url=jdbc:postgresql://127.0.0.1:57104/payment_db' '-Dpayment.service.test.username=payment_verify' "-Dpayment.service.test.password=$($state.Password)" test
```

## O–Q. Changes, limitations and cleanup

Task05.3 additions: Gateway route config, two Gateway test classes, Payment Identity
client fault test, two PowerShell runtime/HTTP scripts, this guide and runbook link.
Previous migrations V6–V8 and entity validation remain intact. No business service Java
implementation was modified. No new migration/dependency/architecture/event contract.

Cleanup only task-owned processes and resources:

```powershell
.\scripts\Start-Task053Runtime.ps1 -Stop
docker ps --filter label=task.owner=task053
git diff --check
```

Stop verifies JVM jar paths and container owner labels. Never use Compose down/prune,
shared volumes, or a name wildcard. Disposable tmpfs fixture data is lost on stop;
source, logs and CSV remain. Check original container IDs against preflight inventory.
Do not commit the credential state or logs. No actual commit or merge was performed.

### Final state and verdict

Compile completed successfully for all five Java modules. `git diff --check` passed.
Owned JVMs, all five task053 containers and task053 network were stopped/removed;
the credential state file was deleted by the cleanup script. Disposable tmpfs fixture
data is not recoverable. All15 pre-existing container IDs/names matched preflight.
No existing volume/network/container was removed or restarted by this task.

```text
TASK05_3 = PARTIAL
FIVE_JAVA_APPS_CONCURRENT = PASS
DATABASE_PER_SERVICE_PRESERVED = PASS
GATEWAY_IDENTITY = PASS
GATEWAY_CATALOG = PASS
GATEWAY_BOOKING = PASS
GATEWAY_PAYMENT = FAIL
GATEWAY_JWT_SECURITY = PASS
GATEWAY_INTERNAL_BLOCK = PASS
IDENTITY_CATALOG_HTTP = PASS
CATALOG_BOOKING_HTTP = PASS
BOOKING_PAYMENT_HTTP = PASS
PAYMENT_IDENTITY_HTTP = PASS
MANAGER_CINEMA_SCOPE = PASS
FAILURE_ISOLATION = PARTIAL
AI_ROUTE_COMPATIBILITY = PARTIAL
AI_HTTP_INTEGRATION = NOT_VERIFIED
SECURITY_REGRESSION = PASS
BOOKING_MIGRATION_REGRESSION = PASS
MAVEN_BUILD = PASS
GIT_DIFF_CHECK = PASS
EXISTING_STACK_PRESERVED = YES
SAFE_TO_COMMIT = YES
SAFE_TO_MERGE = NO
NEXT_PRIORITY = Repair first-use wallet read-only insert and Catalog availability outage behavior; then add bounded client deadlines and sanitize error responses.
```

`GATEWAY_PAYMENT=FAIL` is conservative: Payment routing itself is correct, but the
required wallet request fails500. `SAFE_TO_COMMIT=YES` means this scoped routing/test/
audit snapshot can be checkpointed after human review, not that the whole system is
ready to merge. Security regression PASS refers to the executed suites, not the17
remaining opt-in tests or a production security certification.

## Task 05.3.1 — Wallet first-use và Catalog availability fail-closed

### A–C. Phạm vi, nguyên nhân và transaction của wallet

Giữ nguyên worktree/branch/HEAD và các thay đổi chưa commit 05.2–05.3. Không sửa
migration, JWT/ownership, payment status, seat lifecycle hay luồng RabbitMQ/AI.

`WalletServiceImpl.getWallet()` có `readOnly=true` nhưng helper `getOrCreateWallet()`
có thể INSERT. PostgreSQL từ chối ghi trong transaction đó, dẫn tới HTTP500.
Helper này chỉ được gọi từ `getWallet`; controller vẫn lấy user ID từ principal.

Fix: chỉ `getWallet()` chuyển thành transaction có khả năng ghi. Khi wallet tồn tại,
trả bản ghi hiện tại, không ghi balance/timestamp hay ledger. Khi chưa tồn tại,
repository dùng INSERT zero-balance với `ON CONFLICT (user_id) DO NOTHING`, rồi đọc
lại trong cùng transaction READ COMMITTED. Unique constraint hiện có của
`cine_wallets.user_id` bảo vệ race cho row chưa tồn tại. Không bắt mọi integrity
exception, không tái sử dụng transaction failed và không dựa vào pessimistic lock
trên row chưa tồn tại. Conflict target chỉ là user_id; không update balance khi conflict.
Các method thực sự chỉ đọc vẫn giữ read-only. Credit/withdrawal implementation không đổi.

### D. Wallet tests và side effects

`WalletFirstUsePostgresTest` dùng Spring transaction proxy, JPA repositories, Flyway
V1–V9 và PostgreSQL disposable thật, schema riêng; không mock database/service.

- First use + repeated GET: một wallet, balance0, cùng ID, không transaction/withdrawal.
- Wallet có sẵn: balance123456 và toàn bộ row/timestamps giữ nguyên.
- 8 lời gọi first-use đồng thời: cùng một wallet ID, một row, balance0, không ledger.
- Credit50000 idempotent; withdrawal10000 rồi reject/approve: balance và5 ledger rows
  đúng như flow cũ, chỉ là dữ liệu fixture, không có chuyển khoản ngân hàng.

Targeted: 4 PostgreSQL tests và3 `WalletAuthorizationTest` tests đều PASS.
HTTP harness bổ sung 8 GET đồng thời qua Gateway, repeated GET, side-effect SQL và
spoofed X-User-Id/target userId không đổi chủ ví. WalletResponse vẫn là id/balance/createdAt;
không thêm userId vào DTO để phục vụ test.

### E–G. Catalog contract và mọi caller

Trước fix, `BookingClient.getOccupiedSeats()` trả empty list khi upstream lỗi;
`computeAvailableSeatsForShowtimes()` còn bắt lỗi và tính occupied=0. Hai caller
của client là `getSeatMap()` và hàm compute. Hàm compute được dùng bởi customer
schedule và cả nhánh showtime hiện tại/tương đương trong resolve. Seat-map public
và admin cùng dùng service nên đều nhận fail-closed contract.

Fix ở client/service, không chỉ controller:

- Chỉ chấp nhận `success=true`, data là list không null. Empty list hợp lệ vẫn được phép.
- Mỗi occupancy phải có seatId dương, duy nhất, runtimeStatus thuộc
  HOLDING/BOOKED/UNAVAILABLE. Null/malformed/incomplete/unverified response không phải empty.
- Connection refused, timeout, HTTP403/5xx và payload lỗi phát sinh exception chuyên biệt.
- Connect timeout2000ms, read timeout3000ms; không thêm circuit breaker/cache.
- Bỏ catch-and-continue ở compute. Schedule/resolve không tự giả định occupied=0.
- Handler riêng trả503 theo ErrorResponse hiện có, message cố định
  `Seat availability is temporarily unavailable`, không trả data availability hay secret.
- Handler400/401/403/404 và các business errors khác không bị đổi thành503.

`OccupancyHttpContractTest`: service/controller/handler thật, RestClient thật và
HTTP fault server loopback; repositories là mocks. Sáu tests bao phủ healthy empty,
BOOKED/HOLDING/UNAVAILABLE, refused connection, read timeout,403/500/503 và8 loại
malformed/incomplete response. Ba API được kiểm tra cả status và body:
seat-map, customer-schedule, resolve. Đây không phải deployed E2E hoặc Booking runtime giả.
H2 `CatalogApplicationSmokeTest` giữ assertion200, nhưng nay khai báo mock Booking
healthy/empty rõ ràng thay vì vô tình phụ thuộc fallback khi Booking offline.

### H. Bằng chứng red → green

- Wallet pre-fix PostgreSQL: 4 tests,1 pass/3 errors, lỗi INSERT trong read-only transaction;
  lỗi tương tự được ghi nhận qua Gateway trong `task0531-http-red.log`.
- Catalog pre-fix compiled implementation: 6 tests,2 pass/4 failures; expected503 nhưng200.
- Sau production fix: Wallet PostgreSQL4/4 + authorization3/3; Catalog6/6, không skip.
- Không xóa/disable assertion. Assertion outage trong harness đổi từ mô tả baseline200
  sang bắt buộc503 và `success=false`, không data; giữ kiểm tra không false availability.
- Host/Docker restart đã làm mất runtime disposable giữa lượt đầu và lúc tiếp tục.
  Tài nguyên được kiểm tra ownership và tạo mới; không dùng lại fixture đã mất.
  Launcher cũ đã dừng trước retest để tránh chạy xen hai bộ fixture.

### I–O. Reproduction, regression và giới hạn

Giữ cách chạy5 JVM/4 databases/broker cô lập ở trên. Build khi các JVM task đã dừng
để tránh Windows jar lock. Không chạy build nặng song song5 JVM khi thiếu RAM.
Host từng chỉ còn370MB RAM trống; chỉ JVM test được dừng, không can thiệp stack cũ.
Cleanup script nay bỏ qua container --rm đã mất sau host restart, kiểm tra network
owner label trước xóa và xóa state chứa test credentials sau cleanup.

```powershell
$state=Import-Clixml target/task053-state.clixml
.\mvnw.cmd -pl payment-service '-Dtest=WalletFirstUsePostgresTest,WalletAuthorizationTest' '-Dwallet.test.url=jdbc:postgresql://127.0.0.1:57104/payment_db' "-Dwallet.test.password=$($state.Password)" test
.\mvnw.cmd -pl catalog-service '-Dtest=OccupancyHttpContractTest' test
.\mvnw.cmd package '-Dspring.config.import=optional:file:.env.example[.properties]' '-Dlogging.pattern.level=%5p'
# Start all five owned JVMs from the newly packaged jars, verify health, then:
.\scripts\Test-Task053Http.ps1
```

Không coi mock/H2 là bằng chứng concurrency PostgreSQL. Không gọi VNPay thật, gửi mail
production, dùng Neon/shared database/shared broker, commit/push/merge hay deploy.
AI runtime và các client deadline/error-sanitization khác vẫn là công việc riêng.

### K–O. Kết quả cuối Task 05.3.1 (thay thế verdict lịch sử 05.3)

| Lượt xác minh | Total | Pass | Fail | Error | Skip |
|---|---:|---:|---:|---:|---:|
| Full reactor package/tests | 341 | 302 | 0 | 0 | 39 |
| PostgreSQL bổ sung: Booking2 + Wallet4 + Payment concurrency16 | 22 | 22 | 0 | 0 | 0 |
| Hợp nhất theo test case duy nhất | 341 | 324 | 0 | 0 | 17 |
| Real five-JVM HTTP/SQL checks | 112 | 112 | 0 | 0 | 0 |

112 checks giữ các scenario của bộ88 cũ và thêm24 kiểm tra. Expected status outage
được siết thành503, không bỏ assertion. Hai assertion cũ từng đỏ nay đều PASS.
17 skip còn lại: Identity authorization PostgreSQL8, Admin Loyalty PostgreSQL3,
4 startup probe riêng lẻ và Rabbit poison-message2. Không tuyên bố các ca đó đã chạy.

Sau fault tests, cả5 JVM lại đồng thời health200. PostgreSQL xác nhận đúng
identity_db/identity_verify, catalog_db/catalog_verify, booking_db/booking_verify,
payment_db/payment_verify và application_name tương ứng. Flyway counts4/14/8/9,
Booking V1–V8 không đổi. Public wallet user1003 và1004 mỗi user đúng1 row,
balance0.00; wallet_transactions=0, withdrawal_requests=0 sau các GET/concurrency checks.
Credit/withdrawal regression chạy trong schema PostgreSQL riêng, không phải bank/payment thật.

Artifact evidence (local, gitignored):
`task0531-wallet-red.log`, `task0531-catalog-red-resume.log`,
`task0531-wallet-green-resume.log`, `task0531-catalog-green.log`,
`task0531-regression-build.log`, `task0531-postgres-regression.log`,
`task0531-http-green.log`, `target/task053-http-results.csv`,
`target/task0531-test-index.csv`.

Các file thay đổi riêng cho05.3.1:

- Payment: `repository/CineWalletRepository.java`, `service/impl/WalletServiceImpl.java`.
- Catalog: `client/BookingClient.java`, `service/impl/ShowtimeServiceImpl.java`,
  `exception/GlobalExceptionHandler.java`, `exception/SeatAvailabilityUnavailableException.java`.
- Tests: `WalletFirstUsePostgresTest`, `OccupancyHttpContractTest`, fixture trong
  `CatalogApplicationSmokeTest` (giữ nguyên toàn bộ assertions cũ).
- Harness: `scripts/Start-Task053Runtime.ps1`, `scripts/Test-Task053Http.ps1`.
- Tài liệu: guide này. Các thay đổi05.2–05.3 trước đó vẫn được giữ nguyên.

Đã cleanup5 JVM owned,4 PostgreSQL containers,1 Rabbit container, network task053
và state credentials. Dữ liệu tmpfs fixture đã mất, không phục hồi được; logs/CSV còn.
Các container IDs cũ được bảo toàn, không có thao tác start/stop/remove trên stack cũ.
Host/Docker đã restart bên ngoài tác vụ; Booking của stack cũ đang Exited137 trước
retest và không được agent khởi động lại. Không coi trạng thái stack cũ là kết quả task này.

```text
TASK05_3_1 = PASS
WALLET_FIRST_USE = PASS
WALLET_EXISTING_USER = PASS
WALLET_CONCURRENT_FIRST_USE = PASS
WALLET_OWNERSHIP_REGRESSION = PASS
CATALOG_EMPTY_OCCUPANCY = PASS
CATALOG_BOOKED_SEAT_PROTECTION = PASS
CATALOG_BOOKING_UNAVAILABLE = PASS
CATALOG_BOOKING_TIMEOUT = PASS
CATALOG_INVALID_UPSTREAM_RESPONSE = PASS
CATALOG_SEAT_MAP_FAIL_CLOSED = PASS
CATALOG_SHOWTIME_AVAILABILITY_FAIL_CLOSED = PASS
FIVE_JAVA_APPS_CONCURRENT = PASS
GATEWAY_PAYMENT_HTTP = PASS
GATEWAY_CATALOG_HTTP = PASS
HTTP_CHECKS = 112/112
MAVEN_TESTS = 324/341 (17 skipped; 0 failed/errors)
MAVEN_BUILD = PASS
GIT_DIFF_CHECK = PASS
DATABASE_PER_SERVICE_PRESERVED = YES
NEON_ISOLATION = PASS
EXISTING_STACK_PRESERVED = YES
SAFE_TO_COMMIT = YES
SAFE_TO_MERGE = YES
NEXT_PRIORITY = Human/CI review of the combined change; remaining opt-in suites and AI/client-hardening work remain separate.
```

Merge readiness áp dụng cho scope Java fixes05.3.1 sau human/CI review, không phải
production deployment approval hay full AI/VNPay E2E. Không commit/push/merge/deploy.
