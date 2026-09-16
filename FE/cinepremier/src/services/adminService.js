import { buildQueryString, request, unwrapListPayload } from './authService';
import { normalizeMovie, normalizeMovieListResponse, normalizeMoviePageResponse } from './movieService';
import { createCrudApi, uploadFile } from './apiFactory';

const enc = encodeURIComponent;

// ─── Resource CRUD APIs ─────────────────────────────────────────────────────
const moviesApi      = createCrudApi('/api/v1/admin/movies');
const actorsApi      = createCrudApi('/api/v1/admin/actors');
const genresApi      = createCrudApi('/api/v1/admin/genres');
const usersApi       = createCrudApi('/api/v1/admin/users');
const staffProfiles  = createCrudApi('/api/v1/admin/staff-profiles');
const roomsApi       = createCrudApi('/api/v1/admin/rooms');
const showtimesApi   = createCrudApi('/api/v1/admin/showtimes');
const pricingRules   = createCrudApi('/api/v1/admin/ticket-pricing/rules');
const pricingCombos  = createCrudApi('/api/v1/admin/ticket-pricing/combos');
const foodItemsApi   = createCrudApi('/api/v1/admin/foods/items');
const foodCombosApi  = createCrudApi('/api/v1/admin/foods/combos');
const bookingsApi    = createCrudApi('/api/v1/admin/bookings');

const normalizePageResponse = (payload = {}) => {
  const items = unwrapListPayload(payload);
  return {
    items,
    page: Number(payload.page ?? payload.number ?? 0),
    size: Number(payload.size ?? payload.pageSize ?? items.length),
    totalPages: Math.max(1, Number(payload.totalPages ?? payload.pageCount ?? 1)),
    totalItems: Number(payload.totalItems ?? payload.totalElements ?? payload.total ?? items.length),
    first: Boolean(payload.first),
    last: Boolean(payload.last)
  };
};

