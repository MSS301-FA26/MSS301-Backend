# Watchlist Migration Plan

## Phase 0 — Architecture and contract decisions

Before code migration:

1. Approve `customer-library-service` ownership.
2. Define the stable identity claim and customer principal contract.
3. Define the catalog batch lookup contract for movie title/poster data.
4. Decide how unpublished/deleted movies appear in a customer's list.
5. Inventory legacy Wishlist rows and identify the source database/environment.
6. Decide whether the current `409` duplicate behavior remains public.

## Phase 1 — Copy/migrate behavior

Build the new service without changing Mobile or removing the legacy controller.

Implement:

- equivalent GET/POST/DELETE behavior;
- customer-scoped authorization;
- request validation for `movieId`;
- uniqueness constraint on `(customer_id, movie_id)`;
- catalog resolution for response fields;
- explicit error mapping for `400`, `401`, `404`, and `409`.

Keep the legacy implementation available as a rollback/reference path, but do not make the new service depend on legacy JPA entities.

## Phase 2 — Gateway exposure

Add a dedicated service target and route:

```text
WISHLIST_SERVICE_URI=<customer-library-service URI>
Path=/api/v1/wishlist,/api/v1/wishlist/**
```

Verify:

- Gateway is the only public entry point;
- `X-Gateway-Secret` is injected and accepted by the new service;
- customer JWT is preserved/validated;
- unauthenticated requests do not reach customer data;
- existing routes remain unchanged.

Do not point this route at catalog-service, booking-service, or an unregistered legacy process.

## Phase 3 — Tests and cutover

Required tests:

### Service tests

- list only current customer items;
- add valid movie reference;
- reject missing movie reference;
- duplicate add behavior;
- remove existing item;
- remove missing item;
- cross-customer isolation;
- invalid/expired JWT;
- Gateway secret required;
- catalog lookup success and failure;
- uniqueness race behavior.

### Gateway tests

- exact and wildcard wishlist predicates;
- correct target URI;
- no 404 caused by missing route;
- secret injection;
- route isolation from catalog/booking.

### Contract tests

- preserve Mobile-compatible response fields or version the API deliberately;
- verify empty list and ordering semantics;
- verify status codes and error envelope.

Cut over traffic only after authenticated GET/POST/DELETE pass through `http://localhost:8080` or the deployed Gateway endpoint. Do not require a customer JWT that is not available; use a real test fixture in controlled tests rather than invented credentials.

## Phase 4 — Legacy data migration

Legacy data migration is required if existing customer wishlists must survive the cutover.

Migration steps:

1. Export legacy `wishlists` rows with legacy user ID/email and movie ID.
2. Map legacy user IDs to stable identity-service customer IDs.
3. Validate movie IDs against catalog-service.
4. Load into the new `wishlist_items` table with conflict-safe upsert.
5. Preserve `created_at` where possible.
6. Quarantine rows with missing customer/movie mappings.
7. Reconcile counts per customer and sampled list contents.

If the legacy database is only development data and product explicitly accepts data loss, migration can be waived, but that waiver must be recorded. The default recommendation is to migrate because Wishlist is customer-owned state.

## Transitional legacy exposure

If immediate access is required before the new service exists, exposing the legacy monolith is a separate, time-boxed bridge:

- deploy the monolith as a named internal service;
- assign a stable URI and health check;
- confirm its JWT secret matches the Gateway contract;
- confirm it accepts `X-Gateway-Secret` or add a compatible trusted filter;
- add only the Wishlist Gateway predicate;
- monitor and set a removal deadline.

The current legacy `cinemaAI` code does not show a Gateway trusted-secret filter equivalent to the microservices' `TrustedGatewayFilter`/`GatewaySecretFilter`. Its security is JWT-centric, so this bridge requires explicit compatibility testing and should not be treated as a zero-change route.

## Phase 5 — Mobile prerequisite and implementation

Only after Phase 3 succeeds:

- Mobile can target the public Gateway path `/api/v1/wishlist`.
- Mobile can implement a production repository/provider/page.
- Movie Detail, Home, and Discover can consume shared server-backed wishlist state.
- Preview favorites can be replaced only for the production route.

Mobile must not implement persistence against the legacy monolith or an internal service port.

## Migration risks

- Legacy user IDs may not map one-to-one to identity-service IDs.
- Legacy and catalog movie IDs may differ after service migration.
- Response composition can fail when catalog data is deleted or unpublished.
- Email-based legacy identity lookup is not a safe long-term key.
- Monolith JWT claims may differ from microservice JWT claims.
- A temporary Gateway bridge can become permanent without an owner/removal date.
- Dual writes during cutover can create duplicates or divergence unless one system is clearly authoritative.

## Final recommendation

Build and expose a dedicated customer-library/wishlist service, migrate legacy rows, then enable Mobile persistence. Use legacy Gateway exposure only as an explicitly approved temporary bridge.
