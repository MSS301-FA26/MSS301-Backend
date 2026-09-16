import React from 'react';
import { Calendar, Heart, Play, Sparkles } from 'lucide-react';

export default function MovieCard({ movie, onSelect, onBook, isWatchlisted = false, onToggleWatchlist, recReason }) {
  const isBookable = movie.status === 'NOW_SHOWING' || (!movie.status && !movie.isUpcoming);
  const isUpcoming = movie.status === 'UPCOMING' || movie.isUpcoming;

  const getAgeRatingColor = (rating) => {
    switch (rating) {
      case 'T18': return 'bg-red-600 text-white';
      case 'T16': return 'bg-amber-500 text-white';
      case 'T13': return 'bg-yellow-400 text-black';
      default: return 'bg-emerald-600 text-white';
    }
  };

  return (
    <div
      className="group relative flex flex-col bg-neutral-900 transition-all duration-300 hover:ring-1 hover:ring-purple-500/60"
      id={`movie-${movie.id}`}
    >
      {/* Poster */}
      <div className="relative aspect-[2/3] w-full overflow-hidden bg-neutral-800">
        <img
          src={movie.posterUrl || 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10'}
          alt={movie.title}
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          referrerPolicy="no-referrer"
          onError={(e) => {
            e.target.src = 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10';
          }}
        />

        {/* Hover overlay */}
        <div className="absolute inset-0 bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
          <div className="absolute inset-x-4 top-1/2 -translate-y-1/2 flex flex-col gap-2">
            <button
              onClick={(e) => { e.stopPropagation(); onSelect(movie.id); }}
              className="flex items-center justify-center gap-2 w-full border border-white bg-black/80 text-white text-[11px] font-sans uppercase tracking-[0.12em] py-2.5 hover:bg-white hover:text-black transition-all duration-200"
            >
              <Play className="h-3 w-3 fill-current" />
              Chi Tiết Phim
            </button>
            {isBookable ? (
              <button
                onClick={(e) => { e.stopPropagation(); onBook(movie); }}
                className="flex items-center justify-center w-full border border-white bg-white text-black text-[11px] font-sans uppercase tracking-[0.12em] py-2.5 hover:bg-black hover:text-white transition-all duration-200"
              >
                Đặt Vé Ngay
              </button>
            ) : (
              <div className="w-full text-center border border-white/20 bg-neutral-900/80 text-white text-[11px] uppercase tracking-[0.12em] py-2.5">
                Sắp Ra Mắt
              </div>
            )}
          </div>
          {onToggleWatchlist && (
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); onToggleWatchlist(movie); }}
              className={`absolute bottom-3 right-3 flex h-8 w-8 items-center justify-center border transition-all duration-200 ${
                isWatchlisted
                  ? 'bg-rose-500 border-rose-400 text-white'
                  : 'bg-black/60 border-white/30 text-white hover:bg-white hover:text-black'
              }`}
              title={isWatchlisted ? 'Xóa khỏi yêu thích' : 'Thêm vào yêu thích'}
            >
              <Heart className={`h-3.5 w-3.5 ${isWatchlisted ? 'fill-current' : ''}`} />
            </button>
          )}
        </div>

        {/* Age rating */}
        <div className={`absolute right-2 top-2 px-1.5 py-0.5 text-[9px] font-black ${getAgeRatingColor(movie.ageRating)}`}>
          {movie.ageRating}
        </div>

        {/* Rating badge */}
        {isBookable && movie.ratings?.overall != null && movie.ratings?.count > 0 && (
          <div className="absolute left-2 top-2 flex items-center gap-1 bg-black/80 px-2 py-1 text-[10px] font-bold text-yellow-400">
            ★ {movie.ratings.overall}
          </div>
        )}

        {/* Upcoming ribbon */}
        {isUpcoming && movie.upcomingDate && (
          <div className="absolute left-2 top-2 flex items-center gap-1 bg-purple-700 px-2 py-1 text-[10px] font-bold text-white">
            <Calendar className="h-3 w-3" />
            {movie.upcomingDate}
          </div>
        )}

      </div>

      {/* Info */}
      <div className="p-3 bg-[#0d0d0d]">
        <h3
          onClick={() => onSelect(movie.id)}
          className="cursor-pointer text-sm font-bold text-white hover:text-purple-300 transition-colors line-clamp-1 leading-snug"
        >
          {movie.title}
        </h3>
        <div className="mt-1.5 flex items-center justify-between gap-2">
          <div className="flex flex-wrap gap-1">
            {movie.genre.slice(0, 2).map((g) => (
              <span key={g} className="border border-white/20 bg-white/5 text-neutral-400 px-1.5 py-0.5 text-[9px] font-semibold uppercase tracking-wide">
                {g}
              </span>
            ))}
          </div>
          <span className="text-[10px] text-neutral-500 shrink-0">{movie.duration} phút</span>
        </div>
        {recReason && (
          <div className="mt-1.5 flex items-center gap-1 text-[10px] text-purple-300/80" title={recReason}>
            <Sparkles className="h-3 w-3 shrink-0" />
            <span className="line-clamp-1">{recReason}</span>
          </div>
        )}
      </div>
    </div>
  );
}
