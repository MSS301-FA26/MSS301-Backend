import { buildQueryString, request } from './authService';

const enc = encodeURIComponent;

/**
 * Tạo object chứa các CRUD operation chuẩn từ một base path.
 *
 * @param {string} basePath  - VD: '/api/v1/admin/movies'
 * @returns {{
 *   getAll:       (token, params?) => Promise,
 *   getOne:       (token, id) => Promise,
 *   create:       (token, payload) => Promise,
 *   update:       (token, id, payload) => Promise,
 *   patchBody:    (token, id, body) => Promise,   // PATCH /status  body: { status }
 *   patchQuery:   (token, id, status) => Promise, // PATCH /status?status=VALUE
 *   remove:       (token, id) => Promise,
 * }}
 */
export const createCrudApi = (basePath) => ({
  getAll:     (token, params = {}) => request(`${basePath}${buildQueryString(params)}`, { token }),
  getOne:     (token, id)          => request(`${basePath}/${enc(id)}`, { token }),
  create:     (token, payload)     => request(basePath, { method: 'POST', token, body: payload }),
  update:     (token, id, payload) => request(`${basePath}/${enc(id)}`, { method: 'PUT', token, body: payload }),
  patchBody:  (token, id, body)    => request(`${basePath}/${enc(id)}/status`, { method: 'PATCH', token, body }),
  patchQuery: (token, id, status)  => request(`${basePath}/${enc(id)}/status?status=${enc(status)}`, { method: 'PATCH', token }),
  remove:     (token, id)          => request(`${basePath}/${enc(id)}`, { method: 'DELETE', token }),
});

/**
 * Upload file qua multipart/form-data.
 *
 * @param {string} token
 * @param {File}   file
 * @param {string} folder   - folder đích trên Cloudinary
 * @param {string} endpoint - VD: '/api/v1/admin/uploads/images'
 */
export const uploadFile = (token, file, folder, endpoint) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('folder', folder);
  return request(endpoint, { method: 'POST', token, body: formData });
};
