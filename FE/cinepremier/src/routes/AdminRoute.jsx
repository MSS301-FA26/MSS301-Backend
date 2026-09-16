import React from 'react';
import { Navigate } from 'react-router-dom';
import { getStoredAuth, hasBackendAdminAccess } from '../services/authService';
import { useAuthStore } from '../stores/useAuthStore';

export default function AdminRoute({ children }) {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const isAuthReady = useAuthStore((state) => state.isAuthReady);
  const currentRole = useAuthStore((state) => state.currentRole);
  const currentUser = useAuthStore((state) => state.currentUser);
  const { accessToken, user } = getStoredAuth();
  const hasStoredAdmin = hasBackendAdminAccess(accessToken, user);
  const hasContextAdmin = currentRole === 'admin' || currentUser?.role === 'admin' || hasBackendAdminAccess(accessToken, currentUser);

  if (!isAuthReady) return null;
  if (!isLoggedIn && !accessToken) return <Navigate to="/" replace />;
  if (!hasStoredAdmin && !hasContextAdmin) return <Navigate to="/" replace />;
  return children;
}
