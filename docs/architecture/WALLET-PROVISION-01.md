# WALLET-PROVISION-01 — Customer activation provisions wallet

Date: 2026-10-11. Scope: Backend Identity/Payment only. No public creation API.

## Decision and evidence

The previous `WalletServiceImpl.getWallet` was read-only but called a helper that
saved a missing wallet, causing PostgreSQL SQLState 25006. GET now only selects;
missing data raises the existing domain `NotFoundException` (HTTP 404), not a
synthetic zero-balance response. Successful WalletResponse is unchanged.

Identity owns registration. `register` stores PendingRegistration only. Normal
OTP verification creates and activates the user, assigns CUSTOMER, then records
the activation. New Google login follows the same semantic event. Returning
active Google/password login does not record an activation. Existing unverified
Google/legacy OTP and administrative activation transitions are covered too.
The outbox boundary requires verified ACTIVE status and CUSTOMER role, excluding
staff/manager activation. The role lookup and native insert flush inside the
same writable account transaction (`Propagation.MANDATORY`). Failure rolls back
the activation; no direct broker send runs in that transaction.

Identity had no outbox/event publisher. The implementation reuses the project's
RabbitMQ transport, flat event DTO/JSON String serialization convention and local
transactional-outbox pattern already present in Payment; it adds an Identity-owned
table/publisher rather than accessing Payment's outbox/database. There was no
suitable customer-created/activated event to reuse.

## Contract and delivery

`verified ACTIVE CUSTOMER -> Identity outbox -> RabbitMQ -> Payment upsert -> GET`

- Event: `CustomerActivatedEvent(eventId, userId, occurredAt, correlationId)`.
- `userId` is the existing internal BIGINT identifier, never a Google subject.
- Safe bounded IDs only; invalid HTTP correlation IDs fall back to event UUID.
- No profile fields, password, access/refresh/Google tokens in payload/logs.
- Topic exchange `cinema.identity.events`, key `customer.activated`.
- Durable queue `payment.customer-activated.queue`; persistent messages.
- Identity V5 creates local `outbox_events`, unique semantic key
  `(aggregate_type, aggregate_id, event_type)` and unique event ID.
- Publisher locks a bounded batch with `FOR UPDATE SKIP LOCKED`. It marks PUBLISHED
  only after correlated positive broker confirmation with no mandatory return.
  Nack/return/timeout/disconnection stays PENDING with capped backoff (up to 256s),
  not a terminal event-loss state. Shutdown interruption is preserved.
- One offline failure ends that batch to bound the lock duration. A crash after
  ACK before DB commit may replay, deliberately providing at-least-once delivery.
- Payment's writable service inserts atomically with `ON CONFLICT(user_id) DO
  NOTHING`. Existing ID, balance and timestamps are untouched on replay.
- Scoped Spring AMQP stateless retry: three attempts for transient failures;
  malformed events reject immediately. Exhaustion rejects without requeue to
  `payment.customer-activated.dlq` via existing topic DLX `cinema.dlx`, key
  `dlq.payment.customer-activated`. No custom consumer retry loop.
- Existing AI wildcard `#` DLX binding also receives copies; it is unchanged.
  Restricted broker access must protect event publishing and DLQ reading.
- Legitimate existing wallet write flows (credit/refund/withdrawal) are unchanged.
  The removed GET helper had no other callers.

## Wallet schema audit

Already defined by Payment V1 and confirmed against disposable migrated PostgreSQL:

| Column | Existing definition |
| --- | --- |
| id | BIGINT identity, primary key |
| user_id | BIGINT NOT NULL UNIQUE |
| balance | NUMERIC(12,2) NOT NULL DEFAULT 0 |
| created_at / updated_at | TIMESTAMP NOT NULL |

V1 timestamps have no default; the native provision insert explicitly supplies
both CURRENT_TIMESTAMP values. No Payment schema migration or balance cleanup
is needed. This audit does not attest to drift in a live deployment database.

## Existing users — EXISTING_USER_BACKFILL_REQUIRED

