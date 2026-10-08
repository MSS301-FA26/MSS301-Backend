# Watchlist Service Ownership Options

## Decision criteria

Wishlist is a customer-owned engagement collection. Its core write model is a relation between an authenticated customer and a catalog movie. The owning service should own wishlist lifecycle and storage, while resolving movie display data through a stable catalog contract rather than owning catalog records.

## Option comparison

| Option | Domain ownership | Data coupling | Gateway/deployment | Scalability | Migration effort | Risk | Decision |
|---|---|---|---|---|---|---|---|
| A. catalog-service | Movie-adjacent but customer-behavior data | Couples catalog DB to customer identity and wishlist writes | Low route complexity; one service owns both | Catalog traffic and customer writes scale together | Medium | Boundary erosion and identity coupling | Reject |
| B. identity/profile service | User-adjacent | Couples profile service to movie references and catalog display joins | Low route complexity | Profile service becomes engagement monolith | Medium | Wrong domain ownership; catalog dependency | Reject |
| C. booking-service | Same customer/auth context as bookings | Couples non-transactional preference data to booking lifecycle | Low route complexity | Booking traffic and wishlist traffic scale together | Medium | Unrelated domain and operational blast radius | Reject |
| D. new wishlist/customer-library service | Customer engagement ownership | Stores customer ID and movie ID references; no cross-DB foreign keys | One new route/service; clean boundary | Independent read/write scaling and future libraries | Higher initial setup | New operational component and identity/catalog contracts | Prefer |
| E. expose legacy cinemaAI | Existing implementation ownership | Retains both legacy foreign keys and monolith database coupling | Requires a new legacy URI and deployment path | Monolith bottleneck; shared release cadence | Lowest short-term | Perpetuates legacy dependency and unclear operations | Transitional only |

## A. Catalog service

`catalog-service` owns Movie entities, repositories, catalog controllers, and the catalog database. That makes it the natural source of movie identity and presentation data, but not the owner of a customer's saved collection.

Moving Wishlist into catalog-service would require either:

1. storing `userId` and `movieId` without a relational user foreign key; or
2. adding identity data/remote identity calls into catalog-service.

Both choices violate the current boundary more than a small independent customer-library service would. A catalog database should not become the write owner for customer engagement state merely because its records are referenced.

## B. Identity/profile service

Identity owns users, credentials, profiles, roles, and authentication. Wishlist is not profile data: it references catalog content, has its own lifecycle, and may grow into recommendations, collections, reminders, and activity signals. Putting it in identity would create a user-service dependency on movie semantics and increase the blast radius of catalog changes.

## C. Booking service

Booking owns holds, bookings, booking-linked food orders, and ticket lifecycle. Wishlist has no booking, payment, seat, or showtime transaction. It should not share booking database load or failure domains.

## D. New wishlist/customer-library service

This service owns customer collections and stores stable references:

- authenticated `customerId` from the JWT subject/identity contract;
- `movieId` from catalog;
- timestamps and optional collection metadata.

It should not define a JPA relationship to catalog or identity tables in separate databases. For list responses, it can either:

- return movie IDs and let an aggregation layer/client resolve movie data; or
- call a catalog internal endpoint/batch endpoint and return a composed response.

The current Mobile contract expects title and poster URL, so a catalog batch lookup or an event-driven snapshot strategy is required before preserving the exact response shape.

## E. Expose the legacy monolith

This is viable only as a time-boxed compatibility bridge. It would require a stable deployed `cinemaAI` process, a Gateway URI, internal-secret compatibility, JWT compatibility, health checks, and a defined database/runtime lifecycle.

It would preserve the current contract with minimal code movement, but it would also preserve:

- a monolith dependency in the Gateway;
- legacy user/movie foreign keys;
- separate release and observability conventions;
- a second source of truth if new service migration begins later.

It is not the long-term ownership choice.

## Recommendation

Choose **D: new wishlist/customer-library service**. Treat **E** only as an explicit temporary bridge if product timing requires immediate Web access and the monolith can be deployed safely. Do not route the path to catalog or booking by convenience.
