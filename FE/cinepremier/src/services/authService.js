import apiClient from '../configs/axios';
import { ADMIN_ACCESS_OVERRIDE, STORAGE_KEYS } from '../configs/constants';

export const parseJwtPayload = (accessToken) => {
  try {
    if (!accessToken || !accessToken.includes('.')) return null;
    return JSON.parse(atob(accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
  } catch (error) {
    return null;
  }
};

const getRoleValues = (accessToken, user = null) => {
  const tokenPayload = parseJwtPayload(accessToken);
  const isTokenExpired = tokenPayload?.exp ? tokenPayload.exp * 1000 <= Date.now() : false;
  if (!accessToken || isTokenExpired) return [];

  return [
    user?.role,
    ...(Array.isArray(user?.roles) ? user.roles : []),
    ...(Array.isArray(tokenPayload?.roles) ? tokenPayload.roles : []),
    ...(Array.isArray(tokenPayload?.authorities) ? tokenPayload.authorities : []),
    ...(Array.isArray(tokenPayload?.scope) ? tokenPayload.scope : String(tokenPayload?.scope || '').split(' '))
  ].map((role) => String(role).toUpperCase()).filter(Boolean);
};

export const hasBackendAdminAccess = (accessToken, user = null) => {
  const roleValues = getRoleValues(accessToken, user);
  return roleValues.includes('ADMIN') || roleValues.includes('ROLE_ADMIN');
};

export const hasBackendStaffAccess = (accessToken, user = null) => {
  const roleValues = getRoleValues(accessToken, user);
  return roleValues.includes('STAFF') || roleValues.includes('ROLE_STAFF');
};

const resolveRole = (roles = []) => {
  const normalized = roles.map((role) => String(role).toUpperCase());
  if (ADMIN_ACCESS_OVERRIDE || normalized.includes('ADMIN') || normalized.includes('ROLE_ADMIN')) return 'admin';
  if (normalized.includes('STAFF') || normalized.includes('ROLE_STAFF')) return 'staff';
  return 'user';
};

const resolveAccessTokenExpiresAt = (authData = {}) => {
  const expiresInMs = Number(authData.expiresInMs);
  if (Number.isFinite(expiresInMs) && expiresInMs > 0) {
    return Date.now() + expiresInMs;
  }

  const tokenPayload = parseJwtPayload(authData.accessToken);
  return tokenPayload?.exp ? tokenPayload.exp * 1000 : null;
};

export const normalizeUser = (user, roles = user?.roles || [], accessToken = null) => {
  if (!user) return null;
  const tokenPayload = parseJwtPayload(accessToken);
  const tokenRoles = [
    ...(Array.isArray(tokenPayload?.roles) ? tokenPayload.roles : []),
    ...(Array.isArray(tokenPayload?.authorities) ? tokenPayload.authorities : []),
    ...(Array.isArray(tokenPayload?.scope) ? tokenPayload.scope : String(tokenPayload?.scope || '').split(' '))
  ];
  const resolvedRoles = Array.from(new Set([
    ...(Array.isArray(roles) ? roles : []),
    ...(Array.isArray(user.roles) ? user.roles : []),
    ...(user.role ? [user.role] : []),
    ...tokenRoles
  ].map((r) => String(r).toUpperCase()).filter(Boolean)));

  return {
    ...user,
    roles: resolvedRoles,
    name: user.fullName || user.name || user.email,
    role: resolveRole(resolvedRoles),
    emailVerified: Boolean(user.emailVerified),
    passwordChangeRequired: Boolean(user.passwordChangeRequired)
  };
};

export const saveAuthSession = (authData) => {
  if (!authData) return null;

  const user = normalizeUser(authData.user, authData.roles, authData.accessToken);
  localStorage.setItem(STORAGE_KEYS.accessToken, authData.accessToken);
  const accessTokenExpiresAt = resolveAccessTokenExpiresAt(authData);
  if (accessTokenExpiresAt) {
    localStorage.setItem(STORAGE_KEYS.accessTokenExpiresAt, String(accessTokenExpiresAt));
  } else {
    localStorage.removeItem(STORAGE_KEYS.accessTokenExpiresAt);
  }
  if (authData.refreshToken) {
    localStorage.setItem(STORAGE_KEYS.refreshToken, authData.refreshToken);
  }
  localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
  localStorage.setItem(STORAGE_KEYS.roles, JSON.stringify(authData.roles || user?.roles || []));

  return user;
};

export const clearAuthSession = () => {
  Object.values(STORAGE_KEYS).forEach((key) => localStorage.removeItem(key));
  Object.keys(localStorage)
    .filter((key) => key.startsWith('cinepremier_chat_conversation_id'))
    .forEach((key) => localStorage.removeItem(key));
};

export const expireAuthSession = () => {
  clearAuthSession();
  window.dispatchEvent(new CustomEvent('auth:session-expired'));
};

export const getStoredAuth = () => {
  const accessToken = localStorage.getItem(STORAGE_KEYS.accessToken);
  const accessTokenExpiresAt = Number(localStorage.getItem(STORAGE_KEYS.accessTokenExpiresAt) || 0) || null;
  const refreshToken = localStorage.getItem(STORAGE_KEYS.refreshToken);
  const roles = JSON.parse(localStorage.getItem(STORAGE_KEYS.roles) || '[]');
  const storedUser = localStorage.getItem(STORAGE_KEYS.user);
  const user = storedUser ? normalizeUser(JSON.parse(storedUser), roles, accessToken) : null;

  return { accessToken, accessTokenExpiresAt, refreshToken, roles, user };
};

const isAccessTokenExpired = (accessToken, accessTokenExpiresAt) => {
  if (!accessToken) return true;
  if (accessTokenExpiresAt) return accessTokenExpiresAt <= Date.now();
  const tokenPayload = parseJwtPayload(accessToken);
  return tokenPayload?.exp ? tokenPayload.exp * 1000 <= Date.now() : false;
};

const createApiError = (payload, status) => {
  const fieldErrors = payload?.fieldErrors
    ? Object.values(payload.fieldErrors).flat().join(', ')
    : Array.isArray(payload?.errors)
      ? payload.errors.map((error) => error.message || `${error.field}: invalid`).join(', ')
      : '';
  const error = new Error(fieldErrors || payload?.message || `Request failed (${status || 'unknown'})`);
  error.status = status;
  return error;
};

const unwrapResponse = (response) => {
  const payload = response?.data ?? null;
  if (payload?.success === false) {
    throw createApiError(payload, response?.status);
  }

  return payload?.data ?? payload;
};

const normalizeAxiosError = (error) => {
  if (error?.response) {
    return createApiError(error.response.data, error.response.status);
  }
  const normalizedError = new Error(error?.message || 'Khong the ket noi may chu.');
  normalizedError.status = error?.status;
  return normalizedError;
};

const tryRefreshToken = async () => {
  const { refreshToken } = getStoredAuth();
  if (!refreshToken) return null;
  try {
    const response = await apiClient.post('/api/v1/auth/refresh', { refreshToken }, {
      headers: { 'Content-Type': 'application/json' }
    });
    const data = unwrapResponse(response);
    if (data?.accessToken) {
      saveAuthSession(data);
      return data.accessToken;
    }
  } catch { /* ignore */ }
  return null;
};

const inflightGetRequests = new Map();

const getDedupeKey = (path, token) => `${token ? `auth:${token}` : 'public'}:${path}`;

const performRequest = async (path, { method = 'GET', body, token, timeout } = {}) => {
  const isFormData = body instanceof FormData;
  const hadToken = Boolean(token);
  let effectiveToken = token;
  if (effectiveToken) {
    const { accessTokenExpiresAt } = getStoredAuth();
    if (isAccessTokenExpired(effectiveToken, accessTokenExpiresAt)) {
      effectiveToken = await tryRefreshToken();
    }
  }

  const headers = isFormData ? {} : { 'Content-Type': 'application/json' };
  if (effectiveToken) headers.Authorization = `Bearer ${effectiveToken}`;

  try {
    const response = await apiClient.request({
      url: path,
      method,
      headers,
      data: body === undefined ? undefined : body,
      ...(timeout !== undefined ? { timeout } : {})
    });

    return unwrapResponse(response);
  } catch (requestError) {
    const status = requestError?.response?.status;
    if (!(hadToken && (status === 401 || status === 403))) {
      throw normalizeAxiosError(requestError);
    }

    const newToken = await tryRefreshToken();
    if (newToken) {
      const retryHeaders = isFormData
        ? { Authorization: `Bearer ${newToken}` }
        : { 'Content-Type': 'application/json', Authorization: `Bearer ${newToken}` };
      try {
        const retryResponse = await apiClient.request({
          url: path,
          method,
          headers: retryHeaders,
          data: body === undefined ? undefined : body,
          ...(timeout !== undefined ? { timeout } : {})
        });
        return unwrapResponse(retryResponse);
      } catch (retryError) {
        throw normalizeAxiosError(retryError);
      }
    }

    expireAuthSession();
    const error = new Error('Phien dang nhap da het han. Vui long dang nhap lai.');
    error.status = 401;
    throw error;
  }
};

export const request = (path, { method = 'GET', body, token, timeout } = {}) => {
  const normalizedMethod = String(method || 'GET').toUpperCase();
  const shouldDedupe = normalizedMethod === 'GET' && body === undefined;

  if (!shouldDedupe) {
    return performRequest(path, { method: normalizedMethod, body, token, timeout });
  }

  const dedupeKey = getDedupeKey(path, token);
  const existingRequest = inflightGetRequests.get(dedupeKey);
  if (existingRequest) return existingRequest;

  const promise = performRequest(path, { method: normalizedMethod, body, token, timeout })
    .finally(() => inflightGetRequests.delete(dedupeKey));
  inflightGetRequests.set(dedupeKey, promise);
  return promise;
};

export const buildQueryString = (params = {}) => {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null && item !== '') searchParams.append(key, item);
      });
      return;
    }
    searchParams.set(key, value);
  });
  const query = searchParams.toString();
  return query ? `?${query}` : '';
};

