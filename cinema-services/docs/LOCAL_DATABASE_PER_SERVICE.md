# Local Database-per-Service

## Scope and safety

Java 21, Docker Desktop (Linux containers), Maven wrapper and PowerShell are required.
Run commands from `cinema-services` in the integration worktree. Never use Neon or a
team database for startup tests: Flyway and seeders write during application startup.
Never stop the existing `mss301-backend` project from another checkout.

The root Compose builds all five Java applications and does not publish DB ports.
`../infra/docker-compose.yml` runs infrastructure only and publishes DB ports
5431/5432/5433/5434, recommendation DB 5435, RabbitMQ 5672/15672. These ports may already
be occupied. AI Compose is separate; its topology and pgvector are not verified here.
Do not start a second stack with the same container names or host ports.

## Configuration

Each application owns its database: Identity `identity_db`, Catalog `catalog_db`,
Booking `booking_db`, Payment `payment_db`. Gateway has no business datasource.
PostgreSQL driver is `org.postgresql.Driver`; use Flyway with JPA `validate`.

For IDE use, copy the module `.env.example` to an ignored `.env` only after reviewing
all fields. Supply development-only credentials outside source control. Examples
are not loaded automatically by Spring. Set the IDE working directory deliberately.
OS/JVM/CLI overrides can change effective configuration; do not assume `.env` wins.

| Caller | IDE destination | Docker destination |
|---|---|---|
| Catalog -> Identity | localhost:8081 | identity-service:8081 |
| Booking -> Payment | localhost:8084 | payment-service:8084 |
| Payment -> Identity | localhost:8081 | identity-service:8081 |

Use `IDENTITY_SERVICE_URL` / `PAYMENT_SERVICE_URL` as appropriate. With non-default
application ports, override these URLs as well. `localhost` inside a container refers
to that container. Root Compose supplies explicit Docker DNS URLs.

## Isolated runtime verification

The following procedure was executed for Task 05.2. Test credentials are disposable;
use one temporary value in `$verifyPassword`, never a production credential.
Application processes are started by Maven/Spring Boot tests, not IntelliJ.
This establishes `LOCAL_JVM_VERIFIED` only for services whose tests pass.

Inspect before setup:

```powershell
git status -sb
docker compose ls
docker ps --format '{{.ID}}|{{.Names}}|{{.Ports}}'
Get-NetTCPConnection -State Listen | Select-Object LocalPort -Unique
java -version
.\mvnw.cmd -version
```

Dedicated test topology:

| Service | Container | JDBC host port | DB user |
|---|---|---:|---|
| Identity | task052-identity-db | 57101 | identity_verify |
| Catalog | task052-catalog-db | 57102 | catalog_verify |
| Booking | task052-booking-db | 57103 | booking_verify |
| Payment | task052-payment-db | 57104 | payment_verify |
| RabbitMQ | task052-rabbit | 57172 | local_verify |

Create only when all names and ports are free. Do not reuse an existing resource
merely because its name matches. The examples below are commands from the executed
procedure with the temporary password replaced by a variable:

```powershell
$verifyPassword = Read-Host 'Temporary disposable test password'
$verifyPorts = @(57101,57102,57103,57104,57172)
if (Get-NetTCPConnection -State Listen | Where-Object LocalPort -In $verifyPorts) {
    throw 'Test ports occupied'
}
docker network create --label task=task052 task052-local
$verifyIndex = 0
foreach ($service in @('identity','catalog','booking','payment')) {
    $port = 57101 + $verifyIndex
    docker run --rm -d --name "task052-$service-db" --label task=task052 `
        --network task052-local -e "POSTGRES_DB=${service}_db" `
        -e "POSTGRES_USER=${service}_verify" -e "POSTGRES_PASSWORD=$verifyPassword" `
        -p "127.0.0.1:${port}:5432" postgres:16-alpine
    if ($LASTEXITCODE -ne 0) { throw 'Database startup failed' }
    $verifyIndex++
}
docker run --rm -d --name task052-rabbit --label task=task052 --network task052-local `
    -e RABBITMQ_DEFAULT_USER=local_verify -e "RABBITMQ_DEFAULT_PASS=$verifyPassword" `
    -p 127.0.0.1:57172:5672 rabbitmq:3.13-management-alpine
