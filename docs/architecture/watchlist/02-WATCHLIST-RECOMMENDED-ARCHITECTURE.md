# Recommended Watchlist Architecture

## Recommended target

Create a small `customer-library-service` (or `wishlist-service` if the platform naming convention favors a focused service) responsible for customer-owned movie collections.

Classification:

```text
WATCHLIST_ARCHITECTURE: CREATE_NEW_SERVICE
```

## Ownership boundary

The new service owns:

- wishlist membership;
- add/list/remove commands;
- customer-scoped authorization;
- duplicate prevention;
- collection timestamps;
- future customer-library capabilities if approved.

Catalog remains the owner of Movie records, title, poster, publication state, and movie metadata. Identity remains the owner of customer identity and JWT issuance. The wishlist service stores references, not cross-service ORM relationships.

## Proposed data model

```text
wishlist_items
  id                BIGINT primary key
  customer_id       BIGINT NOT NULL  -- logical reference to identity service
  movie_id          BIGINT NOT NULL  -- logical reference to catalog service
  created_at        TIMESTAMP NOT NULL
  updated_at        TIMESTAMP NOT NULL
  UNIQUE(customer_id, movie_id)
  INDEX(customer_id, created_at)
  INDEX(movie_id)
```

Do not create foreign keys to identity-service or catalog-service databases. Referential validity must be checked through service contracts or asynchronous lifecycle events.

## Proposed API compatibility

Preserve the current public paths:

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/v1/wishlist` | List current customer's saved movies |
| POST | `/api/v1/wishlist` | Add `{ movieId }` |
| DELETE | `/api/v1/wishlist/{movieId}` | Remove one movie |

The initial response can preserve `WishlistResponse` for Mobile compatibility, but its movie title/poster composition must be implemented through a catalog service contract. A contract revision may be preferable if the platform standard is to return IDs only; that decision must be made before Mobile implementation.

## Authentication

The Gateway continues to validate/forward customer JWTs. The new service validates the forwarded identity according to the platform standard and derives `customerId` from a stable JWT subject or an authenticated identity header issued by a trusted Gateway/identity path.

Do not use email as the primary persisted identity key in the new service. Email can change; a stable identity ID is required.

The service must remain inaccessible directly from Mobile. Gateway remains the only public target and injects the existing `X-Gateway-Secret` for downstream trust.

## Required components

### New service

- Spring Boot service module and Docker build entry.
- Configuration and environment template.
- `WishlistItem` entity and migration.
- Repository with customer-scoped list/add/remove and uniqueness handling.
- Service/application layer.
- REST controller preserving the public contract.
- DTOs and validation.
- JWT/customer principal integration.
- Gateway-secret filter consistent with other microservices.
- Health and OpenAPI registration.
- Catalog client or batch-resolution adapter for title/poster fields.

### Gateway

- `WISHLIST_SERVICE_URI` environment variable.
- A dedicated route such as `wishlist-service` with predicates:

```text
Path=/api/v1/wishlist,/api/v1/wishlist/**
```

- No changes to existing catalog, booking, payment, or identity predicates.
- Existing TrustedGatewayFilter remains responsible for `X-Gateway-Secret` injection.

### Catalog contract

The service needs a supported internal lookup for movie IDs, ideally a batch endpoint. It must define behavior for deleted/unpublished movies and partial lookup failures. The public wishlist API must not perform direct database joins into catalog-service.

## Why not a direct reference to legacy tables

The legacy `Wishlist` entity has JPA foreign keys to legacy `User` and `Movie`. Those relationships cannot be carried into independently owned service databases. Copying the entity unchanged would recreate the monolith boundary violation inside a new deployment.

## Consistency model

- Add/remove membership is strongly consistent within the wishlist service.
- Movie presentation data is eventually consistent if catalog snapshots/events are used.
- A missing/deleted catalog movie must have an explicit policy: hide it, return an unresolved item, or remove it asynchronously.
- Duplicate adds must remain idempotent from the customer perspective, even if the public API continues returning `409` for an existing item.
