import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Camera, CreditCard, Lock, LogOut, Settings, User,
  ChevronRight, Award, Flame, Eye, Film, Sparkles,
  ChevronDown, Check, Shield, Volume2, VolumeX, EyeOff,
  Save, Trash2, Sliders, Smartphone, Mail, Phone, Cake, BadgeCheck, Loader2, Ticket, ScanLine, Wallet, ArrowUpRight, RefreshCw
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { expireAuthSession, getStoredAuth, normalizeUser } from '../../services/authService';
import { userService } from '../../services/userService';
import { bookingService } from '../../services/bookingService';
import { loyaltyService } from '../../services/loyaltyService';
import { walletService } from '../../services/walletService';
import {
  MAX_NAME_LENGTH,
  NAME_VALIDATION_MESSAGE,
  PASSWORD_VALIDATION_MESSAGE,
  PHONE_VALIDATION_MESSAGE,
  isStrongPassword,
  isValidVietnamPhone,
  normalizeNameInput,
  normalizePhoneInput
} from '../../utils/validation';
import { useMovies } from '../../stores/useMovieStore';
import { useAuthStore } from '../../stores/useAuthStore';
import { useUiStore } from '../../stores/useUiStore';

const DEFAULT_LOYALTY_CONFIG = {
  expiryMonth: 12,
  expiryDay: 31,
  expiryTime: '23:59:59',
  lastExpiredAt: null,
  lastResetAt: null,
  lastResetSource: null
};

const formatDateTimeVi = (value) => {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
};

const buildLoyaltyExpiryDateTime = (config = DEFAULT_LOYALTY_CONFIG) => {
  const month = Math.max(1, Math.min(12, Number(config.expiryMonth || 12)));
  const maxDay = new Date(new Date().getFullYear(), month, 0).getDate();
  const day = Math.max(1, Math.min(maxDay, Number(config.expiryDay || 31)));
  const [hour = 23, minute = 59, second = 59] = String(config.expiryTime || '23:59:59').split(':').map(Number);
  const now = new Date();
  const candidate = new Date(now.getFullYear(), month - 1, day, hour || 0, minute || 0, second || 0);
  if (candidate.getTime() < now.getTime()) candidate.setFullYear(candidate.getFullYear() + 1);
  return candidate;
};

