import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Plus, Trash2, Edit3, ShieldAlert, FileText, Database,
  Calendar, Users, DollarSign, Activity, AlertCircle, CheckCircle2,
  Search, Sliders, ChevronDown, Check, RefreshCw, Layers, ShoppingBag,
  BarChart2, Clock, MapPin, Film, Play, Eye, EyeOff, Sparkles, TrendingUp, Info, Globe, Tags, ImageUp, Video
} from 'lucide-react';
import { adminService } from '../../../services/adminService';

export default function AdminMoviesPanel({ ctx }) {
  const {
    activeTab,
    setActiveTab,
    activeChartPoint,
    setActiveChartPoint,
    searchQuery,
    setSearchQuery,
    filmFilter,
    setFilmFilter,
    adminGenreFilter,
    setAdminGenreFilter,
    adminMoviePagination,
    setAdminMoviePagination,
    editingMovie,
    showMovieForm,
    setShowMovieForm,
    isMovieSaving,
    formData,
    setFormData,
    resetMovieForm,
    newShowtime,
    setNewShowtime,
    isAddingShowtime,
    setIsAddingShowtime,
    showtimeSuccessMessage,
    setShowtimeSuccessMessage,
    genres,
    setGenres,
    actors,
    setActors,
    genreSearch,
    setGenreSearch,
    genreForm,
    setGenreForm,
    genreErrors,
    setGenreErrors,
    editingGenreId,
    setEditingGenreId,
    isGenreLoading,
    setIsGenreLoading,
    isGenreSaving,
    setIsGenreSaving,
    foodItems,
    setFoodItems,
    foodCombos,
    setFoodCombos,
    foodSearch,
    setFoodSearch,
    foodKind,
    setFoodKind,
    editingFood,
    setEditingFood,
    foodForm,
    setFoodForm,
    foodErrors,
    setFoodErrors,
    isFoodLoading,
    setIsFoodLoading,
    isFoodSaving,
    setIsFoodSaving,
    visibleFoods,
    HALL_OPTIONS,
    TIME_OPTIONS,
    playPulseSound,
    auditLogs,
    setAuditLogs,
    addAuditLog,
    resetFoodForm,
    validateFoodForm,
    fetchFoods,
    handleFoodSubmit,
    handleEditFood,
    handleToggleFoodStatus,
    getAdminToken,
    changeAdminSection,
    validateGenreForm,
    fetchGenres,
    fetchActors,
    resetGenreForm,
    handleGenreSubmit,
    handleEditGenre,
    performDeleteGenre,
    handleDeleteGenre,
    totalBookingsCount,
    calculatedRevenue,
    averageFillRate,
    handleEditMovie,
    handleCreateMovieSubmit,
    handleUpdateMovieStatus,
    handleDeleteMovie,
    handleAddShowtimeSubmit,
    handleRefundTicket,
    filteredMovies,
    filteredGenres,
    moviesList,
    setMoviesList,
    bookedTickets,
    setBookedTickets,
    onSelectMovie,
    showToast,
    initialSection,
    onSectionChange,
    onFoodCatalogChanged,
    isAdmin
  } = ctx;

  const [actorForm, setActorForm] = useState({ name: '', biography: '', avatarUrl: '' });
  const [isActorSaving, setIsActorSaving] = useState(false);
  const [isActorImageUploading, setIsActorImageUploading] = useState(false);
  const [isPosterUploading, setIsPosterUploading] = useState(false);
  const [isBannerUploading, setIsBannerUploading] = useState(false);
  const [isTrailerUploading, setIsTrailerUploading] = useState(false);
  const [createdActors, setCreatedActors] = useState([]);
  const [focusedDateField, setFocusedDateField] = useState(null);
  const [isDirectorDropdownOpen, setIsDirectorDropdownOpen] = useState(false);
  const [directorPickerSearch, setDirectorPickerSearch] = useState('');
  const [isActorDropdownOpen, setIsActorDropdownOpen] = useState(false);
  const [actorPickerSearch, setActorPickerSearch] = useState('');
  const isMovieMediaUploading = isPosterUploading || isBannerUploading || isTrailerUploading;

  useEffect(() => {
    if (!isDirectorDropdownOpen || typeof fetchActors !== 'function') return undefined;
    const timeoutId = window.setTimeout(() => {
      fetchActors(directorPickerSearch.trim());
    }, 300);
    return () => window.clearTimeout(timeoutId);
  }, [isDirectorDropdownOpen, directorPickerSearch, fetchActors]);

  const hasReleaseDatePassed = (value) => {
    if (!value) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const releaseDate = new Date(value);
    releaseDate.setHours(0, 0, 0, 0);
    return !Number.isNaN(releaseDate.getTime()) && releaseDate < today;
  };

  const todayInputValue = (() => {
    const today = new Date();
    today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
    return today.toISOString().slice(0, 10);
  })();

  const formatDateForDisplay = (value) => {
    if (!value) return '';
    const parts = String(value).split('-');
    if (parts.length !== 3) return value;
    const [year, month, day] = parts;
    return `${day}/${month}/${year}`;
  };

  const resolveMovieStatusFromDates = (releaseDateValue, endDateValue) => {
    const releaseDate = new Date(releaseDateValue);
    const endDate = new Date(endDateValue);
    const today = new Date();
    releaseDate.setHours(0, 0, 0, 0);
    endDate.setHours(0, 0, 0, 0);
    today.setHours(0, 0, 0, 0);
    if (Number.isNaN(releaseDate.getTime()) || Number.isNaN(endDate.getTime())) return 'UPCOMING';
    if (today < releaseDate) return 'UPCOMING';
    if (today > endDate) return 'ENDED';
    return 'NOW_SHOWING';
  };

  const statusRank = (status) => ({ UPCOMING: 0, NOW_SHOWING: 1, ENDED: 2 }[String(status || '').toUpperCase()] ?? -1);

  const isBackwardStatus = (currentStatus, requestedStatus) => {
    const currentRank = statusRank(currentStatus);
    const requestedRank = statusRank(requestedStatus);
    return currentRank >= 0 && requestedRank >= 0 && requestedRank < currentRank;
  };

  const isMovieStatusOptionDisabled = (movie, requestedStatus) => {
    if (requestedStatus === 'INACTIVE') return false;
    if (!hasReleaseDatePassed(movie?.releaseDate)) return false;
    return requestedStatus === 'UPCOMING' || isBackwardStatus(movie?.status, requestedStatus);
  };

  const toggleMovieGenre = (genreId) => {
    const normalizedId = Number(genreId);
    const currentIds = formData.genreIds || [];
    const nextGenreIds = currentIds.includes(normalizedId)
      ? currentIds.filter((id) => id !== normalizedId)
      : [...currentIds, normalizedId];
    const nextGenreNames = genres
      .filter((genre) => nextGenreIds.includes(Number(genre.id)))
      .map((genre) => genre.name)
      .join(', ');

    setFormData({
      ...formData,
      genreIds: nextGenreIds,
      genre: nextGenreNames
    });
  };

  const toggleMovieActor = (actorId) => {
    const id = Number(actorId);
    const actorIds = (formData.actorIds || []).map(Number);
    const selected = actorIds.includes(id);
    setFormData({
      ...formData,
      actorIds: selected ? actorIds.filter((item) => item !== id) : [...actorIds, id],
      mainActorIds: selected
        ? (formData.mainActorIds || []).map(Number).filter((item) => item !== id)
        : formData.mainActorIds || []
    });
  };

  const toggleMovieMainActor = (actorId) => {
    const id = Number(actorId);
    const actorIds = (formData.actorIds || []).map(Number);
    const mainActorIds = (formData.mainActorIds || []).map(Number);
    setFormData({
      ...formData,
      actorIds: actorIds.includes(id) ? actorIds : [...actorIds, id],
      mainActorIds: mainActorIds.includes(id)
        ? mainActorIds.filter((item) => item !== id)
        : [...mainActorIds, id]
    });
  };

  const normalizeSearchText = (value) => String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/đ/g, 'd')
    .replace(/\s+/g, ' ')
    .trim();

  const fuzzyIncludes = (source, query) => {
    const normalizedSource = normalizeSearchText(source);
    const normalizedQuery = normalizeSearchText(query);
    if (!normalizedQuery) return true;
    if (normalizedSource.includes(normalizedQuery)) return true;
    let cursor = 0;
    for (const char of normalizedQuery) {
      cursor = normalizedSource.indexOf(char, cursor);
      if (cursor === -1) return false;
      cursor += 1;
    }
    return true;
  };

  const splitDirectorNames = (value) => String(value || '')
    .split(/[,\n;/]+/)
    .map((item) => item.trim())
    .filter(Boolean);

  const uppercaseMovieTitle = (value) => String(value || '').toLocaleUpperCase('vi-VN');

  const uniqueByName = (names) => {
    const seen = new Set();
    return names.filter((name) => {
      const key = normalizeSearchText(name);
      if (!key || seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  };

  const selectedDirectorNames = uniqueByName(splitDirectorNames(formData.director));
  const getActorDisplayName = (actor) => actor?.name || actor?.fullName || actor?.actorName || actor?.raw?.name || '';
  const directorOptions = uniqueByName([
    ...selectedDirectorNames,
    ...(actors || []).map(getActorDisplayName).filter(Boolean)
  ])
    .filter((name) => fuzzyIncludes(name, directorPickerSearch))
    .sort((left, right) => {
      const leftSelected = selectedDirectorNames.some((name) => normalizeSearchText(name) === normalizeSearchText(left)) ? 0 : 1;
      const rightSelected = selectedDirectorNames.some((name) => normalizeSearchText(name) === normalizeSearchText(right)) ? 0 : 1;
      if (leftSelected !== rightSelected) return leftSelected - rightSelected;
      return left.localeCompare(right, 'vi');
    });

  const setDirectorNames = (names) => {
    const nextNames = uniqueByName(names);
    const nextValue = nextNames.join(', ');
    if (nextValue.length > 255) {
      showToast?.('Danh sách đạo diễn tối đa 255 ký tự.');
      return;
    }
    setFormData({ ...formData, director: nextValue });
  };

  const addDirectorName = (name) => {
    const cleanName = String(name || '').trim();
    if (!cleanName) return;
    setDirectorNames([...selectedDirectorNames, cleanName]);
    setDirectorPickerSearch('');
    setIsDirectorDropdownOpen(true);
  };

  const removeDirectorName = (name) => {
    const removeKey = normalizeSearchText(name);
    setDirectorNames(selectedDirectorNames.filter((directorName) => normalizeSearchText(directorName) !== removeKey));
  };

  const typedDirectorExists = selectedDirectorNames.some((name) => normalizeSearchText(name) === normalizeSearchText(directorPickerSearch))
    || directorOptions.some((name) => normalizeSearchText(name) === normalizeSearchText(directorPickerSearch));

  const selectedActorIds = (formData.actorIds || []).map(Number);
  const selectedMainActorIds = (formData.mainActorIds || []).map(Number);
  const selectedActors = (actors || []).filter((actor) => selectedActorIds.includes(Number(actor.id)));
  const actorPickerOptions = (actors || [])
    .filter((actor) => {
      const query = actorPickerSearch.trim();
      if (!query) return true;
      return fuzzyIncludes(`${actor.name || ''} ${actor.biography || ''} ${actor.id || ''}`, query);
    })
    .sort((left, right) => {
      const leftSelected = selectedActorIds.includes(Number(left.id)) ? 0 : 1;
      const rightSelected = selectedActorIds.includes(Number(right.id)) ? 0 : 1;
      if (leftSelected !== rightSelected) return leftSelected - rightSelected;
      return String(left.name || '').localeCompare(String(right.name || ''), 'vi');
    });

  const handleQuickCreateActor = async (event) => {
    event.preventDefault();
    const token = getAdminToken();
    if (!token) return;
    if (!actorForm.name.trim()) {
      showToast('Vui lòng nhập tên actor.');
      return;
    }

    setIsActorSaving(true);
    try {
      const actor = await adminService.createAdminActor(token, {
        name: actorForm.name.trim(),
        biography: actorForm.biography.trim(),
        avatarUrl: actorForm.avatarUrl.trim()
      });
      const actorId = Number(actor.id);
      if (Number.isFinite(actorId)) {
        setFormData((prev) => ({
          ...prev,
          actorIds: Array.from(new Set([...(prev.actorIds || []), actorId]))
        }));
      }
      setCreatedActors((prev) => [actor, ...prev]);
      setActors((prev) => [actor, ...prev.filter((item) => String(item.id) !== String(actor.id))]);
      setActorForm({ name: '', biography: '', avatarUrl: '' });
      showToast(`Đã tạo actor: ${actor.name}`);
    } catch (error) {
      showToast(error.message || 'Không thể tạo actor.');
    } finally {
      setIsActorSaving(false);
    }
  };

  const handleQuickActorImageUpload = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    const token = getAdminToken();
    if (!token) return;

    setIsActorImageUploading(true);
    try {
      const uploaded = await adminService.uploadAdminImage(token, file, 'actors');
      setActorForm((prev) => ({ ...prev, avatarUrl: uploaded.url }));
      showToast('Đã tải ảnh diễn viên lên Cloudinary.');
    } catch (error) {
      showToast(error.message || 'Không thể tải ảnh lên Cloudinary.');
    } finally {
      setIsActorImageUploading(false);
    }
  };

  const handleMovieImageUpload = async (field, folder, event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    const token = getAdminToken();
    if (!token) return;

    const setUploading = field === 'posterUrl' ? setIsPosterUploading : setIsBannerUploading;
    setUploading(true);
    try {
      const uploaded = await adminService.uploadAdminImage(token, file, folder);
      setFormData((prev) => ({ ...prev, [field]: uploaded.url }));
      showToast(field === 'posterUrl' ? 'Đã tải poster lên Cloudinary.' : 'Đã tải banner lên Cloudinary.');
    } catch (error) {
      showToast(error.message || 'Không thể tải ảnh lên Cloudinary.');
    } finally {
      setUploading(false);
    }
  };

  const handleTrailerVideoUpload = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    const token = getAdminToken();
    if (!token) return;

    setIsTrailerUploading(true);
    try {
      const uploaded = await adminService.uploadAdminVideo(token, file, 'movies/trailers');
      setFormData((prev) => ({ ...prev, trailerUrl: uploaded.url }));
      showToast('Đã tải trailer lên Cloudinary.');
    } catch (error) {
      showToast(error.message || 'Không thể tải trailer lên Cloudinary.');
    } finally {
      setIsTrailerUploading(false);
    }
  };

  const getMovieStatusMeta = (movie) => {
    const status = String(movie?.status || '').toUpperCase();
    if (status === 'UPCOMING') {
      return {
        label: 'SẮP CHIẾU',
        dot: 'bg-amber-500',
        className: 'bg-amber-950/40 text-amber-400 border-amber-500/30'
      };
    }
    if (status === 'ENDED') {
      return {
        label: 'ĐÃ KẾT THÚC',
        dot: 'bg-sky-400',
        className: 'bg-sky-950/30 text-sky-300 border-sky-500/25'
      };
    }
    if (status === 'INACTIVE') {
      return {
        label: 'NGỪNG CÔNG CHIẾU',
        dot: 'bg-rose-500',
        className: 'bg-rose-950/30 text-rose-300 border-rose-500/30'
      };
    }
    return {
      label: 'ĐANG CHIẾU',
      dot: 'bg-emerald-500',
      className: 'bg-emerald-950/30 text-emerald-400 border-emerald-500/20'
    };
  };

  const renderImagePreview = (src, label, className = 'h-14 w-14') => (
    <div className={`shrink-0 overflow-hidden border border-white/[0.06] bg-neutral-950 ${className}`}>
      {src ? (
        <img
          src={src}
          alt={label}
          className="h-full w-full object-cover"
          referrerPolicy="no-referrer"
        />
      ) : (
        <div className="grid h-full w-full place-items-center text-neutral-300">
          <ImageUp className="h-4 w-4" />
        </div>
      )}
    </div>
  );

  const renderVideoPreview = (src) => (
    <div className="h-11 w-20 shrink-0 overflow-hidden border border-white/[0.06] bg-neutral-950">
      {src ? (
        <video
          src={src}
          className="h-full w-full object-cover"
          muted
          playsInline
          preload="metadata"
        />
      ) : (
        <div className="grid h-full w-full place-items-center text-neutral-300">
          <Video className="h-4 w-4" />
        </div>
      )}
    </div>
  );

  return (
    <>
      {/* TAB 2: MOVIES */}
      {activeTab === 'movies' && (
        <motion.div
          key="panel-movies"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          transition={{ duration: 0.2 }}
          className="space-y-6"
        >

          {/* Filter tools */}
          <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-[#070707] border border-white/[0.05] p-4">

            {/* Custom input search */}
            <div className="relative w-full md:max-w-md">
              <Search className="absolute left-3.5 top-3 h-3.5 w-3.5 text-neutral-200" />
              <input
                type="text"
                placeholder="Tìm kiếm phim theo tiêu đề hoặc đạo diễn..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setAdminMoviePagination((prev) => ({ ...prev, page: 0 }));
                }}
                className="w-full bg-black border border-white/[0.06] focus:border-amber-400 p-2.5 pl-10 text-xs text-white focus:outline-none focus:ring-0 placeholder:text-neutral-400 font-sans"
                id="search-all-movies-input"
              />
            </div>

            {/* Grid categories for status filter */}
            <div className="flex gap-2 w-full md:w-auto" id="film-filter-container">
              <select
                value={adminGenreFilter}
                onChange={(event) => {
                  setAdminGenreFilter(event.target.value);
                  setAdminMoviePagination((prev) => ({ ...prev, page: 0 }));
                }}
                className="border border-white/[0.06] bg-black px-3 py-2 text-[9.5px] font-bold uppercase text-neutral-300 focus:border-amber-400 focus:outline-none"
                aria-label="Lọc phim theo thể loại"
              >
                <option value="">TẤT CẢ THỂ LOẠI</option>
                {genres.map((genre) => (
                  <option key={genre.id} value={genre.id}>{genre.name}</option>
                ))}
              </select>
              {[
                { id: 'ALL', name: 'TẤT CẢ PHIM' },
                { id: 'ACTIVE', name: 'ĐANG PHÁT HÀNH' },
                { id: 'UPCOMING', name: 'SẮP PHÁT HÀNH (SẮP CHIẾU)' }
              ].map((filter) => (
                <button
                  key={filter.id}
                  onClick={() => {
                    playPulseSound(420, 'sine', 0.05);
                    setFilmFilter(filter.id);
                    setAdminMoviePagination((prev) => ({ ...prev, page: 0 }));
                  }}
                  className={`px-3 py-2 text-[9.5px] uppercase font-bold transition-all ${filmFilter === filter.id
                    ? 'bg-amber-500 text-black font-extrabold'
                    : 'bg-black text-neutral-200 border border-white/[0.06] hover:border-white/[0.08]'
                    }`}
                >
                  {filter.name}
                </button>
              ))}

              <button
                onClick={() => {
                  playPulseSound(600, 'sine', 0.1);
                  resetMovieForm();
                  setShowMovieForm(true);
                }}
                className="ml-auto md:ml-0 px-4 py-2 bg-white text-black font-sans uppercase text-[9.5px] font-black tracking-wider hover:bg-neutral-250 transition flex items-center gap-1.5 shrink-0"
              >
                <Plus className="h-4 w-4" /> TẠO PHIM
              </button>
            </div>
          </div>

          {/* MOVIE FORM MODAL (ADD OR EDIT RECORD) */}
          <AnimatePresence>
            {showMovieForm && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="border border-amber-500/20 bg-gradient-to-b from-[#0e0c05] to-[#040404] p-5.5 space-y-4"
              >
                <div className="flex justify-between items-center border-b border-amber-500/20 pb-2">
                  <span className="text-[10px] uppercase font-bold tracking-widest text-amber-400 flex items-center gap-1">
                    <Play className="h-3 w-3" /> THIẾT LẬP HỒ SƠ PHÁT HÀNH CHI TIẾT
                  </span>
                  {editingMovie && (
                    <span className="text-[9px] uppercase text-amber-200 font-mono">
                      Đang chỉnh sửa: {editingMovie.title}
                    </span>
                  )}
                  <button
                    onClick={() => {
                      playPulseSound(300, 'sine', 0.05);
                      resetMovieForm();
                      setShowMovieForm(false);
                    }}
                    className="text-xs text-neutral-300 hover:text-white uppercase font-bold"
                  >
                    HỦY THAO TÁC
                  </button>
                </div>

                <form onSubmit={handleCreateMovieSubmit} className="space-y-4 text-xs font-sans">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black block">Tên tác phẩm (Tiếng Việt viết Hoa)</label>
                      <input
                        type="text"
                        placeholder="VD: CHIẾN BINH ÁNH SÁNG (tối đa 50 ký tự)"
                        maxLength={50}
                        value={formData.title}
                        onChange={(e) => setFormData({ ...formData, title: uppercaseMovieTitle(e.target.value) })}
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400 font-bold"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black block">Tên tiếng Anh hoặc tiêu đề gốc</label>
                      <input
                        type="text"
                        placeholder="VD: Dawn of Light (tối đa 30 ký tự)"
                        maxLength={30}
                        value={formData.englishTitle}
                        onChange={(e) => setFormData({ ...formData, englishTitle: e.target.value })}
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="space-y-1.5 md:col-span-2">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Đạo diễn</label>
                      <div className="relative">
                        <button
                          type="button"
                          onClick={() => setIsDirectorDropdownOpen((open) => !open)}
                          className="flex min-h-11 w-full items-center justify-between gap-3 border border-white/[0.06] bg-black px-3 py-2 text-left text-xs text-white transition hover:border-amber-500/60 focus:outline-none focus:border-amber-400"
                        >
                          <span className="min-w-0 flex-1">
                            {selectedDirectorNames.length ? (
                              <span className="flex flex-wrap gap-1.5">
                                {selectedDirectorNames.slice(0, 4).map((directorName) => (
                                  <span key={directorName} className="inline-flex max-w-full items-center gap-1 border border-amber-500/40 bg-amber-500/10 px-2 py-1 text-[10px] font-bold text-amber-200">
                                    <span className="truncate">{directorName}</span>
                                  </span>
                                ))}
                                {selectedDirectorNames.length > 4 && (
                                  <span className="border border-white/[0.08] bg-black px-2 py-1 text-[10px] font-bold text-neutral-200">
                                    +{selectedDirectorNames.length - 4}
                                  </span>
                                )}
                              </span>
                            ) : (
                              <span className="text-neutral-300">Chọn hoặc nhập đạo diễn</span>
                            )}
                          </span>
                          <ChevronDown className={`h-4 w-4 shrink-0 text-amber-400 transition ${isDirectorDropdownOpen ? 'rotate-180' : ''}`} />
                        </button>

                        {isDirectorDropdownOpen && (
                          <div className="absolute left-0 right-0 z-40 mt-2 border border-amber-500/40 bg-[#050505] shadow-2xl shadow-black/60">
                            <div className="border-b border-white/[0.05] p-2">
                              <div className="flex items-center gap-2 border border-white/[0.06] bg-black px-2">
                                <Search className="h-3.5 w-3.5 text-neutral-300" />
                                <input
                                  type="text"
                                  value={directorPickerSearch}
                                  onChange={(event) => setDirectorPickerSearch(event.target.value)}
                                  onKeyDown={(event) => {
                                    if (event.key !== 'Enter') return;
                                    event.preventDefault();
                                    addDirectorName(directorPickerSearch);
                                  }}
                                  placeholder="Tìm gần đúng hoặc nhập tên đạo diễn rồi Enter..."
                                  className="h-9 min-w-0 flex-1 bg-transparent text-xs text-white placeholder:text-neutral-200 focus:outline-none"
                                  autoFocus
                                />
                              </div>
                              {directorPickerSearch.trim() && !typedDirectorExists && (
                                <button
                                  type="button"
                                  onClick={() => addDirectorName(directorPickerSearch)}
                                  className="mt-2 w-full border border-amber-500/30 bg-amber-500/10 px-2 py-2 text-left text-[10px] font-black uppercase tracking-wider text-amber-300 hover:bg-amber-500 hover:text-black"
                                >
                                  Thêm đạo diễn: {directorPickerSearch.trim()}
                                </button>
                              )}
                            </div>

                            <div className="max-h-56 overflow-y-auto custom-scrollbar p-2">
                              {directorOptions.length ? directorOptions.map((directorName) => {
                                const isSelected = selectedDirectorNames.some((name) => normalizeSearchText(name) === normalizeSearchText(directorName));
                                return (
                                  <div
                                    key={directorName}
                                    className={`mb-1 grid grid-cols-[1fr_auto] items-center gap-2 border px-2 py-2 last:mb-0 ${isSelected ? 'border-amber-500/50 bg-amber-500/10' : 'border-white/[0.05] bg-neutral-950'}`}
                                  >
                                    <button type="button" onClick={() => isSelected ? removeDirectorName(directorName) : addDirectorName(directorName)} className="min-w-0 text-left">
                                      <span className="flex items-center gap-2">
                                        <span className={`grid h-4 w-4 shrink-0 place-items-center border ${isSelected ? 'border-amber-400 bg-amber-400 text-black' : 'border-white/[0.08] text-transparent'}`}>
                                          <Check className="h-3 w-3" />
                                        </span>
                                        <span className="block truncate text-xs font-bold text-white">{directorName}</span>
                                      </span>
                                    </button>
                                    {isSelected && (
                                      <button
                                        type="button"
                                        onClick={() => removeDirectorName(directorName)}
                                        className="h-7 border border-rose-500/30 px-2 text-[9px] font-black uppercase text-rose-300 transition hover:bg-rose-500 hover:text-white"
                                      >
                                        Xóa
                                      </button>
                                    )}
                                  </div>
                                );
                              }) : (
                                <p className="px-2 py-6 text-center text-[10px] text-neutral-300">Không tìm thấy đạo diễn phù hợp.</p>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                      {selectedDirectorNames.length > 0 && (
                        <div className="flex flex-wrap gap-1.5">
                          {selectedDirectorNames.map((directorName) => (
                            <span key={directorName} className="inline-flex items-center gap-1.5 border border-white/[0.08] bg-neutral-950 px-2 py-1 text-[10px] font-bold text-neutral-200">
                              {directorName}
                              <button type="button" onClick={() => removeDirectorName(directorName)} className="text-neutral-300 hover:text-rose-300">×</button>
                            </span>
                          ))}
                        </div>
                      )}
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Thể loại (Ngăn nhau bởi dấu phẩy)</label>
                      <input
                        type="text"
                        placeholder="Chọn ít nhất 1 thể loại bên dưới"
                        value={formData.genre}
                        readOnly
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400"
                      />
                      <div className="mt-2 min-h-[42px] max-h-28 overflow-y-auto bg-black border border-white/[0.06] p-2">
                        {isGenreLoading ? (
                          <span className="text-[10px] text-neutral-300 font-mono">Đang tải thể loại...</span>
                        ) : genres.length > 0 ? (
                          <div className="flex flex-wrap gap-1.5">
                            {genres.map((genre) => {
                              const checked = (formData.genreIds || []).includes(Number(genre.id));
                              return (
                                <button
                                  key={genre.id}
                                  type="button"
                                  onClick={() => toggleMovieGenre(genre.id)}
                                  className={`px-2 py-1 text-[9px] uppercase font-bold border transition ${checked
                                    ? 'border-amber-400 bg-amber-500 text-black'
                                    : 'border-white/[0.06] bg-neutral-950 text-neutral-200 hover:border-white/[0.12] hover:text-white'
                                    }`}
                                >
                                  {genre.name}
                                </button>
                              );
                            })}
                          </div>
                        ) : (
                          <span className="text-[10px] text-rose-400 font-mono">Chưa có thể loại từ API admin/genres</span>
                        )}
                      </div>
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Thời lượng (Số phút)</label>
                      <input
                        type="number"
                        placeholder="60 - 180 phút"
                        min={60}
                        max={180}
                        value={formData.duration}
                        onChange={(e) => setFormData({ ...formData, duration: Number(e.target.value) })}
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400 font-mono"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Độ tuổi phân loại</label>
                      <select
                        value={formData.ageRating}
                        onChange={(e) => setFormData({ ...formData, ageRating: e.target.value })}
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400 font-bold"
                      >
                        <option value="P">P (Mọi lứa tuổi)</option>
                        <option value="T13">T13 (Dưới 13 hạn chế)</option>
                        <option value="T16">T16 (Dưới 16 hạn chế)</option>
                        <option value="T18">T18 (Chỉ dành cho người trưởng thành)</option>
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-sans font-bold uppercase tracking-[0.18em] text-neutral-200 block">Ngày bắt đầu chiếu</label>
                      <input
                        type={focusedDateField === 'release' ? 'date' : 'text'}
                        lang="en-GB"
                        title="Bắt buộc chọn ngày phát hành"
                        min={todayInputValue}
                        value={focusedDateField === 'release' ? formData.releaseDate : formatDateForDisplay(formData.releaseDate)}
                        readOnly={focusedDateField !== 'release'}
                        onFocus={() => setFocusedDateField('release')}
                        onBlur={() => setFocusedDateField(null)}
                        onChange={(e) => {
                          const nextReleaseDate = e.target.value && e.target.value < todayInputValue ? todayInputValue : e.target.value;
                          const shouldMoveEndDate = formData.endDate && new Date(formData.endDate) < new Date(nextReleaseDate);
                          setFormData({
                            ...formData,
                            releaseDate: nextReleaseDate,
                            endDate: shouldMoveEndDate ? nextReleaseDate : formData.endDate
                          });
                        }}
                        placeholder="dd/mm/yyyy"
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400 font-mono [color-scheme:dark]"
                      />
                      <p className="text-[10px] font-sans font-bold text-neutral-300">Ngày phim bắt đầu được xếp suất chiếu. Định dạng dd/mm/yyyy, không chọn ngày quá khứ.</p>
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] font-sans font-bold uppercase tracking-[0.18em] text-neutral-200 block">Ngày kết thúc chiếu</label>
                      <input
                        type={focusedDateField === 'end' ? 'date' : 'text'}
                        lang="en-GB"
                        title="Bắt buộc chọn ngày kết thúc"
                        min={formData.releaseDate || todayInputValue}
                        value={focusedDateField === 'end' ? (formData.endDate || '') : formatDateForDisplay(formData.endDate)}
                        readOnly={focusedDateField !== 'end'}
                        onFocus={() => setFocusedDateField('end')}
                        onBlur={() => setFocusedDateField(null)}
                        onChange={(e) => {
                          const minEndDate = formData.releaseDate || todayInputValue;
                          const nextEndDate = e.target.value && e.target.value < minEndDate ? minEndDate : e.target.value;
                          setFormData({ ...formData, endDate: nextEndDate });
                        }}
                        placeholder="dd/mm/yyyy"
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400 font-mono [color-scheme:dark]"
                      />
                      {formData.releaseDate && formData.endDate && new Date(formData.endDate) < new Date(formData.releaseDate) ? (
                        <p className="text-[9px] font-sans font-bold text-rose-300">Ngày kết thúc chiếu phải bằng hoặc sau ngày bắt đầu chiếu.</p>
                      ) : (
                        <p className="text-[9px] font-sans font-bold text-amber-300">
                          Suất chiếu chỉ xếp được trong khoảng này · Trạng thái tự tính: {resolveMovieStatusFromDates(formData.releaseDate, formData.endDate)}
                        </p>
                      )}
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Ngôn ngữ</label>
                      <input
                        type="text"
                        placeholder="VD: Tiếng Việt (tối đa 30 ký tự)"
                        maxLength={30}
                        value={formData.language}
                        onChange={(e) => setFormData({ ...formData, language: e.target.value })}
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Phụ đề</label>
                      <input
                        type="text"
                        placeholder="VD: EN Sub (tối đa 30 ký tự)"
                        maxLength={30}
                        value={formData.subtitleLanguage}
                        onChange={(e) => setFormData({ ...formData, subtitleLanguage: e.target.value })}
                        className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2 space-y-2 border border-white/[0.06] bg-black p-3">
                      <div className="flex items-center justify-between gap-3">
                        <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Chọn diễn viên và vai chính</label>
                        <span className="text-[9px] text-neutral-300">
                          {selectedActorIds.length} diễn viên, {selectedMainActorIds.length} vai chính
                        </span>
                      </div>

                      <div className="relative">
                        <button
                          type="button"
                          onClick={() => setIsActorDropdownOpen((open) => !open)}
                          className="flex min-h-11 w-full items-center justify-between gap-3 border border-white/[0.06] bg-neutral-950 px-3 py-2 text-left text-xs text-white transition hover:border-amber-500/60 focus:outline-none focus:border-amber-400"
                        >
                          <span className="min-w-0 flex-1">
                            {selectedActors.length ? (
                              <span className="flex flex-wrap gap-1.5">
                                {selectedActors.slice(0, 5).map((actor) => {
                                  const actorId = Number(actor.id);
                                  const isMain = selectedMainActorIds.includes(actorId);
                                  return (
                                    <span key={actor.id} className={`inline-flex max-w-full items-center gap-1 border px-2 py-1 text-[10px] font-bold ${isMain ? 'border-amber-400 bg-amber-400 text-black' : 'border-white/[0.08] bg-black text-neutral-300'}`}>
                                      <span className="truncate">{actor.name}</span>
                                      {isMain && <span className="text-[8px] uppercase">Main</span>}
                                    </span>
                                  );
                                })}
                                {selectedActors.length > 5 && (
                                  <span className="border border-white/[0.08] bg-black px-2 py-1 text-[10px] font-bold text-neutral-200">
                                    +{selectedActors.length - 5}
                                  </span>
                                )}
                              </span>
                            ) : (
                              <span className="text-neutral-300">Chọn diễn viên cho phim</span>
                            )}
                          </span>
                          <ChevronDown className={`h-4 w-4 shrink-0 text-amber-400 transition ${isActorDropdownOpen ? 'rotate-180' : ''}`} />
                        </button>

                        {isActorDropdownOpen && (
                          <div className="absolute left-0 right-0 z-30 mt-2 border border-amber-500/40 bg-[#050505] shadow-2xl shadow-black/60">
                            <div className="border-b border-white/[0.05] p-2">
                              <div className="flex items-center gap-2 border border-white/[0.06] bg-black px-2">
                                <Search className="h-3.5 w-3.5 text-neutral-300" />
                                <input
                                  type="text"
                                  value={actorPickerSearch}
                                  onChange={(event) => setActorPickerSearch(event.target.value)}
                                  placeholder="Tìm gần đúng tên diễn viên, tiểu sử hoặc ID..."
                                  className="h-9 min-w-0 flex-1 bg-transparent text-xs text-white placeholder:text-neutral-200 focus:outline-none"
                                  autoFocus
                                />
                              </div>
                            </div>

                            <div className="max-h-64 overflow-y-auto custom-scrollbar p-2">
                              {actorPickerOptions.length ? actorPickerOptions.map((actor) => {
                                const actorId = Number(actor.id);
                                const isSelected = selectedActorIds.includes(actorId);
                                const isMain = selectedMainActorIds.includes(actorId);
                                return (
                                  <div
                                    key={actor.id}
                                    className={`mb-1 grid grid-cols-[1fr_auto_auto] items-center gap-2 border px-2 py-2 last:mb-0 ${isSelected ? 'border-amber-500/50 bg-amber-500/10' : 'border-white/[0.05] bg-neutral-950'}`}
                                  >
                                    <button type="button" onClick={() => toggleMovieActor(actorId)} className="min-w-0 text-left">
                                      <span className="flex items-center gap-2">
                                        <span className={`grid h-4 w-4 shrink-0 place-items-center border ${isSelected ? 'border-amber-400 bg-amber-400 text-black' : 'border-white/[0.08] text-transparent'}`}>
                                          <Check className="h-3 w-3" />
                                        </span>
                                        <span className="min-w-0">
                                          <span className="block truncate text-xs font-bold text-white">{actor.name}</span>
                                          <span className="block truncate text-[9px] text-neutral-300">#{actor.id} · {actor.movieCount || 0} phim</span>
                                        </span>
                                      </span>
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() => toggleMovieMainActor(actorId)}
                                      className={`h-7 px-2 text-[9px] font-black uppercase border ${isMain ? 'border-amber-400 bg-amber-400 text-black' : 'border-white/[0.08] text-neutral-200 hover:border-amber-400 hover:text-amber-300'}`}
                                    >
                                      Vai chính
                                    </button>
                                    {isSelected && (
                                      <button
                                        type="button"
                                        onClick={() => toggleMovieActor(actorId)}
                                        className="h-7 border border-rose-500/30 px-2 text-[9px] font-black uppercase text-rose-300 transition hover:bg-rose-500 hover:text-white"
                                        title="Bỏ chọn diễn viên khỏi phim"
                                      >
                                        Xóa
                                      </button>
                                    )}
                                  </div>
                                );
                              }) : (
                                <p className="px-2 py-6 text-center text-[10px] text-neutral-300">Không tìm thấy diễn viên phù hợp.</p>
                              )}
                            </div>
                          </div>
                        )}
                      </div>

                      {selectedActors.length > 0 && (
                        <div className="flex flex-wrap gap-1.5">
                          {selectedActors.map((actor) => {
                            const actorId = Number(actor.id);
                            const isMain = selectedMainActorIds.includes(actorId);
                            return (
                              <span key={actor.id} className={`inline-flex items-center gap-1.5 border px-2 py-1 text-[10px] font-bold ${isMain ? 'border-amber-400 bg-amber-400 text-black' : 'border-white/[0.08] bg-neutral-950 text-neutral-200'}`}>
                                {actor.name}
                                <button type="button" onClick={() => toggleMovieActor(actorId)} className={isMain ? 'text-black/70 hover:text-black' : 'text-neutral-300 hover:text-rose-300'}>×</button>
                              </span>
                            );
                          })}
                        </div>
                      )}
                    </div>

                    {/* <div className="space-y-2 border border-white/[0.06] bg-black p-3">
                      <div className="grid grid-cols-1 sm:grid-cols-[1fr_1fr_1fr_auto] gap-2">
                        <input
                          type="text"
                          placeholder="Tên actor"
                          value={actorForm.name}
                          onChange={(e) => setActorForm({ ...actorForm, name: e.target.value })}
                          className="bg-neutral-950 border border-white/[0.06] p-2 text-xs text-white focus:outline-none focus:border-amber-400"
                        />
                        <input
                          type="text"
                          placeholder="Tiểu sử"
                          value={actorForm.biography}
                          onChange={(e) => setActorForm({ ...actorForm, biography: e.target.value })}
                          className="bg-neutral-950 border border-white/[0.06] p-2 text-xs text-white focus:outline-none focus:border-amber-400"
                        />
                        <label className={`flex cursor-pointer items-center justify-center gap-2 border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-widest text-amber-300 transition hover:bg-amber-500 hover:text-black ${isActorImageUploading ? 'pointer-events-none opacity-60' : ''}`}>
                          {isActorImageUploading ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <ImageUp className="h-3.5 w-3.5" />}
                          {isActorImageUploading ? 'Đang tải ảnh...' : actorForm.avatarUrl ? 'Đã chọn ảnh local' : 'Chọn ảnh local'}
                          <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleQuickActorImageUpload} className="hidden" />
                        </label>
                        {renderImagePreview(actorForm.avatarUrl, 'Ảnh actor', 'h-10 w-10')}
                      </div>
                      <button
                        type="button"
                        onClick={handleQuickCreateActor}
                        disabled={isActorSaving || isActorImageUploading}
                        className="w-full border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-widest text-amber-300 hover:bg-amber-500 hover:text-black disabled:opacity-50"
                      >
                        {isActorImageUploading ? 'Đang tải ảnh actor...' : isActorSaving ? 'Đang tạo actor...' : 'Tạo actor và gán ID vào phim'}
                      </button>
                      {createdActors.length > 0 && (
                        <div className="text-[9px] text-neutral-200 font-mono">
                          Actor vừa tạo: {createdActors.slice(0, 3).map((actor) => `${actor.name}#${actor.id}`).join(', ')}
                        </div>
                      )}
                    </div> */}

                    <div className="md:col-span-2 grid grid-cols-1 lg:grid-cols-3 gap-3 border border-white/[0.05] bg-neutral-950/40 p-3">
                      <div className="space-y-1.5">
                        <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Trailer *</label>
                        <div className="flex items-center gap-2">
                          <label className={`flex h-11 min-w-0 flex-1 cursor-pointer items-center justify-center gap-1.5 border border-amber-500/40 bg-black px-2 text-[9px] font-black uppercase tracking-wider text-amber-300 transition hover:bg-amber-500 hover:text-black ${isTrailerUploading ? 'pointer-events-none opacity-60' : ''}`}>
                            {isTrailerUploading ? <RefreshCw className="h-3 w-3 animate-spin shrink-0" /> : <Video className="h-3 w-3 shrink-0" />}
                            <span className="truncate">{isTrailerUploading ? 'Đang tải...' : formData.trailerUrl ? 'Đã chọn video' : 'Chọn video'}</span>
                            <input
                              type="file"
                              accept="video/mp4,video/webm,video/quicktime"
                              onChange={handleTrailerVideoUpload}
                              className="hidden"
                            />
                          </label>
                          {renderVideoPreview(formData.trailerUrl)}
                        </div>
                        <p className="text-[8.5px] text-neutral-300">MP4, WEBM, MOV.</p>
                      </div>

                      <div className="space-y-1.5">
                        <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Poster đứng *</label>
                        <div className="flex items-center gap-2">
                          <label className={`flex h-11 min-w-0 flex-1 cursor-pointer items-center justify-center gap-1.5 border border-amber-500/40 bg-black px-2 text-[9px] font-black uppercase tracking-wider text-amber-300 transition hover:bg-amber-500 hover:text-black ${isPosterUploading ? 'pointer-events-none opacity-60' : ''}`}>
                            {isPosterUploading ? <RefreshCw className="h-3 w-3 animate-spin shrink-0" /> : <ImageUp className="h-3 w-3 shrink-0" />}
                            <span className="truncate">{isPosterUploading ? 'Đang tải...' : formData.posterUrl ? 'Đã chọn poster' : 'Chọn poster'}</span>
                            <input
                              type="file"
                              accept="image/jpeg,image/png,image/webp"
                              onChange={(event) => handleMovieImageUpload('posterUrl', 'movies/posters', event)}
                              className="hidden"
                            />
                          </label>
                          {renderImagePreview(formData.posterUrl, 'Poster phim', 'h-14 w-10')}
                        </div>
                      </div>

                      <div className="space-y-1.5">
                        <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 block">Banner ngang *</label>
                        <div className="flex items-center gap-2">
                          <label className={`flex h-11 min-w-0 flex-1 cursor-pointer items-center justify-center gap-1.5 border border-amber-500/40 bg-black px-2 text-[9px] font-black uppercase tracking-wider text-amber-300 transition hover:bg-amber-500 hover:text-black ${isBannerUploading ? 'pointer-events-none opacity-60' : ''}`}>
                            {isBannerUploading ? <RefreshCw className="h-3 w-3 animate-spin shrink-0" /> : <ImageUp className="h-3 w-3 shrink-0" />}
                            <span className="truncate">{isBannerUploading ? 'Đang tải...' : formData.bannerUrl ? 'Đã chọn banner' : 'Chọn banner'}</span>
                            <input
                              type="file"
                              accept="image/jpeg,image/png,image/webp"
                              onChange={(event) => handleMovieImageUpload('bannerUrl', 'movies/banners', event)}
                              className="hidden"
                            />
                          </label>
                          {renderImagePreview(formData.bannerUrl, 'Banner phim', 'h-12 w-20')}
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] uppercase tracking-wider text-neutral-200 block">Tóm tắt cốt truyện cốt lõi</label>
                    <textarea
                      rows={3}
                      maxLength={1000}
                      value={formData.synopsis}
                      onChange={(e) => setFormData({ ...formData, synopsis: e.target.value })}
                      placeholder="Tóm tắt nội dung phim, tối đa 1000 ký tự"
                      className="w-full bg-black border border-white/[0.06] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400 leading-relaxed"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isMovieSaving || isMovieMediaUploading}
                    className="w-full py-4.5 bg-amber-500 hover:bg-amber-400 text-black font-sans font-black text-xs uppercase tracking-widest transition shadow-lg disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isMovieMediaUploading
                      ? 'ĐANG TẢI FILE LOCAL...'
                      : isMovieSaving
                        ? (editingMovie ? 'ĐANG CHUẨN BỊ CẬP NHẬT PHIM...' : 'ĐANG CHUẨN BỊ CẬP NHẬT PHIM...')
                        : (editingMovie ? 'CẬP NHẬT BẢN GHI PHIM' : 'GHI BẢN GHI PHIM & PHÁT HÀNH TRÊN CỔNG TRỰC TUYẾN')}
                  </button>
                </form>
              </motion.div>
            )}
          </AnimatePresence>

          {/* LIST OF MOVIES IN DATABASE - Pristine table layout */}
          <div className="border border-white/[0.05] bg-neutral-950 overflow-x-auto shadow-md">
            <table className="w-full text-left border-collapse min-w-[700px]">
              <thead>
                <tr className="border-b border-white/[0.05] bg-black text-neutral-300 text-[9.5px] uppercase font-bold tracking-wider">
                  <th className="py-3 px-4">Hình ảnh</th>
                  <th className="py-3 px-4">Tên phim & Thời lượng</th>
                  <th className="py-3 px-4">Thể loại</th>
                  <th className="py-3 px-4">Đạo diễn</th>
                  <th className="py-3 px-4">Trạng thái phát hành</th>
                  <th className="py-3 px-4 text-right">Thao tác dữ liệu</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/[0.03] text-xs">
                {filteredMovies.map(mv => (
                  <tr key={mv.id} className="hover:bg-white/[0.02] transition-all">
                    <td className="py-3.5 px-4 shrink-0">
                      <img
                        src={mv.posterUrl}
                        alt={mv.title}
                        className="w-10 h-14 object-cover border border-white/[0.08]"
                        referrerPolicy="no-referrer"
                      />
                    </td>
                    <td className="py-3.5 px-4 font-sans">
                      <div className="font-sans text-sm text-white font-bold truncate max-w-xs">{mv.title}</div>
                      <div className="text-[10px] text-neutral-300 truncate max-w-xs">{mv.englishTitle}</div>
                      <div className="flex gap-2 items-center mt-1">
                        <span className="text-[9.5px] border border-white/[0.05] bg-[#060606] px-1.5 py-0.5 text-neutral-200 font-mono">{mv.duration} phút</span>
                        <span className={`text-[9.5px] px-1.5 py-0.5 font-bold ${mv.ageRating === 'T18' ? 'bg-red-950/20 text-red-400 border border-red-500/20' : 'bg-neutral-900 text-neutral-200'
                          }`}>{mv.ageRating}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="text-[10.5px] text-neutral-200 font-medium">{mv.genre.join(' • ')}</span>
                    </td>
                    <td className="py-3.5 px-4 text-neutral-200 font-semibold">
                      {mv.director}
                    </td>
                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="flex flex-col items-start gap-2">
                        <div className="flex items-center gap-2">
                          {mv.status === 'INACTIVE' ? (
                            <span className="inline-flex items-center px-2 py-1 bg-rose-950/30 text-rose-300 border border-rose-500/30 text-[9px] uppercase font-bold tracking-wider rounded-sm select-none shrink-0 h-6">
                              <span className="w-1.5 h-1.5 rounded-full bg-rose-500 mr-1.5"></span>
                              NGỪNG CÔNG CHIẾU
                            </span>
                          ) : mv.status === 'ENDED' ? (
                            <span className="inline-flex items-center px-2 py-1 bg-sky-950/30 text-sky-300 border border-sky-500/25 text-[9px] uppercase font-bold tracking-wider rounded-sm select-none shrink-0 h-6">
                              <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mr-1.5"></span>
                              ĐÃ KẾT THÚC
                            </span>
                          ) : mv.status === 'UPCOMING' ? (
                            <span className="inline-flex items-center px-2 py-1 bg-amber-950/40 text-amber-400 border border-amber-500/30 text-[9px] uppercase font-bold tracking-wider rounded-sm select-none shrink-0 h-6">
                              <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mr-1.5 animate-pulse"></span>
                              SẮP CHIẾU
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2 py-1 bg-emerald-950/30 text-emerald-400 border border-emerald-500/20 text-[9px] uppercase font-bold tracking-wider rounded-sm select-none shrink-0 h-6">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5 animate-pulse"></span>
                              ĐANG CHIẾU
                            </span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => onSelectMovie(mv.id)}
                          className="p-1 px-2 text-[9px] border border-white/[0.06] hover:border-white transition uppercase font-sans text-neutral-200 hover:text-white"
                          title="Xem trang chi tiết bên ngoài"
                        >
                          Xem trang
                        </button>
                        <button
                          onClick={() => handleEditMovie(mv)}
                          disabled={mv.status === 'INACTIVE' || mv.isInactive}
                          className="p-1.5 text-amber-400 hover:bg-amber-950/10 border border-transparent hover:border-amber-500/30 transition shadow-sm disabled:cursor-not-allowed disabled:opacity-35 disabled:hover:bg-transparent disabled:hover:border-transparent"
                          title="Chỉnh sửa phim"
                        >
                          <Edit3 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          onClick={() => handleDeleteMovie(mv)}
                          className="p-1.5 text-rose-400 hover:bg-rose-950/10 border border-transparent hover:border-rose-500/30 transition shadow-sm"
                          title="Tước quyền phát hành phim"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {filteredMovies.length === 0 && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-neutral-300 font-mono uppercase tracking-wider">
                      Không tìm thấy bản ghi phim phù hợp tiêu chí truy vấn
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border border-white/[0.05] bg-black p-3 text-[10px] uppercase tracking-[0.16em] text-neutral-200">
            <span>
              Tổng {adminMoviePagination.totalElements} phim - Trang {adminMoviePagination.page + 1}/{adminMoviePagination.totalPages}
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={adminMoviePagination.page <= 0}
                onClick={() => setAdminMoviePagination((prev) => ({ ...prev, page: Math.max(0, prev.page - 1) }))}
                className="border border-white/[0.06] px-3 py-2 text-white disabled:opacity-30 hover:border-white transition"
              >
                Trước
              </button>
              <button
                type="button"
                disabled={adminMoviePagination.page + 1 >= adminMoviePagination.totalPages}
                onClick={() => setAdminMoviePagination((prev) => ({ ...prev, page: Math.min(prev.totalPages - 1, prev.page + 1) }))}
                className="border border-white/[0.06] px-3 py-2 text-white disabled:opacity-30 hover:border-white transition"
              >
                Sau
              </button>
            </div>
          </div>
        </motion.div>
      )}
    </>
  );
}


