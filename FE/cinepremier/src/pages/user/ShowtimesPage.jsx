/* Hallmark · page: showtime finder · genre: atmospheric utility · theme: CinePremier dark
 * interaction: filter · select · clear · choose-showtime
 * contrast: pass · Pre-emit critique: P5 · H5 · E5 · S5 · R5 · V4
 */
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, CalendarDays, Check, ChevronDown, Clock, Loader2, Search, ShieldCheck, Tag, Users, X } from 'lucide-react';
import { bookingService } from '../../services/bookingService';
import { useMovies } from '../../stores/useMovieStore';

const toDateKey = (date) => {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

const SHOWTIME_CACHE_TTL_MS = 90 * 1000;
const showtimeCacheByDate = new Map();

const getFreshShowtimes = (dateKey) => {
  const entry = showtimeCacheByDate.get(dateKey);
  if (!entry) return null;
  return Date.now() - entry.updatedAt < SHOWTIME_CACHE_TTL_MS ? entry.data : null;
};

const filterVisibleShowtimes = (rawList) => rawList.filter((st) =>
  (st.status === 'OPEN' || st.status === 'SCHEDULED')
  && new Date(st.startTime) > new Date()
);

const fetchShowtimesForDate = async (dateKey) => {
  const cached = getFreshShowtimes(dateKey);
  if (cached) return cached;
  const data = await bookingService.getShowtimes({ date: dateKey, size: 100 });
  const rawList = Array.isArray(data) ? data : (data?.items ?? data?.content ?? []);
  showtimeCacheByDate.set(dateKey, { data: rawList, updatedAt: Date.now() });
  return rawList;
};

const today = new Date();
const todayKey = toDateKey(today);

const PREFETCH_DATES = Array.from({ length: 7 }, (_, i) => {
  const d = new Date();
  d.setDate(d.getDate() + i);
  return toDateKey(d);
});

const AGE_RATING_OPTIONS = [
  { value: null, label: 'Không lọc độ tuổi', description: 'Hiển thị mọi suất chiếu' },
  { value: 'P', label: 'Mọi độ tuổi', description: 'Phù hợp mọi khán giả' },
  { value: 'T13', label: 'Từ đủ 13 tuổi', description: 'Người xem từ đủ 13 tuổi' },
  { value: 'T16', label: 'Từ đủ 16 tuổi', description: 'Người xem từ đủ 16 tuổi' },
  { value: 'T18', label: 'Từ đủ 18 tuổi', description: 'Người xem từ đủ 18 tuổi' },
];

const AGE_RATING_COLOR = {
  P:   'border-emerald-500/40 bg-emerald-500/10 text-emerald-400',
  T13: 'border-yellow-500/40 bg-yellow-500/10 text-yellow-400',
  T16: 'border-orange-500/40 bg-orange-500/10 text-orange-400',
  T18: 'border-rose-500/40 bg-rose-500/10 text-rose-400',
};
const getAgeColor = (rating) => AGE_RATING_COLOR[String(rating || '').toUpperCase()] || 'border-neutral-700 bg-neutral-900 text-neutral-400';

// ─── Dropdown thể loại ───────────────────────────────────────────────────────
function GenreDropdown({ genres, selectedGenre, onChange }) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const ref = useRef(null);

  useEffect(() => {
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const filtered = useMemo(() => {
    if (!search.trim()) return genres;
    const q = search.trim().toLowerCase();
    return genres.filter((g) => g.toLowerCase().includes(q));
  }, [genres, search]);

  return (
    <div ref={ref} className={`relative ${open ? 'z-[120]' : 'z-0'}`}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-label={`Thể loại: ${selectedGenre || 'Tất cả thể loại'}`}
        className={`flex h-[60px] w-full cursor-pointer items-center gap-2 border bg-neutral-950 py-0 pl-10 pr-4 text-left text-sm transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-300
          ${open ? 'border-purple-400 ring-1 ring-purple-500/40' : 'border-white/10 hover:border-white/20'}`}
      >
        <Tag className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-purple-400" />
        <span className={`flex-1 truncate ${selectedGenre ? 'text-white' : 'text-neutral-500'}`}>
          {selectedGenre || 'Thể loại'}
        </span>
        {selectedGenre ? (
          <X className="h-3.5 w-3.5 shrink-0 text-neutral-400 hover:text-white"
            onClick={(e) => { e.stopPropagation(); onChange(null); setSearch(''); }} />
        ) : (
          <ChevronDown className={`h-3.5 w-3.5 shrink-0 text-neutral-500 transition-transform ${open ? 'rotate-180 text-purple-300' : ''}`} />
        )}
      </button>

      {open && (
        <div className="absolute inset-x-0 z-[130] mt-2 w-full border border-white/15 border-t-2 border-t-purple-400 bg-neutral-950 shadow-2xl shadow-black/70">
          <div className="relative border-b border-white/10">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-neutral-500" />
            <input
              autoFocus
              type="text"
              aria-label="Tìm thể loại"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm thể loại…"
              className="w-full bg-transparent py-2.5 pl-9 pr-4 text-xs text-white placeholder-neutral-600 outline-none"
            />
          </div>
          <div className="max-h-52 overflow-y-auto">
            <button
              onClick={() => { onChange(null); setOpen(false); setSearch(''); }}
              className={`flex w-full items-center px-4 py-2.5 text-xs font-bold uppercase tracking-widest transition
                ${!selectedGenre ? 'bg-purple-500/15 text-purple-300' : 'text-neutral-400 hover:bg-white/5 hover:text-white'}`}
            >Tất cả thể loại</button>
            {genres.length === 0 ? (
              <p className="py-6 text-center text-[11px] text-neutral-600">Chưa có dữ liệu thể loại</p>
            ) : filtered.length === 0 ? (
              <p className="py-6 text-center text-[11px] text-neutral-600">Không tìm thấy thể loại</p>
            ) : (
              filtered.map((g) => (
                <button
                  key={g}
                  onClick={() => { onChange(selectedGenre === g ? null : g); setOpen(false); setSearch(''); }}
                  className={`flex w-full items-center justify-between px-4 py-2.5 text-xs font-bold uppercase tracking-widest transition
                    ${selectedGenre === g ? 'bg-purple-500/15 text-purple-300' : 'text-neutral-400 hover:bg-white/5 hover:text-white'}`}
                >
                  <span>{g}</span>
                  {selectedGenre === g && <span className="h-1.5 w-1.5 rounded-full bg-purple-400" />}
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Dropdown độ tuổi ────────────────────────────────────────────────────────
function AgeRatingDropdown({ selectedAge, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    const closeOnEscape = (e) => { if (e.key === 'Escape') setOpen(false); };
    document.addEventListener('mousedown', handler);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('mousedown', handler);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, []);

  const selected = AGE_RATING_OPTIONS.find((o) => o.value === selectedAge) || AGE_RATING_OPTIONS[0];

  return (
    <div ref={ref} className={`relative min-w-0 ${open ? 'z-[120]' : 'z-0'}`}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls="age-rating-options"
        aria-label={`Phân loại độ tuổi: ${selected.label}`}
        className={`flex h-[60px] min-h-0 w-full cursor-pointer items-center gap-3 border bg-neutral-950 px-4 text-left transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-300 active:bg-purple-500/10 disabled:cursor-not-allowed disabled:opacity-50
          ${open ? 'border-purple-400 bg-purple-500/[0.04]' : 'border-white/10 hover:border-purple-400/40'}`}
      >
        <span className={`flex h-8 w-8 shrink-0 items-center justify-center border transition-colors ${open || selectedAge ? 'border-purple-400/50 bg-purple-500/15 text-purple-200' : 'border-purple-500/25 bg-purple-950/20 text-purple-400'}`}>
          <Users className="h-4 w-4" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-[8px] font-black uppercase tracking-[0.2em] text-neutral-500">Độ tuổi</span>
          <span className={`mt-0.5 block truncate text-[15px] font-extrabold leading-tight ${selectedAge ? 'text-white' : 'text-neutral-300'}`}>
            {selectedAge ? `${selectedAge} · ${selected.label}` : selected.label}
          </span>
        </span>
        <ChevronDown className={`h-4 w-4 shrink-0 text-neutral-500 transition-transform ${open ? 'rotate-180 text-purple-300' : ''}`} />
      </button>

      {open && (
        <div className="absolute inset-x-0 z-[130] mt-2 w-full overflow-hidden border border-white/15 border-t-2 border-t-purple-400 bg-neutral-950 shadow-2xl shadow-black/70">
          <div className="flex h-12 items-center gap-3 border-b border-white/10 bg-purple-500/[0.05] px-3.5">
            <ShieldCheck className="h-4 w-4 shrink-0 text-purple-300" aria-hidden="true" />
            <p className="min-w-0 flex-1 text-[11px] font-black uppercase tracking-[0.16em] text-white">Chọn độ tuổi</p>
          </div>
          <div id="age-rating-options" role="listbox" aria-label="Phân loại độ tuổi" className="p-1.5">
            {AGE_RATING_OPTIONS.map((opt) => (
              <button
                type="button"
                key={opt.value ?? '__all'}
                onClick={() => { onChange(opt.value); setOpen(false); }}
                role="option"
                aria-selected={selectedAge === opt.value}
                className={`group flex min-h-[52px] w-full cursor-pointer items-center gap-3 border-b border-white/[0.06] px-2.5 py-2 text-left transition-colors last:border-b-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-purple-300 active:bg-purple-500/15 disabled:cursor-not-allowed disabled:opacity-50
                  ${selectedAge === opt.value
                    ? 'bg-purple-500/15'
                    : 'hover:bg-white/[0.04]'}`}
              >
                <span className={`flex h-8 w-10 shrink-0 items-center justify-center border text-[9px] font-black ${opt.value ? getAgeColor(opt.value) : selectedAge === null ? 'border-purple-400/40 bg-purple-500/10 text-purple-300' : 'border-white/10 text-neutral-500'}`}>
                  {opt.value || 'ALL'}
                </span>
                <span className="min-w-0 flex-1">
                  <span className={`block break-words text-[13px] font-extrabold leading-4 ${selectedAge === opt.value ? 'text-purple-100' : 'text-neutral-200 group-hover:text-white'}`}>{opt.label}</span>
                  <span className="mt-0.5 block whitespace-normal text-[10px] leading-4 text-neutral-500">{opt.description}</span>
                </span>
                <span className={`flex h-7 w-7 shrink-0 items-center justify-center ${selectedAge === opt.value ? 'bg-purple-500/15 text-purple-200' : 'text-transparent'}`}>
                  <Check className="h-3.5 w-3.5" />
                </span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Trang chính ─────────────────────────────────────────────────────────────
export default function ShowtimesPage() {
  const navigate = useNavigate();
  const { moviesList = [], fetchMoviesPage, isMoviesLoading } = useMovies();

  const [keyword, setKeyword] = useState('');
  const [selectedDate, setSelectedDate] = useState(todayKey);
  const [selectedGenre, setSelectedGenre] = useState(null);
  const [selectedAge, setSelectedAge] = useState(null);

  const [showtimes, setShowtimes] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  // Fetch suất chiếu khi ngày thay đổi
  useEffect(() => {
    let cancelled = false;

    const cached = getFreshShowtimes(selectedDate);
    if (cached) {
      setShowtimes(filterVisibleShowtimes(cached));
      setIsLoading(false);
      return () => { cancelled = true; };
    }

    setIsLoading(true);
    fetchShowtimesForDate(selectedDate)
      .then((rawList) => { if (!cancelled) setShowtimes(filterVisibleShowtimes(rawList)); })
      .catch(() => { if (!cancelled) setShowtimes([]); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, [selectedDate]);

  // Prefetch nền 7 ngày tới
  useEffect(() => {
    let cancelled = false;
    (async () => {
      for (const dateKey of PREFETCH_DATES) {
        if (cancelled) return;
        try { await fetchShowtimesForDate(dateKey); } catch { /* ignore */ }
      }
    })();
    return () => { cancelled = true; };
  }, []);

  // Auto-fetch movies nếu store chưa có dữ liệu (để lấy poster, duration)
  useEffect(() => {
    if (moviesList.length === 0) {
      fetchMoviesPage({ isExplorePage: false }).catch(() => {});
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Build lookup maps để join showtime với movie — chỉ dùng cho poster/duration
  const movieLookupById = useMemo(() => {
    const map = new Map();
    moviesList.forEach((m) => {
      const bid = String(m.backendId ?? m.id ?? '');
      if (bid) map.set(bid, m);
    });
    return map;
  }, [moviesList]);

  const movieLookupByTitle = useMemo(() => {
    const map = new Map();
    moviesList.forEach((m) => {
      const t = String(m.title || m.englishTitle || '').toLowerCase().trim();
      if (t) map.set(t, m);
    });
    return map;
  }, [moviesList]);

  // Tổng hợp genres từ showtimes (đã có trong response từ BE)
  const genreOptions = useMemo(() => {
    const set = new Set();
    showtimes.forEach((st) => {
      if (Array.isArray(st.movieGenreNames)) {
        st.movieGenreNames.forEach((g) => {
          if (g && g.trim()) set.add(g.trim().toUpperCase());
        });
      }
    });
    // Fallback: also pull from moviesList store if showtimes don't have genre data yet
    if (set.size === 0) {
      moviesList.forEach((m) => {
        const gList = Array.isArray(m.genre) ? m.genre : [];
        gList.forEach((g) => {
          const name = (typeof g === 'string' ? g : g?.name || '').trim();
          if (name && name !== 'Dang cap nhat') set.add(name.toUpperCase());
        });
      });
    }
    return [...set].sort();
  }, [showtimes, moviesList]);

  const movieGroups = useMemo(() => {
    const groups = new Map();
    showtimes.forEach((st) => {
      if (!groups.has(st.movieId))
        groups.set(st.movieId, {
          movieId: st.movieId,
          movieTitle: st.movieTitle,
          // genre & ageRating come directly from ShowtimeResponse (BE-side)
          ageRating: st.movieAgeRating || null,
          movieGenres: Array.isArray(st.movieGenreNames)
            ? st.movieGenreNames.filter(Boolean)
            : [],
          slots: [],
        });
      groups.get(st.movieId).slots.push(st);
    });

    const allGroups = [...groups.values()].map((group) => {
      // Join with store only to get poster & duration
      const movie = movieLookupById.get(String(group.movieId))
        || movieLookupByTitle.get(String(group.movieTitle || '').toLowerCase().trim());

      return {
        ...group,
        routeId: movie?.backendId ?? movie?.id ?? group.movieId,
        poster: movie?.posterUrl,
        duration: movie?.durationMinutes || movie?.duration,
        movieFound: Boolean(movie),
        slots: group.slots.sort((a, b) => new Date(a.startTime) - new Date(b.startTime)),
      };
    }).sort((a, b) => String(a.movieTitle).localeCompare(String(b.movieTitle), 'vi'));

    let filtered = allGroups;
    if (keyword.trim()) {
      const kw = keyword.trim().toLowerCase();
      filtered = filtered.filter((g) => g.movieTitle?.toLowerCase().includes(kw));
    }
    if (selectedGenre) {
      filtered = filtered.filter((g) =>
        g.movieGenres.some((genre) => genre.toUpperCase() === selectedGenre.toUpperCase())
      );
    }
    if (selectedAge) {
      filtered = filtered.filter((g) =>
        String(g.ageRating || '').toUpperCase() === selectedAge.toUpperCase()
      );
    }

    return filtered;
  }, [showtimes, movieLookupById, movieLookupByTitle, keyword, selectedGenre, selectedAge]);

  const formatTime = (value) =>
    new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

  const getMovieEndTime = (showtime, duration) => {
    const start = showtime?.startTime ? new Date(showtime.startTime) : null;
    const durationMinutes = Number(duration);
    if (start && !Number.isNaN(start.getTime()) && durationMinutes > 0) {
      return new Date(start.getTime() + durationMinutes * 60 * 1000);
    }

    const fallbackEnd = showtime?.endTime ? new Date(showtime.endTime) : null;
    return fallbackEnd && !Number.isNaN(fallbackEnd.getTime()) ? fallbackEnd : null;
  };

  const hasActiveFilters = keyword.trim() || selectedGenre || selectedAge;
  const visibleShowtimeCount = movieGroups.reduce((total, group) => total + group.slots.length, 0);

  const clearAllFilters = () => { setKeyword(''); setSelectedGenre(null); setSelectedAge(null); };

  return (
    <div className="mx-auto max-w-5xl space-y-6 overflow-x-clip px-4 py-8 sm:px-6">
      {/* Tiêu đề */}
      <div>
        <p className="text-[10px] font-mono font-black uppercase tracking-[0.3em] text-purple-400">
          Tìm suất chiếu
        </p>
        <h1 className="mt-1 min-w-0 break-words text-2xl font-sans font-black uppercase tracking-wide text-white [overflow-wrap:anywhere]">
          THEO MONG MUỐN CỦA BẠN
        </h1>
        <p className="mt-1 text-xs text-neutral-500">
          Lọc theo tên phim, thể loại, độ tuổi hoặc chọn ngày — suất chiếu hiển thị ngay bên dưới.
        </p>
      </div>

      {/* ── 4 filter cùng 1 hàng ── */}
      <div className="border border-white/[0.08] bg-white/[0.015] p-3">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {/* 1. Tìm theo tên phim */}
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-purple-400" />
          <input
            type="text"
            aria-label="Tìm phim theo tên"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Tìm theo tên phim…"
            className="h-[60px] w-full border border-white/10 bg-neutral-950 py-0 pl-10 pr-4 text-sm text-white placeholder-neutral-500
                       outline-none transition focus:border-purple-400 focus:ring-1 focus:ring-purple-500/40"
          />
          {keyword && (
            <button
              type="button"
              onClick={() => setKeyword('')}
              aria-label="Xóa từ khóa tìm kiếm"
              className="absolute right-1 top-1/2 flex h-10 w-10 -translate-y-1/2 cursor-pointer items-center justify-center focus-visible:outline focus-visible:outline-2 focus-visible:outline-purple-300"
            >
              <X className="h-3.5 w-3.5 text-neutral-500 hover:text-white" />
            </button>
          )}
        </div>

        {/* 2. Lọc thể loại */}
        <GenreDropdown genres={genreOptions} selectedGenre={selectedGenre} onChange={setSelectedGenre} />

        {/* 3. Lọc độ tuổi */}
        <AgeRatingDropdown selectedAge={selectedAge} onChange={setSelectedAge} />

        {/* 4. Chọn ngày chiếu */}
        <div className="relative">
          <CalendarDays className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-purple-400" />
          <input
            type="date"
            aria-label="Chọn ngày chiếu"
            value={selectedDate}
            min={todayKey}
            onChange={(e) => { if (e.target.value) setSelectedDate(e.target.value); }}
            className="h-[60px] w-full border border-white/10 bg-neutral-950 py-0 pl-10 pr-4 text-sm text-white
                       outline-none transition focus:border-purple-400 focus:ring-1 focus:ring-purple-500/40
                       [color-scheme:dark]"
          />
        </div>
      </div>
      </div>

      {/* Active filter badges */}
      {hasActiveFilters && (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-[9px] font-bold uppercase tracking-widest text-neutral-600">Bộ lọc:</span>
          {keyword && (
            <span className="flex items-center gap-1.5 border border-purple-500/30 bg-purple-950/30 px-2 py-1 text-[9px] font-bold uppercase tracking-wider text-purple-300">
              <Search className="h-3 w-3" aria-hidden="true" />
              <span className="max-w-48 truncate">{keyword}</span>
              <button type="button" aria-label="Xóa từ khóa" onClick={() => setKeyword('')}><X className="h-3 w-3" /></button>
            </span>
          )}
          {selectedGenre && (
            <span className="flex items-center gap-1.5 border border-purple-500/30 bg-purple-950/30 px-2 py-1 text-[9px] font-bold uppercase tracking-wider text-purple-300">
              <Tag className="h-3 w-3" aria-hidden="true" />
              <span className="max-w-48 truncate">{selectedGenre}</span>
              <button type="button" aria-label="Xóa thể loại" onClick={() => setSelectedGenre(null)}><X className="h-3 w-3" /></button>
            </span>
          )}
          {selectedAge && (
            <span className={`flex items-center gap-1.5 border px-2 py-1 text-[9px] font-bold uppercase tracking-wider ${getAgeColor(selectedAge)}`}>
              <Users className="h-3 w-3" aria-hidden="true" />
              {selectedAge}
              <button type="button" aria-label="Xóa phân loại độ tuổi" onClick={() => setSelectedAge(null)}><X className="h-3 w-3" /></button>
            </span>
          )}
          <button type="button" onClick={clearAllFilters} className="ml-1 cursor-pointer text-[9px] font-bold uppercase tracking-widest text-neutral-500 underline underline-offset-2 transition hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-purple-300">
            Xoá tất cả
          </button>
        </div>
      )}

      {/* Hint khi moviesList đang fetch để lọc genre/ageRating */}
      {isMoviesLoading && moviesList.length === 0 && (
        <div className="flex items-center gap-2 border border-white/5 bg-neutral-950/60 px-4 py-2.5">
          <Loader2 className="h-3.5 w-3.5 animate-spin text-purple-400 shrink-0" />
          <span className="text-[10px] font-bold uppercase tracking-widest text-neutral-500">
            Đang tải thông tin phim — bộ lọc thể loại &amp; độ tuổi sẽ sẵn sàng ngay…
          </span>
        </div>
      )}

      {/* ── Danh sách phim ── */}
      {isLoading ? (
        <div className="flex items-center justify-center gap-2 border border-white/10 bg-neutral-950 py-16 text-neutral-500">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span className="text-xs font-bold uppercase tracking-widest">Đang tải lịch chiếu…</span>
        </div>
      ) : movieGroups.length === 0 ? (
        <div className="border border-dashed border-white/10 bg-neutral-950 py-16 text-center">
          <CalendarDays className="mx-auto h-10 w-10 text-neutral-700" />
          <p className="mt-3 text-xs font-bold uppercase tracking-widest text-neutral-500">
            {hasActiveFilters ? 'Không tìm thấy phim phù hợp với bộ lọc' : 'Chưa có suất chiếu nào trong ngày này'}
          </p>
          {hasActiveFilters && (
            <button
              type="button"
              onClick={clearAllFilters}
              className="mt-3 text-[10px] text-purple-400 hover:text-purple-300 uppercase tracking-widest font-bold underline underline-offset-2"
            >
              Xoá bộ lọc
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex flex-col gap-2 border-b border-white/10 pb-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-[9px] font-black uppercase tracking-[0.22em] text-purple-400">Lịch chiếu khả dụng</p>
              <h2 className="mt-1 text-sm font-extrabold capitalize text-white">
                {new Date(`${selectedDate}T00:00:00`).toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}
              </h2>
            </div>
            <p className="font-mono text-[10px] font-bold uppercase tracking-wider text-neutral-500">
              {movieGroups.length} phim <span className="mx-1 text-neutral-700">·</span> {visibleShowtimeCount} suất chiếu
            </p>
          </div>
          {movieGroups.map((group) => (
            <article key={group.movieId} className="group/card overflow-hidden border border-white/10 bg-neutral-950 transition-colors hover:border-white/20">
              {/* Movie header */}
              <div className="grid min-w-0 gap-4 p-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center sm:px-5 sm:py-4">
                <div className="flex min-w-0 items-center gap-4">
                  {/* Poster */}
                  {group.poster ? (
                    <img
                      src={group.poster}
                      alt={group.movieTitle}
                      className="h-24 w-16 shrink-0 border border-white/10 object-cover"
                      referrerPolicy="no-referrer"
                    />
                  ) : (
                    <div className="flex h-24 w-16 shrink-0 items-center justify-center border border-white/10 bg-neutral-900">
                      <span className="text-neutral-700 text-[8px] font-bold uppercase">No poster</span>
                    </div>
                  )}

                  {/* Movie info */}
                  <div className="min-w-0 flex-1">
                    <h3 className="break-words text-base font-sans font-black uppercase leading-tight tracking-wide text-white">
                      {group.movieTitle}
                    </h3>

                    {group.movieGenres.length > 0 && group.movieGenres[0] !== 'Dang cap nhat' && (
                      <div className="mt-2 flex flex-wrap gap-1">
                        {group.movieGenres.slice(0, 4).map((g) => (
                          <span
                            key={g}
                            className="border border-purple-500/30 bg-purple-950/30 px-1.5 py-0.5 text-[8px] font-bold uppercase tracking-wider text-purple-400"
                          >
                            {g}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                <dl className="grid grid-cols-3 divide-x divide-white/10 border border-white/10 bg-black/35 sm:min-w-[330px]">
                  <div className="min-w-0 px-3 py-3">
                    <dt className="text-[8px] font-black uppercase tracking-[0.16em] text-neutral-600">Thời lượng</dt>
                    <dd className="mt-1 truncate font-mono text-xs font-black text-white">{group.duration ? `${group.duration} phút` : 'Đang cập nhật'}</dd>
                  </div>
                  <div className="min-w-0 px-3 py-3">
                    <dt className="text-[8px] font-black uppercase tracking-[0.16em] text-neutral-600">Phân loại</dt>
                    <dd className="mt-1">
                      {group.ageRating ? (
                        <span className={`inline-flex border px-1.5 py-0.5 text-[9px] font-black uppercase tracking-wider ${getAgeColor(group.ageRating)}`}>
                          {group.ageRating}
                        </span>
                      ) : (
                        <span className="text-[10px] font-bold text-neutral-500">Chưa có</span>
                      )}
                    </dd>
                  </div>
                  <div className="min-w-0 px-3 py-3">
                    <dt className="text-[8px] font-black uppercase tracking-[0.16em] text-neutral-600">Trong ngày</dt>
                    <dd className="mt-1 truncate font-mono text-xs font-black text-white">{group.slots.length} suất</dd>
                  </div>
                </dl>
              </div>

              {/* Showtime slots — always visible */}
              <div className="min-w-0 border-t border-white/10 bg-black/35 px-4 py-4 sm:px-5">
                <p className="mb-3 text-[8px] font-black uppercase tracking-[0.2em] text-neutral-500">
                  <Clock className="inline h-2.5 w-2.5 mr-1 -mt-px" />
                  Suất chiếu — {new Date(selectedDate).toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'numeric' })}
                </p>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                  {group.slots.map((st) => {
                    const endTime = getMovieEndTime(st, group.duration);
                    const startLabel = formatTime(st.startTime);
                    const endLabel = endTime ? formatTime(endTime) : '--:--';
                    const roomName = st.roomName || st.room?.name || (st.roomId ? `Phòng ${st.roomId}` : 'Phòng chiếu');

                    return (
                      <button
                        type="button"
                        key={st.id}
                        onClick={() => navigate(`/movies/${group.routeId}/book?showtimeId=${st.id}`)}
                        aria-label={`Chọn suất chiếu ${roomName} từ ${startLabel} đến ${endLabel}`}
                        className="group flex w-full min-w-0 cursor-pointer flex-col border border-white/15 bg-black px-3.5 py-3 text-left transition-colors hover:border-purple-400 hover:bg-purple-500/10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-300 active:bg-purple-500/15"
                        title={`${roomName} · Suất chiếu từ ${startLabel} đến ${endLabel}`}
                      >
                        <div className="mb-2 flex items-center justify-between border-b border-white/10 pb-1.5">
                          <span className="truncate text-[9.5px] font-black uppercase tracking-[0.14em] text-purple-300 transition group-hover:text-purple-200">
                            {roomName}
                          </span>
                        </div>
                        <span className="grid w-full grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-2">
                          <span className="grid min-w-0 grid-cols-[12px_minmax(0,1fr)] items-start gap-1.5">
                            <Clock className="mt-0.5 h-3 w-3 shrink-0 text-neutral-500 transition group-hover:text-purple-300" />
                            <span className="min-w-0">
                              <span className="block font-mono text-sm font-black text-white transition group-hover:text-purple-200">{startLabel}</span>
                              <span className="mt-1 block text-[8px] font-bold uppercase tracking-[0.14em] text-neutral-600 transition group-hover:text-purple-300/70">Bắt đầu</span>
                            </span>
                          </span>
                          <ArrowRight className="h-3 w-3 shrink-0 text-neutral-600 transition group-hover:translate-x-0.5 group-hover:text-purple-400" aria-hidden="true" />
                          <span className="min-w-0 text-right">
                            <span className="block font-mono text-sm font-black text-purple-300">{endLabel}</span>
                            <span className="mt-1 block text-[8px] font-bold uppercase tracking-[0.14em] text-neutral-600 transition group-hover:text-purple-300/70">Kết thúc</span>
                          </span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