No existing reconciliation job, replayable customer event history or safe admin
backfill command was found. This change provisions future activations; returning
login deliberately does not repair old users. Old customers without wallets get
the safe domain 404 until reconciliation. Backfill has NOT been run or implemented
as a startup hook, data migration, cross-service query or customer endpoint.

Recommended separate, owner-approved reconciliation:

1. Deploy Payment's consumer/queue before enabling Identity publishing. Apply V5
   and confirm matching broker/vhost credentials and declarations in both services.
2. Add a restricted, resumable one-time command/job owned by Identity. Page/keyset
   through Identity's own verified ACTIVE CUSTOMER IDs only, using a captured
   high-water mark and durable progress checkpoint. Do not read Payment's DB.
3. For each eligible ID, call the same outbox service in an Identity transaction;
   unique semantic keys make reruns safe. Customers with PENDING outbox rows need
   publisher recovery, not a second event. Persist each page's progress only after
   its outbox records commit. Never change account state to force an activation.
4. Publish these events through the normal publisher. Payment's unique insert
   ignores already-existing wallets and preserves every non-zero balance.
5. PUBLISHED rows with missing wallets require delivery/DLQ investigation first;
   replay the original authorized event through the same exchange/consumer once
   the cause is fixed. Do not delete outbox dedupe rows or manually INSERT wallets.
6. Reconcile via service-owned counts/checkpoints and authenticated wallet reads.
   Each service inspects only its own DB; exchange safe IDs/reports administratively,
   not cross-database joins. Record failures and resume checkpoints; no blind reset.

This separately authorized job is necessary to call existing-data closure complete.

## Deployment and operations

- Identity now uses the same `RABBITMQ_HOST/PORT/USERNAME/PASSWORD` convention as
  Payment. Example env and root Compose configure it. Do not use example guest
  credentials in production. No real local .env secret was edited.
- Both retain existing default vhost behavior. For a non-default vhost configure
  the standard `SPRING_RABBITMQ_VIRTUAL_HOST` consistently for all participating
  services; Identity-only RABBITMQ_VHOST configuration was removed after review.
- Identity enables correlated confirms and publisher returns; mandatory sends are
  required. An unrouted event remains PENDING until Payment topology is available.
- Alerts/runbook should watch oldest PENDING age, retry count, consumer failures
  and DLQ depth. Logs identify eventId/userId and correlation metadata without
  dumping payloads or exception messages. No new observability platform was added.
- After recovery, PENDING rows retry automatically. After a long DB/broker outage
  exhausts consumer retries, inspect the DLQ, fix the cause, then replay the original
  event with an authorized operator; provisioning remains idempotent. No automated
  DLQ drain/replay or deletion was executed.
- Do not roll back to lazy GET creation. If publishing must pause, keep account
  activation recording enabled; `CUSTOMER_ACTIVATION_OUTBOX_ENABLED=false` pauses
  scheduling only, retaining events. DB migration must remain while new code runs.
- Asynchronous provisioning allows a brief 404 immediately after activation;
  clients retain existing section-level error/retry behavior. No Mobile change.
- Publisher locks only outbox rows during a bounded confirm wait. Database row
  locking through network confirmation is the accepted small-batch trade-off.

## Validation and review

Behavioral RED was captured for the previous GET lazy-write branch and missing
activation hooks, then implementation was applied. Unit tests cover lifecycle
guards, wire conversion, minimum payload, correlation safety, ACK/nack/return/
timeout/interruption, retries, redacted failures and read-only domain errors.
PostgreSQL integration uses actual migrations, Spring JPA transactions/native SQL,
normal/Google auth service boundaries, atomic rollback and concurrent duplicate
events. Google verification/mail/token boundaries are doubles; no Google network.
Identity exports actual event JSON to target; Payment consumes it and verifies
authenticated-principal controller GET 200. The PostgreSQL profile tests this
contract relay. A separate broker profile also exercises real RabbitMQ delivery,
correlated confirms, the discovered production `@RabbitListener`, its argument
conversion, duplicate preservation and retry-to-DLQ routing. Neither profile
tests a deployed Gateway's JWT enforcement or real Google network authentication.

Run default suites/package from `cinema-services`:

