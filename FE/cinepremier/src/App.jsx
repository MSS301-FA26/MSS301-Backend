import React, { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Toaster } from 'sonner';
import AppRoutes from './routes/AppRoutes';
import { useAuthStore } from './stores/useAuthStore';
import { useMovieStore } from './stores/useMovieStore';
import { useUiStore } from './stores/useUiStore';

function AuthBootstrap() {
  const restoreSession = useAuthStore((state) => state.restoreSession);
  const subscribeSessionExpired = useAuthStore((state) => state.subscribeSessionExpired);
  const setShowOTP = useUiStore((state) => state.setShowOTP);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  useEffect(() => subscribeSessionExpired({ setShowOTP }), [setShowOTP, subscribeSessionExpired]);

  return null;
}

function MovieBootstrap() {
  const location = useLocation();
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const currentRole = useAuthStore((state) => state.currentRole);
  const fetchPublicCinema = useMovieStore((state) => state.fetchPublicCinema);
  const fetchGenres = useMovieStore((state) => state.fetchGenres);
  const fetchMoviesPage = useMovieStore((state) => state.fetchMoviesPage);
  const fetchWishlist = useMovieStore((state) => state.fetchWishlist);
  const setFoodCatalog = useMovieStore((state) => state.setFoodCatalog);
  const searchQuery = useMovieStore((state) => state.searchQuery);
  const selectedGenreId = useMovieStore((state) => state.selectedGenreId);
  const movieDateFilter = useMovieStore((state) => state.movieDateFilter);
  const moviePage = useMovieStore((state) => state.moviePagination.page);
  const moviePageSize = useMovieStore((state) => state.moviePagination.size);
  const isExplorePage = location.pathname === '/movies';

  useEffect(() => {
    fetchPublicCinema();
    fetchGenres();
  }, [fetchGenres, fetchPublicCinema]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      fetchMoviesPage({ isExplorePage });
    }, 350);
    return () => window.clearTimeout(timeoutId);
  }, [fetchMoviesPage, isExplorePage, searchQuery, selectedGenreId, movieDateFilter, moviePage, moviePageSize]);

  useEffect(() => {
    if (!isLoggedIn) setFoodCatalog([]);
    fetchWishlist({ isLoggedIn, currentRole });
  }, [currentRole, fetchWishlist, isLoggedIn, setFoodCatalog]);

  return null;
}

export default function App() {
  return (
    <>
      <AuthBootstrap />
      <MovieBootstrap />
      <Toaster
        position="top-right"
        theme="dark"
        richColors={false}
        expand
        visibleToasts={3}
        className="cine-sonner-center"
        toastOptions={{ duration: 6000 }}
      />
      <AppRoutes />
    </>
  );
}
