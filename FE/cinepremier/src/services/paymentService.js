import { request } from './authService';

export const paymentService = {
  // Tính năng giảm giá/khuyến mãi đã được gỡ bỏ khỏi nghiệp vụ.
  createVnpayPayment: (token, bookingId) => request(`/api/v1/payments/vnpay/create?bookingId=${bookingId}`, { method: 'POST', token }),
  mockPayment: (token, bookingId) => request(`/api/v1/payments/mock?bookingId=${bookingId}`, { method: 'POST', token }),
  getPaymentByBooking: (token, bookingId) => request(`/api/v1/payments/booking/${bookingId}`, { token }),
  createVnpayFoodOrderPayment: (token, foodOrderId) => request(`/api/v1/payments/food-orders/${foodOrderId}/vnpay/create`, { method: 'POST', token }),
  mockFoodOrderPayment: (token, foodOrderId) => request(`/api/v1/payments/food-orders/${foodOrderId}/mock`, { method: 'POST', token })
};
