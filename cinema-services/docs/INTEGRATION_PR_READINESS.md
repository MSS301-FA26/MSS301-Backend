# Task 05.3.2 — Final integration review and PR draft

Review date: 2026-10-24. Scope: uncommitted Tasks 05.2–05.3.1, Java/local
integration only. This document supersedes older readiness verdicts in the two
historical runbooks. No commit, push, PR creation, merge or deployment is authorized.

## A. Git state

Branch `feature/microservices-integration-audit`. After `git fetch origin`, both
HEAD and `origin/dev` were `ed62e84378f27886734d0daea20f9d807f85d1d0`; there were
no incoming commits to reconcile. The index was empty. Existing uncommitted work
was preserved; no reset, clean, stash, rebase or writes to other worktrees.

## B. Changed-files audit

The original review set comprised 14 tracked modifications and 18 new files
(including individual files under `scripts`, not counting that directory as a file).
All were inspected, including untracked source that `git diff HEAD` omits.

| Group | Files / purpose reviewed |
|---|---|
| Configuration | Root `docker-compose.yml`; Gateway `application.properties`; Booking/Payment properties; Booking/Catalog/Payment `.env.example` |
| Wallet | `CineWalletRepository`, `WalletServiceImpl`, new `WalletFirstUsePostgresTest` |
| Availability | Catalog `BookingClient`, `ShowtimeServiceImpl`, `GlobalExceptionHandler`, new `SeatAvailabilityUnavailableException`, `OccupancyHttpContractTest`, smoke-test fixture |
| Migration | New Booking V6/V7/V8 and `BookingSchemaMigrationPostgresTest`; no V1–V5 edits |
| Gateway tests | `GatewayRouteConfigurationTest`, `ConfiguredGatewayHttpTest` |
| Local verification | Four `LocalDatabaseRuntimeVerificationTest` classes; Booking seat-concurrency infrastructure adjustment; Payment `IdentityClientHttpFailureTest` |
| Reproduction | Both Task053 PowerShell scripts and both local-integration runbooks |

This review adds documentation and one cleanup regression test, plus a null guard
in the existing launcher cleanup. The temporary production mutation was fully restored.
No new dependency, financial workflow, auth policy, seat state machine,
event contract or AI implementation is introduced. Follow the existing split into
configuration/migrations, routing/harness, wallet and availability when preparing
reviewable commits; do not mix unrelated refactoring into them.

## C. Wallet review

`getWallet` uses a writable Spring transaction; the helper has only that caller.
Existing rows return immediately, without touching balance or timestamps. Absent
rows use a parameterized native INSERT with balance zero and
`ON CONFLICT (user_id) DO NOTHING`, then SELECT. Payment V1 already declares
`user_id BIGINT NOT NULL UNIQUE`. The conflict target cannot hide unrelated SQL
errors; there is no catch/retry on an aborted transaction. Under the configured
default READ COMMITTED isolation, the subsequent SELECT sees the winning commit.
Concurrent SERIALIZABLE/REPEATABLE READ operation is not claimed.

GET does not create a WalletTransaction or WithdrawalRequest. Both wallet aliases
derive user ID from `AuthenticatedUser`, not query/header input. Credit/refund and
withdrawal production methods are unchanged. Real PostgreSQL tests exercise
first/repeated reads, an existing nonzero wallet with unchanged timestamps, eight
concurrent first uses, idempotent credit, and rejection/approval bookkeeping.
The HTTP harness separately checks ownership through real JWT/Gateway processing.
Native creation timestamps use the database clock; deployment should keep JVM/DB
timezone conventions aligned with existing timestamp-without-time-zone mappings.

## D. Catalog review

Verified empty occupancy (`success=true`, non-null empty list) remains valid.
Null/invalid envelopes, duplicate/nonpositive/null seat IDs, null/unknown status,
transport failures, timeouts and upstream HTTP failures cannot become empty seats.
Only HOLDING/BOOKED/UNAVAILABLE are accepted as occupied states. Exceptions expose
the fixed safe message through the existing ErrorResponse contract with HTTP503,
`success=false` and no availability data. Occupancy logging includes ID and exception
class, not the response body or internal secret.

