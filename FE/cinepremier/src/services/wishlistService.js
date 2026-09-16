import { request, unwrapListPayload } from './authService';
import { normalizeMovie } from './movieService';

export const normalizeWishlistResponse = (payload) => unwrapListPayload(payload).map((item) => normalizeMovie({
  id: item.movieId,
  movieId: item.movieId,
  title: item.movieTitle,
  posterUrl: item.posterUrl,
  createdAt: item.createdAt
}, {
  id: item.movieId,
  backendId: item.movieId,
  englishTitle: item.movieTitle || 'CinePremier Feature',
  posterUrl: item.posterUrl
}));

export const wishlistService = {
  getWishlist: (token) => request('/api/v1/wishlist', { token }).then(normalizeWishlistResponse),
  addWishlist: (token, movieId) => request('/api/v1/wishlist', {
    method: 'POST',
    token,
    body: { movieId: Number(movieId) }
  }),
  removeWishlist: (token, movieId) => request(`/api/v1/wishlist/${encodeURIComponent(movieId)}`, {
    method: 'DELETE',
    token
  })
};
