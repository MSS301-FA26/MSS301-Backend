# Task 04 Booking lifecycle matrix

This matrix records transitions implemented by the current source. It does not introduce new states.

| Current state | Action or event | Preconditions enforced in source | Next state | Seat state | Side effects |
|---|---|---|---|---|---|
| `HOLDING` | Checkout | Owner; hold deadline is present and strictly after `now`; authoritative Catalog quote is valid | `PENDING_PAYMENT` | `HOLDING` | Refresh booking, ticket, and food snapshots from Catalog |
| `HOLDING` | Update items | Owner; hold deadline is present and strictly after `now`; authoritative Catalog quote is valid | `HOLDING` | `HOLDING` | Refresh booking, ticket, and food snapshots from Catalog |
| `HOLDING` | User cancellation | Owner; deadline is still valid | `CANCELLED` | `HOLDING -> RELEASED` | Set `cancelledAt` |
| `HOLDING` | Scheduler, lazy expiry, or replacement hold | Deadline is null or `holdExpiresAt <= now` | `EXPIRED` | `HOLDING -> RELEASED`; sold or checked-in seats unchanged | Set `cancelledAt` |
| `HOLDING` | New hold by same user/showtime | Existing unfinished hold found | `CANCELLED` | `HOLDING -> RELEASED` | Set `cancelledAt` before creating replacement |
| `PENDING_PAYMENT` | Internal mark-paid or valid payment event | Deadline is present and strictly after `now`; loyalty guard passes; event path also requires every seat to be `HOLDING` | `PAID` | `HOLDING -> BOOKED` | Set `paidAt`, booking QR, ticket codes/QR; award loyalty through existing idempotent booking contract |
| `PENDING_PAYMENT` | User cancellation | Owner; deadline is still valid | `CANCELLED` | `HOLDING -> RELEASED` | Set `cancelledAt` |
| `PENDING_PAYMENT` | Scheduler, lazy expiry, mark-paid, or late payment event | Deadline is null or `holdExpiresAt <= now` | `EXPIRED` | `HOLDING -> RELEASED`; sold or checked-in seats unchanged | Set `cancelledAt`; captured payment remains reconciliation evidence and no ticket is issued |
| `PAID` | Full or final partial check-in | Staff/admin cinema scope and valid ticket/booking code | `USED` | `BOOKED -> CHECKED_IN` | Set booking/seat check-in timestamps |
| `PAID` | Duplicate internal mark-paid/event | Already paid | `PAID` | `BOOKED` | No new ticket or loyalty side effect in the hardened paths |
| `PAID` | Showtime cancellation/refund | Existing admin/manager incident flow | `REFUNDED` | `BOOKED -> RELEASED` | Wallet refund, loyalty adjustment, email, audit log |
| `PAID` | Admin/manager single-ticket cancellation | Existing scoped cancel endpoint | `CANCELLED` | All seats `-> RELEASED` | Cancellation audit; no gateway refund in this path |
| `USED` | Showtime cancellation/refund | Existing admin/manager incident flow | `REFUNDED` | `CHECKED_IN -> RELEASED` | Wallet refund, loyalty adjustment, email, audit log |
| `CANCELLED` | Payment event or internal mark-paid | Rejected by hardened payment fulfillment guards | `CANCELLED` | Unchanged | Captured payment requires manual reconciliation |
| `EXPIRED` | Checkout, update, cancel, mark-paid, or payment event | Terminal expiration state | `EXPIRED` | Unchanged | Request/event denied; captured payment requires manual reconciliation |
| `REFUNDED` | Payment event or check-in | Terminal refund state | `REFUNDED` | Unchanged | Request/event denied |

Seat runtime transitions supported by source are `HOLDING -> BOOKED`, `HOLDING -> RELEASED`, and `BOOKED -> CHECKED_IN`. Existing administrative cancellation/refund paths can also move `BOOKED` or `CHECKED_IN` to `RELEASED`. No source transition restores a `RELEASED` row; a later booking creates a new `BookingSeat` row, protected in PostgreSQL by `uq_booking_seats_active_showtime_seat` for `HOLDING`, `BOOKED`, and `CHECKED_IN`.