All callers were followed: `getSeatMap` directly calls the client; customer schedule
and both self/equivalent resolution branches call `computeAvailableSeatsForShowtimes`.
The latter no longer catches and substitutes zero. Existing master-seat UNAVAILABLE
handling remains intact. No Booking seat lifecycle code changed. Healthy results and
other error handlers remain unchanged. H2 smoke tests now explicitly mock a healthy
Booking dependency; they are not evidence of a running Booking service.

The client now has connect/read deadlines of 2000/3000 ms. Its shared RestClient also
bounds eligibility and cancellation calls; their existing Optional/null fallbacks
were not changed. Long-running upstream operations can therefore hit this deadline;
this review does not certify cancellation orchestration or aggregate schedule latency.
The HTTP contract suite covers the direct resolution branch; the equivalent branch
was source-traced to the same throwing helper, not separately claimed as an HTTP case.

## E. Gateway review

Identity staff aliases and admin audit-log paths are narrow additions to the
Identity route. Catalog/Booking/Payment route groups remain unchanged. Adaptive
search is an exact path to the configured AI target, without adding a catch-all.
Existing AI rewrite paths are checked by real Gateway HTTP with local probe targets,
not by a running FastAPI application.

The unchanged trusted filter overwrites the gateway secret, strips all client
X-User-* identity/scope headers and the internal-service secret, preserves Bearer
authorization, and blocks `/internal/` paths. Removed AddRequestHeader filters had
duplicated a hardcoded AI secret; the configured trusted filter is now the only
writer. Gateway does not authenticate JWTs itself: downstream security does that.
No public internal route or new production secret was added.

## F. Database and migration review

Booking V6–V8 add seven nullable columns already mapped by entities. They do not
rewrite old migrations, historical rows, statuses or active-seat uniqueness.
Tests migrate clean schemas and V5 schemas containing historical paid Booking/Food
Order rows, compare previous values, inspect column types/nullability and the active
seat unique index, then validate all mappings with Hibernate. New snapshot writes
are also exercised. JPA `validate` is retained, not replaced by update/create.

Runtime databases/users are identity_db/identity_verify, catalog_db/catalog_verify,
booking_db/booking_verify and payment_db/payment_verify on four separate PostgreSQL
containers. Gateway has no datasource. Direct test property overrides and fixed
loopback URL guards prevent these local tests from using developer `.env` or Neon.
Authorization suites use separate disposable identity_auth/payment_auth databases
and create-drop there only. Their shared test-only `coverage_user` is not a runtime
business-service credential. This is local JVM verification, not full Docker deployment.

## G. Security regression and evidence

Evidence from Task05.3.1 was checked against source and local artifacts, not accepted
solely from its verdict: package log, supplemental PostgreSQL log, Surefire XML,
HTTP CSV and red/green logs. Baseline unique cases: 341 total, 324 passed, 17 skipped,
zero failures/errors. Package contained all five Java modules. The default package
run itself passed 302 with 39 skipped; the separate 22 PostgreSQL cases explain the
consolidated 324, rather than adding 22 new tests.

Task05.3.2 reran `package` with every opt-in gate enabled on owned infrastructure:

| Module | Tests | Pass | Failure/error/skip |
|---|---:|---:|---:|
| Gateway | 7 | 7 | 0/0/0 |
| Identity | 33 | 33 | 0/0/0 |
| Catalog | 74 | 74 | 0/0/0 |
| Booking | 129 | 129 | 0/0/0 |
| Payment | 98 | 98 | 0/0/0 |
| Total | 341 | 341 | 0/0/0 |

All five module packages passed (reactor BUILD SUCCESS, 11:17). Actual Surefire XML
was indexed per class/method into ignored `target/task0532-test-index.csv`; no
Failsafe reports or separate Failsafe execution were present. All 17 baseline skips
in section H ran successfully; critical tests pending: zero. No test was disabled.

