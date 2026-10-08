# Cinema Management System — End-to-End Project Specification

## 1. Document purpose

This document defines the target end-to-end design for the Cinema Management System. It is the business and architecture baseline used before detailed database schemas, APIs and implementation.

The system is designed as a microservice platform for multiple cinema branches. Each branch may have independent rooms, seats, showtimes, prices and food availability.

The conceptual ERD is available at [cinema-conceptual-erd.puml](./cinema-conceptual-erd.puml).

## 2. Business scope

The platform supports:

- Customer registration and authentication.
- Administration of cinema branches, rooms and seats.
- Movie, genre and performer management.
- Showtime scheduling.
- Branch-specific ticket pricing.
- Concession products and food combos.
- Ticket booking with seat availability status.
- Payment retries, payment transactions and refunds.
- Staff ticket check-in.
- Movie interaction tracking and recommendations.

The system is branch-aware: a price or product configuration belonging to one cinema branch must not silently affect another branch.

## 3. Business roles

### Guest

A visitor who has not authenticated. A Guest can browse movies, branches and showtimes, and can register as a Customer. Guest is a use-case actor, not a persisted conceptual ERD entity.

### Customer

A registered customer who can create bookings, pay for tickets, order food, cancel eligible bookings and receive recommendations.

### Staff

A cinema employee assigned to zero or one branch. Staff can check tickets, support branch operations and process eligible refunds.

### Admin

An administrator who manages accounts, branches, rooms, seats, movies, showtimes, pricing and products.

## 4. Microservice architecture

Each service owns its domain model and database. A service must not create physical foreign keys into another service database.

| Service | Responsibility | Main domain concepts |
|---|---|---|
| Identity Service | Authentication, accounts and roles | Account, Customer, Customer Profile, Staff, Admin |
| Catalog Service | Branch, movie, showtime, pricing and products | Cinema, Room, Seat, Movie, Genre, Performer, Showtime, Pricing Rule, Food Item, Food Combo |
| Booking Service | Bookings, tickets and check-in | Booking, Ticket, Ticket Check-in, Order Line |
| Payment Service | Payment attempts and refunds | Payment, Payment Transaction, Refund |
| Recommendation Service | Interactions, preferences and recommendations | Movie Interaction, Preferences, Recommendation Set, Recommendation |
| API Gateway | Routing and security boundary | Infrastructure only; not a conceptual domain entity |

Cross-service relationships are logical references implemented by IDs, synchronous APIs, events or snapshots.

## 5. Identity Service conceptual model

### Entities

- `Account`
- `Customer`
- `Customer Profile`
- `Staff`
- `Admin`

### Relationships

```text
Account 1 — 0..1 Customer
Account 1 — 0..1 Staff
Account 1 — 0..1 Admin
Customer 1 — 1 Customer Profile
Cinema 0..1 — 0..N Staff
```

### Core requirements

- A Guest can register as a Customer.
- Each Customer has exactly one Customer Profile.
- An Account can authenticate according to its role.
- An Admin can create, disable and update Staff accounts.
- A Staff member may be assigned to at most one Cinema.
- Staff assignment is a logical cross-service reference to Catalog.

## 6. Catalog Service conceptual model

### Cinema structure

```text
Cinema 1 — 0..N Room
Room 1 — 1..N Seat Row
Seat Row 1 — 1..N Seat
```

Requirements:

- Admin can create and update cinema branches.
- A Room belongs to exactly one Cinema.
- A Room can be active, under maintenance or inactive.
- A Seat Row belongs to exactly one Room.
- A Seat belongs to exactly one Seat Row.
- Seats can be standard, VIP or couple seats.
- Seats can be unavailable or under maintenance.

### Movie catalog

```text
Movie 1..N — 0..N Genre
Movie 1 — 0..N Movie Cast
Performer 1 — 0..N Movie Cast
```

Requirements:

- Admin can create and update movies.
- A Movie must have at least one Genre before publication.
- A Performer can participate in multiple Movies.
- `Movie Cast` identifies whether a performer is a main performer.
- Movies have a status such as upcoming, now showing, ended or inactive.
- Age rating must be enforced when selling restricted tickets.

### Showtime

```text
Movie 1 — 0..N Showtime
Room 1 — 0..N Showtime
```

Requirements:

- Each Showtime is for exactly one Movie and one Room.
- A Room must not contain overlapping Showtimes.
- A Showtime has a start time, end time and lifecycle status.
- A Showtime cannot be created in an inactive or maintenance Room.
- Cancelling a Showtime must trigger booking and refund handling.

## 7. Branch-specific ticket pricing

The branch is represented by `Cinema`. Pricing must be configured per branch.

```text
Cinema 1 — 0..N Ticket Pricing Rule
Ticket Category 1 — 0..N Ticket Pricing Rule
Showtime 1 — 1..N Showtime Price
Ticket Category 1 — 0..N Showtime Price
Ticket Pricing Rule 0..1 — 0..N Showtime Price
```

Pricing rules may depend on:

