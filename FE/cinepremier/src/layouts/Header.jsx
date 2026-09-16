import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Search, MapPin, Ticket, User, Heart, Compass, Home,
  Building2, ChevronDown, Phone, Settings2, X, ExternalLink, LogOut, Popcorn, CalendarDays, Coins, ShieldCheck
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { createPortal } from 'react-dom';
import { movieService } from '../services/movieService';
import { loyaltyService } from '../services/loyaltyService';
import { getStoredAuth } from '../services/authService';

export default function Header({
  activeTab,
  onTabChange,
  searchQuery,
  onSearchCommit = () => { },
  moviesList = [],
  cinema = null,
  onManageCinema = () => { },
  onOpenOTP,
  isLoggedIn,
  currentUser,
  currentRole = 'user',
  loyaltyRefreshKey = '',
  showToast = () => { },
  handleLogout = () => { },
  navigate = () => { }
}) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [loyaltyPoints, setLoyaltyPoints] = useState(null);

  // Ô tìm phim: state cục bộ + dropdown gợi ý (gõ không còn nhảy trang;
  // Enter/"Xem tất cả" mới sang Khám Phá, click gợi ý đi thẳng trang phim)
  const [headerQuery, setHeaderQuery] = useState(searchQuery || '');
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [activeSuggestion, setActiveSuggestion] = useState(-1);
  const searchBoxRef = useRef(null);

  useEffect(() => { setHeaderQuery(searchQuery || ''); }, [searchQuery]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchBoxRef.current && !searchBoxRef.current.contains(event.target)) setIsSearchOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Danh mục riêng cho gợi ý — tải 1 lần khi bắt đầu gõ, KHÔNG dùng moviesList
  // của store (danh sách đó bị thay bằng kết quả lọc của trang Khám Phá).
  const [suggestionCatalog, setSuggestionCatalog] = useState(null);
  useEffect(() => {
    if (!headerQuery.trim() || suggestionCatalog !== null) return undefined;
    let cancelled = false;
    movieService.searchMovies({ size: 100 })
      .then((list) => { if (!cancelled) setSuggestionCatalog(Array.isArray(list) ? list : []); })
      .catch(() => { if (!cancelled) setSuggestionCatalog([]); });
    return () => { cancelled = true; };
  }, [headerQuery, suggestionCatalog]);

  const searchSuggestions = useMemo(() => {
    const q = headerQuery.trim().toLowerCase();
    if (!q) return [];
    const source = suggestionCatalog?.length ? suggestionCatalog : (moviesList || []);
    return source
      .filter((m) => m && m.status !== 'INACTIVE' && !m.isInactive)
      .filter((m) => (
        (m.title || '').toLowerCase().includes(q)
        || (m.englishTitle || '').toLowerCase().includes(q)
        || (m.director || '').toLowerCase().includes(q)
        || (m.genre || []).some((g) => String(g || '').toLowerCase().includes(q))
      ))
      .slice(0, 5);
  }, [headerQuery, moviesList, suggestionCatalog]);

  const openMovieSuggestion = (movie) => {
    setIsSearchOpen(false);
    setActiveSuggestion(-1);
    setHeaderQuery('');
    navigate(`/movies/${movie.id}`);
  };

  const commitSearch = () => {
    const q = headerQuery.trim();
    if (!q) return;
    setIsSearchOpen(false);
    setActiveSuggestion(-1);
    onSearchCommit(q);
  };

  const handleSearchKeyDown = (event) => {
    if (event.key === 'Escape') { setIsSearchOpen(false); setActiveSuggestion(-1); return; }
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setIsSearchOpen(true);
      setActiveSuggestion((i) => Math.min(i + 1, searchSuggestions.length - 1));
      return;
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveSuggestion((i) => Math.max(i - 1, -1));
      return;
    }
    if (event.key === 'Enter') {
      if (activeSuggestion >= 0 && searchSuggestions[activeSuggestion]) openMovieSuggestion(searchSuggestions[activeSuggestion]);
      else commitSearch();
    }
  };

  const cinemaAddress = [cinema?.address, cinema?.city].filter(Boolean).join(', ');
  const googleMapsUrl = cinemaAddress
    ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(cinemaAddress)}`
    : 'https://www.google.com/maps';
  const isAdminRole = currentRole === 'admin' || currentUser?.role === 'admin';
  const isStaffRole = currentRole === 'staff' || currentUser?.role === 'staff';
  const canUseWishlist = !isAdminRole && !isStaffRole;
  const canShowLoyalty = isLoggedIn && !isAdminRole && !isStaffRole;

  useEffect(() => {
    if (!canShowLoyalty) {
      setLoyaltyPoints(null);
      return undefined;
    }

    let cancelled = false;
    const loadLoyalty = async () => {
      const { accessToken } = getStoredAuth();
      if (!accessToken) return;
      try {
        const loyalty = await loyaltyService.getMyLoyalty(accessToken);
        if (!cancelled) setLoyaltyPoints(Number(loyalty?.points ?? 0));
      } catch {
        if (!cancelled) setLoyaltyPoints(null);
      }
    };

    void loadLoyalty();
    window.addEventListener('focus', loadLoyalty);
    window.addEventListener('loyalty:updated', loadLoyalty);
    return () => {
      cancelled = true;
      window.removeEventListener('focus', loadLoyalty);
      window.removeEventListener('loyalty:updated', loadLoyalty);
    };
  }, [canShowLoyalty, currentUser?.id, loyaltyRefreshKey]);

  return (
    <header className="sticky top-0 z-[100] w-full max-w-full overflow-visible border-b border-white/10 bg-black/95 backdrop-blur-md">
      <div className="mx-auto flex h-16 w-full max-w-5xl min-w-0 items-center justify-between gap-2 px-4 sm:px-6 lg:px-8">
        {/* Logo CINEPREMIER */}
        <motion.div
          onClick={() => onTabChange(isStaffRole ? 'staff' : 'home')}
          className="flex cursor-pointer items-center space-x-3.5 group select-none mr-4"
          id="header-logo"
          whileHover={{ scale: 1.015 }}
          whileTap={{ scale: 0.985 }}
        >
          <motion.div
            className="relative h-10 w-10 flex items-center justify-center bg-zinc-950 border border-white/15 overflow-hidden shadow-[inset_0_0_12px_rgba(255,255,255,0.05)] group-hover:border-amber-400/40 transition-all duration-300 rounded-lg"
            animate={{
              boxShadow: ["inset 0 0 10px rgba(255,255,255,0.02)", "inset 0 0 10px rgba(245,158,11,0.08)", "inset 0 0 10px rgba(255,255,255,0.02)"]
            }}
            transition={{ repeat: Infinity, duration: 4, ease: "easeInOut" }}
          >
            <span className="absolute top-1 left-1 h-1.5 w-1.5 border-t border-l border-white/30 group-hover:border-amber-500 group-hover:-translate-x-0.5 group-hover:-translate-y-0.5 transition-all duration-300"></span>
            <span className="absolute top-1 right-1 h-1.5 w-1.5 border-t border-r border-white/30 group-hover:border-amber-500 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-all duration-300"></span>
            <span className="absolute bottom-1 left-1 h-1.5 w-1.5 border-b border-l border-white/30 group-hover:border-amber-500 group-hover:-translate-x-0.5 group-hover:translate-y-0.5 transition-all duration-300"></span>
            <span className="absolute bottom-1 right-1 h-1.5 w-1.5 border-b border-r border-white/30 group-hover:border-amber-500 group-hover:translate-x-0.5 group-hover:translate-y-0.5 transition-all duration-300"></span>

            <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(245,158,11,0.15)_0%,transparent_70%)] opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
            <div className="absolute inset-y-0 -left-full w-1/2 bg-gradient-to-r from-transparent via-amber-500/20 to-transparent skew-x-12 group-hover:animate-sweep pointer-events-none" />

            <span className="relative font-serif text-lg italic font-extrabold tracking-normal text-transparent bg-clip-text bg-gradient-to-b from-white via-amber-200 to-amber-400 group-hover:from-amber-100 group-hover:via-amber-300 group-hover:to-amber-500 drop-shadow-[0_2px_4px_rgba(245,158,11,0.3)] transition-all duration-300 select-none">
              C
            </span>
          </motion.div>

          <div className="flex flex-col justify-center">
            <span className="font-sans font-black tracking-[0.22em] text-xs sm:text-sm text-white uppercase leading-none group-hover:text-amber-400 transition-colors duration-300">
              CINE<span className="text-amber-400">PREMIER</span>
            </span>
            <span className="text-[7.5px] font-mono tracking-[0.45em] text-neutral-200 uppercase mt-1 leading-none group-hover:text-neutral-100 transition-colors">
              STUDIOS
            </span>
          </div>
        </motion.div>

        {isStaffRole && (
          <div className="flex items-center gap-2 border border-emerald-500/30 bg-emerald-500/10 px-3.5 py-1.5 rounded-full text-emerald-300 text-[10px] font-sans font-extrabold tracking-widest uppercase shadow-[0_0_15px_rgba(16,185,129,0.2)] select-none">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse shadow-[0_0_8px_#10b981]" />
            <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
            <span>VẬN HÀNH STAFF</span>
          </div>
        )}

        {/* Right Header Section */}
        <div className="flex min-w-0 shrink-0 items-center space-x-2.5">

          {/* Quick action buttons & search (hidden for staff) */}
          {!isStaffRole && (
            <>
              <button
                onClick={() => onTabChange('explore')}
                className="hidden lg:flex h-9 items-center gap-1.5 bg-yellow-500 hover:bg-yellow-600 px-3.5 text-[10px] font-sans font-extrabold uppercase tracking-[0.12em] text-black transition whitespace-nowrap rounded"
                id="btn-book-now"
              >
                <Ticket className="h-3.5 w-3.5" /> ĐẶT VÉ NGAY
              </button>
              <button
                onClick={() => onTabChange('concessions')}
                className="hidden lg:flex h-9 items-center gap-1.5 bg-purple-700 hover:bg-purple-800 px-3.5 text-[10px] font-sans font-extrabold uppercase tracking-[0.12em] text-white transition whitespace-nowrap rounded"
                id="btn-order-food"
              >
                <Popcorn className="h-3.5 w-3.5" /> ĐẶT BẮP NƯỚC
              </button>

              {/* Search Box + dropdown gợi ý */}
              <div ref={searchBoxRef} className="relative hidden xl:block w-40 xl:w-48 h-9">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-neutral-200 pointer-events-none" />
                <input
                  type="text"
                  placeholder="TÌM PHIM..."
                  value={headerQuery}
                  onChange={(e) => { setHeaderQuery(e.target.value); setIsSearchOpen(true); setActiveSuggestion(-1); }}
                  onFocus={() => { if (headerQuery.trim()) setIsSearchOpen(true); }}
                  onKeyDown={handleSearchKeyDown}
                  className="w-full h-full border border-white/10 bg-black/60 pl-9 pr-4 text-[9.5px] font-semibold text-white tracking-widest placeholder:font-semibold placeholder-neutral-500 uppercase focus:border-white/30 focus:bg-black/80 focus:outline-none transition-all duration-300 rounded"
                  id="search-input"
                  autoComplete="off"
                  role="combobox"
                  aria-expanded={isSearchOpen && headerQuery.trim().length > 0}
                  aria-controls="search-suggestions"
                />

                {isSearchOpen && headerQuery.trim().length > 0 && (
                  <div
                    id="search-suggestions"
                    role="listbox"
                    className="absolute left-0 right-0 top-full z-[200] mt-1.5 w-64 overflow-hidden rounded border border-white/15 bg-[#0d0d0d] shadow-[0_16px_50px_rgba(0,0,0,0.7)]"
                  >
                    {searchSuggestions.length === 0 ? (
                      <p className="px-3.5 py-3 text-[10px] font-sans uppercase tracking-widest text-neutral-500">
                        Không có phim khớp "{headerQuery.trim()}"
                      </p>
                    ) : (
                      <>
                        {searchSuggestions.map((movie, index) => (
                          <button
                            key={movie.id}
                            role="option"
                            aria-selected={index === activeSuggestion}
                            onMouseEnter={() => setActiveSuggestion(index)}
                            onClick={() => openMovieSuggestion(movie)}
                            className={`flex w-full items-center gap-2.5 px-3 py-2 text-left transition ${index === activeSuggestion ? 'bg-white/10' : 'hover:bg-white/5'
                              }`}
                          >
                            {movie.posterUrl ? (
                              <img src={movie.posterUrl} alt="" className="h-11 w-8 shrink-0 rounded-sm border border-white/10 object-cover" referrerPolicy="no-referrer" />
                            ) : (
                              <span className="h-11 w-8 shrink-0 rounded-sm border border-white/10 bg-neutral-900" />
                            )}
                            <span className="min-w-0">
                              <span className="block truncate text-[11px] font-sans font-bold text-white">{movie.title}</span>
                              <span className="block truncate text-[9px] font-mono uppercase tracking-widest text-neutral-500">
                                {[movie.durationMinutes || movie.duration ? `${movie.durationMinutes || movie.duration} phút` : '', (movie.genre || [])[0]].filter(Boolean).join(' · ') || movie.englishTitle || ''}
                              </span>
                            </span>
                          </button>
                        ))}
                        <button
                          onClick={commitSearch}
                          className="flex w-full items-center justify-between border-t border-white/10 px-3.5 py-2.5 text-[9.5px] font-sans font-black uppercase tracking-widest text-amber-400 transition hover:bg-amber-500/10"
                        >
                          <span>Xem tất cả kết quả</span>
                          <span className="font-mono normal-case text-neutral-500">Enter ↵</span>
                        </button>
                      </>
                    )}
                  </div>
                )}
              </div>
            </>
          )}

          {/* Location Selector */}
          <div className="relative">
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className="group flex h-10 items-center justify-center gap-2 whitespace-nowrap border border-white/10 bg-black/60 px-3.5 text-[10px] font-sans uppercase tracking-[0.15em] text-neutral-200 transition-all duration-300 hover:border-amber-500/40 hover:bg-black/80 hover:text-white rounded"
              id="location-button"
            >
              <span className="flex h-6 w-6 items-center justify-center border border-white/10 text-neutral-200">
                <MapPin className="h-3.5 w-3.5" />
              </span>
              <span className="hidden max-w-[120px] text-left sm:block">
                <span className="block truncate font-black text-white">
                  {cinema?.name || 'Chưa có rạp'}
                </span>
                <span className="mt-0.5 block truncate text-[7px] font-bold tracking-[0.18em] text-neutral-300">
                  {cinema?.city || 'ĐỊA ĐIỂM CHIẾU'}
                </span>
              </span>
              <ChevronDown className={`h-3 w-3 text-neutral-200 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`} />
            </button>
            {typeof document !== 'undefined' && createPortal(<AnimatePresence>
              {dropdownOpen && (
                <motion.div
                  key="cinema-modal"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  onClick={() => setDropdownOpen(false)}
                  className="fixed inset-0 z-[300] flex items-center justify-center bg-black/80 p-4 backdrop-blur-md"
                >
                  <motion.div
                    initial={{ opacity: 0, y: 16, scale: 0.98 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 12, scale: 0.98 }}
                    onClick={(event) => event.stopPropagation()}
                    className="w-full max-w-3xl overflow-hidden border border-white/15 bg-[#070707] shadow-[0_28px_100px_rgba(0,0,0,0.85)] rounded-lg"
                    id="location-dropdown"
                  >
                    <div className="flex items-center justify-between border-b border-white/10 px-5 py-4 sm:px-7">
                      <div className="flex items-center gap-3">
                        <span className="flex h-11 w-11 items-center justify-center border border-amber-500/35 bg-amber-500/10 text-amber-400">
                          <Building2 className="h-5 w-5" />
                        </span>
                        <div>
                          <p className="text-[9px] font-black uppercase tracking-[0.24em] text-amber-400">Thông tin rạp chiếu</p>
                          <h2 className="mt-1 text-lg font-black uppercase tracking-wide text-white">{cinema?.name || 'Chưa cấu hình rạp'}</h2>
                        </div>
                      </div>
                      <button type="button" onClick={() => setDropdownOpen(false)} className="p-2 text-neutral-300 transition hover:bg-white/5 hover:text-white" aria-label="Dong">
                        <X className="h-5 w-5" />
                      </button>
                    </div>

                    <div className="grid grid-cols-1 gap-0 md:grid-cols-[1.2fr_0.8fr]">
                      <div className="space-y-5 border-b border-white/10 p-5 sm:p-7 md:border-b-0 md:border-r">
                        <div>
                          <p className="text-[9px] font-black uppercase tracking-[0.2em] text-neutral-600">Địa chỉ</p>
                          <a
                            href={googleMapsUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-2 flex items-start gap-3 border border-white/10 bg-black p-4 text-sm font-bold leading-relaxed text-white transition hover:border-amber-400/50 hover:text-amber-200"
                          >
                            <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
                            <span className="flex-1">{cinemaAddress || 'Chua co Địa chỉ rap'}</span>
                            <ExternalLink className="h-4 w-4 shrink-0 text-neutral-300" />
                          </a>
                        </div>

                        {cinema?.phone && (
                          <div>
                            <p className="text-[9px] font-black uppercase tracking-[0.2em] text-neutral-600">Hotline</p>
                            <a href={`tel:${cinema.phone}`} className="mt-2 flex items-center gap-3 border border-white/10 bg-black p-4 text-sm font-bold text-white transition hover:border-amber-400/50 hover:text-amber-200">
                              <Phone className="h-4 w-4 text-amber-400" />
                              {cinema.phone}
                            </a>
                          </div>
                        )}
                      </div>

                      <div className="flex flex-col justify-between gap-6 p-5 sm:p-7">
                        <div className="space-y-4">
                          <div>
                            <p className="text-[9px] font-black uppercase tracking-[0.2em] text-neutral-600">Trạng thái</p>
                            <span className={`mt-2 inline-flex border px-3 py-2 text-[10px] font-black uppercase tracking-widest ${cinema?.status === 'ACTIVE' ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' : 'border-rose-500/30 bg-rose-500/10 text-rose-300'}`}>
                              {cinema?.status === 'ACTIVE' ? 'Đang hoạt động' : cinema?.status || 'Chưa cấu hình'}
                            </span>
                          </div>
                          <p className="text-xs leading-relaxed text-neutral-300">
                            Thông tin được tải trực tiếp từ hệ thống khi ứng dụng khởi động.
                          </p>
                        </div>

                        <div className="space-y-2">
                          <a href={googleMapsUrl} target="_blank" rel="noreferrer" className="flex w-full items-center justify-center gap-2 bg-white px-4 py-3 text-[10px] font-black uppercase tracking-widest text-black transition hover:bg-amber-400">
                            <MapPin className="h-4 w-4" /> Mở Google Maps
                          </a>
                          {currentRole === 'admin' && (
                            <button type="button" onClick={() => { setDropdownOpen(false); onManageCinema(); }} className="flex w-full items-center justify-center gap-2 border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-[10px] font-black uppercase tracking-widest text-amber-200 transition hover:border-amber-400 hover:bg-amber-500/20">
                              <Settings2 className="h-4 w-4" /> Quản lý rạp chiếu
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  </motion.div>
                </motion.div>
              )}
            </AnimatePresence>, document.body)}
          </div>

          {/* User Signin Profile Indicator (hidden for staff) */}
          {!isStaffRole && (
            <button
              onClick={() => {
                if (isLoggedIn) {
                  onTabChange('profile');
                } else {
                  onOpenOTP();
                }
              }}
              className={`group flex h-10 items-center justify-center gap-2 border px-3.5 font-sans uppercase font-bold shadow-md transition-colors whitespace-nowrap rounded focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-yellow-300 active:translate-y-px ${isLoggedIn
                ? 'border-yellow-500/40 bg-yellow-500/10 text-yellow-400 hover:bg-yellow-500 hover:text-black hover:border-yellow-400 shadow-[0_0_10px_rgba(234,179,8,0.15)]'
                : 'border-white/20 bg-black/60 text-white hover:bg-white hover:text-black'
                }`}
              id="signin-button"
            >
              <User className="h-3.5 w-3.5 shrink-0" />
              <span className="flex min-w-0 flex-col items-start leading-none">
                <span className="max-w-28 truncate text-[10px] tracking-[0.15em]">
                  {isLoggedIn ? (currentUser?.name) : 'ĐĂNG NHẬP'}
                </span>
                {canShowLoyalty && (
                  <span className="mt-1 flex items-center gap-1 text-[7px] font-black tracking-[0.12em] text-amber-200/80 group-hover:text-black/70">
                    <Coins className="h-2.5 w-2.5" />
                    {loyaltyPoints === null ? 'CINEPOINTS' : `${loyaltyPoints.toLocaleString('vi-VN')} CINEPOINTS`}
                  </span>
                )}
              </span>
            </button>
          )}

          {/* Logout Button */}
          {isLoggedIn && (
            <button
              onClick={() => handleLogout({ navigate, showToast })}
              className="flex h-9 items-center justify-center space-x-1.5 border border-white/10 bg-black/60 px-3.5 text-[10px] font-sans uppercase tracking-[0.15em] font-bold text-white hover:bg-white hover:text-black transition-all duration-300 whitespace-nowrap shadow-md rounded"
              id="logout-button"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span>ĐĂNG XUẤT</span>
            </button>
          )}
        </div>

      </div>

      {/* Row 2: Navigation (hidden for staff) */}
      {!isStaffRole && (
        <div className="border-t border-white/5 bg-gradient-to-r from-indigo-950/40 via-black to-fuchsia-950/30">
          <div className="mx-auto flex h-11 w-full max-w-5xl items-center justify-center px-4 sm:px-6 lg:px-8">
            <nav className="flex min-w-0 max-w-full items-center justify-start gap-1 overflow-x-auto [scrollbar-width:none] md:justify-center [&::-webkit-scrollbar]:hidden" id="main-nav-bar">
              <button
                onClick={() => onTabChange('home')}
                className={`px-3.5 py-1.5 text-[10px] font-sans uppercase tracking-[0.2em] transition-all duration-300 flex items-center gap-1.5 whitespace-nowrap border-b-2 ${activeTab === 'home'
                  ? 'text-white border-white font-bold'
                  : 'text-neutral-300 hover:text-white border-transparent'
                  }`}
                id="nav-home"
              >
                <Home className="h-3.5 w-3.5" />
                <span>TRANG CHỦ</span>
              </button>

              <button
                onClick={() => onTabChange('explore')}
                className={`px-3.5 py-1.5 text-[10px] font-sans uppercase tracking-[0.2em] transition-all duration-300 flex items-center gap-1.5 whitespace-nowrap border-b-2 ${activeTab === 'explore'
                  ? 'text-white border-white font-bold'
                  : 'text-neutral-300 hover:text-white border-transparent'
                  }`}
                id="nav-explore"
              >
                <Compass className="h-3.5 w-3.5" />
                <span>KHÁM PHÁ</span>
              </button>

              <button
                onClick={() => onTabChange('showtimes')}
                className={`px-3.5 py-1.5 text-[10px] font-sans uppercase tracking-[0.2em] transition-all duration-300 flex items-center gap-1.5 whitespace-nowrap border-b-2 ${activeTab === 'showtimes'
                  ? 'text-white border-white font-bold'
                  : 'text-neutral-300 hover:text-white border-transparent'
                  }`}
                id="nav-showtimes"
              >
                <Search className="h-3.5 w-3.5" />
                <span>TÌM PHIM</span>
              </button>

              {!isAdminRole && (
                <button
                  onClick={() => onTabChange('my-tickets')}
                  className={`px-3.5 py-1.5 text-[10px] font-sans uppercase tracking-[0.2em] transition-all duration-300 flex items-center gap-1.5 whitespace-nowrap border-b-2 ${activeTab === 'my-tickets'
                    ? 'text-white border-white font-bold'
                    : 'text-neutral-300 hover:text-white border-transparent'
                    }`}
                  id="nav-my-bookings"
                >
                  <Ticket className="h-3.5 w-3.5" />
                  <span>ĐƠN CỦA TÔI</span>
                </button>
              )}

              {canUseWishlist && (
                <button
                  onClick={() => onTabChange('wishlist')}
                  className={`px-3.5 py-1.5 text-[10px] font-sans uppercase tracking-[0.2em] transition-all duration-300 flex items-center gap-1.5 whitespace-nowrap border-b-2 ${activeTab === 'wishlist'
                    ? 'text-white border-white font-bold'
                    : 'text-neutral-300 hover:text-white border-transparent'
                    }`}
                  id="nav-wishlist"
                >
                  <Heart className="h-3.5 w-3.5" />
                  <span>WATCHLIST</span>
                </button>
              )}

              {isLoggedIn && (
                <button
                  onClick={() => onTabChange('profile')}
                  className={`px-3.5 py-1.5 text-[10px] font-sans uppercase tracking-[0.2em] transition-all duration-300 flex items-center gap-1.5 whitespace-nowrap border-b-2 ${activeTab === 'profile'
                    ? 'text-white border-white font-bold'
                    : 'text-neutral-300 hover:text-white border-transparent'
                    }`}
                  id="nav-profile-tab"
                >
                  <User className="h-3.5 w-3.5" />
                  <span>CÁ NHÂN</span>
                </button>
              )}
            </nav>
          </div>
        </div>
      )}
    </header >
  );
}
