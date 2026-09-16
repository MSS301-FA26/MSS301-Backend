import { buildQueryString, request, unwrapListPayload } from './authService';

const normalizeGenres = (movie = {}) => {
  const source = movie.genreNames || movie.genreNameList || movie.genres || movie.genre || movie.categories || [];
  const values = Array.isArray(source) ? source : String(source || '').split(',');
  const genres = values
    .map((item) => {
      if (!item) return '';
      if (typeof item === 'string') return item.trim();
      return item.name || item.genreName || item.title || '';
    })
    .filter(Boolean);

  return genres.length ? genres : ['Dang cap nhat'];
};

const normalizeMovieStatus = (movie = {}) => String(movie.status || movie.movieStatus || '').toUpperCase();

const getMovieTrailerUrl = (movie = {}, fallback = {}) => (
  movie.trailerUrl
  || movie.trailerURL
  || movie.trailer_url
  || movie.trailer
  || movie.videoUrl
  || movie.videoURL
  || movie.video_url
  || fallback.trailerUrl
  || ''
);

export const normalizeMovie = (movie = {}, fallback = {}) => {
  const status = normalizeMovieStatus(movie) || normalizeMovieStatus(fallback);
  const id = movie.id ?? movie.movieId ?? movie.slug ?? movie.code ?? fallback.id;
  const ratings = movie.ratings || {};
  const overall = Number(movie.rating ?? movie.averageRating ?? ratings.overall ?? fallback.ratings?.overall ?? 8.8);
  const statusFromFlags = (movie.isUpcoming ?? fallback.isUpcoming) ? 'UPCOMING' : 'NOW_SHOWING';
  const effectiveStatus = status || statusFromFlags;
  const isUpcoming = ['UPCOMING', 'COMING_SOON', 'SCHEDULED', 'DRAFT'].includes(effectiveStatus);
  const isInactive = effectiveStatus === 'INACTIVE';

  return {
    ...fallback,
    ...movie,
    id: String(id || ''),
    backendId: movie.id ?? movie.movieId ?? fallback.backendId,
    title: movie.title || movie.name || movie.movieTitle || fallback.title || 'Phim chua dat ten',
    englishTitle: movie.englishTitle || movie.originalTitle || movie.subTitle || movie.titleEn || fallback.englishTitle || movie.title || movie.name || 'CinePremier Feature',
    genre: normalizeGenres(movie),
    synopsis: movie.synopsis || movie.description || movie.overview || movie.content || fallback.synopsis || 'Thong tin noi dung phim dang duoc cap nhat.',
    duration: Number(movie.duration ?? movie.durationMinutes ?? movie.runningTime ?? fallback.duration ?? 100),
    ageRating: movie.ageRating || movie.ratingLabel || movie.ageLimit || fallback.ageRating || 'P',
    posterUrl: movie.posterUrl || movie.poster || movie.posterImageUrl || movie.imageUrl || movie.thumbnailUrl || fallback.posterUrl || 'https://res.cloudinary.com/dmcodhbcc/image/upload/v1784275470/cinemams/posters/placeholder.jpg',
    bannerUrl: movie.bannerUrl || movie.avatarUrl || movie.backdropUrl || movie.coverUrl || movie.bannerImageUrl || fallback.bannerUrl || movie.posterUrl || fallback.posterUrl || 'https://res.cloudinary.com/dmcodhbcc/image/upload/v1784275470/cinemams/posters/placeholder.jpg',
    releaseDate: movie.releaseDate || movie.premiereDate || movie.startDate || fallback.releaseDate || 'Dang cap nhat',
    endDate: movie.endDate || movie.finishDate || movie.endedDate || fallback.endDate || '',
    trailerUrl: getMovieTrailerUrl(movie, fallback),
    director: movie.director || movie.directorName || fallback.director || 'Dang cap nhat',
    mainActors: movie.mainActors || fallback.mainActors || movie.castList || fallback.castList || '',
    castList: movie.castList || fallback.castList || movie.mainActors || fallback.mainActors || '',
    actorIds: Array.isArray(movie.actors)
      ? movie.actors.map((actor) => actor.id ?? actor.actorId).filter((actorId) => actorId !== undefined && actorId !== null)
      : (movie.actorIds || fallback.actorIds || []),
    mainActorIds: movie.mainActorIds || fallback.mainActorIds || [],
    actors: movie.actors || fallback.actors || [],
    language: movie.language || fallback.language || 'Dang cap nhat',
    status: effectiveStatus,
    isUpcoming: Boolean(isUpcoming),
    isInactive,
    isNowShowing: effectiveStatus === 'NOW_SHOWING',
    isEnded: effectiveStatus === 'ENDED',
    upcomingDate: movie.upcomingDate || fallback.upcomingDate,
    ratings: {
      ...fallback.ratings,
      ...ratings,
      overall,
      story: Number(ratings.story ?? fallback.ratings?.story ?? Math.max(0, overall - 0.2)),
      acting: Number(ratings.acting ?? fallback.ratings?.acting ?? Math.max(0, overall - 0.3)),
      visual: Number(ratings.visual ?? fallback.ratings?.visual ?? overall),
      audio: Number(ratings.audio ?? fallback.ratings?.audio ?? Math.max(0, overall - 0.1))
    },
    tags: movie.tags || fallback.tags || [],
    raw: movie
  };
};

export const normalizeMovieListResponse = (payload) => unwrapListPayload(payload).map((movie) => normalizeMovie(movie));

const firstNumber = (...values) => {
  const value = values.find((item) => item !== undefined && item !== null && item !== '');
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
};

export const normalizeMoviePageResponse = (payload) => {
  const items = normalizeMovieListResponse(payload);
  const size = firstNumber(payload?.size, payload?.pageSize, payload?.pageable?.pageSize, items.length) || items.length || 8;
  const page = firstNumber(payload?.number, payload?.pageNumber, payload?.page, payload?.currentPage, payload?.pageable?.pageNumber, 0) || 0;
  const totalElements = firstNumber(payload?.totalElements, payload?.totalItems, payload?.total, payload?.totalRecords, items.length) || items.length;
  const totalPages = firstNumber(payload?.totalPages, payload?.pageCount, Math.ceil(totalElements / Math.max(size, 1))) || 1;

  return {
    items,
    page,
    size,
    totalElements,
    totalPages: Math.max(1, totalPages),
    raw: payload
  };
};

export const movieService = {
  searchMovies: (params = {}) => request(`/api/v1/movies${buildQueryString(params)}`)
    .then(normalizeMovieListResponse),
  searchMoviesPage: (params = {}) => request(`/api/v1/movies${buildQueryString(params)}`)
    .then(normalizeMoviePageResponse),
  getMovieDetail: (movieId) => request(`/api/v1/movies/${encodeURIComponent(movieId)}`)
    .then((movie) => normalizeMovie(movie)),
  getActors: () => request('/api/v1/actors'),
  getActorDetail: (actorId) => request(`/api/v1/actors/${encodeURIComponent(actorId)}`),
  getMoviesByActor: (actorId) => request(`/api/v1/actors/${encodeURIComponent(actorId)}/movies`)
    .then(normalizeMovieListResponse),
  getGenres: () => request('/api/v1/genres'),
  getFoodItems: () => request('/api/v1/foods/items').then(unwrapListPayload),
  getFoodCombos: () => request('/api/v1/foods/combos').then(unwrapListPayload),
  getPublicCinema: () => request('/api/v1/cinema')
};

