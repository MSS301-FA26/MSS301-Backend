import { request } from './authService';

export const userService = {
  getMyProfile: (token) => request('/api/v1/users/me', { token }),
  updateMyProfile: (token, payload) => request('/api/v1/users/me', {
    method: 'PUT',
    token,
    body: payload
  }),
  uploadMyAvatar: (token, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request('/api/v1/users/me/avatar', {
      method: 'POST',
      token,
      body: formData
    });
  },
  changeMyPassword: (token, payload) => request('/api/v1/users/me/password', {
    method: 'POST',
    token,
    body: payload
  })
};