Mutation experiment: invert the occupancy envelope's success check. The six-case
contract suite failed three cases, including `success=false` incorrectly returning
200 instead of503. Restore from the saved source content: SHA256 matched
`A2FF3BD3C000D693E617C2ACF0056DBCA1B3B5B458ACC74B61A3E6CBF2EC28CE`.
The restored suite passed6/6 and Catalog was repackaged successfully. This deliberate
negative experiment is not a remaining regression. Cleanup has an additional
dependency-free PowerShell regression with four PID scenarios; it is not a 342nd
Maven case.

Evidence: ignored `task0532-full-regression.log`, `task0532-catalog-mutation.log`,
`task0532-catalog-restored.log`, `task0532-cleanup-green.log` and the case index.
Test passwords/JWTs/state and Surefire reports remain under ignored local artifacts;
do not publish them. No live VNPay, production mail, Neon, or shared RabbitMQ was used.

## H. Classification of the 17 baseline skips

Each row is relevant and selected for pre-merge rerun on owned infrastructure.
Skip means missing opt-in configuration, not a passing or RabbitMQ-only result.

| Service / class | Methods (count) | Baseline reason / activation | Disposable dependency / relevance |
|---|---|---|---|
| Identity `AuthorizationDatabaseIntegrationTest` | `adminAssignmentPersistsAndInvalidRequestsDoNotMutate`, `selfReassignmentAndAnonymousFailWithoutDatabaseMutation`, `bothStaffAliasesEnforceRoleMatrixAndPreserveAuthoritativeData`, `managersManageOnlyStaffInTheirCurrentDatabaseCinema` (4) | Missing `authorization.identity.url`; set to loopback non-5432 port with DB `identity_auth` | PostgreSQL; actual service/security persistence for newly exposed aliases and scope |
| Same class, inherited from `IdentityApplicationSmokeTest` | `directAccessWithoutGatewaySecretIsForbidden`, `accessWithGatewaySecretPassesSecretCheck`, `actuatorHealthIsWhitelistedWithoutSecret`, `correlationIdIsPropagated` (4) | Same class-level gate | Same PostgreSQL context; secret boundary/health/correlation regression |
| Payment `security.AdminLoyaltyDatabaseIntegrationTest` | `adminWritesPersistExactBalancesHistoryAndConfiguration`, `deniedRequestsAndInvalidTargetsHaveNoDatabaseSideEffects`, `managerConfigurationUsesCurrentIdentityScopeAndPersistsOnlyAssignedCinema` (3) | Missing `authorization.payment.url`; loopback non-5432 port with DB `payment_auth` | PostgreSQL; persisted admin/manager permissions and denial side effects; external Identity mocked |
| Identity/Catalog/Booking/Payment `LocalDatabaseRuntimeVerificationTest` | `effectiveDatasourceMigrationAndHttpHealthMatchOwnedDatabase` in each module (4) | Missing `local.db.verify=true`; exact per-module `local.db.<service>.url` and `local.db.password` | Four PostgreSQL servers and owned broker as required by real contexts; configuration, Flyway, Hibernate, SQL identity, HTTP health |
| Booking `config.RabbitMqPoisonMessageIntegrationTest` | `legacyQueueAcceptsNewDeclarationsAndPoisonMessageIsRetainedInDlq`, `failedDlqRepublishDoesNotAcknowledgeOriginalDelivery` (2) | Missing env `CK06_RABBIT_PORT`; loopback broker, test user `ck06`, vhost `/ck06-b4` | RabbitMQ; unchanged production retry/recovery topology regression, not DB isolation proof |

Authorization suites require the existing test-only role/password
`coverage_user`/`coverage_pass`. Rabbit tests require `ck06`/`ck06-test-only` and
permissions on `/ck06-b4`. These values are intentionally public disposable test
fixtures already in test source, never credentials for an existing stack.

## I. Runtime verification

The Task05.3.1 log and CSV contain 112/112 successful checks, including five health
endpoints while JVMs coexist, separate datasources, real Gateway/JWT forwarding,
Java-to-Java calls, SQL side effects, wallet first use/concurrency and Booking-down
seat-map/schedule/resolve503. Eight concurrent HTTP calls are additionally backed
by the real transaction-proxy PostgreSQL test, not offered as its substitute.

