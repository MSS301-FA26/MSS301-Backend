# Admin reports

The three admin pages use live Booking and Catalog data through the API gateway.

| Page | Purpose | Main request |
| --- | --- | --- |
| `/admin/overview` | Business summary, revenue trend, cinema performance, top movies and booking indicators | `GET /api/v1/admin/reports/analytics` |
| `/admin/statistics` | Detailed ticket, revenue, movie, day, room and showtime statistics | `GET /api/v1/admin/reports/analytics` |
| `/admin/fnb-report` | Food and beverage revenue, products, daily sales and sales channels | `GET /api/v1/admin/reports/concessions` |

## Request contract

- Bearer authentication; only ADMIN and MANAGER may read these reports.
- Query parameters: `from`, `to` (ISO local dates), optional `cinemaId`.
- Dates include both endpoints; maximum 366 days. Defaults are the first of the current month through today.
- ADMIN can select all cinemas or one cinema. MANAGER scope is enforced by the server using the authoritative assigned cinema, regardless of a supplied `cinemaId`.
- Responses use the existing `ApiResponse` envelope.
- The cinema selector uses `GET /api/v1/admin/cinemas`.
- Compatibility report routes are available under the same prefix: `/revenue`, `/top-movies`, `/daily-occupancy`, `/occupancy`, `/showtime-fill`, `/no-shows`, `/peak-hours`, `/abandoned`.
- Catalog supplies actual room capacities and scheduled shows through secret-protected `GET /internal/v1/catalog/showtimes/reporting` with the same range and cinema parameters.

## Metric definitions

- Revenue includes retained bookings in PAID/USED and F&B orders in PAID/PICKED_UP. Payment time determines the revenue date; legacy records without payment time fall back to creation time.
- Booking revenue is split between tickets and F&B proportionally to their original line amounts, so discounts are reflected and the parts reconcile to the amount paid.
- F&B includes food purchased WITH_TICKET, separate ADD_ON orders, and STANDALONE orders. Each paid order is counted once. Product allocations reconcile to the actual paid amount.
- Ticket counts in revenue cards use payment dates. Occupancy uses showtime dates, all non-cancelled scheduled shows (including unsold shows), and actual currently available seats from Catalog. These two date bases can produce different ticket totals.
- Booking status charts describe the current status of bookings created in the selected period, not a historical status snapshot.
- No-show rates include only paid bookings for shows in the selected period that have already ended. Future shows are excluded.
- Expiration rate excludes bookings still holding seats or awaiting payment.
- Refund count and amount use refund dates and are displayed separately. Refunded bookings are already excluded from retained revenue.
- Historical orders without cinema attribution appear under an unknown cinema in all-cinema results; no cinema is guessed.
- Product snapshot names are grouped together. Historical capacity and cost of goods are not stored by this report. Profit/margin are unavailable and are not displayed as invented figures.
- Unavailable stock counts, profit and margin are returned as `null`, not zero or an assumed percentage. The shared food-management summary displays these as unavailable.

## Frontend behavior

All pages have date/cinema filters, presets, refresh, explicit loading/error/empty states and CSV export. Draft filters are distinguished from applied filters. CSV exports the entire selected detail table, including rows outside the visible pagination, with UTF-8 BOM, semicolon separators, escaped cells and spreadsheet-formula protection. Mobile navigation collapses and wide tables scroll within their own container.

## Verification

- Frontend: `npm run build` and `node --test src/pages/admin/overview/reportUtils.test.js src/utils/movieSearch.test.js`.
- Booking: `AdminAnalyticsServiceTest`, `AdminReportControllerTest`, and existing `CinemaSecurityServiceTest` cover reconciliation, date boundaries, real capacity, no-shows, scope and authorization.
- Catalog: `ReportingShowtimesTest`, `InternalShowtimeControllerSecurityTest`, and existing `CinemaSecurityServiceTest` cover capacities, unsold shows, cancelled shows and internal-secret access.
- Live gateway checks covered all ten new report routes, real data and cinema filtering. Empty periods, invalid ranges and authorization are also covered by automated tests. Local services must be running to repeat gateway checks.
- Browser checks cover the three pages, filters, search, pagination, downloaded CSV reconciliation and mobile layout/navigation.

Java 21 is required by this workspace. After backend source changes, restart Booking and Catalog with the compiled classes. Vite reloads the frontend automatically during development.