export default function ProfileView() {
  const navigate = useNavigate();
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const currentUser = useAuthStore((state) => state.currentUser);
  const currentRole = useAuthStore((state) => state.currentRole);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
  const setCurrentRole = useAuthStore((state) => state.setCurrentRole);
  const handleLogout = useAuthStore((state) => state.handleLogout);
  const showToast = useUiStore((state) => state.showToast);
  const { moviesList } = useMovies();
  const onLogout = () => handleLogout({ navigate, showToast });
  const onSelectMovie = (id) => navigate(`/movies/${id}`);
  const onTabChange = (tab) => { const paths = { home: '/', explore: '/movies', 'my-tickets': '/tickets' }; navigate(paths[tab] || '/'); };
  const onProfileUpdated = (user) => { setCurrentUser(user); setCurrentRole(user.role || 'user'); };
  const [profileImg, setProfileImg] = useState('https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10');
  const [name, setName] = useState(currentUser?.name || 'MINH HỒNG (VIP)');
  const [isEditingName, setIsEditingName] = useState(false);
  const [isSavingName, setIsSavingName] = useState(false);
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const avatarInputRef = React.useRef(null);

  // Advanced Interactive Settings States
  const [activePanel, setActivePanel] = useState(null); // null | 'profile' | 'payment' | 'security' | 'vibe'
  const [soundOn, setSoundOn] = useState(true);
  const [glowColor, setGlowColor] = useState('gold'); // 'gold' | 'neon' | 'ruby' | 'emerald'

  // Settings - Profile Form states
  const [profileNameInput, setProfileNameInput] = useState('Minh Hong');
  const [profileBioInput, setProfileBioInput] = useState('Chuyên gia phê bình Điện ảnh VIP Gold của CinePremier.');
  const [profileEmailInput, setProfileEmailInput] = useState('minhhong.vip@cinepremier.vn');
  const [profilePhoneInput, setProfilePhoneInput] = useState('');
  const [profileDateOfBirth, setProfileDateOfBirth] = useState('');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [recentBookings, setRecentBookings] = useState([]);
  const [loyaltyPoints, setLoyaltyPoints] = useState(0);
  const [loyaltyConfig, setLoyaltyConfig] = useState(DEFAULT_LOYALTY_CONFIG);
  const [wallet, setWallet] = useState(null);
  const [walletTxs, setWalletTxs] = useState([]);
  const [withdrawals, setWithdrawals] = useState([]);
  const [showWithdrawals, setShowWithdrawals] = useState(false);
  const [showWithdrawForm, setShowWithdrawForm] = useState(false);
  const [withdrawForm, setWithdrawForm] = useState({ amount: '', bankName: '', accountNumber: '', accountHolder: '', walletPhone: '' });
  const [withdrawLoading, setWithdrawLoading] = useState(false);
  const [withdrawErrors, setWithdrawErrors] = useState({});
  const [bankList, setBankList] = useState([]);
  const [bankSearch, setBankSearch] = useState('');
  const [showBankDropdown, setShowBankDropdown] = useState(false);
  const [isProfileLoading, setIsProfileLoading] = useState(false);

  // Settings - Payment card states
  const [cardNumber, setCardNumber] = useState('4611 •••• •••• 8899');
  const [cardHolder, setCardHolder] = useState('MINH HONG');
  const [cardExpiry, setCardExpiry] = useState('12/29');
  const [cardCvv, setCardCvv] = useState('***');
  const [isCardFlipped, setIsCardFlipped] = useState(false);
  const [cardType, setCardType] = useState('visa'); // visa | mastercard
  const [linkingSuccess, setLinkingSuccess] = useState(false);

  // Settings - Password & Security states
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOldPass, setShowOldPass] = useState(false);
  const [showNewPass, setShowNewPass] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [twoFactorAuth, setTwoFactorAuth] = useState(true);
  const [passwordNotification, setPasswordNotification] = useState(null); // { type, text }

  React.useEffect(() => {
    if (currentUser) {
      const displayName = currentUser.name || currentUser.fullName || currentUser.email;
      setName(displayName);
      setProfileNameInput(displayName);
      if (currentUser.email) {
        setProfileEmailInput(currentUser.email);
      }
      setProfilePhoneInput(normalizePhoneInput(currentUser.phone || ''));
      setProfileDateOfBirth(currentUser.birthYear ? String(currentUser.birthYear) : '');
      if (currentUser.avatarUrl) setProfileImg(currentUser.avatarUrl);
    }
  }, [currentUser]);
  React.useEffect(() => {
    if (!isLoggedIn) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;

    let cancelled = false;
    setIsProfileLoading(true);
    Promise.all([
      userService.getMyProfile(accessToken),
      bookingService.getMyBookings(accessToken),
      loyaltyService.getMyLoyalty(accessToken).catch(() => null),
      loyaltyService.getConfiguration(accessToken).catch(() => null)
    ])
      .then(([profile, bookings, loyalty, loyaltyConfiguration]) => {
        if (cancelled) return;
        const nextUser = normalizeUser(profile, profile.roles || currentUser?.roles || []);
        localStorage.setItem('cinepremier_auth_user', JSON.stringify(nextUser));
        onProfileUpdated(nextUser);
        const bookingList = Array.isArray(bookings) ? bookings : (bookings?.items ?? bookings?.content ?? []);
        setRecentBookings(bookingList);
        setLoyaltyPoints(Number(loyalty?.points ?? 0));
        setLoyaltyConfig({ ...DEFAULT_LOYALTY_CONFIG, ...(loyaltyConfiguration || {}) });
        // Fetch wallet + giao dịch + lịch sử rút tiền
        Promise.all([
          walletService.getWallet(accessToken),
          walletService.getTransactions(accessToken, { page: 0, size: 5 }),
          walletService.getMyWithdrawals(accessToken, { page: 0, size: 10 }),
        ]).then(([w, txData, wdData]) => {
          setWallet(w);
          setWalletTxs(txData?.content ?? txData?.items ?? []);
          setWithdrawals(wdData?.content ?? wdData?.items ?? []);
        }).catch(() => { });
      })
      .catch((error) => {
        if (!cancelled) showToast(error.message || 'Không thể tải dữ liệu hồ sơ từ hệ thống.');
      })
      .finally(() => {
        if (!cancelled) setIsProfileLoading(false);
      });

    return () => { cancelled = true; };
  }, [isLoggedIn]);

  const handleSaveProfile = async () => {
    const cleanName = profileNameInput.trim();
    const cleanPhone = profilePhoneInput.trim();

    if (!cleanName) {
      showToast("Vui lòng nhập tên hồ sơ.");
      return;
    }
    if (cleanName.length > MAX_NAME_LENGTH) {
      showToast(NAME_VALIDATION_MESSAGE);
      return;
    }
    if (!isValidVietnamPhone(cleanPhone)) {
      showToast(PHONE_VALIDATION_MESSAGE);
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
      expireAuthSession();
      return;
    }

    setIsSavingProfile(true);
    try {
      const updatedProfile = await userService.updateMyProfile(accessToken, {
        fullName: cleanName,
        phone: cleanPhone,
        birthYear: profileDateOfBirth ? parseInt(profileDateOfBirth) : null
      });
      const nextUser = normalizeUser(updatedProfile, updatedProfile.roles || currentUser?.roles || []);
      localStorage.setItem('cinepremier_auth_user', JSON.stringify(nextUser));
      setName(nextUser.name);
      onProfileUpdated(nextUser);
      playPing(880, 'sine', 0.25);
      setIsEditingName(false);
      showToast("Thông tin hồ sơ được cập nhật thành công!");

      setActivePanel(null);
    } catch (error) {
      showToast(error.message || "Không thể cập nhật hồ sơ.");
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleChangePassword = async () => {
    if (!oldPassword || !newPassword || !confirmPassword) {
      showToast("Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu.");
      return;
    }
    if (!isStrongPassword(newPassword)) {
      showToast(PASSWORD_VALIDATION_MESSAGE);
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast("Mật khẩu xác nhận không khớp.");
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
      expireAuthSession();
      return;
    }

    setIsChangingPassword(true);
    try {
      await userService.changeMyPassword(accessToken, {
        oldPassword,
        newPassword,
        confirmPassword
      });
      playPing(880, 'sine', 0.25);
      showToast("Đổi mật khẩu thành công.");
      setPasswordNotification({ type: 'success', text: 'Mật khẩu đã được cập nhật an toàn.' });
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setActivePanel(null);
    } catch (error) {
      showToast(error.message || "Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu hiện tại.");
      setPasswordNotification({ type: 'error', text: error.message || 'Không thể đổi mật khẩu.' });
    } finally {
      setIsChangingPassword(false);
    }
  };

  const playPing = (freq = 440, type = 'sine', duration = 0.1) => {
    if (!soundOn) return;
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = type;
      osc.frequency.setValueAtTime(freq, ctx.currentTime);
      gain.gain.setValueAtTime(0.04, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + duration);
    } catch (e) { }
  };

  const recentApiBookings = [...recentBookings]
    .sort((a, b) => new Date(b.paidAt || b.showtimeStart || 0) - new Date(a.paidAt || a.showtimeStart || 0))
    .slice(0, 3);
  const watchedCount = recentBookings.filter((booking) => booking.status === 'USED').length;
  const loyaltyResetScheduleLabel = formatDateTimeVi(buildLoyaltyExpiryDateTime(loyaltyConfig));
  const loyaltyAdminResetAt = loyaltyConfig.lastResetSource === 'ADMIN'
    ? formatDateTimeVi(loyaltyConfig.lastResetAt || loyaltyConfig.lastExpiredAt)
    : '';
  const memberSince = currentUser?.createdAt
    ? new Date(currentUser.createdAt).toLocaleDateString('vi-VN', { month: '2-digit', year: 'numeric' })
    : 'Đang cập nhật';
  const getBookingPoster = (booking) => (
    moviesList.find((movie) => movie.title === booking.movieTitle)?.posterUrl
    || 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10'
  );
  const bookingStatusMeta = {
    PAID: { label: 'Đã thanh toán', className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' },
    USED: { label: 'Đã sử dụng', className: 'border-neutral-600 bg-neutral-900 text-neutral-400' },
    HOLDING: { label: 'Đang giữ', className: 'border-amber-500/30 bg-amber-500/10 text-amber-300' },
    CANCELLED: { label: 'Đã hủy', className: 'border-rose-500/30 bg-rose-500/10 text-rose-300' },
    EXPIRED: { label: 'Hết hạn', className: 'border-rose-500/20 bg-rose-950/20 text-rose-400' },
    REFUND_REQUESTED: { label: 'Chờ hoàn tiền', className: 'border-sky-500/30 bg-sky-500/10 text-sky-300' },
    REFUNDED: { label: 'Đã hoàn tiền', className: 'border-purple-500/30 bg-purple-500/10 text-purple-300' }
  };

  // Radar Chart Calculations for 5-axis polygon:
  // Center is (100, 100), Radius is 60
  // Categories: SCI-FI (0 deg -> dynamic coordinates: point index 0), NOIR (72 deg), THRILLER (144 deg), DRAMA (216 deg), ACTION (288 deg)
  const categories = ['SCI-FI', 'NOIR', 'THRILLER', 'DRAMA', 'ACTION'];
  // Relative percentages representing personal metrics matching the screenshots
  const values = [0.85, 0.45, 0.65, 0.55, 0.80];

  const getCoordinates = (index, value) => {
    const angle = (Math.PI * 2 / 5) * index - Math.PI / 2; // Subtracting PI/2 to align the first axis strictly to top
    const radius = 60 * value;
    const x = 100 + radius * Math.cos(angle);
    const y = 100 + radius * Math.sin(angle);
    return `${x},${y}`;
  };

  const polyPoints = categories.map((_, i) => getCoordinates(i, values[i])).join(' ');
  const outerWebPoints = categories.map((_, i) => getCoordinates(i, 1.0)).join(' ');
  const innerWebPoints1 = categories.map((_, i) => getCoordinates(i, 0.75)).join(' ');
  const innerWebPoints2 = categories.map((_, i) => getCoordinates(i, 0.5)).join(' ');
  const innerWebPoints3 = categories.map((_, i) => getCoordinates(i, 0.25)).join(' ');

  // Get labels locations outside the radar
  const labelPositions = [
    { name: 'SCI-FI', x: 100, y: 22, textAnchor: 'middle' },
    { name: 'NOIR', x: 168, y: 76, textAnchor: 'start' },
    { name: 'THRILLER', x: 146, y: 154, textAnchor: 'start' },
    { name: 'DRAMA', x: 50, y: 154, textAnchor: 'end' },
    { name: 'ACTION', x: 28, y: 76, textAnchor: 'end' }
  ];

  const handleTriggerEditName = async () => {
    if (!isEditingName) {
      setName(currentUser?.name || currentUser?.fullName || '');
      setIsEditingName(true);
      return;
    }

    const cleanName = name.trim();
    if (!cleanName || cleanName.length > MAX_NAME_LENGTH) {
      showToast(!cleanName ? 'Vui lòng nhập tên hồ sơ.' : NAME_VALIDATION_MESSAGE);
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      expireAuthSession();
      return;
    }

    setIsSavingName(true);
    try {
      const updatedProfile = await userService.updateMyProfile(accessToken, {
        fullName: cleanName,
        phone: currentUser?.phone || profilePhoneInput || null,
        birthYear: currentUser?.birthYear || (profileDateOfBirth ? Number(profileDateOfBirth) : null)
      });
      const nextUser = normalizeUser(updatedProfile, updatedProfile.roles || currentUser?.roles || []);
      localStorage.setItem('cinepremier_auth_user', JSON.stringify(nextUser));
      onProfileUpdated(nextUser);
      setProfileNameInput(nextUser.name);
      setName(nextUser.name);
      setIsEditingName(false);
      showToast('Đã cập nhật tên hồ sơ.');
    } catch (error) {
      showToast(error.message || 'Không thể cập nhật tên hồ sơ.');
    } finally {
      setIsSavingName(false);
    }
  };

  const handleProfileImageChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      expireAuthSession();
      return;
    }

    setIsUploadingAvatar(true);
    try {
      const updatedProfile = await userService.uploadMyAvatar(accessToken, file);
      const nextUser = normalizeUser(updatedProfile, updatedProfile.roles || currentUser?.roles || []);
      localStorage.setItem('cinepremier_auth_user', JSON.stringify(nextUser));
      onProfileUpdated(nextUser);
      setProfileImg(nextUser.avatarUrl);
      showToast('Đã cập nhật ảnh đại diện.');
    } catch (error) {
      showToast(error.message || 'Không thể tải ảnh đại diện.');
    } finally {
      setIsUploadingAvatar(false);
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-12">

      {!isLoggedIn && (
        <div className="border border-white/10 bg-[#0E0E0E] p-6 text-center space-y-4 max-w-md mx-auto">
          <Award className="h-10 w-10 text-white mx-auto animate-pulse" />
          <h3 className="text-base font-serif font-light italic text-white uppercase tracking-wider">CinePremier VIP Club</h3>
          <p className="text-xs text-neutral-400 font-sans leading-relaxed">
            Vui lòng đăng nhập tài khoản Cinephile VIP của quý khách để tích lũy CinePoints, thăng hạng thành viên và kiểm tra toàn bộ lịch sử lịch rạp cá nhân.
          </p>
          <button
            onClick={onOpenOTP}
            className="border border-white bg-white hover:bg-black hover:text-white text-black px-6 py-2.5 text-xs font-sans font-bold tracking-widest uppercase transition-all"
          >
            Đăng nhập ngay
          </button>
        </div>
      )}

      {isLoggedIn && (
        <>
          {currentRole === 'admin' && (
            <div className="border border-amber-500/25 bg-[#090806] p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-[9px] font-black uppercase tracking-[0.25em] text-amber-500">Tài khoản quản trị viên</p>
                  <h2 className="mt-1 text-sm font-black uppercase tracking-[0.18em] text-white">Chọn khu vực làm việc</h2>
                </div>
                <div className="flex flex-col gap-2 sm:flex-row">
                  <button
                    type="button"
                    className="flex items-center justify-center gap-2 border border-white bg-white px-5 py-3 text-[10px] font-black uppercase tracking-widest text-black"
                  >
                    <User className="h-4 w-4" />
                    Thông tin cá nhân
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/admin/overview')}
                    className="flex items-center justify-center gap-2 border border-amber-500/50 bg-amber-500/10 px-5 py-3 text-[10px] font-black uppercase tracking-widest text-amber-300 transition hover:bg-amber-500 hover:text-black"
                  >
                    <Shield className="h-4 w-4" />
                    Quản trị viên
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* {currentRole === 'staff' && (
            <div className="border border-emerald-400/25 bg-[#050b08] p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-[9px] font-black uppercase tracking-[0.25em] text-emerald-400">Tài khoản nhân viên</p>
                  <h2 className="mt-1 text-sm font-black uppercase tracking-[0.18em] text-white">Khu vực làm việc tại quầy</h2>
                  <p className="mt-1 text-xs text-neutral-500">Check-in vé bằng QR và đổi trạng thái bắp nước khi hết hàng đột ngột.</p>
                </div>
                <div className="flex flex-col gap-2 sm:flex-row">
                  <button
                    type="button"
                    className="flex items-center justify-center gap-2 border border-white bg-white px-5 py-3 text-[10px] font-black uppercase tracking-widest text-black"
                  >
                    <User className="h-4 w-4" />
                    Thông tin cá nhân
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/staff')}
                    className="flex items-center justify-center gap-2 border border-emerald-400/50 bg-emerald-400/10 px-5 py-3 text-[10px] font-black uppercase tracking-widest text-emerald-300 transition hover:bg-emerald-400 hover:text-black"
                  >
                    <ScanLine className="h-4 w-4" />
                    Check-in vé
                  </button>
                </div>
              </div>
            </div>
          )} */}

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">

            {/* LEFT SECTION: MAIN PROFILE SUMMARY INFO & BOOKINGS */}
            <div className="lg:col-span-8 space-y-8">

              {/* HERO PROFILE BOX BACKGROUND */}
              <div className="relative border border-white/10 bg-[#070707] p-6 md:p-8 overflow-hidden">

                <div className="absolute right-0 top-0 text-[100px] font-bold text-neutral-900/10 italic select-none font-serif leading-none -mr-10 -mt-8 pointer-events-none uppercase">
                  VIP
                </div>

                <div className="flex flex-col md:flex-row md:items-start items-center gap-6 relative z-10">

                  {/* Profile circular avatar with editor button */}
                  <div className="relative group md:translate-y-[65px]">
                    <input
                      ref={avatarInputRef}
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={handleProfileImageChange}
                      className="hidden"
                    />
                    <div className="h-24 w-24 overflow-hidden rounded-md border border-white/15 bg-neutral-950 flex-shrink-0 shadow-2xl">
                      <img
                        src={profileImg}
                        alt={name}
                        className="h-full w-full object-cover group-hover:scale-105 transition duration-500"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                    <button
                      onClick={() => avatarInputRef.current?.click()}
                      disabled={isUploadingAvatar}
                      className="absolute -bottom-2 -right-2 bg-black border border-white/20 hover:border-white p-2 text-white shadow-xl hover:scale-110 transition shrink-0"
                      title="Tải ảnh đại diện từ máy tính"
                    >
                      {isUploadingAvatar ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Camera className="h-3.5 w-3.5" />}
                    </button>
                  </div>

                  {/* Name and metrics progress */}
                  <div className="flex-1 space-y-4 text-center md:text-left min-w-0">
                    <div className="space-y-1">
                      <div className="flex flex-col sm:flex-row sm:items-center gap-2 justify-center md:justify-start">
                        {isEditingName ? (
                          <input
                            type="text"
                            maxLength={MAX_NAME_LENGTH}
                            value={name}
                            onChange={(e) => setName(normalizeNameInput(e.target.value))}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter') handleTriggerEditName();
                              if (e.key === 'Escape') {
                                setName(currentUser?.name || currentUser?.fullName || '');
                                setIsEditingName(false);
                              }
                            }}
                            className="bg-black border border-amber-400/50 text-white text-2xl px-2 py-0.5 focus:outline-none focus:border-amber-300 font-sans max-w-[320px]"
                            autoFocus
                          />
                        ) : (
                          <h2 className="text-3xl font-sans font-bold text-white truncate max-w-[380px]">
                            {name}
                          </h2>
                        )}


                      </div>
                      <p className="text-xs text-neutral-500 uppercase tracking-widest font-sans flex items-center justify-center md:justify-start gap-1">
                        Thành viên từ {memberSince} • {currentUser?.status === 'ACTIVE' ? 'Tài khoản đang hoạt động' : currentUser?.status || 'Đang cập nhật'}
                      </p>
                    </div>

                    {/* Live personal information from /api/v1/users/me */}
                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                      {[
                        { icon: Mail, label: 'Email tài khoản', value: currentUser?.email || 'Đang cập nhật' },
                        { icon: Phone, label: 'Số điện thoại', value: currentUser?.phone || 'Chưa cập nhật' },
                        { icon: Cake, label: 'Năm sinh', value: currentUser?.birthYear || 'Chưa cập nhật' },
                        { icon: BadgeCheck, label: 'Xác thực', value: currentUser?.emailVerified ? 'Email đã xác thực' : 'Chưa xác thực email' }
                      ].map(({ icon: InfoIcon, label, value }) => (
                        <div key={label} className="flex min-w-0 items-center gap-3 border border-white/5 bg-black/50 px-4 py-3 text-left">
                          <span className="flex h-9 w-9 shrink-0 items-center justify-center border border-amber-500/20 bg-amber-500/5 text-amber-400">
                            <InfoIcon className="h-4 w-4" />
                          </span>
                          <span className="min-w-0">
                            <span className="block text-[9px] font-black uppercase tracking-[0.16em] text-neutral-600">{label}</span>
                            <span className="mt-1 block truncate text-xs font-bold text-neutral-300">{value}</span>
                          </span>
                        </div>
                      ))}
                    </div>



                    {/* Cinepoints & Watched points metrics cards mockup */}
                    <div className="grid grid-cols-2 gap-3 max-w-sm mx-auto md:mx-0 pointer-events-none">

                      <div className="border border-white/5 bg-black/60 p-4 flex flex-col justify-center items-start">
                        <span className="text-[10px] uppercase tracking-widest font-black flex items-center gap-1 bg-[linear-gradient(90deg,#ff3b7f,#ffb703,#38ef7d,#00c2ff,#8b5cf6)] bg-clip-text text-transparent">
                          <Flame className="h-3.5 w-3.5 text-pink-400 drop-shadow-[0_0_8px_rgba(244,114,182,0.75)]" /> CinePoints
                        </span>
                        <span className="text-2xl font-mono font-bold text-white mt-1">{loyaltyPoints.toLocaleString()}</span>
                      </div>

                      <div className="border border-white/5 bg-black/60 p-4 flex flex-col justify-center items-start">
                        <span className="text-[10px] text-neutral-500 uppercase tracking-widest font-bold flex items-center gap-1">
                          <Film className="h-3.5 w-3.5 text-neutral-500" /> Phim đã xem
                        </span>
                        <span className="text-2xl font-mono font-bold text-white mt-1">
                          {watchedCount}
                        </span>
                      </div>

                    </div>

                    {(loyaltyResetScheduleLabel || (loyaltyAdminResetAt && Number(loyaltyPoints || 0) === 0)) && (
                      <div className="max-w-sm mx-auto md:mx-0 border border-emerald-500/15 bg-emerald-950/10 px-4 py-3 text-left">
                        {loyaltyResetScheduleLabel && (
                          <p className="text-[12px] font-mono text-white/70">
                            Điểm sẽ được reset vào thời gian: {loyaltyResetScheduleLabel}
                          </p>
                        )}
                        {loyaltyAdminResetAt && Number(loyaltyPoints || 0) === 0 && (
                          <p className="mt-2 border-l border-amber-400/40 pl-2 text-[10px] font-bold leading-snug text-amber-300">
                            Quản trị viên đã reset điểm lúc {loyaltyAdminResetAt}.
                          </p>
                        )}
                      </div>
                    )}

                  </div>

                  {/* Edit Button right-aligned for PC size */}
                  <div className="hidden md:block">
                    <button
                      onClick={handleTriggerEditName}
                      disabled={isSavingName}
                      className="border border-white/10 hover:border-white/50 bg-black text-[10px] uppercase tracking-widest px-5 py-2.5 text-white font-sans transition disabled:opacity-50"
                    >
                      {isSavingName ? 'ĐANG LƯU...' : isEditingName ? 'LƯU TÊN' : 'SỬA TÊN'}
                    </button>
                    {isEditingName && (
                      <button
                        onClick={() => {
                          setName(currentUser?.name || currentUser?.fullName || '');
                          setIsEditingName(false);
                        }}
                        className="ml-2 border border-white/5 px-4 py-2.5 text-[10px] font-bold uppercase tracking-widest text-neutral-500 transition hover:text-white"
                      >
                        Hủy
                      </button>
                    )}
                  </div>

                </div>

              </div>

              {/* RECENT BOOKINGS WIDGET */}
              <div className="space-y-4">
                <div className="flex items-center justify-between border-b border-white/10 pb-2">
                  <span className="text-[10px] uppercase font-sans font-black tracking-widest text-neutral-400">
                    ĐẶT VÉ GẦN ĐÂY
                  </span>
                  <button
                    onClick={() => onTabChange('my-tickets')}
                    className="text-[10px] uppercase font-sans tracking-widest text-neutral-500 hover:text-white font-semibold transition"
                  >
                    TẤT CẢ
                  </button>
                </div>

                <div className="space-y-4">
                  {isProfileLoading && (
                    <div className="flex items-center justify-center gap-2 border border-white/10 bg-[#080808] py-10 text-[10px] font-bold uppercase tracking-widest text-neutral-500">
                      <Loader2 className="h-4 w-4 animate-spin text-amber-400" />
                      Đang tải đặt vé từ hệ thống
                    </div>
                  )}

                  {!isProfileLoading && recentApiBookings.map((booking) => {
                    const statusMeta = bookingStatusMeta[booking.status] || {
                      label: booking.status || 'Đang cập nhật',
                      className: 'border-white/10 bg-neutral-900 text-neutral-400'
                    };
                    const showtimeLabel = booking.showtimeStart
                      ? new Date(booking.showtimeStart).toLocaleString('vi-VN', {
                        hour: '2-digit',
                        minute: '2-digit',
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric'
                      })
                      : 'Chưa có lịch chiếu';
                    const seats = booking.seats?.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(', ') || 'Chưa chọn ghế';

                    return (
                      <div
                        key={booking.id}
                        className="border border-white/10 hover:border-white/20 bg-[#0A0A0A] p-4 flex gap-4 transition items-center"
                      >
                        <div className="h-16 w-12 overflow-hidden flex-shrink-0 bg-neutral-950 border border-white/5">
                          <img
                            src={getBookingPoster(booking)}
                            alt={booking.movieTitle}
                            className="h-full w-full object-cover"
                            referrerPolicy="no-referrer"
                          />
                        </div>

                        <div className="flex-1 min-w-0">
                          <h4 className="text-xs font-sans font-bold text-white truncate">
                            {booking.movieTitle}
                          </h4>
                          <p className="text-[10px] text-neutral-400 font-sans mt-0.5 truncate uppercase">
                            {showtimeLabel} • {booking.roomName} • {booking.cinemaName}
                          </p>

                          <div className="flex items-center gap-1.5 mt-1">
                            <span className={`border px-1.5 text-[8px] font-bold uppercase ${statusMeta.className}`}>
                              {statusMeta.label}
                            </span>
                            <span className="text-[9px] font-mono text-neutral-500 uppercase">
                              GHẾ: {seats}
                            </span>
                            <span className="hidden text-[9px] font-mono text-amber-500 sm:inline">
                              {Number(booking.totalAmount || 0).toLocaleString('vi-VN')}đ
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center gap-3">
                          {/* Barcode line mock matching screen */}
                          <div className="hidden sm:flex gap-0.5 items-center px-1 py-1 bg-white/5 h-8">
                            <div className="w-[1.5px] bg-neutral-500 h-6"></div>
                            <div className="w-[2.5px] bg-neutral-200 h-6"></div>
                            <div className="w-[1px] bg-neutral-500 h-6"></div>
                            <div className="w-[4px] bg-white h-6"></div>
                            <div className="w-[1.5px] bg-neutral-400 h-6"></div>
                            <div className="w-[2px] bg-neutral-500 h-6"></div>
                          </div>

                          <button
                            onClick={() => onTabChange('my-tickets')}
                            className="bg-white hover:bg-neutral-200 text-black px-4 py-2 text-[9px] font-sans tracking-widest font-bold uppercase transition"
                          >
                            Chi Tiết
                          </button>
                        </div>
                      </div>
                    );
                  })}

                  {!isProfileLoading && recentApiBookings.length === 0 && (
                    <div className="border border-dashed border-white/10 bg-[#080808] py-10 text-center">
                      <Ticket className="mx-auto h-6 w-6 text-neutral-700" />
                      <p className="mt-3 text-[10px] font-bold uppercase tracking-widest text-neutral-500">Bạn chưa có đặt vé nào</p>
                      <button onClick={() => onTabChange('explore')} className="mt-4 border border-white/15 px-4 py-2 text-[9px] font-black uppercase tracking-widest text-white transition hover:bg-white hover:text-black">
                        Khám phá phim
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* ── CINEWALLET CARD ─────────────────────────────────────────────── */}
              <div className="border border-amber-500/20 bg-gradient-to-br from-[#0d0d00] to-[#050500] p-5 space-y-4 mt-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-lg bg-amber-500/15 border border-amber-500/30 flex items-center justify-center">
                      <Wallet className="h-4 w-4 text-amber-400" />
                    </div>
                    <div>
                      <p className="text-[9px] font-black uppercase tracking-[0.14em] text-amber-500">CineWallet</p>
                      <p className="text-xl font-mono font-black text-white">
                        {wallet ? `${Number(wallet.balance).toLocaleString('vi-VN')}đ` : '—'}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      const next = !showWithdrawForm;
                      setShowWithdrawForm(next);
                      setShowBankDropdown(false);
                      if (next && bankList.length === 0) {
                        fetch('https://api.vietqr.io/v2/banks')
                          .then(r => r.json())
                          .then(d => setBankList(d.data || []))
                          .catch(() => { });
                      }
                    }}
                    className="flex items-center gap-1.5 border border-amber-500/30 bg-amber-500/10 px-3 py-1.5 text-[9px] font-black uppercase tracking-widest text-amber-400 hover:bg-amber-500/20 transition"
                  >
                    <ArrowUpRight className="h-3 w-3" /> Rút tiền
                  </button>
                </div>

                {walletTxs.filter((tx) => tx.type !== 'WITHDRAWAL_PAID' && Math.abs(Number(tx.amount || 0)) > 0).length > 0 && (
                  <div className="space-y-1.5">
                    <p className="text-[8px] font-black uppercase tracking-[0.14em] text-neutral-500">Giao dịch gần đây</p>
                    {walletTxs.filter((tx) => tx.type !== 'WITHDRAWAL_PAID' && Math.abs(Number(tx.amount || 0)) > 0).map((tx, i) => (
                      <div key={tx.id || i} className="flex items-center justify-between py-2 border-b border-white/5 last:border-0">
                        <div>
                          <p className="text-[10px] font-bold text-white">
                            {tx.type === 'REFUND_CREDIT' ? '+ Hoàn tiền'
                              : tx.type === 'WITHDRAWAL_HOLD' ? '- Yêu cầu rút'
                                : tx.type === 'WITHDRAWAL_REJECTED' ? '↩ Hoàn lại'
                                  : tx.type}
                          </p>
                          <p className="text-[9px] text-neutral-500">{tx.referenceCode}</p>
                        </div>
                        <span className={`text-[11px] font-black font-mono ${tx.type === 'REFUND_CREDIT' || tx.type === 'WITHDRAWAL_REJECTED' ? 'text-emerald-400' : 'text-amber-400'}`}>
                          {tx.type === 'REFUND_CREDIT' || tx.type === 'WITHDRAWAL_REJECTED' ? '+' : '-'}
                          {Math.abs(Number(tx.amount)).toLocaleString('vi-VN')}đ
                        </span>
                      </div>
                    ))}
                  </div>
                )}

                {withdrawals.length > 0 && (
                  <div>
                    <button
                      onClick={() => setShowWithdrawals(s => !s)}
                      className="flex w-full items-center justify-between border-t border-white/5 pt-3 text-left"
                    >
                      <span className="text-[8px] font-black uppercase tracking-[0.14em] text-neutral-500">
                        Lịch sử rút tiền ({withdrawals.length})
                      </span>
                      <ChevronDown className={`h-3.5 w-3.5 text-neutral-500 transition-transform ${showWithdrawals ? 'rotate-180' : ''}`} />
                    </button>
                    <AnimatePresence>
                      {showWithdrawals && (
                        <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                          <div className="mt-2 space-y-2 " >
                            {withdrawals.map((w, i) => {
                              const meta = w.status === 'PAID'
                                ? { label: 'Đã chuyển khoản', cls: 'border-emerald-500/30 bg-emerald-950/30 text-emerald-300' }
                                : w.status === 'REJECTED'
                                  ? { label: 'Từ chối · đã hoàn ví', cls: 'border-rose-500/30 bg-rose-950/30 text-rose-300' }
                                  : { label: 'Đang chờ duyệt', cls: 'border-amber-500/30 bg-amber-950/30 text-amber-300' };
                              const acc = w.accountNumber ? `•••• ${String(w.accountNumber).slice(-4)}` : '';
                              return (
                                <div key={w.id || i} className="border border-white/10 bg-black/40 p-3">
                                  <div className="flex items-start justify-between gap-2">
                                    <div>
                                      <p className="text-[12px]  text-white font-black font-mono text-white">
                                        {Number(w.amount).toLocaleString('vi-VN')}đ
                                      </p>
                                      <p className="text-[11px] text-neutral-500">
                                        {w.bankName} {acc} · {w.accountHolder}
                                      </p>
                                    </div>
                                    <span className={`shrink-0 border px-2 py-0.5 text-[8px]  font-black uppercase tracking-wider ${meta.cls}`}>
                                      {meta.label}
                                    </span>
                                  </div>
                                  <div className="mt-1.5 flex items-center justify-between text-[10px] text-neutral-600">
                                    <span>Gửi: {w.createdAt ? new Date(w.createdAt).toLocaleDateString('vi-VN') : '—'}</span>
                                    {w.processedAt && (
                                      <span className="text-[10px] text-white">Xử lý: {new Date(w.processedAt).toLocaleDateString('vi-VN')}{w.processedMethod ? ` · ${w.processedMethod}` : ''}</span>
                                    )}
                                  </div>
                                  {w.processedNote && (
                                    <p className="mt-1 text-[10px] italic text-neutral-500">"{w.processedNote}"</p>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                )}

                <AnimatePresence>
                  {showWithdrawForm && (
                    <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                      <div className="pt-3 border-t border-amber-500/15 space-y-3">
                        <p className="text-[9px] font-black uppercase tracking-[0.14em] text-amber-500/70">Yêu cầu rút tiền</p>

                        {/* Số tiền */}
                        <div>
                          <label className="block text-[10px] font-black text-neutral-400 uppercase tracking-[0.18em] mb-1">Số tiền (VND)</label>
                          <input
                            type="number"
                            value={withdrawForm.amount}
                            onChange={e => { setWithdrawForm(f => ({ ...f, amount: e.target.value })); setWithdrawErrors(err => ({ ...err, amount: '' })); }}
                            className={`w-full bg-black/40 border px-3 py-2 text-[11px] text-white outline-none transition ${withdrawErrors.amount ? 'border-rose-500/70' : 'border-white/10 focus:border-amber-500/50'}`}
                          />
                          {withdrawErrors.amount && <p className="mt-1 text-[9px] text-rose-400">{withdrawErrors.amount}</p>}
                        </div>

                        {/* Ngân hàng */}
                        <div className="relative">
                          <label className="block text-[10px] font-black text-neutral-400 uppercase tracking-[0.18em] mb-1">Ngân hàng</label>

                          {/* Trigger button */}
                          <button
                            type="button"
                            onClick={() => setShowBankDropdown(v => !v)}
                            className={`w-full flex items-center gap-2 bg-[#111] border px-3 py-2 text-[11px] text-left outline-none transition ${withdrawErrors.bankName ? 'border-rose-500/70' : 'border-white/10 hover:border-amber-500/40'}`}
                          >
                            {withdrawForm.bankName ? (
                              <>
                                {bankList.find(b => b.short_name === withdrawForm.bankName || b.shortName === withdrawForm.bankName)?.logo && (
                                  <img
                                    src={bankList.find(b => b.short_name === withdrawForm.bankName || b.shortName === withdrawForm.bankName)?.logo}
                                    alt=""
                                    className="h-5 w-8 object-contain bg-white/5 rounded"
                                  />
                                )}
                                <span className="text-white font-bold flex-1">{withdrawForm.bankName}</span>
                              </>
                            ) : (
                              <span className="text-neutral-500 flex-1">-- Chọn ngân hàng --</span>
                            )}
                            <ChevronDown className={`h-3.5 w-3.5 text-neutral-500 transition-transform ${showBankDropdown ? 'rotate-180' : ''}`} />
                          </button>

                          {/* Dropdown panel */}
                          {showBankDropdown && (
                            <div className="absolute z-50 left-0 right-0 mt-1 bg-[#0f0f0f] border border-white/10 shadow-2xl max-h-56 flex flex-col">
                              {/* Search */}
                              <div className="p-2 border-b border-white/5">
                                <input
                                  autoFocus
                                  type="text"
                                  placeholder="Tìm ngân hàng..."
                                  value={bankSearch}
                                  onChange={e => setBankSearch(e.target.value)}
                                  className="w-full bg-black/60 border border-white/10 px-2.5 py-1.5 text-[11px] text-white outline-none focus:border-amber-500/40 placeholder:text-neutral-600"
                                />
                              </div>
                              {/* List */}
                              <div className="overflow-y-auto flex-1">
                                {bankList
                                  .filter(b => {
                                    const q = bankSearch.toLowerCase();
                                    return !q || (b.short_name || b.shortName || '').toLowerCase().includes(q) || (b.name || '').toLowerCase().includes(q);
                                  })
                                  .map(b => {
                                    const sn = b.short_name || b.shortName;
                                    const selected = withdrawForm.bankName === sn;
                                    return (
                                      <button
                                        key={b.bin}
                                        type="button"
                                        onClick={() => {
                                          setWithdrawForm(f => ({ ...f, bankName: sn }));
                                          setWithdrawErrors(err => ({ ...err, bankName: '' }));
                                          setShowBankDropdown(false);
                                          setBankSearch('');
                                        }}
                                        className={`w-full flex items-center gap-2.5 px-3 py-2 text-left text-[11px] transition hover:bg-white/5 ${selected ? 'bg-amber-500/10' : ''}`}
                                      >
                                        {b.logo ? (
                                          <img src={b.logo} alt={sn} className="h-5 w-8 object-contain bg-white/5 rounded flex-shrink-0" />
                                        ) : (
                                          <div className="h-5 w-8 bg-white/5 rounded flex-shrink-0" />
                                        )}
                                        <span className={`font-bold flex-shrink-0 ${selected ? 'text-amber-400' : 'text-white'}`}>{sn}</span>
                                        <span className="text-neutral-500 truncate text-[10px]">{b.name}</span>
                                      </button>
                                    );
                                  })}
                                {bankList.filter(b => {
                                  const q = bankSearch.toLowerCase();
                                  return !q || (b.short_name || b.shortName || '').toLowerCase().includes(q) || (b.name || '').toLowerCase().includes(q);
                                }).length === 0 && (
                                    <p className="text-center text-[10px] text-neutral-600 py-4">Không tìm thấy ngân hàng</p>
                                  )}
                              </div>
                            </div>
                          )}
                          {withdrawErrors.bankName && <p className="mt-1 text-[9px] text-rose-400">{withdrawErrors.bankName}</p>}
                        </div>

                        {/* Số tài khoản */}
                        <div>
                          <label className="block text-[10px] font-black text-neutral-400 uppercase tracking-[0.18em] mb-1">Số tài khoản</label>
                          <input
                            type="text"
                            inputMode="numeric"
                            maxLength={15}
                            value={withdrawForm.accountNumber}
                            onChange={e => {
                              const digits = e.target.value.replace(/\D/g, '').slice(0, 15);
                              setWithdrawForm(f => ({ ...f, accountNumber: digits }));
                              setWithdrawErrors(err => ({ ...err, accountNumber: '' }));
                            }}
                            placeholder="8 – 15 chữ số"
                            className={`w-full bg-black/40 border px-3 py-2 text-[11px] text-white outline-none transition font-mono tracking-widest ${withdrawErrors.accountNumber ? 'border-rose-500/70' : 'border-white/10 focus:border-amber-500/50'}`}
                          />
                          {withdrawErrors.accountNumber && <p className="mt-1 text-[9px] text-rose-400">{withdrawErrors.accountNumber}</p>}
                        </div>

                        {/* Chủ tài khoản */}
                        <div>
                          <label className="block text-[10px] font-black text-neutral-400 uppercase tracking-[0.18em] mb-1">Chủ tài khoản</label>
                          <input
                            type="text"
                            value={withdrawForm.accountHolder}
                            onChange={e => {
                              // Strip digits and special chars, auto-uppercase
                              const cleaned = e.target.value.replace(/[^a-zA-Z\s\u00C0-\u024F\u1E00-\u1EFF]/g, '').toUpperCase();
                              setWithdrawForm(f => ({ ...f, accountHolder: cleaned }));
                              setWithdrawErrors(err => ({ ...err, accountHolder: '' }));
                            }}
                            placeholder="VD: NGUYEN VAN A"
                            className={`w-full bg-black/40 border px-3 py-2 text-[11px] text-white outline-none transition uppercase tracking-wider ${withdrawErrors.accountHolder ? 'border-rose-500/70' : 'border-white/10 focus:border-amber-500/50'}`}
                          />
                          {withdrawErrors.accountHolder && <p className="mt-1 text-[9px] text-rose-400">{withdrawErrors.accountHolder}</p>}
                        </div>

                        {withdrawErrors.general && (
                          <p className="text-[9px] text-rose-400 bg-rose-500/10 border border-rose-500/20 px-3 py-2">{withdrawErrors.general}</p>
                        )}
                        <button
                          disabled={withdrawLoading}
                          onClick={async () => {
                            const errs = {};
                            if (!withdrawForm.amount || Number(withdrawForm.amount) < 10000) errs.amount = 'Số tiền tối thiểu 10,000đ';
                            if (!withdrawForm.bankName) errs.bankName = 'Vui lòng chọn ngân hàng';
                            if (!withdrawForm.accountNumber || withdrawForm.accountNumber.length < 8) errs.accountNumber = 'Số tài khoản phải có tối thiểu 8 chữ số';
                            else if (withdrawForm.accountNumber.length > 15) errs.accountNumber = 'Số tài khoản không quá 15 chữ số';
                            if (!withdrawForm.accountHolder.trim()) errs.accountHolder = 'Vui lòng nhập tên chủ tài khoản';
                            else if (withdrawForm.accountHolder.trim().length < 2) errs.accountHolder = 'Tên chủ tài khoản không hợp lệ';
                            if (Object.keys(errs).length) { setWithdrawErrors(errs); return; }
                            const { accessToken } = getStoredAuth();
                            if (!accessToken) return;
                            setWithdrawLoading(true);
                            setWithdrawErrors({});
                            try {
                              await walletService.createWithdrawal(accessToken, {
                                amount: Number(withdrawForm.amount),
                                bankName: withdrawForm.bankName,
                                accountNumber: withdrawForm.accountNumber,
                                accountHolder: withdrawForm.accountHolder,
                                walletPhone: withdrawForm.walletPhone || null,
                              });
                              const [w, txData, wdData] = await Promise.all([
                                walletService.getWallet(accessToken),
                                walletService.getTransactions(accessToken, { page: 0, size: 5 }),
                                walletService.getMyWithdrawals(accessToken, { page: 0, size: 10 }),
                              ]);
                              setWallet(w);
                              setWalletTxs(txData?.content ?? txData?.items ?? []);
                              setWithdrawals(wdData?.content ?? wdData?.items ?? []);
                              setShowWithdrawForm(false);
                              setShowWithdrawals(true);
                              setWithdrawForm({ amount: '', bankName: '', accountNumber: '', accountHolder: '', walletPhone: '' });
                            } catch (e) {
                              setWithdrawErrors({ general: e.message || 'Không thể tạo yêu cầu rút tiền. Vui lòng thử lại.' });
                            } finally { setWithdrawLoading(false); }
                          }}
                          className="w-full bg-amber-500 text-black text-[10px] font-black uppercase tracking-widest py-2.5 hover:bg-amber-400 transition disabled:opacity-50 flex items-center justify-center gap-2"
                        >
                          {withdrawLoading ? <RefreshCw className="h-3 w-3 animate-spin" /> : <ArrowUpRight className="h-3 w-3" />}
                          Gửi yêu cầu rút tiền
                        </button>
                        <p className="text-[9px] text-neutral-600 leading-relaxed">Staff sẽ xem xét và chuyển khoản thủ công trong vòng 1-3 ngày làm việc.</p>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </div>

            {/* RIGHT SECTION: PERSONALIZED PREFERENCES & SETTINGS */}
            <div className="lg:col-span-4 space-y-6">



              {/* SETTINGS OPTIONS WIDGETS SECTION */}
              <div className={`border bg-gradient-to-b from-[#0a0a0a] to-[#040404] p-5.5 space-y-5 font-sans transition-all duration-300 relative overflow-hidden rounded-none ${glowColor === 'gold' ? 'border-amber-500/20 shadow-[0_0_25px_rgba(245,158,11,0.08)]' :
                glowColor === 'neon' ? 'border-cyan-500/20 shadow-[0_0_25px_rgba(6,182,212,0.08)]' :
                  glowColor === 'ruby' ? 'border-rose-500/20 shadow-[0_0_25px_rgba(244,63,94,0.08)]' :
                    'border-emerald-500/20 shadow-[0_0_25px_rgba(16,185,129,0.08)]'
                }`} id="settings-interactive-box">

                {/* Audio controller and Glow selector Header */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/5 pb-3">
                  <div className="flex items-center space-x-2 text-neutral-400">
                    <Settings className={`h-4.5 w-4.5 animate-spin-slow ${glowColor === 'gold' ? 'text-amber-400' :
                      glowColor === 'neon' ? 'text-cyan-400' :
                        glowColor === 'ruby' ? 'text-rose-400' : 'text-emerald-400'
                      }`} />
                    <span className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-300">TRUNG TÂM KIỂM SOÁT VIP</span>
                  </div>

                  {/* Micro Controllers */}
                  <div className="flex items-center space-x-3 self-end sm:self-auto">
                    {/* Sound Toggle */}
                    <button
                      onClick={() => {
                        const ns = !soundOn;
                        setSoundOn(ns);
                        if (ns) {
                          // test ping
                          try {
                            const AudioCtx = window.AudioContext || window.webkitAudioContext;
                            const ctx = new AudioCtx();
                            const osc = ctx.createOscillator();
                            const gain = ctx.createGain();
                            osc.frequency.value = 600;
                            gain.gain.setValueAtTime(0.03, ctx.currentTime);
                            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15);
                            osc.connect(gain);
                            gain.connect(ctx.destination);
                            osc.start();
                            osc.stop(ctx.currentTime + 0.15);
                          } catch (e) { }
                        }
                      }}
                      className="p-1.5 border border-neutral-900 bg-black text-neutral-400 hover:text-white hover:border-neutral-800 transition"
                      title={soundOn ? "Tắt âm phản hồi" : "Bật âm phản hồi"}
                    >
                      {soundOn ? <Volume2 className="h-3.5 w-3.5 text-amber-500 animate-pulse" /> : <VolumeX className="h-3.5 w-3.5 text-neutral-600" />}
                    </button>

                    {/* Vibe Theme Selector with dynamic frequencies on hover/click */}
                    <div className="flex items-center space-x-1 border border-neutral-900 bg-black p-0.5" id="theme-accent-dots">
                      {[
                        { id: 'gold', class: 'bg-amber-400', freq: 554.37 },
                        { id: 'neon', class: 'bg-cyan-400', freq: 659.25 },
                        { id: 'ruby', class: 'bg-rose-500', freq: 783.99 },
                        { id: 'emerald', class: 'bg-emerald-400', freq: 880.00 }
                      ].map((dot) => (
                        <button
                          key={dot.id}
                          onClick={() => {
                            setGlowColor(dot.id);
                            playPing(dot.freq, 'sine', 0.2);
                          }}
                          className={`h-3 w-3 rounded-full transition-all duration-300 ${dot.class} ${glowColor === dot.id ? 'scale-125 ring-2 ring-white/60' : 'opacity-40 hover:opacity-100'
                            }`}
                          title={`Chủ đề ${dot.id.toUpperCase()}`}
                        />
                      ))}
                    </div>

                  </div>
                </div>

                {/* Options Accumulators */}
                <div className="space-y-2">

                  {/* 1. CHỈNH SỬA HỒ SƠ */}
                  <div className="border border-white/5 bg-black/40 overflow-hidden text-neutral-400">
                    <button
                      onClick={() => {
                        playPing(activePanel === 'profile' ? 380 : 450, 'sine', 0.1);
                        setActivePanel(activePanel === 'profile' ? null : 'profile');
                      }}
                      className={`w-full flex items-center justify-between p-3.5 text-xs text-neutral-400 hover:text-white transition duration-200 text-left ${activePanel === 'profile' ? 'bg-neutral-950/60 pb-2 text-white border-b border-white/5' : ''
                        }`}
                    >
                      <span className="flex items-center gap-2.5 font-bold uppercase tracking-wider">
                        <User className={`h-4 w-4 ${activePanel === 'profile' ? 'text-amber-400' : 'text-neutral-500'}`} />
                        Chỉnh sửa hồ sơ VIP
                      </span>
                      <ChevronDown className={`h-3.5 w-3.5 text-neutral-500 transition-transform duration-300 ${activePanel === 'profile' ? 'rotate-180 text-white' : ''
                        }`} />
                    </button>

                    <AnimatePresence initial={false}>
                      {activePanel === 'profile' && (
                        <motion.div
                          initial={{ height: 0, opacity: 0 }}
                          animate={{ height: 'auto', opacity: 1 }}
                          exit={{ height: 0, opacity: 0 }}
                          className="overflow-hidden"
                        >
                          <div className="p-4 space-y-4 text-xs">
                            {/* Profile settings fields form */}
                            <div className="space-y-3.5">
                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] font-black text-neutral-400">Tên Thượng Khách</label>
                                <input
                                  type="text"
                                  maxLength={MAX_NAME_LENGTH}
                                  value={profileNameInput}
                                  onChange={(e) => setProfileNameInput(normalizeNameInput(e.target.value))}
                                  className="w-full bg-black border border-neutral-850 focus:border-amber-400 text-white p-2 text-xs focus:outline-none focus:ring-0 rounded-none font-bold"
                                />
                                <p className="text-[9px] text-neutral-600 font-mono text-right">{profileNameInput.length}/{MAX_NAME_LENGTH}</p>
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] font-black text-neutral-400">Tiểu sử Điện ảnh</label>
                                <textarea
                                  value={profileBioInput}
                                  onChange={(e) => setProfileBioInput(e.target.value)}
                                  rows={2}
                                  className="w-full bg-black border border-neutral-850 focus:border-amber-400 text-white p-2 text-xs focus:outline-none focus:ring-0 rounded-none leading-relaxed"
                                />
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] font-black text-neutral-400">Địa chỉ Email Liên hệ</label>
                                <input
                                  type="email"
                                  value={profileEmailInput}
                                  disabled
                                  className="w-full bg-black border border-neutral-850 text-neutral-500 p-2 text-xs focus:outline-none focus:ring-0 rounded-none font-mono cursor-not-allowed"
                                />
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] font-black text-neutral-400">Số điện thoại</label>
                                <input
                                  type="tel"
                                  inputMode="numeric"
                                  pattern="(03|05|08|09)[0-9]{8}"
                                  maxLength={10}
                                  placeholder="0912345678"
                                  value={profilePhoneInput}
                                  onChange={(e) => setProfilePhoneInput(normalizePhoneInput(e.target.value))}
                                  className="w-full bg-black border border-neutral-850 focus:border-amber-400 text-white p-2 text-xs focus:outline-none focus:ring-0 rounded-none font-mono"
                                />
                                <p className="text-[9px] text-neutral-600 font-mono">10 số, bắt đầu 03/05/08/09</p>
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] font-black text-neutral-400">Năm sinh</label>
                                <input
                                  type="number"
                                  min={1900}
                                  max={new Date().getFullYear() - 5}
                                  placeholder="VD: 2000"
                                  value={profileDateOfBirth}
                                  onChange={(e) => setProfileDateOfBirth(e.target.value)}
                                  className="w-full bg-black border border-neutral-850 focus:border-amber-400 text-white p-2 text-xs focus:outline-none focus:ring-0 rounded-none font-mono"
                                />
                                <p className="text-[9px] text-neutral-600 font-mono">Dùng để xác minh độ tuổi xem phim</p>
                              </div>
                            </div>

                            <button
                              onClick={handleSaveProfile}
                              disabled={isSavingProfile}
                              className="w-full py-2.5 bg-white text-black font-sans font-black tracking-widest uppercase text-[10px] hover:bg-neutral-250 transition flex items-center justify-center gap-1.5"
                            >
                              {isSavingProfile ? (
                                <span className="h-3.5 w-3.5 border-2 border-black border-t-transparent animate-spin rounded-full inline-block"></span>
                              ) : (
                                <><Save className="h-3.5 w-3.5" /> LƯU THAY ĐỔI HỒ SƠ</>
                              )}
                            </button>
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>

                  {/* 2. PHƯƠNG THỨC THANH TOÁN (Visa / Master Card VIP golden linkage) */}
                  <div className="border border-white/5 bg-black/40 overflow-hidden text-neutral-400">
                    <button
                      onClick={() => {
                        playPing(activePanel === 'payment' ? 380 : 450, 'sine', 0.1);
                        setActivePanel(activePanel === 'payment' ? null : 'payment');
                        setLinkingSuccess(false);
                      }}
                      className={`w-full flex items-center justify-between p-3.5 text-xs text-neutral-400 hover:text-white transition duration-200 text-left ${activePanel === 'payment' ? 'bg-neutral-950/60 pb-2 text-white border-b border-white/5' : ''
                        }`}
                    >
                      <span className="flex items-center gap-2.5 font-bold uppercase tracking-wider">
                        <CreditCard className={`h-4 w-4 ${activePanel === 'payment' ? 'text-amber-400' : 'text-neutral-500'}`} />
                        Phương thức thanh toán VIP
                      </span>
                      <ChevronDown className={`h-3.5 w-3.5 text-neutral-500 transition-transform duration-300 ${activePanel === 'payment' ? 'rotate-180 text-white' : ''
                        }`} />
                    </button>

                    <AnimatePresence initial={false}>
                      {activePanel === 'payment' && (
                        <motion.div
                          initial={{ height: 0, opacity: 0 }}
                          animate={{ height: 'auto', opacity: 1 }}
                          exit={{ height: 0, opacity: 0 }}
                          className="overflow-hidden"
                        >
                          <div className="p-4 space-y-4 text-xs">

                            {/* Interactive Credit Card Widget Representation */}
                            <div
                              className="relative w-full aspect-[1.58/1] rounded-lg p-5 overflow-hidden text-white flex flex-col justify-between cursor-pointer border shadow-2xl transition-all duration-500 hover:scale-102"
                              style={{
                                background: glowColor === 'gold' ? 'linear-gradient(135deg, #161208 0%, #0e0a03 50%, #1e1505 100%)' :
                                  glowColor === 'neon' ? 'linear-gradient(135deg, #091a1e 0%, #040d10 50%, #0a252d 100%)' :
                                    glowColor === 'ruby' ? 'linear-gradient(135deg, #1e090c 0%, #0e0304 50%, #2e0d13 100%)' :
                                      'linear-gradient(135deg, #091e11 0%, #030d06 50%, #112d1b 100%)',
                                borderColor: glowColor === 'gold' ? 'rgba(234, 179, 8, 0.4)' :
                                  glowColor === 'neon' ? 'rgba(6, 182, 212, 0.4)' :
                                    glowColor === 'ruby' ? 'rgba(244, 63, 94, 0.4)' :
                                      'rgba(16, 185, 129, 0.4)'
                              }}
                              onClick={() => {
                                playPing(600, 'sine', 0.15);
                                setIsCardFlipped(!isCardFlipped);
                              }}
                              title="Nhấp để xoay lật mặt thẻ bảo mật"
                            >
                              <div className="absolute top-0 right-0 w-32 h-32 bg-white/2 rounded-full -mr-10 -mt-10 pointer-events-none blur-lg"></div>

                              {/* Card state wrapper with nice motion flip */}
                              <AnimatePresence mode="wait">
                                {!isCardFlipped ? (
                                  <motion.div
                                    key="front"
                                    initial={{ opacity: 0, rotateY: 90 }}
                                    animate={{ opacity: 1, rotateY: 0 }}
                                    exit={{ opacity: 0, rotateY: -90 }}
                                    className="h-full flex flex-col justify-between"
                                  >
                                    {/* Top chip and logo */}
                                    <div className="flex justify-between items-start">
                                      <div className="space-y-1">
                                        <span className="text-[8px] tracking-[0.2em] font-black uppercase text-neutral-400">
                                          CINEPREMIER
                                        </span>
                                        {/* Golden Chip */}
                                        <div className="h-5 w-7 bg-amber-500/30 border border-amber-300/30 rounded-[3px] shadow"></div>
                                      </div>
                                      <span className="text-sm font-serif italic font-black uppercase tracking-wider">
                                        {cardType === 'visa' ? 'VISA PREMIUM' : 'MASTER CARD'}
                                      </span>
                                    </div>

                                    {/* Middle number with secure layout */}
                                    <div className="text-base sm:text-lg font-mono tracking-widest font-bold py-1 select-all text-center">
                                      {cardNumber || '•••• •••• •••• ••••'}
                                    </div>

                                    {/* Lowerholder info */}
                                    <div className="flex justify-between items-end text-[9px] font-mono">
                                      <div>
                                        <span className="block text-[7px] text-neutral-500 font-extrabold">CHỦ THẺ</span>
                                        <span className="uppercase text-neutral-300 font-black tracking-wide">{cardHolder || 'MINH HONG'}</span>
                                      </div>
                                      <div className="text-right">
                                        <span className="block text-[7px] text-neutral-500 font-extrabold">HẾT HẠN</span>
                                        <span className="text-neutral-300 font-black">{cardExpiry || '12/29'}</span>
                                      </div>
                                    </div>
                                  </motion.div>
                                ) : (
                                  <motion.div
                                    key="back"
                                    initial={{ opacity: 0, rotateY: -90 }}
                                    animate={{ opacity: 1, rotateY: 0 }}
                                    exit={{ opacity: 0, rotateY: 90 }}
                                    className="h-full flex flex-col justify-between py-2"
                                  >
                                    {/* Back design magnetic strip */}
                                    <div className="w-full h-7 bg-neutral-900 -mx-5 mt-1 border-t border-b border-black"></div>

                                    {/* Sign strip */}
                                    <div className="flex items-center gap-3">
                                      <div className="flex-1 h-5 bg-neutral-800 text-[8px] font-mono text-neutral-400 flex items-center px-2 select-all font-black">
                                        Mã bảo an quốc tế:
                                      </div>
                                      <div className="bg-amber-100 text-black px-2.5 py-0.5 text-[10px] font-mono font-bold tracking-widest rounded-sm">
                                        {cardCvv || '***'}
                                      </div>
                                    </div>

                                    <div className="text-center text-[7px] text-neutral-500 tracking-wider">
                                      THÀNH VIÊN ĐẶC QUYỀN ĐỒNG THỂ • CINEPREMIER 2026
                                    </div>
                                  </motion.div>
                                )}
                              </AnimatePresence>
                            </div>

                            <span className="block text-[8px] text-zinc-500 text-center font-mono uppercase tracking-widest">
                              💡 Nhấp vào hình ảnh thẻ ở trên để xoay lật mặt bảo mật
                            </span>

                            {/* Inputs with real dynamic visualization */}
                            <div className="grid grid-cols-2 gap-2.5 pt-1">
                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black">Tên chủ thẻ</label>
                                <input
                                  type="text"
                                  placeholder="MINH HONG"
                                  value={cardHolder}
                                  onChange={(e) => setCardHolder(e.target.value.toUpperCase())}
                                  className="w-full bg-black border border-neutral-850 p-2 text-[11px] text-white focus:outline-none focus:border-white uppercase font-bold"
                                />
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black">Số Thẻ Di Động</label>
                                <input
                                  type="text"
                                  placeholder="4611 1234 5678 8899"
                                  value={cardNumber}
                                  onChange={(e) => setCardNumber(e.target.value)}
                                  className="w-full bg-black border border-neutral-850 p-2 text-[11px] text-white focus:outline-none focus:border-white font-mono"
                                />
                              </div>
                            </div>

                            <div className="grid grid-cols-3 gap-2.5">
                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black">Hạn dùng</label>
                                <input
                                  type="text"
                                  placeholder="12/29"
                                  maxLength={5}
                                  value={cardExpiry}
                                  onChange={(e) => setCardExpiry(e.target.value)}
                                  className="w-full bg-black border border-neutral-850 p-2 text-[11px] text-white focus:outline-none focus:border-white font-mono text-center"
                                />
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black">Mã CVV</label>
                                <input
                                  type="password"
                                  placeholder="***"
                                  maxLength={3}
                                  value={cardCvv}
                                  onChange={(e) => setCardCvv(e.target.value)}
                                  onFocus={() => { playPing(400, 'sine', 0.1); setIsCardFlipped(true); }}
                                  onBlur={() => setIsCardFlipped(false)}
                                  className="w-full bg-black border border-neutral-850 p-2 text-[11px] text-white focus:outline-none focus:border-white font-mono text-center tracking-widest"
                                />
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black">Loại thẻ</label>
                                <select
                                  value={cardType}
                                  onChange={(e) => { playPing(480, 'sine', 0.05); setCardType(e.target.value); }}
                                  className="w-full bg-black border border-neutral-850 p-2 text-[11px] text-white focus:outline-none focus:border-white"
                                >
                                  <option value="visa">VISA VIP</option>
                                  <option value="mastercard">MASTER</option>
                                </select>
                              </div>
                            </div>

                            <button
                              onClick={() => {
                                playPing(987.77, 'sine', 0.3);
                                setLinkingSuccess(true);
                                showToast("Mã hóa liên kết thẻ tín dụng Thượng hạng thành công!");
                                setActivePanel(null);
                              }}
                              className="w-full py-2.5 bg-neutral-900 border border-white/10 text-white font-sans uppercase text-[10px] hover:border-white hover:bg-neutral-950 transition tracking-widest font-black"
                            >
                              ✓ XÁC THỰC & LIÊN KẾT AN TOÀN
                            </button>

                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>

                  {/* 3. MẬT KHẨU & BẢO MẬT */}
                  <div className="border border-white/5 bg-black/40 overflow-hidden text-neutral-400">
                    <button
                      onClick={() => {
                        playPing(activePanel === 'security' ? 380 : 450, 'sine', 0.1);
                        setActivePanel(activePanel === 'security' ? null : 'security');
                      }}
                      className={`w-full flex items-center justify-between p-3.5 text-xs text-neutral-400 hover:text-white transition duration-200 text-left ${activePanel === 'security' ? 'bg-neutral-950/60 pb-2 text-white border-b border-white/5' : ''
                        }`}
                    >
                      <span className="flex items-center gap-2.5 font-bold uppercase tracking-wider">
                        <Lock className={`h-4 w-4 ${activePanel === 'security' ? 'text-amber-400' : 'text-neutral-500'}`} />
                        Mật khẩu & Bảo an
                      </span>
                      <ChevronDown className={`h-3.5 w-3.5 text-neutral-500 transition-transform duration-300 ${activePanel === 'security' ? 'rotate-180 text-white' : ''
                        }`} />
                    </button>

                    <AnimatePresence initial={false}>
                      {activePanel === 'security' && (
                        <motion.div
                          initial={{ height: 0, opacity: 0 }}
                          animate={{ height: 'auto', opacity: 1 }}
                          exit={{ height: 0, opacity: 0 }}
                          className="overflow-hidden"
                        >
                          <div className="p-4 space-y-4 text-xs">

                            <p className="text-[10px] text-neutral-400 leading-relaxed">
                              Cập nhật khóa bảo vệ cá nhân định kì để giữ vững hạng tài khoản Thẻ VIP và chống thâm nhập mật trái phép.
                            </p>

                            <div className="space-y-3">
                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black block">Mật khẩu cũ</label>
                                <div className="relative">
                                  <input
                                    type={showOldPass ? "text" : "password"}
                                    value={oldPassword}
                                    onChange={(e) => setOldPassword(e.target.value)}
                                    placeholder="Nhập mật khẩu hiện tại..."
                                    className="w-full bg-black border border-neutral-850 p-2 text-xs text-white focus:outline-none focus:border-white tracking-widest placeholder-neutral-805"
                                  />
                                  <button
                                    type="button"
                                    onClick={() => { playPing(350, 'sine', 0.05); setShowOldPass(!showOldPass); }}
                                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-neutral-550 hover:text-white"
                                  >
                                    {showOldPass ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                                  </button>
                                </div>
                              </div>

                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black block">Mật khẩu mới</label>
                                <div className="relative">
                                  <input
                                    type={showNewPass ? "text" : "password"}
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                    placeholder="Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt..."
                                    className="w-full bg-black border border-neutral-850 p-2 text-xs text-white focus:outline-none focus:border-white tracking-widest placeholder-neutral-805"
                                  />
                                  <button
                                    type="button"
                                    onClick={() => { playPing(350, 'sine', 0.05); setShowNewPass(!showNewPass); }}
                                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-neutral-550 hover:text-white"
                                  >
                                    {showNewPass ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                                  </button>
                                </div>
                              </div>
                            </div>



                            <div className="space-y-3">
                              <div className="space-y-1">
                                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-400 font-black block">Xác nhận mật khẩu mới</label>
                                <div className="relative">
                                  <input
                                    type={showNewPass ? "text" : "password"}
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    placeholder="Nhập lại mật khẩu mới..."
                                    className="w-full bg-black border border-neutral-850 p-2 text-xs text-white focus:outline-none focus:border-white tracking-widest placeholder-neutral-805"
                                  />
                                  <button
                                    type="button"
                                    onClick={() => { playPing(350, 'sine', 0.05); setShowNewPass(!showNewPass); }}
                                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-neutral-550 hover:text-white"
                                  >
                                    {showNewPass ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                                  </button>
                                </div>
                              </div>
                            </div>

                            {passwordNotification && (
                              <div className={`border p-2 text-[10px] font-semibold leading-relaxed ${passwordNotification.type === 'success'
                                ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                                : 'border-rose-500/30 bg-rose-500/10 text-rose-300'
                                }`}>
                                {passwordNotification.text}
                              </div>
                            )}

                            <button
                              onClick={handleChangePassword}
                              disabled={isChangingPassword}
                              className="w-full py-2.5 bg-[#121212] border border-white/10 text-white font-sans uppercase text-[10px] hover:border-white transition tracking-widest font-black disabled:opacity-60"
                            >
                              {isChangingPassword ? 'ĐANG ĐỔI MẬT KHẨU...' : 'XÁC NHẬN ĐỔI MẬT KHẨU'}
                            </button>

                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>

                </div>

                {/* LOGOUT SECURE ACTION BAR */}
                <div className="border-t border-white/5 pt-3.5">
                  <button
                    onClick={() => {
                      playPing(300, 'sawtooth', 0.3);
                      onLogout();
                    }}
                    className="w-full flex items-center justify-center gap-2 p-3 text-xs font-black text-rose-400 hover:text-white bg-rose-950/5 hover:bg-rose-900/30 border border-rose-950/30 hover:border-rose-500/40 transition duration-300 uppercase tracking-[0.16em] text-center"
                  >
                    <LogOut className="h-3.5 w-3.5" />
                    ĐĂNG XUẤT</button>
                </div>

              </div>

            </div>

          </div>
        </>
      )}

    </div>
  );
}
