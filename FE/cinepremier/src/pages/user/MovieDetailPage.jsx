import React, { useState, useEffect, useRef } from 'react';
const popcornBot = new URL('../../assets/banners/—Pngtree—barrel popcorn pattern_4538379.png', import.meta.url).href;
import { createPortal } from 'react-dom';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ArrowLeft, Play, Star, Clock, Heart, Loader2, ChevronLeft, ChevronRight } from 'lucide-react';
import { useMovies } from '../../stores/useMovieStore';
import { getStoredAuth } from '../../services/authService';
import { adminService } from '../../services/adminService';
import { movieService } from '../../services/movieService';
import { reviewService } from '../../services/reviewService';
import { recommendationService } from '../../services/recommendationService';
import { chatService } from '../../services/chatService';
import { useAuthStore } from '../../stores/useAuthStore';

const extractYoutubeId = (url = '') => {
  const trimmed = url.trim();
  if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) return trimmed;

  try {
    const parsed = new URL(trimmed);
    const host = parsed.hostname.replace(/^www\./, '');
    const parts = parsed.pathname.split('/').filter(Boolean);

    if (host === 'youtu.be') return parts[0] || '';
    if (host.endsWith('youtube.com')) {
      const videoId = parsed.searchParams.get('v');
      if (videoId) return videoId;
      if (['embed', 'shorts', 'live'].includes(parts[0])) return parts[1] || '';
    }
  } catch {
    // Fall back to regex parsing for partially pasted URLs.
  }

  const patterns = [
    /youtube\.com\/watch\?v=([a-zA-Z0-9_-]{11})/,
    /youtu\.be\/([a-zA-Z0-9_-]{11})/,
    /youtube\.com\/embed\/([a-zA-Z0-9_-]{11})/,
    /youtube\.com\/shorts\/([a-zA-Z0-9_-]{11})/,
    /youtube\.com\/live\/([a-zA-Z0-9_-]{11})/,
    /[?&]v=([a-zA-Z0-9_-]{11})/
  ];

  return patterns.map((pattern) => trimmed.match(pattern)?.[1]).find(Boolean) || '';
};

