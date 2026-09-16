import React from 'react';
import { Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import UserLayout from '../layouts/UserLayout';
import HomeView from '@/pages/user/HomePage';
import ExploreView from '@/pages/user/ExplorePage';
import DetailView from '@/pages/user/MovieDetailPage';
import BookingView from '@/pages/user/BookingPage';
import ProfileView from '@/pages/user/ProfilePage';
import MyOrdersPage from '@/pages/user/MyOrdersPage';
import ShowtimesPage from '@/pages/user/ShowtimesPage';
import WishlistView from '@/pages/user/WishlistPage';
import AdminDashboard from '@/pages/admin/AdminPage';
import PoliciesPage from '@/pages/user/PoliciesPage';
import PaymentCallbackPage from '@/pages/user/PaymentCallbackPage';
import ConcessionsPage from '@/pages/user/ConcessionsPage';
import StaffCheckInPage from '@/pages/staff/StaffCheckInPage';
import GooglePasswordSetupPage from '@/pages/auth/GooglePasswordSetupPage';
import AdminRoute from './AdminRoute';
import StaffRoute from './StaffRoute';
import ProtectedRoute from './ProtectedRoute';
import { getStoredAuth, hasBackendAdminAccess, hasBackendStaffAccess } from '../services/authService';
import { useMovies } from '../stores/useMovieStore';
import { useAuthStore } from '../stores/useAuthStore';
import { useUiStore } from '../stores/useUiStore';

function AppShell({ children }) {
  return <UserLayout>{children}</UserLayout>;
}

function HomeRoute() {
  const navigate = useNavigate();
  const { moviesList } = useMovies();
  const showToast = useUiStore((state) => state.showToast);

  const goToTab = (tab) => {
    const paths = { home: '/', explore: '/movies', 'my-tickets': '/tickets', wishlist: '/watchlist', profile: '/profile', policies: '/policies' };
    navigate(paths[tab] || '/');
  };

  const handleBookMovie = (movie) => {
    const isBookable = movie?.status === 'NOW_SHOWING' || (!movie?.status && !movie?.isUpcoming);
    if (!isBookable) {
      showToast('Phim sắp chiếu chưa mở bán vé.');
      navigate(`/movies/${movie.id}`);
      return;
    }
    navigate(`/movies/${movie.id}/book`);
  };

  return (
    <HomeView
      moviesList={moviesList}
      onSelectMovie={(id) => navigate(`/movies/${id}`)}
      onBookMovie={handleBookMovie}
      onTabChange={goToTab}
    />
  );
}

function AdminRouteView() {
  const navigate = useNavigate();
  const { section = 'overview' } = useParams();
  const showToast = useUiStore((state) => state.showToast);
  const currentRole = useAuthStore((state) => state.currentRole);
  const currentUser = useAuthStore((state) => state.currentUser);
  const {
    moviesList,
    setMoviesList,
    bookedTickets,
    setBookedTickets,
    fetchPublicFoodCatalog,
    publicCinema,
    fetchPublicCinema,
  } = useMovies();

  return (
    <AdminRoute>
      <AdminDashboard
        moviesList={moviesList}
        setMoviesList={setMoviesList}
        bookedTickets={bookedTickets}
        setBookedTickets={setBookedTickets}
        publicCinema={publicCinema}
        onCinemaChanged={fetchPublicCinema}
        onSelectMovie={(id) => navigate(`/movies/${id}`)}
        showToast={showToast}
        initialSection={section}
        onSectionChange={(nextSection) => navigate(`/admin/${nextSection}`)}
        onFoodCatalogChanged={() => fetchPublicFoodCatalog({ force: true })}
        isAdmin={currentRole === 'admin'}
        currentUser={currentUser}
      />
    </AdminRoute>
  );
}

export default function AppRoutes() {
  const currentUser = useAuthStore((state) => state.currentUser);
  const currentRole = useAuthStore((state) => state.currentRole);
  const mustSetupPassword = currentUser?.passwordChangeRequired;
  const { accessToken, user } = getStoredAuth();
  const isAdmin = currentRole === 'admin' || currentUser?.role === 'admin' || hasBackendAdminAccess(accessToken, user);
  const isStaff = currentRole === 'staff' || currentUser?.role === 'staff' || hasBackendStaffAccess(accessToken, user);

  return (
    <Routes>
      <Route path="/setup-password" element={<GooglePasswordSetupPage />} />
      {mustSetupPassword && <Route path="*" element={<Navigate to="/setup-password" replace />} />}
      <Route path="/payment-callback" element={<PaymentCallbackPage />} />
      <Route path="/staff" element={<AppShell><StaffRoute><StaffCheckInPage /></StaffRoute></AppShell>} />
      <Route
        path="/"
        element={
          isAdmin ? (
            <Navigate to="/admin/overview" replace />
          ) : isStaff ? (
            <Navigate to="/staff" replace />
          ) : (
            <AppShell><HomeRoute /></AppShell>
          )
        }
      />
      <Route path="/movies" element={<AppShell><ExploreView /></AppShell>} />
      <Route path="/showtimes" element={<AppShell><ShowtimesPage /></AppShell>} />
      <Route path="/movies/:id" element={<AppShell><DetailView /></AppShell>} />
      <Route path="/movies/:id/book" element={<AppShell><ProtectedRoute><BookingView /></ProtectedRoute></AppShell>} />
      <Route path="/concessions" element={<AppShell><ProtectedRoute><ConcessionsPage /></ProtectedRoute></AppShell>} />
      <Route path="/tickets" element={<AppShell><ProtectedRoute><MyOrdersPage /></ProtectedRoute></AppShell>} />
      <Route path="/watchlist" element={isAdmin ? <Navigate to="/admin/overview" replace /> : isStaff ? <Navigate to="/staff" replace /> : <AppShell><ProtectedRoute><WishlistView /></ProtectedRoute></AppShell>} />
      <Route path="/profile" element={<AppShell><ProtectedRoute><ProfileView /></ProtectedRoute></AppShell>} />
      <Route path="/policies" element={<AppShell><PoliciesPage /></AppShell>} />
      <Route path="/admin" element={<Navigate to="/admin/overview" replace />} />
      <Route path="/admin/:section" element={<AppShell><AdminRouteView /></AppShell>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