export const unwrapListPayload = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.movies)) return payload.movies;
  if (Array.isArray(payload?.results)) return payload.results;
  if (Array.isArray(payload?.data)) return payload.data;
  return [];
};

export const authService = {
  register: (payload) => request('/api/v1/auth/register', { method: 'POST', body: payload }),
  verifyEmail: (email, otp) => request('/api/v1/auth/verify-email', { method: 'POST', body: { email, otp } }),
  requestEmailVerification: (email) => request('/api/v1/auth/verify-email/request', {
    method: 'POST',
    body: { email }
  }),
  login: (payload) => request('/api/v1/auth/login', { method: 'POST', body: payload }),
  loginWithGoogle: (credential) => request('/api/v1/auth/google', {
    method: 'POST',
    body: { credential }
  }),
  verifyGoogleLoginOtp: (email, otp) => request('/api/v1/auth/google/verify', {
    method: 'POST',
    body: { email, otp }
  }),
  refresh: (refreshToken) => request('/api/v1/auth/refresh', { method: 'POST', body: { refreshToken } }),
  logout: (refreshToken) => request('/api/v1/auth/logout', { method: 'POST', body: { refreshToken } }),
  requestPasswordReset: (email) => request('/api/v1/auth/password-reset/request', {
    method: 'POST',
    body: { email }
  }),
  verifyPasswordResetOtp: ({ email, otp }) => request('/api/v1/auth/password-reset/verify', {
    method: 'POST',
    body: { email, otp }
  }),
  confirmPasswordReset: ({ email, otp, newPassword, confirmPassword }) => request('/api/v1/auth/password-reset/confirm', {
    method: 'POST',
    body: { email, otp, newPassword, confirmPassword }
  })
};
