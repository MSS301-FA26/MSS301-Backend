import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Lock, Loader2, Mail, ShieldCheck } from 'lucide-react';
import { clearAuthSession, getStoredAuth } from '../../services/authService';
import { userService } from '../../services/userService';
import { useAuthStore } from '../../stores/useAuthStore';
import { useUiStore } from '../../stores/useUiStore';
import { PASSWORD_VALIDATION_MESSAGE, isStrongPassword } from '../../utils/validation';

export default function GooglePasswordSetupPage() {
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
  const setCurrentRole = useAuthStore((state) => state.setCurrentRole);
  const setIsLoggedIn = useAuthStore((state) => state.setIsLoggedIn);
  const isAuthReady = useAuthStore((state) => state.isAuthReady);
  const showToast = useUiStore((state) => state.showToast);
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPasswords, setShowPasswords] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isAuthReady) return;
    if (!currentUser) {
      navigate('/', { replace: true });
      return;
    }
    if (!currentUser.passwordChangeRequired) {
      navigate('/', { replace: true });
    }
  }, [currentUser, isAuthReady, navigate]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!oldPassword || !newPassword || !confirmPassword) {
      showToast('Vui lòng nhập đầy đủ mật khẩu tạm, mật khẩu mới và xác nhận mật khẩu.');
      return;
    }
    if (!isStrongPassword(newPassword)) {
      showToast(PASSWORD_VALIDATION_MESSAGE);
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast('Mật khẩu xác nhận không khớp.');
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      restartGoogleLogin();
      return;
    }

    setIsSubmitting(true);
    try {
      await userService.changeMyPassword(accessToken, {
        oldPassword,
        newPassword,
        confirmPassword
      });

      const nextUser = {
        ...currentUser,
        passwordChangeRequired: false,
        passwordSetupProvider: null
      };
      localStorage.setItem('cinepremier_auth_user', JSON.stringify(nextUser));
      setCurrentUser(nextUser);
      setCurrentRole(nextUser.role || 'user');
      showToast('Đã thiết lập mật khẩu. Bạn có thể đăng nhập bằng Google hoặc email/mật khẩu.');
      navigate('/', { replace: true });
    } catch (error) {
      showToast(error.message || 'Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu tạm.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const restartGoogleLogin = () => {
    clearAuthSession();
    setIsLoggedIn(false);
    setCurrentUser(null);
    setCurrentRole('user');
    showToast('Vui lòng đăng nhập Google lại để hệ thống gửi mật khẩu tạm mới qua email.');
    navigate('/', { replace: true });
  };

  return (
    <main className="min-h-screen bg-black px-4 py-10 text-white">
      <div className="mx-auto flex min-h-[calc(100vh-5rem)] max-w-5xl items-center justify-center">
        <section className="w-full max-w-xl border border-amber-400/30 bg-[#050505] p-6 shadow-[0_24px_90px_rgba(245,158,11,0.12)] sm:p-8">
          <div className="flex items-start gap-4 border-b border-white/10 pb-6">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center border border-amber-400/40 bg-amber-400/10 text-amber-300">
              <ShieldCheck className="h-6 w-6" />
            </div>
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.25em] text-amber-300">
                Thiết lập mật khẩu
              </p>
              <h1 className="mt-2 font-serif text-3xl font-black italic tracking-wide text-white">
                Hoàn tất tài khoản Google
              </h1>
              <p className="mt-3 text-sm leading-6 text-neutral-400">
                Tài khoản Google mới cần đặt mật khẩu riêng trước khi vào giao diện chính.
                Sau bước này bạn có thể đăng nhập bằng cả Google và email/mật khẩu.
              </p>
            </div>
          </div>

          <div className="mt-6 border border-amber-500/30 bg-amber-500/10 p-4">
            <div className="flex items-start gap-3">
              <Mail className="mt-0.5 h-5 w-5 shrink-0 text-amber-300" />
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.18em] text-amber-300">
                  Kiểm tra email Google
                </p>
                <p className="mt-2 text-xs leading-6 text-amber-100/85">
                  Hệ thống đã gửi mật khẩu tạm đến email Google của bạn. Nhập mật khẩu tạm đó bên dưới rồi đặt mật khẩu mới.
                </p>
              </div>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="mt-6 space-y-4">
            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-[0.18em] text-neutral-500">Mật khẩu tạm</span>
              <div className="relative mt-2">
                <input
                  type={showPasswords ? 'text' : 'password'}
                  value={oldPassword}
                  onChange={(event) => setOldPassword(event.target.value)}
                  className="w-full border border-white/15 bg-black px-4 py-3 pr-11 text-sm text-white outline-none transition focus:border-white"
                  placeholder="Nhập mật khẩu tạm..."
                />
                <Lock className="absolute right-4 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-600" />
              </div>
            </label>

            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-[0.18em] text-neutral-500">Mật khẩu mới</span>
              <input
                type={showPasswords ? 'text' : 'password'}
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                className="mt-2 w-full border border-white/15 bg-black px-4 py-3 text-sm text-white outline-none transition focus:border-white"
                placeholder="Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt..."
              />
            </label>

            <label className="block">
              <span className="text-[10px] font-black uppercase tracking-[0.18em] text-neutral-500">Xác nhận mật khẩu mới</span>
              <input
                type={showPasswords ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                className="mt-2 w-full border border-white/15 bg-black px-4 py-3 text-sm text-white outline-none transition focus:border-white"
                placeholder="Nhập lại mật khẩu mới..."
              />
            </label>

            <button
              type="button"
              onClick={() => setShowPasswords((value) => !value)}
              className="inline-flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.18em] text-neutral-400 transition hover:text-white"
            >
              {showPasswords ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              {showPasswords ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            </button>

            <button
              type="button"
              onClick={restartGoogleLogin}
              className="block text-[10px] font-black uppercase tracking-[0.18em] text-amber-300 transition hover:text-white"
            >
              Đăng nhập Google lại để gửi mật khẩu tạm
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="flex w-full items-center justify-center gap-2 border border-white bg-white px-5 py-3 text-[10px] font-black uppercase tracking-[0.18em] text-black transition hover:bg-amber-300 hover:border-amber-300 disabled:cursor-wait disabled:opacity-60"
            >
              {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
              Đổi mật khẩu và vào hệ thống
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}
