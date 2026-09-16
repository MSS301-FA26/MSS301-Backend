import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { Sparkles, ChevronLeft, ChevronRight, X, Send, BrainCircuit, Info } from 'lucide-react';
import MovieCard from '@/components/common/MovieCard';
import cinema1 from "@/assets/banners/cinema1.png";
import cinema2 from "@/assets/banners/cinema2.png";
const popcornBot = new URL('../../assets/banners/—Pngtree—barrel popcorn pattern_4538379.png', import.meta.url).href;
import { useMovies } from '../../stores/useMovieStore';
import { useAuthStore } from '../../stores/useAuthStore';
import { getStoredAuth } from '../../services/authService';
import { movieService } from '../../services/movieService';
import { recommendationService, pickRecExplanation } from '../../services/recommendationService';
import { chatService, clearStoredConversationId } from '../../services/chatService';
import Snowfall from 'react-snowfall';
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

const loadYoutubeIframeApi = () => {
  if (typeof window === 'undefined') return Promise.resolve(null);
  if (window.YT?.Player) return Promise.resolve(window.YT);

  if (!window.__cinepremierYoutubeApiPromise) {
    window.__cinepremierYoutubeApiPromise = new Promise((resolve) => {
      const previousReady = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = () => {
        if (typeof previousReady === 'function') previousReady();
        resolve(window.YT);
      };

      const existingScript = document.querySelector('script[src="https://www.youtube.com/iframe_api"]');
      if (!existingScript) {
        const script = document.createElement('script');
        script.src = 'https://www.youtube.com/iframe_api';
        script.async = true;
        document.body.appendChild(script);
      }
    });
  }

  return window.__cinepremierYoutubeApiPromise;
};


const banners = [cinema1, cinema2];

// Câu hỏi mẫu — chọn theo đúng từ khóa router.py nhận diện (search_movies/get_ticket_price)
// để đảm bảo luôn có data trả về, không rơi vào nhánh chat chung chung.
const CHAT_QUICK_QUESTIONS = [
  'Tìm phim hành động',
  'Phim nào đang chiếu?',
  'Giá vé xem phim bao nhiêu?',
  'Phim giống Inception',
];

const ChatMovieResults = ({ movies, onPlayTrailer }) => {
  const navigate = useNavigate();
  return (
    <div className="grid grid-cols-2 gap-2 w-full">
      {movies.map((movie) => (
        <div
          key={movie.id}
          className="text-left bg-neutral-900 border border-white/10 hover:border-purple-400/60 transition overflow-hidden rounded-lg"
        >
          <button type="button" onClick={() => navigate(`/movies/${movie.id}`)} className="block w-full text-left">
            <div className="relative aspect-[2/3] w-full overflow-hidden bg-black">
              {movie.poster ? (
                <img src={movie.poster} alt={movie.title} loading="lazy" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full items-center justify-center text-[9px] uppercase tracking-widest text-neutral-600">Không có poster</div>
              )}
              {typeof movie.rating === 'number' && (
                <span className="absolute top-1 right-1 bg-black/80 border border-white/20 px-1.5 py-0.5 text-[9px] font-sans font-bold text-amber-400">
                  ★ {movie.rating}
                </span>
              )}
            </div>
            <p className="px-2 pt-1.5 text-[11px] font-sans text-white leading-snug line-clamp-2">{movie.title}</p>
            {movie.year && (
              <p className="px-2 pt-0.5 text-[10px] font-sans text-neutral-500">{movie.year}</p>
            )}
            {movie.overview && (
              <p className="px-2 pt-1 text-[10px] font-sans text-neutral-400 leading-snug line-clamp-2">{movie.overview}</p>
            )}
          </button>
          {movie.trailerUrl && (
            <button
              type="button"
              onClick={() => onPlayTrailer(movie.trailerUrl)}
              className="flex items-center justify-center gap-1 w-[calc(100%-1rem)] mx-2 mb-2 mt-1.5 py-1.5 text-[10px] font-sans font-bold uppercase tracking-wide text-purple-300 border border-purple-500/30 hover:bg-purple-500/10 transition rounded"
            >
              ▶ Xem trailer
            </button>
          )}
        </div>
      ))}
    </div>
  );
};

