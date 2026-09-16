import { request, buildQueryString } from './authService';

const enc = encodeURIComponent;

export const walletService = {
  // ─── User Endpoints ──────────────────────────────────────────────────────
  getWallet:            (token) => request('/api/v1/wallet', { token }),
  getTransactions:      (token, params = {}) => request(`/api/v1/wallet/transactions${buildQueryString(params)}`, { token }),
  createWithdrawal:     (token, payload) => request('/api/v1/wallet/withdrawals', { method: 'POST', token, body: payload }),
  getMyWithdrawals:     (token, params = {}) => request(`/api/v1/wallet/withdrawals${buildQueryString(params)}`, { token }),
};
