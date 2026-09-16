import { buildQueryString, request, unwrapListPayload } from './authService';

export const bookingService = {
  getShowtimes: (params = {}) => request(`/api/v1/showtimes${buildQueryString(params)}`),
  getSeatMap: (showtimeId) => request(`/api/v1/showtimes/${showtimeId}/seat-map`),
  getShowtimeDetail: (showtimeId) => request(`/api/v1/showtimes/${showtimeId}`),
  validateTicketPrice: (token, body) => request('/api/v1/ticket-pricing/validate', { method: 'POST', token, body }),
  holdSeats: (token, body) => request('/api/v1/bookings/hold', { method: 'POST', token, body }),
  updateHoldingBooking: (token, bookingId, body) => request(`/api/v1/bookings/${bookingId}/items`, { method: 'PUT', token, body }),
  createBooking: (token, body) => request('/api/v1/bookings', { method: 'POST', token, body }),
  getMyBookings: (token) => request('/api/v1/bookings?size=100', { token }).then(unwrapListPayload),
  getMyBooking: (token, bookingId) => request(`/api/v1/bookings/${bookingId}`, { token }),
  cancelBooking: (token, bookingId) => request(`/api/v1/bookings/${bookingId}`, { method: 'DELETE', token }),
  createStandaloneFoodOrder: (token, body) => request('/api/v1/food-orders', { method: 'POST', token, body }),
  getMyFoodOrders: (token) => request('/api/v1/food-orders/my', { token }).then(unwrapListPayload),
  cancelFoodOrder: (token, foodOrderId) => request(`/api/v1/food-orders/${foodOrderId}`, { method: 'DELETE', token }),
  createFoodOrder: (token, bookingId, body) => request(`/api/v1/bookings/${bookingId}/food-orders`, { method: 'POST', token, body }),
  getFoodOrders: (token, bookingId) => request(`/api/v1/bookings/${bookingId}/food-orders`, { token })
};
