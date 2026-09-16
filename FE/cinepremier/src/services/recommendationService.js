import { request, unwrapListPayload } from './authService';
import { normalizeMovie } from './movieService';

const getRecommendationMovie = (item = {}) => item.movie || item.movieInfo || item.film || item;

const normalizeRecommendation = (item = {}) => {
  const source = getRecommendationMovie(item);
  const movieId = item.movieId ?? item.movie_id ?? source.id ?? source.movieId;
  const posterUrl = item.posterUrl
    || item.poster_url
    || item.moviePosterUrl
    || item.imageUrl
    || source.posterUrl
    || source.poster_url
    || source.moviePosterUrl
    || source.imageUrl
    || source.thumbnailUrl;

  return {
    ...normalizeMovie({
      ...source,
      id: movieId,
      movieId,
      title: item.title || source.title || source.name,
      posterUrl
    }, {
      id: movieId,
      backendId: movieId,
      posterUrl
    }),
    similarity: typeof item.similarity === 'number' ? item.similarity : null,
    ...pickRecExplanation(item)
  };
};

// Explanation metadata từ recommender (source/reason/...) — giữ nguyên khi normalize
export const REC_EXPLANATION_FIELDS = [
  'source', 'reason', 'predictedRating', 'neighborCount', 'anchorTitle',
  'matchedGenres', 'matchedActors', 'sameDirector', 'directorName',
  'avgRating', 'ratingCount', 'bookingCount'
];

export const pickRecExplanation = (item = {}) => {
  const out = {};
  for (const key of REC_EXPLANATION_FIELDS) {
    if (item[key] !== undefined && item[key] !== null) out[key] = item[key];
  }
  if (typeof item.similarity === 'number') out.similarity = item.similarity;
  return out;
};

export const normalizeRecommendationResponse = (payload) =>
  unwrapListPayload(payload).map(normalizeRecommendation);

export const recommendationService = {
  // Content-based: "similar movies" — public endpoint
  getContentRecommendations: (movieId) =>
    request(`/api/v1/recommendation/content/${encodeURIComponent(movieId)}`)
      .then(normalizeRecommendationResponse),
  // Collaborative: "recommended for you" — requires auth token
  getCollaborativeRecommendations: (userId, token) =>
    request(`/api/v1/recommendation/collaborative/${encodeURIComponent(userId)}`, { token })
      .then(normalizeRecommendationResponse),
  // Live stats: số liệu thật hệ gợi ý đang phân tích (modal "Cách hoạt động") — public
  getRecommendationStats: () => request('/api/v1/recommendation/stats')
};