const ChatShowtimeResults = ({ showtimes }) => (
  <div className="flex flex-col gap-1.5 w-full">
    {showtimes.map((s, i) => (
      <div key={i} className="px-3 py-2 text-[11px] font-sans bg-neutral-900 border border-white/8 text-neutral-300 rounded-lg">
        <span className="text-purple-300">{s.show_date} {s.start_time}</span> · {s.cinema_name} - {s.room_name}
      </div>
    ))}
  </div>
);

export default function HomeView({ onSelectMovie, onBookMovie, onTabChange, moviesList = [] }) {
   const [currentIndex, setCurrentIndex] = useState(0);
  const { watchlist = [], handleToggleWatchlist } = useMovies();
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const currentUser = useAuthStore((state) => state.currentUser);
  const [personalRecs, setPersonalRecs] = useState([]);
  const [recInfoOpen, setRecInfoOpen] = useState(false);
  const [recStats, setRecStats] = useState(null);
  const [chatOpen, setChatOpen] = useState(false);
  const [chatInput, setChatInput] = useState('');
  const [chatMessages, setChatMessages] = useState([
    { role: 'bot', text: 'Xin chào! 🍿 Tôi là PopBot — trợ lý AI của CinePremier. Bạn muốn tìm phim gì hôm nay?' }
  ]);
  const [chatSending, setChatSending] = useState(false);
  const [chatTrailerUrl, setChatTrailerUrl] = useState(null);
  const chatEndRef = useRef(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages]);

  const replaceLastBotMessage = (text, data = null) => {
    setChatMessages((prev) => {
      const next = [...prev];
      next[next.length - 1] = { role: 'bot', text, data };
      return next;
    });
  };

  const handleSendChat = async (overrideMessage) => {
    const userMsg = (overrideMessage ?? chatInput).trim();
    if (!userMsg || chatSending) return;

    setChatMessages((prev) => [
      ...prev,
      { role: 'user', text: userMsg },
      { role: 'bot', text: '🤖 Đang suy nghĩ...' }
    ]);
    setChatInput('');
    setChatSending(true);

    try {
      const { accessToken } = getStoredAuth();
      const res = await chatService.sendMessage({
        message: userMsg,
        userId: currentUser?.id,
        token: accessToken,
        scope: 'home'
      });
      replaceLastBotMessage(res?.message || 'Xin lỗi, tôi chưa có câu trả lời.', res?.data || null);
    } catch (error) {
      replaceLastBotMessage('⚠️ Có lỗi xảy ra, thử lại sau nhé!');
    } finally {
      setChatSending(false);
    }
  };

  const handleResetChat = () => {
    clearStoredConversationId('home');
    setChatMessages([
      { role: 'bot', text: 'Xin chào! 🍿 Tôi là PopBot — trợ lý AI của CinePremier. Bạn muốn tìm phim gì hôm nay?' }
    ]);
  };

  // Filter movies for "Now Playing" and "Upcoming"
  const sourceMovies = Array.isArray(moviesList) ? moviesList : [];
  const publicMovies = sourceMovies.filter((m) => m.status !== 'INACTIVE' && !m.isInactive);
  const nowPlaying = publicMovies.filter((m) => m.status === 'NOW_SHOWING' || (!m.status && !m.isUpcoming));
  const nowPlayingRef = useRef(null);
  const scrollNowPlaying = (dir) => {
    const el = nowPlayingRef.current;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  };
  const upcoming = publicMovies.filter((m) => m.status === 'UPCOMING' || m.isUpcoming);
  const upcomingRef = useRef(null);
  const scrollUpcoming = (dir) => {
    const el = upcomingRef.current;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  };
  const personalRecsRef = useRef(null);
  const scrollPersonalRecs = (dir) => {
    const el = personalRecsRef.current;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  };

  // Hero movie index state to cycle beautifully
  const [currentHeroIndex, setCurrentHeroIndex] = useState(0);
  const [isMuted, setIsMuted] = useState(true);
  const [isPlaying, setIsPlaying] = useState(true);
  const heroVideoRef = useRef(null);
  const youtubeFrameRef = useRef(null);
  const youtubePlayerRef = useRef(null);

  const heroMovies = nowPlaying.length ? nowPlaying : publicMovies;
  const heroMovie = heroMovies[currentHeroIndex] || heroMovies[0] || null;
  const heroTrailerUrl = heroMovie?.trailerUrl?.trim() || '';
  const heroYoutubeId = extractYoutubeId(heroTrailerUrl);
  const hasDirectHeroVideo = !heroYoutubeId && isDirectVideoUrl(heroTrailerUrl);
  const youtubeOrigin = typeof window !== 'undefined' ? window.location.origin : '';
  const heroYoutubeSrc = heroYoutubeId
    ? `https://www.youtube.com/embed/${heroYoutubeId}?autoplay=${isPlaying ? 1 : 0}&mute=${isMuted ? 1 : 0}&controls=0&loop=1&playlist=${heroYoutubeId}&playsinline=1&rel=0&modestbranding=1&iv_load_policy=3&disablekb=1&enablejsapi=1${youtubeOrigin ? `&origin=${encodeURIComponent(youtubeOrigin)}` : ''}`
    : '';

  useEffect(() => {
    const video = heroVideoRef.current;
    if (!video) return;
    video.muted = isMuted;
    if (isPlaying) {
      video.play().catch(() => {});
    } else {
      video.pause();
    }
  }, [isPlaying, isMuted, heroTrailerUrl]);

  useEffect(() => {
    let cancelled = false;

    if (!heroYoutubeId) {
      if (youtubePlayerRef.current?.destroy) {
        youtubePlayerRef.current.destroy();
        youtubePlayerRef.current = null;
      }
      return undefined;
    }

    loadYoutubeIframeApi().then((YT) => {
      if (cancelled || !YT?.Player || !youtubeFrameRef.current) return;

      if (youtubePlayerRef.current?.destroy) {
        youtubePlayerRef.current.destroy();
        youtubePlayerRef.current = null;
      }

      youtubePlayerRef.current = new YT.Player(youtubeFrameRef.current, {
        events: {
          onReady: (event) => {
            if (isMuted) event.target.mute();
            else event.target.unMute();
            if (isPlaying) event.target.playVideo();
          },
          onStateChange: (event) => {
            if (event.data === YT.PlayerState.ENDED) {
              event.target.seekTo(0, true);
              if (isPlaying) event.target.playVideo();
            }
          }
        }
      });
    });

    return () => {
      cancelled = true;
      if (youtubePlayerRef.current?.destroy) {
        youtubePlayerRef.current.destroy();
        youtubePlayerRef.current = null;
      }
    };
  }, [heroYoutubeId]);

  useEffect(() => {
    const player = youtubePlayerRef.current;
    if (!heroYoutubeId || !player?.playVideo) return;

    try {
      if (isMuted) player.mute();
      else player.unMute();

      if (isPlaying) player.playVideo();
      else player.pauseVideo();
    } catch {
      // The iframe API can ignore commands while the player is still booting.
    }
  }, [heroYoutubeId, isPlaying, isMuted]);

  const restartHeroVideo = () => {
    const video = heroVideoRef.current;
    if (!video) return;
    video.currentTime = 0;
    if (isPlaying) video.play().catch(() => {});
  };

  const handlePrevHero = () => {
    if (!heroMovies.length) return;
    setCurrentHeroIndex((prev) => (prev === 0 ? heroMovies.length - 1 : prev - 1));
  };

  const handleNextHero = () => {
    if (!heroMovies.length) return;
    setCurrentHeroIndex((prev) => (prev === heroMovies.length - 1 ? 0 : prev + 1));
  };

  const isMovieBookable = (movie) => movie?.status === 'NOW_SHOWING' || (!movie?.status && !movie?.isUpcoming);
  const isMovieWatchlisted = (movie) => watchlist.some((item) => (
    String(item.backendId || item.movieId || item.id) === String(movie?.backendId || movie?.movieId || movie?.id)
  ));

  // Personalized recommendations from the AI service (collaborative filtering).
  useEffect(() => {
    if (!isLoggedIn || !currentUser?.id) {
      setPersonalRecs([]);
      return undefined;
    }
    let cancelled = false;
    const { accessToken } = getStoredAuth();
    recommendationService.getCollaborativeRecommendations(currentUser.id, accessToken)
      .then(async (items) => {
        if (cancelled) return;
        const list = Array.isArray(items) ? [...items] : [];
        // The featured card shows synopsis/director, which the slim rec payload lacks —
        // fetch the full detail for the top recommendation (pagination-independent).
        if (list.length) {
          try {
            const detail = await movieService.getMovieDetail(list[0].backendId || list[0].id);
            if (detail?.id) list[0] = { ...detail, ...pickRecExplanation(list[0]) };
          } catch { /* keep the slim rec if the detail call fails */ }
        }
        if (!cancelled) setPersonalRecs(list);
      })
      .catch(() => {
        if (!cancelled) setPersonalRecs([]);
      });
    return () => {
      cancelled = true;
    };
  }, [isLoggedIn, currentUser?.id]);


  // Enrich a recommendation item with full movie data (synopsis, status…) when we already have it.
  const enrichRecommendation = (rec) => {
    const full = publicMovies.find((m) => (
      String(m.backendId || m.movieId || m.id) === String(rec.backendId || rec.id)
    ));
    return full ? { ...full, ...pickRecExplanation(rec) } : rec;
  };

  const openRecInfo = () => {
    setRecInfoOpen(true);
    if (!recStats) {
      recommendationService.getRecommendationStats()
        .then((stats) => { if (stats) setRecStats(stats); })
        .catch(() => {});
    }
  };

  // Trạng thái của khối gợi ý: cá nhân hóa thật / cold-start fallback / AI service không phản hồi.
  const recSource = personalRecs[0]?.source;
  const isPersonalizedRecs = recSource === 'collaborative';
  const isFallbackRecs = typeof recSource === 'string' && recSource.startsWith('fallback');
  const hasAiRecs = personalRecs.length > 0;
  const recAnchorTitle = personalRecs.find((r) => r.anchorTitle)?.anchorTitle;

  const genresList = [
    { title: 'CINEMATIC NOIR', tags: 'Kịch Tính • Tăm Tối', bg: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10' },
    { title: 'SCI-FI CYBER', tags: 'Tương Lai • Lượng Tử', bg: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10' },
    { title: 'VISION QUEST', tags: 'Kỳ Ảo • Hoạt Họa', bg: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10' },
    { title: 'PURE ACTION', tags: 'Võ Thuật • Rượt Đuổi', bg: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10' }
  ];

  return (
    <div className="pb-32 relative">
      {/* Snowfall */}
      <Snowfall color="rgba(255,255,255,0.4)" snowflakeCount={10} style={{ position: 'fixed', width: '100vw', height: '100vh', zIndex: 1 }} />

      {/* Ambient glow */}
      <div className="pointer-events-none fixed inset-0 -z-50">
        <div className="absolute left-1/2 top-0 h-[700px] w-[900px] -translate-x-1/2 bg-purple-700/15 blur-[180px]" />
        <div className="absolute left-1/4 top-1/2 h-[400px] w-[400px] bg-purple-900/10 blur-[120px]" />
      </div>

      {/* ── BANNER ── */}
      <div className="relative mb-16 mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
        <div className="relative overflow-hidden" style={{ height: '320px' }}>
          {banners.map((banner, index) => (
            <img
              key={index}
              src={banner}
              className={`absolute inset-0 w-full h-full object-cover transition-transform duration-700 ${
                index === currentIndex ? 'translate-x-0' : index < currentIndex ? '-translate-x-full' : 'translate-x-full'
              }`}
              alt={`Banner ${index + 1}`}
            />
          ))}
          {/* bottom fade */}
          <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-black to-transparent" />
          {/* side controls */}
          <button onClick={() => setCurrentIndex((p) => (p === 0 ? banners.length - 1 : p - 1))}
            className="absolute left-4 top-1/2 -translate-y-1/2 z-20 flex h-10 w-10 items-center justify-center bg-black/40 border border-white/20 text-white hover:bg-black/70 transition">
            <ChevronLeft className="h-5 w-5" />
          </button>
          <button onClick={() => setCurrentIndex((p) => (p === banners.length - 1 ? 0 : p + 1))}
            className="absolute right-4 top-1/2 -translate-y-1/2 z-20 flex h-10 w-10 items-center justify-center bg-black/40 border border-white/20 text-white hover:bg-black/70 transition">
            <ChevronRight className="h-5 w-5" />
          </button>
          {/* dot indicators */}
          <div className="absolute bottom-6 left-1/2 -translate-x-1/2 flex gap-2 z-20">
            {banners.map((_, i) => (
              <button key={i} onClick={() => setCurrentIndex(i)}
                className={`h-1 transition-all duration-300 ${i === currentIndex ? 'w-6 bg-purple-400' : 'w-2 bg-white/30'}`} />
            ))}
          </div>
        </div>
      </div>

      {/* ── SECTIONS CONTAINER ── */}
      <div className="space-y-20 px-4 sm:px-8 lg:px-12 xl:px-16 mx-auto max-w-[1400px]">

      {/* 2. NOW PLAYING GRID */}
      <section id="now-playing-section">
        <div className="section-heading">
          <h2 className="section-title">Phim Đang Chiếu</h2>
          <p className="section-subtitle">Các tác phẩm đang chiếu tại rạp</p>
        </div>

        <div className="relative">
          <button
            type="button"
            onClick={() => scrollNowPlaying(-1)}
            aria-label="Phim trước"
            className="absolute -left-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-left-8"
          >
            <ChevronLeft className="h-6 w-6" />
          </button>

          <div ref={nowPlayingRef} id="now-playing-grid"
            className="flex gap-5 overflow-x-auto scroll-smooth snap-x snap-mandatory pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {nowPlaying.map((movie) => (
              <div key={movie.id} className="snap-start shrink-0 w-[185px] sm:w-[220px] lg:w-[calc((100%-3.75rem)/5)]">
                <MovieCard movie={movie} onSelect={onSelectMovie} onBook={onBookMovie}
                  isWatchlisted={isMovieWatchlisted(movie)} onToggleWatchlist={handleToggleWatchlist} />
              </div>
            ))}
          </div>

          <button
            type="button"
            onClick={() => scrollNowPlaying(1)}
            aria-label="Phim sau"
            className="absolute -right-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-right-8"
          >
            <ChevronRight className="h-6 w-6" />
          </button>
        </div>
        <div className="mt-10 flex justify-center">
          <button onClick={() => onTabChange('explore')} className="section-cta-btn">Xem Thêm</button>
        </div>
      </section>

      {/* 4. UPCOMING RELEASES */}
      <section id="upcoming-section">
        <div className="section-heading">
          <h2 className="section-title">Phim Sắp Chiếu VIP</h2>
          <p className="section-subtitle">Lưu trước thời khắc khởi chiếu và đặt chỗ tiên phong</p>
        </div>
        <div className="relative">
          <button type="button" onClick={() => scrollUpcoming(-1)} aria-label="Phim trước"
            className="absolute -left-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-left-8">
            <ChevronLeft className="h-6 w-6" />
          </button>
          <div ref={upcomingRef} id="upcoming-grid"
            className="flex gap-5 overflow-x-auto scroll-smooth snap-x snap-mandatory pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            {upcoming.map((movie) => (
              <div key={movie.id} className="snap-start shrink-0 w-[185px] sm:w-[220px] lg:w-[calc((100%-3.75rem)/5)]">
                <MovieCard movie={movie} onSelect={onSelectMovie} onBook={onBookMovie}
                  isWatchlisted={isMovieWatchlisted(movie)} onToggleWatchlist={handleToggleWatchlist} />
              </div>
            ))}
          </div>
          <button type="button" onClick={() => scrollUpcoming(1)} aria-label="Phim sau"
            className="absolute -right-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-right-8">
            <ChevronRight className="h-6 w-6" />
          </button>
        </div>
        <div className="mt-10 flex justify-center">
          <button onClick={() => onTabChange('explore')} className="section-cta-btn">Xem Thêm</button>
        </div>
      </section>

      {/* 3. PERSONALIZED RECOMMENDATIONS */}
      {isLoggedIn && (personalRecs.length > 0 || publicMovies.length > 0) && (
        <section id="personalized-highlights-section">
          <div className="section-heading">
            {hasAiRecs && (
              <div className="mb-3 flex items-center justify-center gap-2">
                <span className="inline-flex items-center gap-1.5 border border-purple-500/40 bg-purple-950/40 px-3 py-1 text-[10px] font-black uppercase tracking-[0.18em] text-purple-300">
                  <BrainCircuit className="h-3.5 w-3.5" />
                  {isPersonalizedRecs ? 'AI Gợi Ý · Collaborative Filtering' : 'AI Gợi Ý · Dữ Liệu Thực'}
                </span>
                <button type="button" onClick={openRecInfo} title="Cách hệ gợi ý hoạt động"
                  className="flex h-6 w-6 items-center justify-center text-purple-300/70 hover:text-purple-200 transition">
                  <Info className="h-4 w-4" />
                </button>
              </div>
            )}
            <h2 className="section-title">
              {isPersonalizedRecs ? 'Chọn Riêng Cho Bạn' : isFallbackRecs ? 'Phim Nổi Bật' : 'Phim Đang Chiếu'}
            </h2>
            <p className="section-subtitle">
              {isPersonalizedRecs
                ? (recAnchorTitle
                  ? `Phân tích đánh giá thực của khán giả có gu giống bạn — vì bạn đã chấm cao "${recAnchorTitle}"`
                  : 'Phân tích đánh giá thực của khán giả có gu giống bạn')
                : isFallbackRecs
                  ? 'Bạn chưa có đánh giá nào — đây là phim được chấm điểm và đặt vé nhiều nhất. Hãy đánh giá phim để nhận gợi ý cá nhân hóa.'
                  : 'Danh sách phim đang chiếu tại rạp'}
            </p>
          </div>
          <div className="relative">
            <button type="button" onClick={() => scrollPersonalRecs(-1)} aria-label="Phim trước"
              className="absolute -left-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-left-8">
              <ChevronLeft className="h-6 w-6" />
            </button>
            <div ref={personalRecsRef}
              className="flex gap-5 overflow-x-auto scroll-smooth snap-x snap-mandatory pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {(personalRecs.length > 0 ? personalRecs.map(enrichRecommendation) : publicMovies.slice(0, 8)).map((rec) => (
                <div key={rec.id} className="snap-start shrink-0 w-[185px] sm:w-[220px] lg:w-[calc((100%-3.75rem)/5)]">
                  <MovieCard movie={rec} onSelect={onSelectMovie} onBook={onBookMovie} recReason={rec.reason}
                    isWatchlisted={isMovieWatchlisted(rec)} onToggleWatchlist={handleToggleWatchlist} />
                </div>
              ))}
            </div>
            <button type="button" onClick={() => scrollPersonalRecs(1)} aria-label="Phim sau"
              className="absolute -right-4 top-1/2 z-20 flex h-8 w-8 -translate-y-1/2 items-center justify-center text-white/70 hover:text-white transition sm:-right-8">
              <ChevronRight className="h-6 w-6" />
            </button>
          </div>
          <div className="mt-10 flex justify-center">
            <button onClick={() => onTabChange('explore')} className="section-cta-btn">Xem Thêm</button>
          </div>
        </section>
      )}

      {/* 5. DISCOVER GENRES */}
      <section id="genres-section">
        <div className="section-heading">
          <h2 className="section-title">Khám Phá Vũ Trụ Thể Loại</h2>
          <p className="section-subtitle">Tìm phim theo thể loại yêu thích</p>
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4" id="discover-genres-grid">
          {genresList.map((g, i) => (
            <div key={i} onClick={() => onTabChange('explore')}
              className="group relative h-32 border border-purple-500/20 bg-gradient-to-br from-purple-950/40 to-black p-5 flex flex-col justify-end cursor-pointer hover:border-purple-400/60 transition-all duration-300 overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-t from-black via-black/50 to-purple-900/10" />
              <div className="absolute inset-0 bg-purple-500/0 group-hover:bg-purple-500/5 transition-all duration-300" />
              <div className="relative z-10">
                <h4 className="text-sm font-black uppercase tracking-[0.15em] text-white">{g.title}</h4>
                <p className="text-[10px] text-purple-300/70 mt-1 uppercase tracking-widest">{g.tags}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      </div>{/* end sections container */}

      {/* RECOMMENDER "HOW IT WORKS" MODAL */}
      {recInfoOpen && (
        <div className="fixed inset-0 z-[210] flex items-center justify-center bg-black/85 p-4" onClick={() => setRecInfoOpen(false)}>
          <div className="w-full max-w-md border border-purple-500/30 bg-[#0d0d0d] p-6" onClick={(e) => e.stopPropagation()}>
            <div className="mb-4 flex items-center justify-between gap-3">
              <h3 className="flex items-center gap-2 text-[12px] font-sans font-black uppercase tracking-[0.18em] text-white">
                <BrainCircuit className="h-4 w-4 text-purple-300" />
                Cách hệ gợi ý hoạt động
              </h3>
              <button onClick={() => setRecInfoOpen(false)} className="text-neutral-500 hover:text-white transition p-1">
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="space-y-3 text-[12px] font-sans leading-relaxed text-neutral-300">
              <p>
                <span className="font-bold text-purple-300">Gợi ý cá nhân hóa (Collaborative Filtering):</span>{' '}
                hệ thống so sánh vector đánh giá của bạn với toàn bộ khán giả khác bằng cosine similarity,
                chọn 20 người có gu giống bạn nhất và dự đoán điểm cho từng phim bạn chưa xem.
              </p>
              <p>
                <span className="font-bold text-purple-300">Phim tương tự (Content-based):</span>{' '}
                mô tả, thể loại, diễn viên và đạo diễn của mỗi phim được mã hóa thành embedding
                bằng mô hình SBERT (all-MiniLM-L6-v2), rồi so độ tương đồng ngữ nghĩa giữa các phim.
              </p>
              <div className="border border-purple-500/20 bg-purple-950/20 p-3">
                {recStats ? (
                  <p>
                    Đang phân tích <span className="font-black text-white">{recStats.reviewCount}</span> đánh giá
                    từ <span className="font-black text-white">{recStats.reviewerCount}</span> khán giả,{' '}
                    <span className="font-black text-white">{recStats.paidBookingCount}</span> vé đã bán
                    và <span className="font-black text-white">{recStats.embeddedMovieCount}</span> phim đã được mã hóa embedding.
                  </p>
                ) : (
                  <p className="text-neutral-500">Đang tải số liệu…</p>
                )}
              </div>
              <p className="text-[11px] text-neutral-500">
                Toàn bộ số liệu lấy trực tiếp từ cơ sở dữ liệu đánh giá và đặt vé của hệ thống — không dùng dữ liệu giả lập.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* POPCORN AI CHATBOT */}
      <div className="fixed bottom-6 right-6 z-[200] flex flex-col items-end gap-3">
        {chatOpen && (
          <div className="w-[340px] bg-[#0d0d0d] border border-purple-500/40 flex flex-col overflow-hidden rounded-2xl" style={{ height: 440, boxShadow: '0 8px 40px rgba(0,0,0,0.7), 0 0 0 1px rgba(168,85,247,0.15), 0 0 60px rgba(139,92,246,0.2)' }}>
            {/* Header */}
            <div className="flex items-center gap-3 px-4 py-3 bg-purple-950/50 border-b border-purple-500/20 rounded-t-2xl">
              <img src={popcornBot} alt="PopBot" className="w-9 h-9 object-cover flex-shrink-0 border border-purple-400/30 rounded-full" />
              <div className="flex-1 min-w-0">
                <p className="text-[11px] font-sans font-black text-white uppercase tracking-[0.12em]">PopBot AI</p>
              </div>
              <button onClick={handleResetChat} title="Bắt đầu hội thoại mới" className="text-neutral-500 hover:text-white transition p-1">
                <Sparkles className="w-4 h-4" />
              </button>
              <button onClick={() => setChatOpen(false)} className="text-neutral-500 hover:text-white transition p-1">
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3 [scrollbar-width:thin] [scrollbar-color:rgba(168,85,247,0.2)_transparent]">
              {chatMessages.map((msg, i) => (
                <div key={i} className={`flex gap-2 ${msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                  {msg.role === 'bot' && (
                    <img src={popcornBot} alt="bot" className="w-6 h-6 object-cover border border-purple-400/20 flex-shrink-0 mt-0.5 rounded-full" />
                  )}
                  <div className={`flex flex-col gap-2 ${msg.role === 'user' ? 'max-w-[78%] items-end' : 'max-w-[90%] items-start'}`}>
                    <div className={`px-3.5 py-2.5 text-[12px] font-sans leading-relaxed rounded-2xl ${
                      msg.role === 'user'
                        ? 'bg-purple-700 text-white rounded-tr-sm'
                        : 'bg-neutral-900 border border-white/8 text-neutral-200 rounded-tl-sm'
                    }`}>
                      {msg.text}
                    </div>
                    {msg.data?.movies?.length > 0 && (
                      <ChatMovieResults movies={msg.data.movies} onPlayTrailer={setChatTrailerUrl} />
                    )}
                    {msg.data?.showtimes?.length > 0 && (
                      <ChatShowtimeResults showtimes={msg.data.showtimes} />
                    )}
                  </div>
                </div>
              ))}
              {chatMessages.length === 1 && (
                <div className="flex flex-wrap gap-1.5 pl-8">
                  {CHAT_QUICK_QUESTIONS.map((q) => (
                    <button
                      key={q}
                      type="button"
                      onClick={() => handleSendChat(q)}
                      className="px-3 py-1.5 text-[11px] font-sans text-purple-300 border border-purple-500/30 hover:bg-purple-500/10 transition rounded-full"
                    >
                      {q}
                    </button>
                  ))}
                </div>
              )}
              <div ref={chatEndRef} />
            </div>

            {/* Input */}
            <div className="flex items-center gap-2 px-4 py-3 border-t border-white/8">
              <input
                type="text"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSendChat();
                }}
                disabled={chatSending}
                placeholder="Hỏi về phim, lịch chiếu..."
                className="flex-1 bg-neutral-800/60 px-4 py-2.5 text-[12px] font-sans text-white placeholder-neutral-500 outline-none rounded-full disabled:opacity-60"
              />
              <button
                onClick={() => handleSendChat()}
                disabled={chatSending}
                className="bg-purple-600 hover:bg-purple-500 transition p-2.5 text-white rounded-full flex-shrink-0 disabled:opacity-60"
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* Floating button */}
        <button
          onClick={() => setChatOpen((o) => !o)}
          className="relative w-14 h-14 overflow-hidden border-2 border-purple-400/50 hover:border-purple-400 shadow-lg shadow-purple-900/40 hover:scale-105 transition-all duration-200 rounded-full"
          title="Chat với PopBot AI"
        >
          <img src={popcornBot} alt="PopBot" className="w-full h-full object-cover" />
        </button>
      </div>

      {chatTrailerUrl && createPortal(
        <div
          className="fixed inset-0 z-[300] flex items-center justify-center bg-black/90 p-4"
          onClick={() => setChatTrailerUrl(null)}
        >
          <div className="relative w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => setChatTrailerUrl(null)}
              className="absolute -top-10 right-0 text-white/70 hover:text-white transition"
              aria-label="Đóng trailer"
            >
              <X className="w-6 h-6" />
            </button>
            <video src={chatTrailerUrl} className="w-full rounded-lg" controls autoPlay playsInline />
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}