// ─── Admin Service ──────────────────────────────────────────────────────────
export const adminService = {

  // ── Movies ─────────────────────────────────────────────────────────────────
  searchAdminMovies:        (token, params = {}) => moviesApi.getAll(token, params).then(normalizeMovieListResponse),
  searchAdminMoviesPage:    (token, params = {}) => moviesApi.getAll(token, params).then(normalizeMoviePageResponse),
  getAdminMovieDetail:      (token, movieId)     => moviesApi.getOne(token, movieId).then(normalizeMovie),
  createAdminMovie:         (token, payload)     => moviesApi.create(token, payload).then(normalizeMovie),
  updateAdminMovie:         (token, movieId, payload) => moviesApi.update(token, movieId, payload).then(normalizeMovie),
  updateAdminMovieStatus:   (token, movieId, status)  => moviesApi.patchBody(token, movieId, { status }).then(normalizeMovie),
  deleteAdminMovie:         (token, movieId)     => moviesApi.remove(token, movieId),

  // ── Actors ─────────────────────────────────────────────────────────────────
  getAdminActors:   (token, params = {}) => actorsApi.getAll(token, params).then(unwrapListPayload),
  getAdminActorsPage: (token, params = {}) => actorsApi.getAll(token, params).then(normalizePageResponse),
  createAdminActor: (token, payload)     => actorsApi.create(token, payload),
  updateAdminActor: (token, id, payload) => actorsApi.update(token, id, payload),
  deleteAdminActor: (token, id)          => actorsApi.remove(token, id),

  // ── Uploads ────────────────────────────────────────────────────────────────
  uploadAdminImage: (token, file, folder = 'images') => uploadFile(token, file, folder, '/api/v1/admin/uploads/images'),
  uploadAdminVideo: (token, file, folder = 'videos') => uploadFile(token, file, folder, '/api/v1/admin/uploads/videos'),

  // ── Users ──────────────────────────────────────────────────────────────────
  getAdminUsers:          (token)          => usersApi.getAll(token),
  getAdminUserDetail:     (token, userId)  => usersApi.getOne(token, userId),
  createAdminStaff:       (token, payload) => request('/api/v1/admin/users/staff', { method: 'POST', token, body: payload }),
  updateAdminUserStatus:  (token, userId, status) => usersApi.patchBody(token, userId, { status }),

  // ── Staff Profiles ─────────────────────────────────────────────────────────
  getAdminStaffProfiles:          (token, params = {}) => staffProfiles.getAll(token, params),
  createAdminStaffProfile:        (token, payload)     => staffProfiles.create(token, payload),
  updateAdminStaffProfile:        (token, id, payload) => staffProfiles.update(token, id, payload),
  updateAdminStaffProfileStatus:  (token, id, status)  => staffProfiles.patchQuery(token, id, status),

  // ── Genres ─────────────────────────────────────────────────────────────────
  getAdminGenres:   (params = {})          => request(`/api/v1/genres${buildQueryString(params)}`),   // endpoint public, không cần token
  createAdminGenre: (token, payload)     => genresApi.create(token, payload),
  updateAdminGenre: (token, id, payload) => genresApi.update(token, id, payload),
  deleteAdminGenre: (token, id)          => genresApi.remove(token, id),

  // ── Cinema (single resource, không có ID) ──────────────────────────────────
  getAdminCinema:         (token)          => request('/api/v1/admin/cinema', { token }),
  updateAdminCinema:      (token, payload) => request('/api/v1/admin/cinema', { method: 'PUT', token, body: payload }),
  updateAdminCinemaStatus: (token, status) => request(`/api/v1/admin/cinema/status?status=${enc(status)}`, { method: 'PATCH', token }),

  // ── Rooms ──────────────────────────────────────────────────────────────────
  getAdminRooms:         (token)              => roomsApi.getAll(token),
  getAdminRoom:          (token, id)          => roomsApi.getOne(token, id),
  createAdminRoom:       (token, payload)     => roomsApi.create(token, payload),
  updateAdminRoom:       (token, id, payload) => roomsApi.update(token, id, payload),
  updateAdminRoomStatus: (token, id, status)  => roomsApi.patchQuery(token, id, status),
  // Sub-resources: seats
  getAdminRoomSeats:     (token, roomId)          => request(`/api/v1/admin/rooms/${enc(roomId)}/seats`, { token }),
  createAdminRoomSeats:  (token, roomId, payload) => request(`/api/v1/admin/rooms/${enc(roomId)}/seats/generate`, { method: 'POST', token, body: payload }),
  replaceAdminRoomSeats: (token, roomId, payload) => request(`/api/v1/admin/rooms/${enc(roomId)}/seats`, { method: 'PUT', token, body: payload }),
  updateAdminSeat:       (token, seatId, payload) => request(`/api/v1/admin/rooms/seats/${enc(seatId)}`, { method: 'PUT', token, body: payload }),
  deactivateAdminSeat:   (token, seatId)          => request(`/api/v1/admin/rooms/seats/${enc(seatId)}`, { method: 'DELETE', token }),

  // ── Showtimes ──────────────────────────────────────────────────────────────
  getAdminShowtimes:        (token, params = {}) => showtimesApi.getAll(token, params),
  getAdminShowtime:         (token, id)          => showtimesApi.getOne(token, id),
  getAdminShowtimeSeatMap:  (token, id)          => request(`/api/v1/admin/showtimes/${id}/seat-map`, { token }),
  createAdminShowtime:      (token, payload)     => showtimesApi.create(token, payload),
  createAdminShowtimesBulk: (token, payload)     => request('/api/v1/admin/showtimes/bulk', { method: 'POST', token, body: payload }),
  updateAdminShowtime:      (token, id, payload) => showtimesApi.update(token, id, payload),
  updateAdminShowtimeStatus: (token, id, status) => showtimesApi.patchQuery(token, id, status),
  cancelShowtimeAndRefund:  (token, id, reason)  => request(`/api/v1/admin/showtimes/${enc(id)}/cancel-and-refund`, { method: 'POST', token, body: { reason } }),
  deleteAdminShowtime:      (token, id)          => showtimesApi.remove(token, id),

  // CineWallet
  getWalletDashboard:       (token) => request('/api/v1/admin/wallet/dashboard', { token }),
  getAdminWithdrawals:      (token, params = {}) => request(`/api/v1/admin/wallet/withdrawals${buildQueryString(params)}`, { token }),
  approveWithdrawal:        (token, id, payload) => request(`/api/v1/admin/wallet/withdrawals/${enc(id)}/approve`, { method: 'POST', token, body: payload }),
  rejectWithdrawal:         (token, id, payload) => request(`/api/v1/admin/wallet/withdrawals/${enc(id)}/reject`, { method: 'POST', token, body: payload }),


  // Loyalty points
  getLoyaltyConfiguration:    (token) => request('/api/v1/admin/loyalty/config', { token }),
  updateLoyaltyConfiguration: (token, payload) => request('/api/v1/admin/loyalty/config', { method: 'PUT', token, body: payload }),
  getLoyaltyTransactions:     (token, params = {}) => request(`/api/v1/admin/loyalty/transactions${buildQueryString(params)}`, { token }),
  getLoyaltyReport:           (token, params = {}) => request(`/api/v1/admin/loyalty/report${buildQueryString(params)}`, { token }),
  expireLoyaltyPointsNow:     (token) => request('/api/v1/admin/loyalty/expire-now', { method: 'POST', token }),
  grantLoyaltyPoints:         (token, payload) => request('/api/v1/admin/loyalty/add', { method: 'POST', token, body: payload }),

  // Reviews
  getAdminReviews:    (token, params = {}) => request(`/api/v1/admin/reviews${buildQueryString(params)}`, { token }).then(normalizePageResponse),
  deleteAdminReview:  (token, reviewId) => request(`/api/v1/admin/reviews/${enc(reviewId)}`, { method: 'DELETE', token }),

  // ── Ticket Pricing ─────────────────────────────────────────────────────────
  getAdminTicketPricingRules:   (token, params = {}) => pricingRules.getAll(token, params),
  createAdminTicketPricingRule: (token, payload)     => pricingRules.create(token, payload),
  updateAdminTicketPricingRule: (token, id, payload) => pricingRules.update(token, id, payload),
  deleteAdminTicketPricingRule: (token, id)          => pricingRules.remove(token, id),

  getAdminTicketCombos:   (token, params = {}) => pricingCombos.getAll(token, params),
  createAdminTicketCombo: (token, payload)     => pricingCombos.create(token, payload),
  updateAdminTicketCombo: (token, id, payload) => pricingCombos.update(token, id, payload),
  deleteAdminTicketCombo: (token, id)          => pricingCombos.remove(token, id),

  // ── Foods ──────────────────────────────────────────────────────────────────
  getAdminFoodItems:    (token)              => foodItemsApi.getAll(token).then(unwrapListPayload),
  getAdminFoodCombos:   (token)              => foodCombosApi.getAll(token).then(unwrapListPayload),
  createAdminFoodItem:  (token, payload)     => foodItemsApi.create(token, payload),
  createAdminFoodCombo: (token, payload)     => foodCombosApi.create(token, payload),
  updateAdminFoodItem:  (token, id, payload) => foodItemsApi.update(token, id, payload),
  updateAdminFoodCombo: (token, id, payload) => foodCombosApi.update(token, id, payload),

  // ── Reports ────────────────────────────────────────────────────────────────
  getRevenueReport:  (token, params = {}) => request(`/api/v1/admin/reports/revenue${buildQueryString(params)}`, { token }),
  getTopMovies:      (token, params = {}) => request(`/api/v1/admin/reports/top-movies${buildQueryString(params)}`, { token }),
  getRoomOccupancy:  (token, params = {}) => request(`/api/v1/admin/reports/occupancy${buildQueryString(params)}`, { token }),
  getDailyOccupancy: (token, params = {}) => request(`/api/v1/admin/reports/occupancy-daily${buildQueryString(params)}`, { token }),
  getShowtimeFill:   (token, params = {}) => request(`/api/v1/admin/reports/showtime-fill${buildQueryString(params)}`, { token }),
  getNoShowReport:   (token, params = {}) => request(`/api/v1/admin/reports/no-shows${buildQueryString(params)}`, { token }),
  getPeakHours:      (token, params = {}) => request(`/api/v1/admin/reports/peak-hours${buildQueryString(params)}`, { token }),
  getTopSeats:       (token, params = {}) => request(`/api/v1/admin/reports/top-seats${buildQueryString(params)}`, { token }),
  getAbandonedRate:  (token, params = {}) => request(`/api/v1/admin/reports/abandoned${buildQueryString(params)}`, { token }),
  getConcessionSales: (token, params = {}) => request(`/api/v1/admin/reports/concessions${buildQueryString(params)}`, { token }),
  getExpiredUsers:   (token, params = {}) => request(`/api/v1/admin/reports/expired-users${buildQueryString(params)}`, { token }),
  getShowtimeIncidentsReport: (token, params = {}) => request(`/api/v1/admin/reports/showtime-incidents${buildQueryString(params)}`, { token }),

  // ── Audit logs ─────────────────────────────────────────────────────────────
  getAuditLogs:      (token, params = {}) => request(`/api/v1/admin/audit-logs${buildQueryString(params)}`, { token }).then(normalizePageResponse),

  // ── Bookings (quản lý vé) ──────────────────────────────────────────────────
  getAdminBookings:      (token, params = {}) => bookingsApi.getAll(token, params).then(normalizePageResponse),
  getAdminBookingDetail: (token, bookingId)   => bookingsApi.getOne(token, bookingId),

  // ── Showtime slots ─────────────────────────────────────────────────────────
  getAvailableShowtimeSlots: (token, params = {}) => request(`/api/v1/admin/showtimes/available-slots${buildQueryString(params)}`, { token }),

  // ── Bookings / Giao dịch ───────────────────────────────────────────────────
  getAdminBookings:     (token, params = {}) => bookingsApi.getAll(token, params),
  getAdminBooking:      (token, id)          => bookingsApi.getOne(token, id),
  // Hủy vé; nếu vé đã thanh toán (PAID) backend tự hoàn tiền về CineWallet của khách.
  cancelBookingAdmin:   (token, id, reason)  => request(`/api/v1/admin/bookings/${enc(id)}${reason ? `?reason=${enc(reason)}` : ''}`, { method: 'DELETE', token }),
};
