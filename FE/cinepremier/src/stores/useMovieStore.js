import { create } from 'zustand';
import { getStoredAuth, hasBackendAdminAccess, hasBackendStaffAccess } from '../services/authService';
import { movieService } from '../services/movieService';
import { wishlistService } from '../services/wishlistService';
import { useAuthStore } from './useAuthStore';
import { useUiStore } from './useUiStore';

const PUBLIC_CACHE_TTL_MS = 5 * 60 * 1000;
const movieStoreCache = {
  publicCinema: null,
  genres: null,
  moviePages: new Map(),
  foodCatalog: null,
};

const getFreshCache = (entry) => {
  if (!entry) return null;
  return Date.now() - entry.updatedAt < PUBLIC_CACHE_TTL_MS ? entry.data : null;
};

const toCacheEntry = (data) => ({ data, updatedAt: Date.now() });

const normalizeFoodCatalog = (items = [], combos = []) => (
  [
    ...items.map(item => ({ ...item, id: `item-${item.id}`, backendId: item.id, foodItemId: item.id, category: 'item' })),
    ...combos.map(item => ({ ...item, id: `combo-${item.id}`, backendId: item.id, foodComboId: item.id, category: 'combo' })),
  ]
);

export const useMovieStore = create((set, get) => ({
  moviesList: [],
  moviePagination: {
    page: 0,
    size: 10,
    totalPages: 1,
    totalElements: 0,
  },
  isMoviesLoading: false,
  searchQuery: '',
  movieDateFilter: '',
  genres: [],
  selectedGenreId: '',
  publicCinema: null,
  watchlist: [],
  bookedTickets: [],
  foodCatalog: [],

  setMoviesList: (moviesList) => set((state) => ({
    moviesList: typeof moviesList === 'function' ? moviesList(state.moviesList) : moviesList
  })),
  setMoviePagination: (moviePagination) => set((state) => ({
    moviePagination: typeof moviePagination === 'function' ? moviePagination(state.moviePagination) : moviePagination
  })),
  setSearchQuery: (searchQuery) => set({ searchQuery }),
  setMovieDateFilter: (movieDateFilter) => set({ movieDateFilter }),
  setSelectedGenreId: (selectedGenreId) => set({ selectedGenreId }),
  setBookedTickets: (bookedTickets) => set((state) => ({
    bookedTickets: typeof bookedTickets === 'function' ? bookedTickets(state.bookedTickets) : bookedTickets
  })),
  setFoodCatalog: (foodCatalog) => set({ foodCatalog }),

  fetchPublicCinema: async ({ force = false } = {}) => {
    const cachedCinema = force ? null : getFreshCache(movieStoreCache.publicCinema);
    if (cachedCinema) {
      set({ publicCinema: cachedCinema });
      return cachedCinema;
    }

    try {
      const cinema = await movieService.getPublicCinema();
      movieStoreCache.publicCinema = toCacheEntry(cinema);
      set({ publicCinema: cinema });
      return cinema;
    } catch {
      set({ publicCinema: null });
      return null;
    }
  },

  fetchGenres: async () => {
    const cachedGenres = getFreshCache(movieStoreCache.genres);
    if (cachedGenres) {
      set({ genres: cachedGenres });
      return cachedGenres;
    }

    try {
      const data = await movieService.getGenres();
      const nextGenres = Array.isArray(data) ? data : [];
      movieStoreCache.genres = toCacheEntry(nextGenres);
      set({ genres: nextGenres });
      return nextGenres;
    } catch {
      set({ genres: [] });
      return [];
    }
  },

  fetchMoviesPage: async ({ isExplorePage = false } = {}) => {
    const { searchQuery, selectedGenreId, movieDateFilter, moviePagination } = get();
    const moviePageParams = {
      keyword: isExplorePage ? searchQuery.trim() : '',
      genreId: isExplorePage ? selectedGenreId : '',
      fromDate: isExplorePage ? movieDateFilter : '',
      toDate: isExplorePage ? movieDateFilter : '',
      page: moviePagination.page,
      size: moviePagination.size,
    };
    const moviePageKey = JSON.stringify(moviePageParams);

    const applyMoviePage = (pageData) => {
      const visibleMovies = pageData.items.filter(movie => movie.status !== 'INACTIVE' && !movie.isInactive);
      set((state) => ({
        moviesList: visibleMovies.map((movie) => {
          const existing = state.moviesList.find((item) => (
            String(item.backendId || item.id) === String(movie.backendId || movie.id)
          ));
          if (!existing) return movie;

          const hasActors = Array.isArray(movie.actors) && movie.actors.length > 0;
          const hasActorIds = Array.isArray(movie.actorIds) && movie.actorIds.length > 0;
          const hasMainActorIds = Array.isArray(movie.mainActorIds) && movie.mainActorIds.length > 0;
          const hasDetailText = movie.raw?.description || movie.raw?.synopsis || movie.raw?.overview || movie.raw?.content;
          const hasDirector = movie.raw?.director || movie.raw?.directorName;
          const hasTrailer = movie.raw?.trailerUrl
            || movie.raw?.trailerURL
            || movie.raw?.trailer_url
            || movie.raw?.trailer
            || movie.raw?.videoUrl
            || movie.raw?.videoURL
            || movie.raw?.video_url;

          return {
            ...existing,
            ...movie,
            synopsis: hasDetailText ? movie.synopsis : existing.synopsis,
            director: hasDirector ? movie.director : existing.director,
            trailerUrl: hasTrailer ? movie.trailerUrl : existing.trailerUrl,
            actors: hasActors ? movie.actors : existing.actors,
            actorIds: hasActorIds ? movie.actorIds : existing.actorIds,
            mainActorIds: hasMainActorIds ? movie.mainActorIds : existing.mainActorIds,
            mainActors: movie.mainActors || existing.mainActors,
            castList: movie.castList || existing.castList,
          };
        }),
        moviePagination: {
          ...state.moviePagination,
          page: pageData.page,
          size: pageData.size,
          totalPages: pageData.totalPages,
          totalElements: pageData.totalElements,
        }
      }));
    };

    const cachedMoviePage = getFreshCache(movieStoreCache.moviePages.get(moviePageKey));
    if (cachedMoviePage) {
      applyMoviePage(cachedMoviePage);
      return cachedMoviePage;
    }

    set({ isMoviesLoading: true });
    try {
      const pageData = await movieService.searchMoviesPage(moviePageParams);
      movieStoreCache.moviePages.set(moviePageKey, toCacheEntry(pageData));
      applyMoviePage(pageData);
      return pageData;
    } catch (error) {
      useUiStore.getState().showToast(error.message || 'Khong the tai danh sach phim.', 5000, null, 'sad');
      return null;
    } finally {
      set({ isMoviesLoading: false });
    }
  },

  fetchWishlist: async ({ isLoggedIn, currentRole } = {}) => {
    const { accessToken, user } = getStoredAuth();
    const isAdmin = currentRole === 'admin' || hasBackendAdminAccess(accessToken, user);
    const isStaff = currentRole === 'staff' || hasBackendStaffAccess(accessToken, user);
    if (!isLoggedIn || !accessToken || isAdmin || isStaff) {
      set({ watchlist: [] });
      return [];
    }
    try {
      const items = await wishlistService.getWishlist(accessToken);
      const moviesList = get().moviesList;
      const watchlist = items.map((item) => {
        const match = moviesList.find((movie) => String(movie.backendId || movie.id) === String(item.backendId || item.id));
        return match ? { ...match, ...item, backendId: match.backendId || item.backendId || item.id } : item;
      });
      set({ watchlist });
      return watchlist;
    } catch {
      set({ watchlist: [] });
      return [];
    }
  },

  fetchPublicFoodCatalog: async ({ force = false } = {}) => {
    const cachedFoodCatalog = force ? null : getFreshCache(movieStoreCache.foodCatalog);
    if (cachedFoodCatalog) {
      set({ foodCatalog: cachedFoodCatalog });
      return cachedFoodCatalog;
    }

    try {
      const [items, combos] = await Promise.all([movieService.getFoodItems(), movieService.getFoodCombos()]);
      const catalog = normalizeFoodCatalog(items || [], combos || []);
      movieStoreCache.foodCatalog = toCacheEntry(catalog);
      set({ foodCatalog: catalog });
      return catalog;
    } catch {
      set({ foodCatalog: [] });
      return [];
    }
  },

  handleToggleWatchlist: async (movie, { isLoggedIn = useAuthStore.getState().isLoggedIn } = {}) => {
    const backendMovieId = movie.backendId || movie.movieId || movie.id;
    const { accessToken, user } = getStoredAuth();
    const showToast = useUiStore.getState().showToast;
    const authState = useAuthStore.getState();
    const isAdmin = authState.currentRole === 'admin' || hasBackendAdminAccess(accessToken, user || authState.currentUser);
    const isStaff = authState.currentRole === 'staff' || hasBackendStaffAccess(accessToken, user || authState.currentUser);
    if (isAdmin || isStaff) {
      set({ watchlist: [] });
      return;
    }
    if (!isLoggedIn || !accessToken) {
      showToast('Vui long dang nhap de dong bo watchlist voi he thong.', 4500, null, 'sad');
      return;
    }
    if (isNaN(Number(backendMovieId))) {
      showToast('Phim mau chua co ma backend nen chua the luu watchlist.', 4500, null, 'sad');
      return;
    }

    const { watchlist } = get();
    const exists = watchlist.some(item => String(item.backendId || item.id) === String(backendMovieId));
    const previous = watchlist;
    set({
      watchlist: exists
        ? watchlist.filter(item => String(item.backendId || item.id) !== String(backendMovieId))
        : [...watchlist, { ...movie, backendId: Number(backendMovieId) }]
    });

    try {
      if (exists) {
        await wishlistService.removeWishlist(accessToken, backendMovieId);
        showToast('Da xoa phim khoi watchlist.');
      } else {
        await wishlistService.addWishlist(accessToken, backendMovieId);
        showToast('Da them phim vao watchlist.');
      }
    } catch (error) {
      set({ watchlist: previous });
      showToast(error.message || 'Khong the dong bo watchlist voi backend.', 4500, null, 'sad');
    }
  }
}));

export const useMovies = useMovieStore;
