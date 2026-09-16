import { request } from './authService';

export const loyaltyService = {
  getMyLoyalty: (token) => request('/api/v1/loyalty/me', { token }),
  getConfiguration: (token) => request('/api/v1/loyalty/config', { token }),
  redeemMyPoints: (token, points) => request(`/api/v1/loyalty/me/redeem?points=${encodeURIComponent(points)}`, {
    method: 'POST',
    token
  })
};