const isDirectVideoUrl = (url = '') => {
  const value = url.trim().toLowerCase();
  return /\.(mp4|webm|mov)(\?|#|$)/.test(value) || value.includes('/video/upload/');
};

const splitDirectorNames = (value) => String(value || '')
  .split(/[,\n;/]+/)
  .map((item) => item.trim())
  .filter(Boolean);

const getTrailerEmbedSrc = (url = '') => {
  const youtubeId = extractYoutubeId(url);
  if (youtubeId) {
    return `https://www.youtube.com/embed/${youtubeId}?autoplay=1&mute=1&controls=1&rel=0&modestbranding=1&playsinline=1`;
  }
  return url;
};

const hasSourcePoster = (movie = {}) => Boolean(
  movie.raw?.posterUrl
  || movie.raw?.poster
  || movie.raw?.posterImageUrl
  || movie.raw?.imageUrl
  || movie.raw?.thumbnailUrl
);

export default function DetailView() {
  const navigate = useNavigate();
  const { id } = useParams();
  const similarMoviesRef = useRef(null);
  const scrollSimilar = (dir) => {
    const el = similarMoviesRef.current;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  };
  const { moviesList, setMoviesList, watchlist = [], handleToggleWatchlist } = useMovies();
  const currentRole = useAuthStore((state) => state.currentRole);
  const currentUser = useAuthStore((state) => state.currentUser);
  const movie = moviesList.find(m => String(m.id) === String(id) || String(m.backendId) === String(id));
  const onBack = () => navigate(-1);
  const onBook = (mv) => navigate(`/movies/${mv.id}/book`);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    const fetchDetail = async () => {
      try {
        const { accessToken } = getStoredAuth();
        const detailMovieId = movie?.backendId || id;
        const detail = currentRole === 'admin' && accessToken
          ? await adminService.getAdminMovieDetail(accessToken, detailMovieId)
          : await movieService.getMovieDetail(detailMovieId);
        if (cancelled || !detail?.id) return;
        setMoviesList(prev => {
          const exists = prev.some(m => (
            String(m.id) === String(id)
            || String(m.id) === String(detail.id)
            || String(m.backendId) === String(detail.id)
          ));
          if (!exists) return [{ ...detail }, ...prev];
          return prev.map(m => (
            String(m.id) === String(id)
            || String(m.id) === String(detail.id)
            || String(m.backendId) === String(detail.id)
          ) ? { ...m, ...detail } : m);
        });
      } catch { /* use fallback from list */ }
    };
    fetchDetail();
    return () => { cancelled = true; };
  }, [id, currentRole, movie?.backendId]);

  const [searchParams, setSearchParams] = useSearchParams();
  const [showTrailer, setShowTrailerState] = useState(searchParams.get('trailer') === '1');
  const setShowTrailer = (value) => {
    setShowTrailerState(value);
    if (!value && searchParams.get('trailer')) {
      const next = new URLSearchParams(searchParams);
      next.delete('trailer');
      setSearchParams(next, { replace: true });
    }
  };
  const [reviews, setReviews] = useState([]);
  const [isLoadingReviews, setIsLoadingReviews] = useState(false);
  const [likedReviews, setLikedReviews] = useState({});
  const [similarMovies, setSimilarMovies] = useState([]);
  const isBookable = movie?.status === 'NOW_SHOWING' || (!movie?.status && !movie?.isUpcoming);
  const isWatchlisted = movie && watchlist.some((item) => (
    String(item.backendId || item.movieId || item.id) === String(movie.backendId || movie.movieId || movie.id)
  ));
  const trailerUrl = movie?.trailerUrl?.trim() || '';
  const trailerEmbedSrc = getTrailerEmbedSrc(trailerUrl);
  const hasDirectTrailerVideo = isDirectVideoUrl(trailerUrl);

  const detailMovieId = movie?.backendId || movie?.movieId || movie?.id || id;

  const [trailerChatInput, setTrailerChatInput] = useState('');
  const [trailerChatMessages, setTrailerChatMessages] = useState([]);
  const [trailerChatSending, setTrailerChatSending] = useState(false);
  const trailerChatEndRef = useRef(null);

  useEffect(() => {
    trailerChatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [trailerChatMessages]);

  const handleSendTrailerChat = async () => {
    const userMsg = trailerChatInput.trim();
    if (!userMsg || trailerChatSending) return;

    setTrailerChatMessages((prev) => [
      ...prev,
      { role: 'user', text: userMsg },
      { role: 'bot', text: '🤖 Đang suy nghĩ...' }
    ]);
    setTrailerChatInput('');
    setTrailerChatSending(true);

    try {
      const { accessToken } = getStoredAuth();
      const res = await chatService.sendMessage({
        message: userMsg,
        movieId: detailMovieId,
        userId: currentUser?.id,
        token: accessToken,
        scope: 'trailer'
      });
      setTrailerChatMessages((prev) => {
        const next = [...prev];
        next[next.length - 1] = { role: 'bot', text: res?.message || 'Xin lỗi, tôi chưa có câu trả lời phù hợp.' };
        return next;
      });
    } catch (error) {
      setTrailerChatMessages((prev) => {
        const next = [...prev];
        next[next.length - 1] = { role: 'bot', text: '⚠️ Có lỗi xảy ra, thử lại sau nhé!' };
        return next;
      });
    } finally {
      setTrailerChatSending(false);
    }
  };

  useEffect(() => {
    if (!detailMovieId) return;
    let cancelled = false;
    setIsLoadingReviews(true);
    reviewService.getMovieReviews(detailMovieId)
      .then((payload) => {
        if (cancelled) return;
        setReviews(Array.isArray(payload) ? payload : payload?.items || []);
      })
      .catch(() => {
        if (!cancelled) setReviews([]);
      })
      .finally(() => {
        if (!cancelled) setIsLoadingReviews(false);
      });
    return () => {
      cancelled = true;
    };
  }, [detailMovieId]);

  // Close the trailer modal with the Escape key + lock body scroll.
  useEffect(() => {
    if (!showTrailer) return undefined;
    const onKeyDown = (e) => { if (e.key === 'Escape') setShowTrailer(false); };
    const scrollY = window.scrollY;
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.position = 'fixed';
    document.body.style.top = `-${scrollY}px`;
    document.body.style.width = '100%';
    document.body.style.paddingRight = `${scrollbarWidth}px`;

    const redirectScroll = (e) => {
      const inner = document.getElementById('trailer-modal-inner');
      if (!inner) return;
      e.preventDefault();
      // If already scrolling inside modal, let the modal scroll
      // If scrolling outside (backdrop), redirect to modal
      inner.scrollBy({ top: e.deltaY, behavior: 'auto' });
    };
    document.addEventListener('wheel', redirectScroll, { passive: false });
    document.addEventListener('touchmove', redirectScroll, { passive: false });

    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      document.removeEventListener('wheel', redirectScroll);
      document.removeEventListener('touchmove', redirectScroll);
      document.body.style.position = '';
      document.body.style.top = '';
      document.body.style.width = '';
      document.body.style.paddingRight = '';
      window.scrollTo(0, scrollY);
    };
  }, [showTrailer]);

  useEffect(() => {
    if (!detailMovieId) return;
    let cancelled = false;
    recommendationService.getContentRecommendations(detailMovieId)
      .then(async (items) => {
        if (cancelled) return;
        const list = Array.isArray(items) ? items : [];
        const enriched = await Promise.all(list.map(async (rec) => {
          const recId = rec.backendId || rec.movieId || rec.id;
          if (!recId) return rec;

          const localMovie = moviesList.find((m) => (
            String(m.backendId || m.movieId || m.id) === String(recId)
          ));

          if (localMovie?.posterUrl) {
            return { ...rec, ...localMovie, similarity: rec.similarity };
          }

          if (hasSourcePoster(rec)) return rec;

          try {
            const detail = await movieService.getMovieDetail(recId);
            return detail?.id ? { ...rec, ...detail, similarity: rec.similarity } : rec;
          } catch {
            return rec;
          }
        }));
        if (!cancelled) setSimilarMovies(enriched);
      })
      .catch(() => {
        if (!cancelled) setSimilarMovies([]);
      });
    return () => {
      cancelled = true;
    };
  }, [detailMovieId, moviesList]);

  const handleLikeReview = (id) => {
    setLikedReviews(prev => {
      const isAlreadyLiked = !!prev[id];
      setReviews(current => current.map(r => {
        if (r.id === id) {
          const currentLikes = Number(r.likes || 0);
          return { ...r, likes: isAlreadyLiked ? Math.max(0, currentLikes - 1) : currentLikes + 1 };
        }
        return r;
      }));
      return { ...prev, [id]: !isAlreadyLiked };
    });
  };

  if (!movie) return <div className="min-h-screen bg-black flex items-center justify-center text-white">Đang tải...</div>;

  const mainActorIdSet = new Set((movie.mainActorIds || []).map((actorId) => Number(actorId)));
  const currentCasts = Array.isArray(movie.actors)
    ? movie.actors.map((actor) => ({
      id: Number.isFinite(Number(actor.id ?? actor.actorId)) ? Number(actor.id ?? actor.actorId) : actor.name,
      name: actor.name || actor.fullName || actor.actorName || 'Diễn viên',
      role: actor.role || actor.characterName || actor.description || 'Diễn viên',
      avatarUrl: actor.avatarUrl || actor.imageUrl || actor.photoUrl || '',
      isMain: mainActorIdSet.has(Number(actor.id ?? actor.actorId))
    }))
    : [];
  const mainCasts = currentCasts.filter((cast) => cast.isMain);
  const supportingCasts = currentCasts.filter((cast) => !cast.isMain);
  const directorNames = splitDirectorNames(movie.director);

  return (
    <div className="pb-24 space-y-12 relative">
      {/* Background lighting — same as homepage */}
      <div className="pointer-events-none fixed inset-0 -z-50">
        <div className="absolute left-1/2 top-1/3 h-[900px] w-[900px] -translate-x-1/2 rounded-full bg-purple-600/20 blur-[150px]" />
      </div>

      {/* 1. BLURRED BANNER HERO BACKGROUND */}
      <section
        className="relative min-h-[55vh] flex items-end bg-cover bg-center px-4 sm:px-6 lg:px-8 py-10"
        style={{ backgroundImage: `url(${movie.bannerUrl})` }}
      >
        <div className="absolute inset-0 bg-gradient-to-t from-black via-black/80 to-transparent z-10" />
        <div className="absolute inset-0 bg-black/50 z-0 backdrop-blur-[1px]" />

        <div className="relative max-w-5xl w-full mx-auto z-20 grid grid-cols-1 md:grid-cols-12 gap-4 items-center">

          {/* Movie Poster Vertical Card */}
          <div className="md:col-span-4 flex justify-center md:justify-start">
            <div className="relative w-72 aspect-[2/3] overflow-hidden border border-white/10 shadow-2xl flex-shrink-0 bg-black">
              <img
                src={movie.posterUrl}
                alt={movie.title}
                className="w-full h-full object-cover"
                referrerPolicy="no-referrer"
              />
              <div className="absolute left-3 top-3 border border-white/20 bg-black text-[9px] font-bold px-1.5 py-0.5 tracking-widest text-white">
                {movie.ageRating}
              </div>
            </div>
          </div>

          {/* Quick texts and Action indicators */}
          <div className="md:col-span-8 space-y-4 text-center md:text-left">
            <div className="flex flex-wrap justify-center md:justify-start gap-2">
              {movie.genre.map((gen) => (
                <span key={gen} className="border border-white/15 bg-black text-white font-sans text-[10px] tracking-[0.1em] uppercase px-3 py-1">
                  {gen}
                </span>
              ))}
              <span className="border border-white/10 bg-black text-white font-sans text-[10px] tracking-[0.1em] uppercase px-3 py-1 flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5" />
                {movie.duration} MIN
              </span>
            </div>

            <div className="space-y-1">
              <h1 className="text-3xl sm:text-5xl font-serif font-light text-white tracking-wider leading-none uppercase italic">
                {movie.title}
              </h1>
              <p className="text-xs sm:text-sm font-sans tracking-[0.2em] text-white uppercase pt-1">
                {movie.englishTitle}
              </p>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-2 text-[10px] text-white font-sans uppercase tracking-[0.15em] md:justify-start">
              <span>ĐẠO DIỄN:</span>
              {directorNames.length ? directorNames.map((directorName) => (
                <span key={directorName} className="border border-white/15 bg-black/50 px-2 py-1 font-bold text-white">
                  {directorName}
                </span>
              )) : (
                <span className="text-white font-bold">Đang cập nhật</span>
              )}
              <span className="text-neutral-500">•</span>
              <span>RA MẮT: {movie.releaseDate}</span>
            </div>

            {/* Book & Trailer Action trigger buttons */}
            <div className="flex flex-wrap items-center justify-center md:justify-start gap-3 pt-3">
              {isBookable ? (
                <button
                  onClick={() => onBook(movie)}
                  className="border border-purple-400/70 bg-purple-600 text-white text-xs font-bold font-sans uppercase tracking-[0.15em] px-8 py-3.5 hover:bg-purple-500 hover:border-purple-300 transition duration-300"
                  id="detail-book-now"
                >
                  ĐẶT VÉ NGAY
                </button>
              ) : (
                <span className="border border-white/20 bg-neutral-900 text-white uppercase text-[10px] tracking-widest px-8 py-3.5 font-bold font-sans">
                  {movie.upcomingDate} - CHỜ MỞ BÁN
                </span>
              )}

              <button
                onClick={() => setShowTrailer(true)}
                disabled={!trailerUrl}
                className="border border-purple-400/50 bg-black/40 hover:bg-purple-950/40 hover:border-purple-400/70 text-white px-6 py-3.5 text-xs font-sans uppercase tracking-[0.15em] flex items-center gap-2 transition duration-300 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-black/40 disabled:hover:border-purple-400/20"
                id="detail-trailer-button"
              >
                <Play className="h-4 w-4 fill-white text-white" />
                XEM TRAILER
              </button>

              <button
                type="button"
                onClick={() => handleToggleWatchlist(movie)}
                className={`border px-5 py-3.5 text-xs font-sans uppercase tracking-[0.15em] flex items-center gap-2 transition duration-300 ${isWatchlisted
                  ? 'border-rose-400/70 bg-rose-500 text-white hover:bg-black'
                  : 'border-white/10 bg-black/40 text-white hover:bg-white hover:text-black'
                  }`}
                id="detail-watchlist-button"
              >
                <Heart className={`h-4 w-4 ${isWatchlisted ? 'fill-current' : ''}`} />
                {isWatchlisted ? 'ĐÃ LƯU' : 'LƯU PHIM'}
              </button>




            </div>
          </div>

        </div>
      </section>

      {/* Back button - outside banner section */}
      <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 -mt-8 relative z-20">
        <button
          onClick={onBack}
          className="flex items-center space-x-2 border border-purple-400/50 bg-black px-4 py-2.5 text-[10px] uppercase tracking-[0.15em] font-sans text-white hover:bg-purple-600 hover:text-white transition-all duration-300"
          id="detail-back-button"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>QUAY LẠI</span>
        </button>
      </div>

      {/* 2. MAIN DETAILS GRID */}
      <section className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 space-y-12">

        {/* Synopsis */}
        <div className="space-y-3">
          <h3 className="text-[10px] font-black uppercase tracking-[0.22em] text-purple-300 pb-2 border-b border-purple-500/20">Tóm tắt nội dung</h3>
          <p className="text-sm text-neutral-300 leading-relaxed font-sans">{movie.synopsis}</p>
        </div>

        {/* Main Cast */}
        <div className="space-y-4">
          <h3 className="text-[10px] font-black uppercase tracking-[0.22em] text-purple-300 pb-2 border-b border-purple-500/20">Diễn viên chính</h3>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3" id="cast-list">
            {mainCasts.length === 0 && (
              <p className="col-span-full border border-dashed border-white/10 p-4 text-xs text-neutral-500">Chưa chọn diễn viên chính.</p>
            )}
            {mainCasts.map((cast) => (
              <div key={cast.id || cast.name} className="flex items-center gap-3 bg-white/5 hover:bg-white/8 transition p-3 border border-white/8">
                <div className="h-9 w-9 overflow-hidden rounded-full border border-purple-500/30 bg-neutral-800 flex-shrink-0">
                  {cast.avatarUrl ? (
                    <img src={cast.avatarUrl} alt={cast.name} className="h-full w-full object-cover" referrerPolicy="no-referrer" />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center text-xs font-black text-purple-300">{cast.name.slice(0, 1)}</div>
                  )}
                </div>
                <div className="min-w-0">
                  <h4 className="text-xs font-sans text-white truncate font-semibold">{cast.name}</h4>
                  <p className="text-[9px] text-purple-300/70 uppercase tracking-wider truncate mt-0.5">{cast.role || 'Diễn viên'}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Supporting Cast */}
        {supportingCasts.length > 0 && (
          <div className="space-y-4">
            <h3 className="text-[10px] font-black uppercase tracking-[0.22em] text-purple-300 pb-2 border-b border-purple-500/20">Một số diễn viên trong phim</h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3" id="supporting-cast-list">
              {supportingCasts.map((cast) => (
                <div key={cast.id || cast.name} className="flex items-center gap-3 bg-white/5 hover:bg-white/8 transition p-3 border border-white/8">
                  <div className="h-9 w-9 overflow-hidden rounded-full border border-white/10 bg-neutral-800 flex-shrink-0">
                    {cast.avatarUrl ? (
                      <img src={cast.avatarUrl} alt={cast.name} className="h-full w-full object-cover" referrerPolicy="no-referrer" />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center text-xs font-black text-neutral-400">{cast.name.slice(0, 1)}</div>
                    )}
                  </div>
                  <div className="min-w-0">
                    <h4 className="text-xs font-sans text-white truncate font-semibold">{cast.name}</h4>
                    <p className="text-[9px] text-neutral-500 uppercase tracking-wider truncate mt-0.5">{cast.role || 'Diễn viên'}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Reviews */}
        <div className="space-y-5">
          <h3 className="text-[10px] font-black uppercase tracking-[0.22em] text-purple-300 pb-2 border-b border-purple-500/20">
            Nhận xét của cinephile ({reviews.length})
          </h3>

          <p className="text-xs text-neutral-500 font-sans">Đánh giá bên dưới được lấy từ 100% đánh giá thực tế của khách hàng đã xem phim.</p>

          <div className="space-y-3 max-h-[400px] overflow-y-auto pr-1 [scrollbar-width:thin] [scrollbar-color:rgba(255,255,255,0.1)_transparent]">
            {isLoadingReviews && (
              <div className="flex items-center justify-center gap-2 p-6 text-xs text-neutral-500">
                <Loader2 className="h-4 w-4 animate-spin" /> Đang tải đánh giá...
              </div>
            )}
            {!isLoadingReviews && reviews.filter((r) => !r.status || r.status === 'VISIBLE').length === 0 && (
              <div className="border border-dashed border-white/10 p-6 text-center text-xs text-neutral-600 uppercase tracking-widest">
                Chưa có đánh giá công khai cho phim này.
              </div>
            )}
            {!isLoadingReviews && reviews.filter((r) => !r.status || r.status === 'VISIBLE').map((rev) => (
              <div key={rev.id} className="bg-white/4 hover:bg-white/6 transition p-4 border border-white/8 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-purple-600/40 border border-purple-500/30 text-[10px] font-black text-purple-200">
                      {(rev.userFullName || rev.userEmail || 'C').slice(0, 1).toUpperCase()}
                    </div>
                    <div>
                      <h4 className="text-xs text-white font-semibold">{rev.userFullName || rev.userEmail || 'Cinephile'}</h4>
                      <span className="text-[10px] text-neutral-500">
                        {rev.createdAt ? new Date(rev.createdAt).toLocaleDateString('vi-VN') : ''}
                      </span>
                    </div>
                  </div>
                  <div className="flex gap-0.5">
                    {Array.from({ length: 5 }, (_, idx) => (
                      <Star key={idx} className={`h-3 w-3 ${idx < rev.rating ? 'text-amber-400 fill-current' : 'text-neutral-700'}`} />
                    ))}
                  </div>
                </div>
                <p className="text-neutral-300 text-xs leading-relaxed font-sans pl-11">"{rev.comment || ''}"</p>
                <div className="pl-11 pt-2 border-t border-white/5">
                  <button
                    onClick={() => handleLikeReview(rev.id)}
                    className={`flex items-center gap-1.5 text-[10px] transition uppercase tracking-wider ${likedReviews[rev.id] ? 'text-rose-400' : 'text-neutral-500 hover:text-white'}`}
                  >
                    <Heart className={`h-3 w-3 ${likedReviews[rev.id] ? 'fill-current' : ''}`} />
                    Thích {rev.likes || 0}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

      </section>

      {/* 3. SIMILAR MOVIES (content-based AI recommendation) */}
      {similarMovies.length > 0 && (
        <section className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8 space-y-4" id="similar-movies-section">
          <h3 className="flex flex-wrap items-center gap-x-3 gap-y-2 text-[11px] font-sans font-bold uppercase tracking-[0.2em] text-white border-b border-white/10 pb-2">
            PHIM TƯƠNG TỰ
            <span className="inline-flex items-center gap-1 border border-purple-500/40 bg-purple-950/40 px-2 py-0.5 text-[9px] font-black tracking-[0.15em] text-purple-300">
              AI · CONTENT-BASED (SBERT)
            </span>
            <span className="font-light normal-case tracking-normal text-neutral-300">AI so sánh nội dung, thể loại và ê-kíp của từng phim</span>
          </h3>
          <div className="relative">
            <button type="button" onClick={() => scrollSimilar(-1)} aria-label="Phim trước"
              className="absolute -left-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-left-8">
              <ChevronLeft className="h-6 w-6" />
            </button>
            <div ref={similarMoviesRef}
              className="flex gap-4 overflow-x-auto scroll-smooth snap-x snap-mandatory pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {similarMovies.map((rec) => (
                <button
                  key={rec.id ?? rec.movieId}
                  type="button"
                  onClick={() => navigate(`/movies/${rec.backendId || rec.id}`)}
                  className="snap-start shrink-0 w-[calc((100%-6rem)/7)] group relative text-left bg-neutral-950 border border-white/10 hover:border-white/40 transition overflow-hidden cursor-pointer"
                >
                  <div className="aspect-[2/3] w-full overflow-hidden bg-black">
                    {rec.posterUrl ? (
                      <img
                        src={rec.posterUrl}
                        alt={rec.title}
                        loading="lazy"
                        className="h-full w-full object-cover opacity-80 group-hover:opacity-100 group-hover:scale-105 transition duration-500"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center text-[10px] uppercase tracking-widest text-neutral-600">Không có poster</div>
                    )}
                  </div>
                  {typeof rec.similarity === 'number' && (
                    <span className="absolute top-2 right-2 bg-black/80 border border-white/20 px-2 py-1 text-[9px] font-sans font-bold tracking-wider text-white">
                      {Math.round(rec.similarity * 100)}% TƯƠNG ĐỒNG
                    </span>
                  )}
                  <div className="p-3">
                    <p className="text-xs font-serif text-white leading-snug line-clamp-2">{rec.title}</p>
                    {rec.reason && (
                      <p className="mt-1 text-[10px] text-purple-300/80 leading-snug line-clamp-2" title={rec.reason}>
                        {rec.reason}
                      </p>
                    )}
                  </div>
                </button>
              ))}
            </div>
            <button type="button" onClick={() => scrollSimilar(1)} aria-label="Phim sau"
              className="absolute -right-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-right-8">
              <ChevronRight className="h-6 w-6" />
            </button>
          </div>
        </section>
      )}

      {/* 4. VIDEO TRAILER DIALOG COMPONENT MODAL — portaled to <body> to escape the page's stacking context (sticky header is z-[100]) */}
      {showTrailer && trailerUrl && createPortal(
        <div
          className="fixed inset-0 z-[110] flex items-center justify-center bg-black/95 p-4 transition duration-300"
          id="trailer-modal"
        >
          <div
            id="trailer-modal-inner"
            className="relative w-full max-w-4xl border border-white/20 bg-neutral-950 shadow-2xl overflow-y-auto max-h-[95vh] [scrollbar-width:thin] [scrollbar-color:rgba(168,85,247,0.4)_transparent]"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close bar */}
            <div className="flex items-center justify-between px-4 py-2 bg-black">
              <span className="text-[10px] text-neutral-500 font-sans uppercase tracking-[0.2em]">Trailer chính thức · {movie.title}</span>
              <button
                onClick={() => setShowTrailer(false)}
                className="flex items-center gap-2 text-neutral-400 hover:text-white transition text-xs font-sans cursor-pointer"
                id="close-trailer-modal"
              >
                <span className="text-base leading-none">✕</span>
                <span className="tracking-widest uppercase text-[10px]">Đóng</span>
                <span className="text-neutral-600 text-[10px]">ESC</span>
              </button>
            </div>

            <div className="aspect-video w-full bg-black">
              {hasDirectTrailerVideo ? (
                <video src={trailerUrl} className="h-full w-full" controls autoPlay playsInline />
              ) : (
                <iframe
                  title={`${movie.title} Trailer`}
                  src={trailerEmbedSrc}
                  className="h-full w-full border-none"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                />
              )}
            </div>

            {/* AI Chat */}
            <div className="flex flex-col border-t border-purple-500/30 bg-neutral-950/80">
              {/* Header */}
              <div className="flex items-center gap-3 px-5 py-3.5 border-b border-white/8 bg-purple-950/40">
                <img src={popcornBot} alt="AI" className="w-6 h-6 object-cover flex-shrink-0 rounded-full border border-purple-400/30" />
                <span className="text-[11px] font-sans font-bold text-white uppercase tracking-[0.15em]">CinePremier AI</span>
                <div className="ml-auto flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 bg-emerald-400 animate-pulse" />
                  <span className="text-[9px] text-emerald-400 font-sans uppercase tracking-widest">Online</span>
                </div>
              </div>
              {/* Messages */}
              <div className="px-5 py-4 space-y-3 max-h-64 overflow-y-auto">
                <div className="flex items-start gap-2.5">
                  <img src={popcornBot} alt="AI" className="w-6 h-6 object-cover flex-shrink-0 mt-0.5 rounded-full border border-purple-400/30" />
                  <div className="bg-neutral-800/60 border border-white/8 px-4 py-3 max-w-sm rounded-2xl rounded-tl-sm">
                    <p className="text-[12px] font-sans text-neutral-200 leading-relaxed">
                      Xin chào! Bạn muốn biết gì về <span className="font-semibold text-purple-300">{movie.title}</span>?
                    </p>
                  </div>
                </div>
                {trailerChatMessages.map((msg, i) => (
                  <div key={i} className={`flex items-start gap-2.5 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}>
                    {msg.role === 'bot' && (
                      <img src={popcornBot} alt="AI" className="w-6 h-6 object-cover flex-shrink-0 mt-0.5 rounded-full border border-purple-400/30" />
                    )}
                    <div className={`px-4 py-3 max-w-sm text-[12px] font-sans leading-relaxed rounded-2xl ${
                      msg.role === 'user'
                        ? 'bg-purple-700 text-white rounded-tr-sm'
                        : 'bg-neutral-800/60 border border-white/8 text-neutral-200 rounded-tl-sm'
                    }`}>
                      {msg.text}
                    </div>
                  </div>
                ))}
                <div ref={trailerChatEndRef} />
              </div>
              {/* Input */}
              <div className="flex items-center gap-3 px-4 pt-3 pb-5 border-t border-white/8">
                <input
                  type="text"
                  value={trailerChatInput}
                  onChange={(e) => setTrailerChatInput(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleSendTrailerChat(); }}
                  disabled={trailerChatSending}
                  placeholder="Nhập câu hỏi về phim..."
                  className="flex-1 bg-neutral-800/50 px-4 py-2.5 text-[12px] font-sans text-white placeholder-neutral-600 outline-none rounded-full disabled:opacity-60"
                />
                <button
                  onClick={handleSendTrailerChat}
                  disabled={trailerChatSending}
                  className="bg-purple-700 hover:bg-purple-600 transition text-white px-5 py-2.5 text-[10px] font-sans font-bold uppercase tracking-[0.15em] rounded-full flex-shrink-0 disabled:opacity-60"
                >
                  Gửi
                </button>
              </div>
            </div>

          </div>
        </div>,
        document.body
      )}


    </div>
  );
}

