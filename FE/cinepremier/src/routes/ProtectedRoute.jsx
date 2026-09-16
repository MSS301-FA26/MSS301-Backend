import React, { useEffect, useRef } from 'react';
import { motion } from 'motion/react';
import { Lock, Ticket, UserPlus } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores/useAuthStore';
import { useUiStore } from '../stores/useUiStore';

export default function ProtectedRoute({ children }) {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const isAuthReady = useAuthStore((state) => state.isAuthReady);
  const setAuthMode = useUiStore((state) => state.setAuthMode);
  const setShowOTP = useUiStore((state) => state.setShowOTP);
  const showToast = useUiStore((state) => state.showToast);
  const location = useLocation();
  const promptedRef = useRef(false);

  useEffect(() => {
    if (!isAuthReady) return;
    if (isLoggedIn) return;
    const redirectPath = `${location.pathname}${location.search}${location.hash}`;
    sessionStorage.setItem('cinepremier_post_login_redirect', redirectPath);
    setAuthMode('login');
    setShowOTP(true);
    if (!promptedRef.current) {
      promptedRef.current = true;
      showToast('Vui lòng đăng nhập hoặc đăng ký để tiếp tục đặt vé.', 6000);
    }
  }, [isAuthReady, isLoggedIn, location.hash, location.pathname, location.search, setAuthMode, setShowOTP, showToast]);

  if (!isAuthReady) {
    return (
      <section className="flex min-h-[72vh] items-center justify-center bg-black px-5 py-24 text-white">
        <div className="border border-white/10 bg-neutral-950 px-6 py-4 text-[10px] font-black uppercase tracking-[0.22em] text-neutral-400">
          Đang khôi phục phiên đăng nhập...
        </div>
      </section>
    );
  }

  if (isLoggedIn) return children;

  const openAuth = (mode) => {
    const redirectPath = `${location.pathname}${location.search}${location.hash}`;
    sessionStorage.setItem('cinepremier_post_login_redirect', redirectPath);
    setAuthMode(mode);
    setShowOTP(true);
  };

  return (
    <section className="relative min-h-[72vh] overflow-hidden bg-black px-5 py-24 text-white">
      <motion.div
        aria-hidden="true"
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.7, ease: 'easeOut' }}
        className="absolute inset-0 bg-[radial-gradient(circle_at_50%_20%,rgba(245,158,11,0.18),transparent_32%),radial-gradient(circle_at_20%_70%,rgba(255,255,255,0.08),transparent_26%),linear-gradient(180deg,#080808_0%,#000_100%)]"
      />
      <motion.div
        aria-hidden="true"
        initial={{ x: '-20%', opacity: 0 }}
        animate={{ x: '120%', opacity: [0, 0.35, 0] }}
        transition={{ duration: 2.2, ease: 'easeInOut', repeat: Infinity, repeatDelay: 1.2 }}
        className="absolute top-0 h-full w-1/3 -skew-x-12 bg-gradient-to-r from-transparent via-amber-200/15 to-transparent"
      />

      <motion.div
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: 'easeOut' }}
        className="relative mx-auto flex max-w-2xl flex-col items-center border border-amber-400/20 bg-neutral-950/70 p-7 text-center shadow-[0_24px_80px_rgba(0,0,0,0.55)] backdrop-blur-md md:p-10"
      >
        <div className="mb-5 flex h-14 w-14 items-center justify-center border border-amber-300/40 bg-amber-400/10 text-amber-200 shadow-[0_0_35px_rgba(245,158,11,0.18)]">
          <Lock className="h-6 w-6" />
        </div>
        <p className="text-[10px] font-black uppercase tracking-[0.28em] text-amber-300">Cần xác thực tài khoản</p>
        <h1 className="mt-3 font text-2xl font-black italic tracking-tight text-white md:text-4xl">
          Đăng nhập để tiếp tục đặt vé
        </h1>
        <p className="mt-4 max-w-xl text-sm leading-7 text-neutral-300">
          Mình đã giữ bạn ở đúng trang đặt vé này. Sau khi đăng nhập hoặc tạo tài khoản xong,
          hệ thống sẽ đưa bạn quay lại đây để chọn suất chiếu và ghế.
        </p>

        <div className="mt-8 grid w-full gap-3 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => openAuth('login')}
            className="group flex items-center justify-center gap-2 border border-white bg-white px-5 py-3 text-[11px] font-black uppercase tracking-[0.18em] text-black transition duration-300 hover:bg-amber-300 hover:border-amber-300"
          >
            <Ticket className="h-4 w-4 transition group-hover:scale-110" />
            Đăng nhập
          </button>
          <button
            type="button"
            onClick={() => openAuth('register')}
            className="group flex items-center justify-center gap-2 border border-amber-300/45 bg-amber-400/10 px-5 py-3 text-[11px] font-black uppercase tracking-[0.18em] text-amber-100 transition duration-300 hover:bg-amber-400 hover:text-black"
          >
            <UserPlus className="h-4 w-4 transition group-hover:scale-110" />
            Đăng ký mới
          </button>
        </div>
      </motion.div>
    </section>
  );
}
