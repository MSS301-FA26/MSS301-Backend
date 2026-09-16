export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export const STORAGE_KEYS = {
  accessToken: 'cinepremier_access_token',
  accessTokenExpiresAt: 'cinepremier_access_token_expires_at',
  refreshToken: 'cinepremier_refresh_token',
  user: 'cinepremier_auth_user',
  roles: 'cinepremier_auth_roles'
};

export const ADMIN_ACCESS_OVERRIDE = false;
