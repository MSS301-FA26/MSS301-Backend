import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, ChevronRight, ChevronLeft, Film, Filter, CalendarDays, X } from 'lucide-react';
import MovieCard from '@/components/common/MovieCard';
import { useMovies } from '../../stores/useMovieStore';

export default function ExploreView() {
  const navigate = useNavigate();
  const {
    moviesList = [],
    isMoviesLoading: isLoading = false,
    moviePagination: pagination = null,
    setMoviePagination,
    searchQuery = '',
    setSearchQuery,
    movieDateFilter: selectedDate = '',
    setMovieDateFilter,
    genres = [],
    selectedGenreId = '',
    setSelectedGenreId,
    watchlist = [],
    handleToggleWatchlist,
  } = useMovies();
  const onSearchChange = (q) => { setSearchQuery(q); setMoviePagination(prev => ({ ...prev, page: 0 })); };
  const onDateChange = (d) => { setMovieDateFilter(d); setMoviePagination(prev => ({ ...prev, page: 0 })); };
  const onPageChange = (page) => setMoviePagination(prev => ({ ...prev, page: page - 1 }));
  const onSelectMovie = (id) => navigate(`/movies/${id}`);
  const onBookMovie = (movie) => navigate(`/movies/${movie.id}/book`);
  const isMovieWatchlisted = (movie) => watchlist.some((item) => (
    String(item.backendId || item.movieId || item.id) === String(movie.backendId || movie.movieId || movie.id)
  ));
  const [sortBy, setSortBy] = useState('rating'); // rating, newest, duration
  const [localPage, setLocalPage] = useState(1);
  const itemsPerPage = 10; // keep in sync with useMovieStore moviePagination.size
  const currentPage = pagination ? (Number(pagination.page) || 0) + 1 : localPage;

  // Filter and sort logical step
  const processedMovies = useMemo(() => {
    let result = moviesList.filter((movie) => movie.status !== 'INACTIVE' && !movie.isInactive);

    // 1. Text Search Filter (null-guard: phim có thể thiếu englishTitle/director/genre)
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter(
        (m) =>
          (m.title || '').toLowerCase().includes(q) ||
          (m.englishTitle || '').toLowerCase().includes(q) ||
          (m.director || '').toLowerCase().includes(q) ||
          (m.genre || []).some(g => String(g || '').toLowerCase().includes(q))
      );
    }

    // 2. Sorting
    result.sort((a, b) => {
      if (sortBy === 'rating') {
        const aRating = a.ratings?.overall || 0;
        const bRating = b.ratings?.overall || 0;
        return bRating - aRating;
      }
      if (sortBy === 'newest') {
        return b.id.localeCompare(a.id);
      }
      if (sortBy === 'duration') {
        return b.duration - a.duration;
      }
      return 0;
    });

    return result;
  }, [searchQuery, sortBy, moviesList]);

  // Pagination logical step
  const totalPages = pagination?.totalPages || Math.max(1, Math.ceil(processedMovies.length / itemsPerPage));
  const currentMovies = useMemo(() => {
    if (pagination) return processedMovies;
    const start = (currentPage - 1) * itemsPerPage;
    return processedMovies.slice(start, start + itemsPerPage);
  }, [processedMovies, currentPage, pagination]);

  const handlePageChange = (page) => {
    if (page >= 1 && page <= totalPages) {
      if (pagination) {
        onPageChange(page);
      } else {
        setLocalPage(page);
      }
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8 space-y-10 pb-24">

      {/* Search Header Container */}
      <div className="space-y-4 border-b border-white/5 pb-6">
        <div className="flex items-center space-x-3">
          <Film className="h-5 w-5 text-white" />
          <h1 className="text-3xl sm:text-5xl font-serif text-white uppercase tracking-wider font-light italic">
            CinePremier / Explore
          </h1>
        </div>
        <p className="text-xs sm:text-sm text-neutral-400 font-sans leading-relaxed max-w-2xl">
          Trải nghiệm vũ trụ điện ảnh và khám phá bộ sưu tập phim hấp dẫn tại CinePremier.
        </p>
      </div>

      {/* Grid movies or Blank fallback page */}
      {currentMovies.length > 0 ? (
        <div className="grid grid-cols-2 gap-5 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5" id="explore-movies-grid">
          {currentMovies.map((movie) => (
            <MovieCard
              key={movie.id}
              movie={movie}
              onSelect={onSelectMovie}
              onBook={onBookMovie}
              isWatchlisted={isMovieWatchlisted(movie)}
              onToggleWatchlist={handleToggleWatchlist}
            />
          ))}
        </div>
      ) : (
        <div className="border border-dashed border-white/10 bg-black p-16 text-center space-y-4">
          <Filter className="h-8 w-8 text-neutral-600 mx-auto" />
          <h3 className="text-base font-sans font-bold text-white">Không tìm thấy phim phù hợp</h3>
          <p className="text-xs text-neutral-500 max-w-md mx-auto font-sans leading-relaxed">
            {selectedDate
              ? `Bộ lọc ngày ${selectedDate.split('-').reverse().join('/')} đang được áp dụng — thử bỏ lọc ngày hoặc đổi từ khóa khác.`
              : 'Thử từ khóa khác hoặc xóa bộ lọc để xem toàn bộ phim đang chiếu.'}
          </p>
          <button
            onClick={() => {
              setSelectedGenreId('');
              onSearchChange('');
              onDateChange('');
              handlePageChange(1);
            }}
            className="border border-white bg-white text-black px-6 py-2.5 text-[10px] font-sans tracking-widest uppercase hover:bg-black hover:text-white transition"
          >
            Xóa bộ lọc
          </button>
        </div>
      )}

      {/* Pagination Container */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center space-x-2 pt-8" id="pagination">

          {/* Back button */}
          <button
            disabled={currentPage === 1}
            onClick={() => handlePageChange(currentPage - 1)}
            className="border border-white/10 bg-black p-2.5 text-white hover:border-white disabled:opacity-30 transition"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>

          {/* Numeric buttons */}
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((pg) => (
            <button
              key={pg}
              onClick={() => handlePageChange(pg)}
              className={`h-9 w-9 text-xs transition font-sans ${currentPage === pg
                  ? 'bg-white text-black font-bold border border-white'
                  : 'border border-white/10 bg-black text-neutral-400 hover:border-white hover:text-white'
                }`}
            >
              {pg}
            </button>
          ))}

          {/* Forward button */}
          <button
            disabled={currentPage === totalPages}
            onClick={() => handlePageChange(currentPage + 1)}
            className="border border-white/10 bg-black p-2.5 text-white hover:border-white disabled:opacity-30 transition"
          >
            <ChevronRight className="h-4 w-4" />
          </button>

        </div>
      )}

    </div>
  );
}
