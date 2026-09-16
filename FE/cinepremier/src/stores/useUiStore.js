import { create } from 'zustand';

let toastTimerId = null;

const resolveNextValue = (nextValue, previousValue) => (
  typeof nextValue === 'function' ? nextValue(previousValue) : nextValue
);

const clearToastTimer = () => {
  if (toastTimerId) {
    window.clearInterval(toastTimerId);
    toastTimerId = null;
  }
};

const startToastTimer = (set, get) => {
  clearToastTimer();
  if (!get().toast) return;

  toastTimerId = window.setInterval(() => {
    set((state) => {
      if (!state.toast) return state;
      const nextRemaining = Math.max(state.toast.remainingMs - 250, 0);
      if (nextRemaining === 0) {
        clearToastTimer();
        return { toast: null };
      }
      return { toast: { ...state.toast, remainingMs: nextRemaining } };
    });
  }, 250);
};

export const useUiStore = create((set, get) => ({
  showOTP: false,
  authMode: 'login',
  showWatchlist: false,
  toast: null,
  adminSidebarCollapsed: false,
  toggleAdminSidebar: () => set((state) => ({ adminSidebarCollapsed: !state.adminSidebarCollapsed })),

  setShowOTP: (showOTP) => set((state) => ({
    showOTP: resolveNextValue(showOTP, state.showOTP)
  })),
  setAuthMode: (authMode) => set((state) => ({
    authMode: resolveNextValue(authMode, state.authMode) || 'login'
  })),
  setShowWatchlist: (showWatchlist) => set((state) => ({
    showWatchlist: resolveNextValue(showWatchlist, state.showWatchlist)
  })),
  setToast: (toast) => {
    set((state) => ({
      toast: resolveNextValue(toast, state.toast)
    }));
    if (get().toast) {
      startToastTimer(set, get);
    } else {
      clearToastTimer();
    }
  },
  showToast: (text, durationMs = 4500, action = null, tone = 'success') => {
    let nextDuration = durationMs;
    let nextAction = action;
    let nextTone = tone;

    if (typeof durationMs === 'string') {
      nextTone = durationMs;
      nextDuration = 4500;
      nextAction = null;
    }

    if (nextTone === 'error') nextTone = 'sad';

    if (!Number.isFinite(Number(nextDuration)) || Number(nextDuration) <= 0) {
      nextDuration = 4500;
    }

    set({
      toast: {
        id: Date.now(),
        text,
        durationMs: Number(nextDuration),
        remainingMs: Number(nextDuration),
        action: nextAction,
        tone: nextTone
      }
    });
    startToastTimer(set, get);
  }
}));
