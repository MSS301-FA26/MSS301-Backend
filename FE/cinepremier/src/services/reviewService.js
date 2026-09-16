import { request } from './authService';

export const reviewService = {
  getMovieReviews: (movieId) => request(`/api/v1/reviews/movies/${encodeURIComponent(movieId)}`),
  getAverageRating: (movieId) => request(`/api/v1/reviews/movies/${encodeURIComponent(movieId)}/average-rating`),
  getMyReviews: (token) => request('/api/v1/reviews/me', { token }),
  createReview: (token, movieId, payload) => request(`/api/v1/reviews/movies/${encodeURIComponent(movieId)}`, {
    method: 'POST',
    token,
    body: payload
  }),
  updateReview: (token, reviewId, payload) => request(`/api/v1/reviews/${encodeURIComponent(reviewId)}`, {
    method: 'PUT',
    token,
    body: payload
  }),
  deleteReview: (token, reviewId) => request(`/api/v1/reviews/${encodeURIComponent(reviewId)}`, {
    method: 'DELETE',
    token
  })
};