Fault probes use controlled JVM stops/restarts, not simulated success responses.
Controlled HTTP fault servers in unit/contract tests are labeled separately.
No AI runtime, full Docker application deployment, live banking or mail E2E is claimed.

Task05.3.2 independently reran the complete harness on newly provisioned runtime
fixtures and final packaged jars: **112/112 PASS**, no failed assertions. All five
JVMs returned health200 before testing and again after fault recovery. Final PIDs
were Identity12832, Catalog21576, Booking3060, Payment23680, Gateway21524; all jar
ownership checks passed. SQL independently confirmed the four database/user pairs,
`task053-{service}` application names and Flyway success counts4/14/8/9. Users1003
and1004 each had one wallet with balance0.00; public wallet_transactions and
withdrawal_requests both had zero rows. Booking-down seat-map, schedule and resolve
all returned503. No cold-quote assertion failed in this run; warmup remains explicit.
Evidence: ignored `task0532-http.log`, `task0532-runtime-evidence.log` and
`target/task053-http-results.csv` (112 rows, all Passed=True).

## J. Documentation, scripts and reproduction

Scripts derive paths from `$PSScriptRoot`; historical personal checkout paths in
the runbook are context only, not execution dependencies. Java21, Maven wrapper,
PowerShell and Docker Linux containers are prerequisites. Build before starting
JVMs (Windows jar locks). Reserve sufficient RAM; avoid a heavy build alongside
all five application JVMs. Do not reuse a previous harness fixture run.

`Start-Task053Runtime.ps1` rejects existing state/occupied ports, creates its own
labelled network, loopback-only containers and tmpfs data, then records owned PIDs
and jar paths. Stop verifies jar paths and owner labels, skips already-removed
`--rm` containers and removes only the owned network/state. The HTTP script verifies
DB labels before fixture SQL and checks PIDs before fault injection. State is trusted
local operator input, not an interface safe for attacker-edited CLIXML. Keep it private.
No compose-down, prune, shared volume or wildcard cleanup is used.
Actual review cleanup reproduced a null-command-line failure after an old PID was
reused. Cleanup now requires a nonempty command line before jar matching, and skips
unidentifiable PIDs without stopping them. `Test-Task053Cleanup.ps1` fakes every OS,
Docker and removal operation: missing, null-command-line, unrelated and matching
PIDs prove that only the matching owned JVM is stopped. The new test reproduced the
original null-method error before the guard; all four cases pass after it. An initial
test path assertion was corrected to compare exact `Join-Path` output on Windows,
not a slash-specific wildcard. The real owned-runtime cleanup then passed as well.
FYI: the supplied Booking `*.cleanup.enabled=false` settings are not consumed by
the existing `SeatHoldCleanupScheduler` (it has no conditional enable flag).
Do not treat them as a scheduler kill switch: cleanup may run on disposable fixture
data. Isolation, fresh fixtures and loopback-only dependencies provide safety here;
changing scheduler behavior is outside this review.

For the normal five-JVM reproduction, run from a fresh checkout's `cinema-services`:

```powershell
.\mvnw.cmd package '-Dspring.config.import=optional:file:.env.example[.properties]' '-Dlogging.pattern.level=%5p'
.\scripts\Test-Task053Cleanup.ps1
.\scripts\Start-Task053Runtime.ps1
# Wait for all five http://127.0.0.1:15800..15804/actuator/health responses: 200.
.\scripts\Test-Task053Http.ps1
# Expect TOTAL=112 PASSED=112 FAILED=0; inspect target/task053-http-results.csv.
.\scripts\Start-Task053Runtime.ps1 -Stop
```

The default Maven command deliberately leaves opt-in tests skipped. To activate
all tests, first provision the four owned DBs/broker as in the runbook, stop only
the five owned app JVMs, retain the infrastructure and state, then create the two
additional authorization databases/role and the Rabbit test vhost/user above.
Never create these on a shared server. Use the following gate properties together
with `package` (the review used exactly these local datasource destinations):