```powershell
.\mvnw.cmd -pl identity-service,payment-service verify
```

PostgreSQL tests run under `wallet-provision-integration`, not silently skipped.
Supply a separately owned loopback PostgreSQL cluster on a non-5432 port, databases
`wallet_identity_test` and `wallet_payment_test`, test user `wallet_provision_test`.
Property values must match the real test cluster; PORT below is a placeholder:

```powershell
.\mvnw.cmd -pl identity-service -Pwallet-provision-integration '-Dwallet.test.jdbc-url=jdbc:postgresql://127.0.0.1:PORT/wallet_identity_test' '-Dwallet.test.username=wallet_provision_test' '-Dwallet.test.password=' verify
.\mvnw.cmd -pl payment-service -Pwallet-provision-integration '-Dwallet.test.jdbc-url=jdbc:postgresql://127.0.0.1:PORT/wallet_payment_test' '-Dwallet.test.username=wallet_provision_test' '-Dwallet.test.password=' verify
git diff --check
git diff --cached --check
```

Run Identity first: it generates `identity-service/target/customer-activation-events.json`
required by Payment's cross-boundary integration test (no production fixture).
Default suites do not require a broker or disposable DB. Integration fail-closed
guards refuse non-loopback or differently named DBs and do not skip on failure.

For the additional transport tests, use a separately owned, fresh loopback
RabbitMQ 3.13 instance on a non-5672 port with test-only credentials
`wallet_provision_test` / `wallet-test-only`. Do not use a shared deployment broker:
the tests declare the production-named topology on this isolated instance.
Run the following separately from the PostgreSQL profile, Identity first and
Payment immediately after it (PG_PORT and RABBIT_PORT are placeholders):

```powershell
.\mvnw.cmd -pl identity-service -Pwallet-provision-broker '-Dwallet.test.jdbc-url=jdbc:postgresql://127.0.0.1:PG_PORT/wallet_identity_test' '-Dwallet.test.username=wallet_provision_test' '-Dwallet.test.password=' '-Dwallet.test.rabbit-port=RABBIT_PORT' verify
.\mvnw.cmd -pl payment-service -Pwallet-provision-broker '-Dwallet.test.jdbc-url=jdbc:postgresql://127.0.0.1:PG_PORT/wallet_payment_test' '-Dwallet.test.username=wallet_provision_test' '-Dwallet.test.password=' '-Dwallet.test.rabbit-port=RABBIT_PORT' verify
```

Identity leaves its two actual OTP/Google messages queued and exports their IDs.
Payment consumes them through the annotation-registered listener into the real
test database, then verifies wallet GET 200. Its second test injects a transient
service failure to observe three consumer attempts within a real broker delivery
and its broker DLQ metadata.
No production database, shared queue purge or production event replay is involved.
Do not run the PostgreSQL profile between these two commands: it replaces the
exported fixture with events that were not sent to this broker. The profiles are
separate because they select different Failsafe integration-test classes.

Fresh-context adversarial review found a real vhost mismatch, fixed by removing
Identity-only configurability. A subsequent review caught a test-harness gap:
manually invoking a consumer would not prove `@RabbitListener` registration. The
broker test now discovers and starts the real registered production listener.
Other static checks found no contract violation. A deployed broker/Gateway smoke
test remains a release requirement. User declined additional cross-model review.
Git index changed externally during the task; it is preserved, so the newer
unstaged changes must be reviewed and staged separately by the user.
No git add, commit or push was executed by this implementation.

Final validation: Identity 49 unit + 8 PostgreSQL + 1 broker integration tests;
Payment 28 unit + 7 PostgreSQL + 2 broker integration tests (95 distinct tests).
All pass without skips. Relevant `verify`
lifecycles compile, test, package and repackage both services. Root Compose config
validation passes. Inverting the ACK condition produced two publisher-test failures
(ACK success and nack handling); the condition was restored and all tests rerun.
Real PostgreSQL publisher tests additionally prove row-lock skipping, committed
PUBLISHED dirty tracking, and committed failure backoff without immediate resend.
Local isolated RabbitMQ transport, confirms, production listener registration,
finite retries and actual DLQ routing passed. No live application deployment was
restarted or modified; this is not a production-runtime or Gateway JWT attestation.
The owned test RabbitMQ container was stopped and auto-removed after validation;
the owned PostgreSQL cluster was stopped. Its generated files remain in the
task-specific temporary directory; no application data was deleted.

