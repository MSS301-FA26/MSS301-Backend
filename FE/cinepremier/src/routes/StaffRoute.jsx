import React from 'react';
import { Navigate } from 'react-router-dom';
import { getStoredAuth, hasBackendStaffAccess } from '../services/authService';
import { useAuthStore } from '../stores/useAuthStore';

export default function StaffRoute({ children }) {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const isAuthReady = useAuthStore((state) => state.isAuthReady);
  const currentRole = useAuthStore((state) => state.currentRole);
  const currentUser = useAuthStore((state) => state.currentUser);
  const { accessToken, user } = getStoredAuth();
  const hasStoredStaff = hasBackendStaffAccess(accessToken, user);
  const hasContextStaff = currentRole === 'staff' || currentUser?.role === 'staff' || hasBackendStaffAccess(accessToken, currentUser);

  if (!isAuthReady) return null;
  if (!isLoggedIn && !accessToken) return <Navigate to="/" replace />;
  if (!hasStoredStaff && !hasContextStaff) return <Navigate to="/" replace />;
  return children;
}