```powershell
$state=Import-Clixml target/task053-state.clixml
# Dedicated infrastructure only; check every target's owner before provisioning.
foreach ($name in @('task053-identity-db','task053-payment-db','task053-rabbit')) {
  $labels=docker inspect $name --format '{{json .Config.Labels}}' | ConvertFrom-Json
  if ($LASTEXITCODE -ne 0 -or $labels.'task.owner' -ne 'task053') { throw 'Ownership check failed' }
}
foreach ($app in $state.Apps) {
  $process=Get-CimInstance Win32_Process -Filter "ProcessId=$($app.Pid)"
  if ($process) {
    if (!$process.CommandLine -or !$process.CommandLine.Contains($app.Jar)) { throw 'PID ownership check failed' }
    Stop-Process -Id $app.Pid
  }
}
# Run once on fresh owned containers, not an existing/shared database.
docker exec task053-identity-db psql -v ON_ERROR_STOP=1 -U identity_verify -d identity_db -c "CREATE ROLE coverage_user LOGIN PASSWORD 'coverage_pass'" -c 'CREATE DATABASE identity_auth OWNER coverage_user'
if ($LASTEXITCODE) { throw 'Identity fixture provisioning failed' }
docker exec task053-payment-db psql -v ON_ERROR_STOP=1 -U payment_verify -d payment_db -c "CREATE ROLE coverage_user LOGIN PASSWORD 'coverage_pass'" -c 'CREATE DATABASE payment_auth OWNER coverage_user'
if ($LASTEXITCODE) { throw 'Payment fixture provisioning failed' }
# Wait for broker readiness before rabbitmqctl provisioning.
docker exec task053-rabbit rabbitmq-diagnostics -q ping
if ($LASTEXITCODE) { throw 'Broker not ready' }
docker exec task053-rabbit rabbitmqctl add_user ck06 ck06-test-only
if ($LASTEXITCODE) { throw 'Broker fixture user failed' }
docker exec task053-rabbit rabbitmqctl add_vhost /ck06-b4
if ($LASTEXITCODE) { throw 'Broker fixture vhost failed' }
docker exec task053-rabbit rabbitmqctl set_permissions -p /ck06-b4 ck06 '.*' '.*' '.*'
if ($LASTEXITCODE) { throw 'Broker fixture permissions failed' }
$env:CK06_RABBIT_PORT='57172'
.\mvnw.cmd '-Dspring.config.import=optional:file:.env.example[.properties]' '-Dlogging.pattern.level=%5p' `
  '-Dlocal.db.verify=true' "-Dlocal.db.password=$($state.Password)" `
  '-Dlocal.db.identity.url=jdbc:postgresql://127.0.0.1:57101/identity_db' `
  '-Dlocal.db.catalog.url=jdbc:postgresql://127.0.0.1:57102/catalog_db' `
  '-Dlocal.db.booking.url=jdbc:postgresql://127.0.0.1:57103/booking_db' `
  '-Dlocal.db.payment.url=jdbc:postgresql://127.0.0.1:57104/payment_db' `
  '-Dauthorization.identity.url=jdbc:postgresql://127.0.0.1:57101/identity_auth' `
  '-Dauthorization.payment.url=jdbc:postgresql://127.0.0.1:57104/payment_auth' `
  '-Dwallet.test.url=jdbc:postgresql://127.0.0.1:57104/payment_db' "-Dwallet.test.password=$($state.Password)" `
  '-Dpayment.concurrency.test.url=jdbc:postgresql://127.0.0.1:57104/payment_db' `
  '-Dpayment.concurrency.test.username=payment_verify' "-Dpayment.concurrency.test.password=$($state.Password)" `
  '-Dpayment.service.test.jdbc-url=jdbc:postgresql://127.0.0.1:57104/payment_db' `
  '-Dpayment.service.test.username=payment_verify' "-Dpayment.service.test.password=$($state.Password)" package
Remove-Item Env:CK06_RABBIT_PORT
# Once all test processes have exited:
.\scripts\Start-Task053Runtime.ps1 -Stop
# For HTTP verification, launch a NEW runtime to obtain fresh public fixtures.
```

Only `target` and `*.log` contain generated evidence. No real credential or runtime
artifact belongs in the commit. Existing sample Compose credentials predate this
diff and are local examples, not production configuration. No dependency changed;
this scoped review is not a fresh ecosystem-wide CVE or production hardening audit.