docker exec task052-rabbit rabbitmq-diagnostics -q ping
```

Wait for broker readiness before the test. No shared volumes are mounted.

```powershell
.\mvnw.cmd -fae -pl 'identity-service,catalog-service,booking-service,payment-service' -am `
    '-Dtest=LocalDatabaseRuntimeVerificationTest' '-Dsurefire.failIfNoSpecifiedTests=false' `
    '-Dlocal.db.verify=true' "-Dlocal.db.password=$verifyPassword" `
    '-Dlocal.db.identity.url=jdbc:postgresql://127.0.0.1:57101/identity_db' `
    '-Dlocal.db.catalog.url=jdbc:postgresql://127.0.0.1:57102/catalog_db' `
    '-Dlocal.db.booking.url=jdbc:postgresql://127.0.0.1:57103/booking_db' `
    '-Dlocal.db.payment.url=jdbc:postgresql://127.0.0.1:57104/payment_db' test
```

The gated tests load example variables but override datasource directly with high
priority test properties, disable developer `.env` imports and reject other JDBC
hosts/ports/database names. They start the actual application on a random loopback
HTTP port. Mail is disabled, external service URLs point to isolated loopback ports,
and no payment/refund requests are submitted. There is no IntelliJ verification.

Evidence markers in Surefire logs:

- `LOCAL_DB_EVIDENCE`: application datasource JDBC metadata and SQL
  `current_database()`, `current_user()`, `inet_server_addr()`, `inet_server_port()`.
- `LOCAL_DB_MIGRATIONS`: successful Flyway history count.
- `LOCAL_DB_HEALTH`: HTTP `/actuator/health` status.

A PostgreSQL server port of 5432 is expected even when Windows publishes 57101-57104.
`pg_isready` alone does not establish application connectivity. A failed startup
test provides no datasource/health PASS; inspect its first root cause instead.

Compile verification:

```powershell
.\mvnw.cmd -pl 'api-gateway,identity-service,catalog-service,booking-service,payment-service' -am -DskipTests compile
```

## Manual IDE procedure (guidance, not IDE-verified)

Start only isolated infrastructure. Configure each Run Configuration with its own
DB URL/user/password, loopback server address and unused application port. Point
clients at the corresponding JVM ports. Start Identity/Catalog, then Booking/Payment,
then Gateway. Calls made before a peer starts may fail; health is not an end-to-end
integration proof. Check `/actuator/health` on each actual application port.

Before enabling external integrations, replace example provider values deliberately.
Do not disable authentication, Hibernate validation or migration checks to obtain green.

## Cleanup

Stop only exact resources created by this procedure, after checking ownership:

```powershell
docker ps --filter label=task=task052 --format '{{.Names}}|{{.Ports}}'
docker inspect task052-identity-db --format '{{json .Config.Labels}}'
# Verify the task label on every target first.
docker stop task052-identity-db task052-catalog-db task052-booking-db task052-payment-db task052-rabbit
docker network inspect task052-local
# Remove this network only after verifying its label and that no endpoints remain.
docker network rm task052-local
```

Containers use `--rm`; their disposable data is removed on stop. Never use
`docker compose down -v` or prune to clean this experiment.

## Troubleshooting

- Port conflict: select a separate test topology; do not stop the team's stack.
- Unknown Docker hostname in IDE: use published loopback ports.
- Connection refused from Docker localhost: set Docker DNS service URL.
- Missing placeholders: examples are not automatic Spring property sources.
- Neon host in effective datasource: stop before migrations and identify the override.
- Health DOWN: distinguish DB, broker and mail indicators; do not infer DB failure
  solely from aggregate health.
- Flyway/JPA mismatch: record the exact mismatch; do not change business entities or
  production migrations within a configuration-only task.

## Recorded Task 05.2 results (2026-10-24)

| Application | Effective database/user | Host JDBC port | Flyway successes | HTTP port/result |
|---|---|---:|---:|---|
| Identity | identity_db / identity_verify | 57101 | 4 | 62282 / 200 UP |
| Catalog | catalog_db / catalog_verify | 57102 | 14 | 60873 / 200 UP |
| Booking | booking_db / booking_verify | 57103 | 5 | Startup blocked by JPA validation |
| Payment | payment_db / payment_verify | 57104 | 9 | 57374 / 200 UP |

For the three successful services, application DataSource metadata and the SQL
identity query agreed with the database/user above. PostgreSQL reported internal
port 5432. The application web contexts ran in Maven test JVMs with the default
profile, random loopback HTTP ports, real Flyway/JPA/security and the isolated broker.
They were not four simultaneously deployed services and were not launched by IDE.

Booking's clean database was migrated successfully, then Hibernate rejected the
schema: `Schema-validation: missing column [customer_email_snapshot] in table [bookings]`.
Source also declares `customer_name_snapshot` and `customer_phone_snapshot`; a
read-only information_schema query found no customer-prefixed columns after all
five migrations. No production migration/entity was modified and validation was
not disabled. Booking runtime remains FAILED pending authorization to repair the
schema/migration mismatch. This run is not a four-service runtime PASS.

Read-only DB checks found respectively 12/24/9/9 public tables in
identity/catalog/booking/payment databases, including Flyway history. Cross-server
login attempts identity->catalog, catalog->booking, booking->payment and
payment->identity using the caller's test role were all rejected. Roles are bootstrap
roles within separate disposable servers; this does not prove least-privilege grants
inside a shared server.

Five-module Maven compile passed. Final focused startup test outcomes are three
passes and one startup error (Booking); earlier logging/bootstrap harness errors
were corrected. No full regression suite was run. AI/pgvector application connectivity
was not verified. The existing team's containers were not targeted by any mutation.

## Task 05.2.1 repair and successful rerun (2026-10-24)

This section supersedes the initial Booking startup blocker above. Authorization
was granted to add new Booking migrations, without altering V1-V5 or business logic.

| Migration | Table / added columns | Type / nullable |
|---|---|---|
| V6 | bookings.customer_name_snapshot, customer_email_snapshot | VARCHAR(255), YES |
| V6 | bookings.customer_phone_snapshot | VARCHAR(50), YES |
| V7 | bookings.showtime_end_snapshot | TIMESTAMP, YES |
| V8 | food_orders.cinema_id | BIGINT, YES |
| V8 | food_orders.cinema_name | VARCHAR(255), YES |
| V8 | food_orders.cinema_address | VARCHAR(500), YES |

V7 and V8 address additional mismatches confirmed by real Hibernate validation
after the preceding migration. All seven columns already had matching nullable
entity mappings. Existing statuses, amounts, payment fields and seat constraints
were not changed.

`BookingSchemaMigrationPostgresTest` verifies both V1-to-latest clean installation
and V5-to-latest upgrade on unique disposable schemas. It seeds historical Booking
and Food Order rows, compares their pre-existing values after upgrade, checks new
column lengths/nullability, retains the active-seat unique index and validates all
Booking service entity mappings using Hibernate `validate`. No table is truncated
or dropped to make the upgrade pass. The upgraded schema accepts snapshot writes.

Executed tests (with the disposable JDBC properties documented above):

```powershell
# Four application verifications were rerun; Booking was rerun after V8 separately.
.\mvnw.cmd -pl booking-service `
    '-Dtest=LocalDatabaseRuntimeVerificationTest,BookingSchemaMigrationPostgresTest,BookingInputValidationTest,BookingOwnershipTest,BookingLoyaltySecurityTest,BookingExpirationLifecycleTest,BookingResponseCompatibilityTest,BookingSeatConcurrencyIntegrationTest,PaymentEventListenerLifecycleTest,SeatHoldCleanupSchedulerTest' `
    '-Dlocal.db.verify=true' '-Dlocal.db.booking.url=jdbc:postgresql://127.0.0.1:57103/booking_db' `
    "-Dlocal.db.password=$verifyPassword" test
```