- Cinema branch.
- Ticket category, such as Adult, Child or Student.
- Room type.
- Seat type.
- Weekday or weekend.
- Holiday.
- Time period, including late night.
- Effective start and end date.

Pricing workflow:

1. Admin selects a Cinema branch.
2. Admin creates or updates that branch's Ticket Pricing Rules.
3. Catalog validates overlapping or conflicting rules.
4. When a Showtime is published, Catalog resolves the applicable rules.
5. Catalog creates Showtime Prices.
6. Booking uses Showtime Prices to quote and sell tickets.
7. The final price is snapshotted on the Booking/Ticket.

Changing a future Pricing Rule must not change historical bookings.

## 8. Food products

```text
Food Combo 1 — 1..N Food Combo Item
Food Item 1 — 0..N Food Combo Item
```

Requirements:

- Admin can manage individual Food Items.
- Admin can build Food Combos from Food Items.
- A Food Combo must contain at least one item.
- Quantity is recorded for each Food Combo Item.
- Food availability and price are branch-aware when required.

## 9. Booking Service conceptual model

### Booking relationships

```text
Customer 1 — 0..N Booking
Showtime 1 — 0..N Booking
Booking 1 — 1..N Ticket
Showtime 1 — 0..N Ticket
Seat 1 — 0..N Ticket
Ticket Category 1 — 0..N Ticket
```

Requirements:

- A Customer can create multiple Bookings.
- Each Booking belongs to exactly one Customer and one Showtime.
- A Booking must contain at least one Ticket.
- Each Ticket belongs to exactly one Booking, Showtime, Seat and Ticket Category.
- A `(Showtime, Seat)` pair can have at most one valid active Ticket.
- Ticket price is copied as a historical snapshot.

### Food in booking

```text
Booking 1 — 0..N Concession Order Line
Food Item or Food Combo 1 — 0..N Concession Order Line
```

Order lines must store quantity, unit-price snapshot and line total.

### Staff check-in

```text
Ticket 1 — 0..1 Ticket Check-in
Staff 1 — 0..N Ticket Check-in
```

Requirements:

- Staff can scan and validate a Ticket.
- A Ticket can be checked in at most once.
- Cancelled, refunded or already checked-in Tickets must be rejected.
- Staff may check in tickets only for their assigned branch.

## 10. Payment Service conceptual model

```text
Booking 1 — 0..N Payment
Payment 1 — 1..N Payment Transaction
Payment 1 — 0..N Refund
Staff 0..1 — 0..N Refund
```

Requirements:

- A Booking can have multiple payment attempts.
- A Payment belongs to exactly one Booking.
- A Payment has one or more gateway transactions.
- Gateway callbacks must be idempotent.
- Only a successful Payment confirms the Booking.
- A Payment may receive partial or full refunds.
- Total refunds cannot exceed the successful payment amount.
- Refund processing may be automatic or performed by Staff.

## 11. Recommendation Service conceptual model

```text
Customer 1 — 0..N Movie Interaction
Movie 1 — 0..N Movie Interaction
Booking 0..1 — 0..1 Movie Interaction

Customer 1 — 0..N Genre Preference
Genre 1 — 0..N Genre Preference

Customer 1 — 0..N Performer Preference
Performer 1 — 0..N Performer Preference

Customer 1 — 0..N Recommendation Set
Recommendation Set 1 — 1..N Recommendation
Movie 1 — 0..N Recommendation
```

Requirements:

- Record views, searches, favorites and successful bookings as Movie Interactions.
- Calculate customer preferences by Genre and Performer.
- Generate Recommendation Sets over time.
- Every Recommendation Set contains at least one Recommendation.
- A Recommendation references exactly one Movie and stores a score and reason.
- Recommendations must not expose inactive or ended Movies unless explicitly allowed.

## 12. End-to-end business workflows

### Customer registration

```text
Guest
  → registration
  → Account creation
  → Customer creation
  → Customer Profile creation
  → email verification
  → authenticated Customer
```

### Browse and pricing

```text
Customer/Guest
  → select Cinema
  → select Movie
  → select Showtime
  → retrieve Showtime Price
  → display seats and prices
```

### Booking and payment

```text
Customer
  → select Showtime
  → select Seats
  → create Booking
  → select Food Item or Food Combo
  → create Payment
  → process Payment Transaction
  → confirm Booking
  → issue Tickets
```

### Failed payment

```text
Payment failure
  → Payment remains unsuccessful
  → Booking remains pending
  → Customer may retry
  → Booking expires according to its payment deadline
```

### Cancellation and refund

```text
Cancellation request
  → validate cancellation policy
  → cancel Booking/Tickets
  → create Refund if eligible
  → release seats
  → publish booking status update
```

### Ticket check-in

```text
Staff
  → scan Ticket
  → validate Booking, Showtime, Cinema and status
  → create Ticket Check-in
  → reject duplicate or invalid scans
```

### Recommendation generation

```text
Customer activity
  → Movie Interaction
  → Genre/Performer Preference
  → Recommendation Set
  → Movie Recommendations
```