## K. Remaining issues and limits

No confirmed new P0/P1 finding from the reviewed production diff. Follow-ups remain:
AI direct-client/envelope/port compatibility; missing deadlines and unsafe error
details in other existing clients; cold quote latency after Catalog restart; production
credential/TLS/least-privilege deployment. These are not silently repaired here or
counted as verified by Java-only local tests. The harness warmup does not prove the
cold-start latency issue solved. None changes the scoped wallet/occupancy verdict.

## L. Proposed PR (draft, not submitted)

Title: **Fix local Java integration, wallet first use and fail-closed seat availability**

Suggested commit subjects (prepare separately after approval):

- `fix: align local service URLs and Booking snapshot migrations`
- `fix: route Identity aliases and configure trusted AI gateway headers`
- `fix: initialize wallets atomically in a writable transaction`
- `fix: fail closed when Booking occupancy cannot be verified`
- `test: document and verify isolated Java service integration`

Description:

> Align local/Docker service URLs and add missing nullable Booking snapshot columns
> without modifying historical migrations. Complete narrow Identity/search Gateway
> routes and remove duplicate hardcoded secret headers. Repair PostgreSQL wallet
> first-use transactions with unique-key conflict handling, preserving principal
> ownership and balances. Return safe503 instead of false seat availability when
> Booking occupancy cannot be verified. Add isolated PostgreSQL, Gateway HTTP and
> five-JVM regression coverage and reproducible cleanup/runbooks.
>
> Scope excludes AI E2E, full Docker deployment, live VNPay/mail, unrelated client
> fallbacks and financial/state-machine redesign. See evidence and limits above.

Reviewer checklist:

- [ ] Review V6–V8 clean/upgrade preservation and unchanged seat uniqueness.
- [ ] Review writable first-use transaction, unique conflict target and ownership.
- [ ] Review safe503 propagation through every occupancy caller.
- [ ] Review route specificity, configured secret and spoofed-header stripping.
- [ ] Run configured Maven and disposable infrastructure gates; do not hide skips.
- [ ] Run the fresh five-JVM HTTP harness and verify112 checks plus cleanup.
- [ ] Exclude state, logs, reports and real `.env` from staging.
- [ ] Obtain required human approval/CI checks before any actual merge.

## Final technical verdict

Cleanup completed after final evidence capture: all five owned JVMs, four PostgreSQL
containers, the RabbitMQ container, task053 network and credential state were removed.
No task053 container/network remained. Disposable tmpfs fixtures are unrecoverable;
source and ignored logs/case-index/HTTP CSV remain. The original fifteen stack
container IDs remained present, including its already-stopped Booking container
(`842faadeb77a`, Exited137); no original stack resource was started/stopped/removed.

No known technical blocker remains for this scoped PR. SAFE_TO_MERGE is a technical
assessment of this diff, not permission to bypass human approval, required GitHub CI
or branch protection, and not production readiness. No commit, push, PR creation,
rebase or merge was performed; the authorized fetch only refreshed remote-tracking refs.

```text
TASK05_3_2 = PASS
WALLET_REVIEW = PASS
CATALOG_REVIEW = PASS
GATEWAY_REVIEW = PASS
BOOKING_MIGRATION_REVIEW = PASS
DATABASE_PER_SERVICE = PASS
SECURITY_REGRESSION = PASS
HTTP_INTEGRATION = PASS
MAVEN_REGRESSION = PASS
SKIPPED_TESTS_CLASSIFIED = YES
CRITICAL_TESTS_PENDING = 0
OUT_OF_SCOPE_CHANGES = 0
SECRETS_EXPOSED = NO
GIT_DIFF_CHECK = PASS
SAFE_TO_COMMIT = YES
TECHNICALLY_READY_FOR_PR = YES
SAFE_TO_MERGE = YES
REMAINING_BLOCKERS = 0
NEXT_ACTION = Human reviews the proposed commit split and PR draft; commit/push/open PR only when explicitly authorized, then satisfy required CI/review before merge.
```