Final Booking group: 42 tests, 42 passed, 0 failures/errors/skips. Other three
application startup verifications: one passed per service. Combined final targeted
verification: 45 passed. This is not a full microservice regression or deployed E2E.
Seat-concurrency test infrastructure now binds its own disposable DB to loopback
and checks TCP readiness, rather than the temporary initialization Unix socket.

| Application | Database/user | JDBC host port | Flyway count | HTTP health |
|---|---|---:|---:|---|
| Identity | identity_db / identity_verify | 57101 | 4 | 50709 / 200 UP |
| Catalog | catalog_db / catalog_verify | 57102 | 14 | 63895 / 200 UP |
| Booking | booking_db / booking_verify | 57103 | 8 | 65160 / 200 UP |
| Payment | payment_db / payment_verify | 57104 | 9 | 59746 / 200 UP |

All four are `LOCAL_JVM_VERIFIED`, using application DataSource metadata, SQL and
real loopback web servers. Test databases were PostgreSQL 16 in separate containers
named `task0521-{service}-db`, with `task=task0521` labels and network `task0521-local`.
Broker: `task0521-rabbit`, loopback 57172. The same cleanup ownership checks apply
using these task-specific names. No existing stack or Neon database was used.
AI and service-to-service deployment verification remain outside this task.

## Task 05.3 — simultaneous HTTP runtime

See [LOCAL_SERVICE_INTEGRATION.md](LOCAL_SERVICE_INTEGRATION.md) for the five-JVM
loopback runtime, real Gateway/service-to-service scenarios, dedicated task053
containers, failure findings and cleanup. This extends the sequential startup
verification above; it does not replace Booking V6–V8 or relax Hibernate validation.
Java HTTP integration and AI runtime verification are reported separately.