## 13. Cross-service communication

Use logical IDs, APIs and events. Do not create database foreign keys across services.

| Event or request | Producer | Consumer |
|---|---|---|
| `ShowtimePublished` | Catalog | Booking, Recommendation |
| `PriceUpdated` | Catalog | Booking |
| `PaymentSucceeded` | Payment | Booking, Recommendation |
| `PaymentFailed` | Payment | Booking |
| `BookingConfirmed` | Booking | Payment, Recommendation |
| `BookingCancelled` | Booking | Payment, Recommendation |
| `RefundCompleted` | Payment | Booking, Identity/audit if required |
| `TicketCheckedIn` | Booking | Recommendation/reporting if required |

Every consumer must handle duplicate events safely.

## 14. Functional requirement summary

### Identity

- FR-ID-01: Guest registration.
- FR-ID-02: Role-aware authentication.
- FR-ID-03: Customer profile management.
- FR-ID-04: Staff account management and branch assignment.
- FR-ID-05: Admin account management.

### Catalog

- FR-CAT-01: Cinema branch management.
- FR-CAT-02: Room management.
- FR-CAT-03: Seat-row and seat-layout management.
- FR-CAT-04: Movie management.
- FR-CAT-05: Genre management and movie classification.
- FR-CAT-06: Performer and movie-cast management.
- FR-CAT-07: Showtime scheduling and conflict detection.
- FR-CAT-08: Branch-specific ticket pricing.
- FR-CAT-09: Showtime price publication.
- FR-CAT-10: Food item and combo management.

### Booking

- FR-BOOK-01: Showtime seat-map display.
- FR-BOOK-02: Booking creation with seat availability validation.
- FR-BOOK-03: Ticket selection and issuance.
- FR-BOOK-04: Food ordering.
- FR-BOOK-05: Booking expiration and cancellation.
- FR-BOOK-06: Staff ticket check-in.

### Payment

- FR-PAY-01: Payment creation.
- FR-PAY-02: Payment retry.
- FR-PAY-03: Idempotent transaction callback handling.
- FR-PAY-04: Booking confirmation after successful payment.
- FR-PAY-05: Partial and full refunds.

### Recommendation

- FR-REC-01: Movie interaction tracking.
- FR-REC-02: Genre preference calculation.
- FR-REC-03: Performer preference calculation.
- FR-REC-04: Recommendation-set generation.

## 15. Non-functional requirements

### Security

- All internal services must reject direct unauthenticated external access.
- Passwords must be hashed using a strong password hashing algorithm.
- Access tokens must be validated at the service boundary.
- Staff and Admin operations require role authorization.
- Sensitive payment data must not be stored unless required and compliant.

### Consistency

- Seat reservation must be concurrency-safe.
- A `(showtimeId, seatId)` ticket conflict must be prevented atomically.
- Payment callbacks must be idempotent.
- Event consumers must tolerate retries and duplicate messages.
- Historical booking and ticket prices must be immutable snapshots.

### Availability and performance

- Catalog reads should be cacheable.
- Seat availability must be strongly consistent during checkout.
- Recommendation generation may be eventually consistent.
- Payment and Booking must provide traceable correlation IDs.

### Observability

- Each request must have a correlation ID.
- Services must expose health checks.
- Payment, booking and refund state changes must be auditable.
- Metrics should cover booking conversion, payment failures, booking expiration and check-in failures.

## 16. Recommended implementation sequence

1. Finalize the conceptual ERD and cardinalities.
2. Define service ownership and cross-service identifiers.
3. Design logical models for Identity and Catalog.
4. Implement branch-aware pricing and Showtime Price snapshots.
5. Implement Booking, Seat availability and Ticket concurrency rules.
6. Implement Payment transactions and idempotent callbacks.
7. Implement refund workflows.
8. Implement Staff check-in.
9. Implement domain events and outbox delivery.
10. Implement Recommendation interactions and preference calculation.
11. Add contract tests across services.
12. Add end-to-end tests for registration, pricing, booking, payment, refund and check-in.

## 17. Open business decisions

The following decisions must be confirmed before logical database design:

1. Whether a Customer must be authenticated before booking.
2. Whether one Booking can contain tickets for only one Showtime.
3. Whether a Staff member can work at more than one Cinema.
4. Whether Admin is a subtype of Staff or an independent Account role.
5. Whether every Movie must have at least one Genre before publication.
6. Whether Food Items and Food Combos are branch-specific.
7. Whether partial refunds are supported.
8. Whether a Booking can use multiple Payment methods.
9. Whether Guest checkout is allowed.
10. The cancellation and refund window before Showtime.

## 18. Current implementation status

The current repository contains substantial Identity and Catalog implementation. Booking and Payment currently provide service scaffolding but do not yet contain the complete domain model described here. Recommendation currently exposes a placeholder recommendation API and does not persist the conceptual entities.

The conceptual model in this document is therefore the target design that should guide the next implementation phase.