## Exact changed files

Paths are relative to the Backend repository. Existing index staging is not changed.

Identity configuration and production:

- `cinema-services/identity-service/.env.example`
- `cinema-services/identity-service/pom.xml`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/config/CustomerActivationRabbitConfig.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/config/CustomerActivationSchedulingConfig.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/entity/CustomerActivationOutboxEvent.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/event/CustomerActivatedEvent.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/repository/CustomerActivationOutboxRepository.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/service/CustomerActivationOutboxPublisher.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/service/CustomerActivationOutboxService.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/service/impl/AuthServiceImpl.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/service/impl/EmailVerificationServiceImpl.java`
- `cinema-services/identity-service/src/main/java/com/cinemaai/identity/service/impl/UserServiceImpl.java`
- `cinema-services/identity-service/src/main/resources/application.properties`
- `cinema-services/identity-service/src/main/resources/db/migration/V5__customer_activation_outbox.sql`

Identity tests:

- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/IdentityApplicationSmokeTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/AuthServiceCustomerActivationTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/AuthServiceRegistrationMappingTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/CustomerActivationBrokerIntegrationTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/CustomerActivationOutboxPublisherTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/CustomerActivationOutboxServiceTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/CustomerActivationPostgresIntegrationTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/CustomerActivationRabbitConfigTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/EmailVerificationCustomerActivationTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/UserServiceCinemaAssignmentTest.java`
- `cinema-services/identity-service/src/test/java/com/cinemaai/identity/service/UserServiceCustomerActivationTest.java`

Payment configuration and production:

- `cinema-services/payment-service/pom.xml`
- `cinema-services/payment-service/src/main/java/com/cinemaai/payment/config/RabbitMqConfig.java`
- `cinema-services/payment-service/src/main/java/com/cinemaai/payment/event/CustomerActivatedEvent.java`
- `cinema-services/payment-service/src/main/java/com/cinemaai/payment/listener/CustomerActivatedEventListener.java`
- `cinema-services/payment-service/src/main/java/com/cinemaai/payment/repository/CineWalletRepository.java`
- `cinema-services/payment-service/src/main/java/com/cinemaai/payment/service/WalletProvisioningService.java`
- `cinema-services/payment-service/src/main/java/com/cinemaai/payment/service/impl/WalletServiceImpl.java`

Payment tests:

- `cinema-services/payment-service/src/test/java/com/cinemaai/payment/config/CustomerActivationRabbitConfigTest.java`
- `cinema-services/payment-service/src/test/java/com/cinemaai/payment/listener/CustomerActivatedEventListenerTest.java`
- `cinema-services/payment-service/src/test/java/com/cinemaai/payment/service/CustomerActivationBrokerIntegrationTest.java`
- `cinema-services/payment-service/src/test/java/com/cinemaai/payment/service/WalletProvisioningPostgresIntegrationTest.java`
- `cinema-services/payment-service/src/test/java/com/cinemaai/payment/service/WalletProvisioningServiceTest.java`
- `cinema-services/payment-service/src/test/java/com/cinemaai/payment/service/WalletServiceImplTest.java`

Deployment and documentation:

- `docker-compose.yml`
- `contracts/events/README.md`
- `contracts/events/customer-activated.schema.json`
- `docs/architecture/WALLET-PROVISION-01.md`

Primary references: [Spring AMQP correlated confirms/returns](https://docs.spring.io/spring-amqp/reference/3.1/amqp/template.html),
[Spring AMQP retry/rejection](https://docs.spring.io/spring-amqp/reference/3.1/amqp/resilience-recovering-from-errors-and-broker-failures.html),
[PostgreSQL ON CONFLICT](https://www.postgresql.org/docs/17/sql-insert.html#SQL-ON-CONFLICT).
