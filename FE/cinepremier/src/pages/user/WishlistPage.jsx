import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Film, SlidersHorizontal, Bell, Ticket, Check, Trash2 } from 'lucide-react';
import { useMovies } from '../../stores/useMovieStore';
import { useUiStore } from '../../stores/useUiStore';

export default function WishlistView() {
  const navigate = useNavigate();
  const { watchlist, handleToggleWatchlist: onToggleWatchlist } = useMovies();
  const showToast = useUiStore((state) => state.showToast);
  const onBookMovie = (movie) => navigate(`/movies/${movie.id}/book`);
  const onSelectMovie = (id) => navigate(`/movies/${id}`);
  const [filter, setFilter] = useState('ALL');
  const [showFilterDropdown, setShowFilterDropdown] = useState(false);
  const [emailInput, setEmailInput] = useState('');
  const [isSubscribed, setIsSubscribed] = useState(false);

  // Use only real watchlist items from backend state.

  const dynamicRealItems = watchlist.map((m) => ({
    id: m.id,
    backendId: m.backendId || m.movieId || m.id,
    title: m.title,
    genreSubtitle: `${(m.genre || []).join(' • ')} • ${(m.duration || 0)} PHÚT`,
    status: m.isUpcoming ? 'UPCOMING' : 'SHOWING',
    badge: m.isUpcoming ? 'SẮP RA MẮT' : 'SÁNG CHIẾU',
    badgeColor: m.isUpcoming ? 'bg-red-650 text-white border-red-500' : 'bg-green-950 text-emerald-400 border-emerald-500/20',
    actionLabel: m.isUpcoming ? 'Nhận thông báo' : 'Đặt vé ngay',
    actionIcon: m.isUpcoming ? 'bell' : 'ticket',
    posterUrl: m.posterUrl,
    isReal: true,
    movie: m
  }));

  const combinedItems = dynamicRealItems;

  // Apply visual filtering
  const filteredItems = combinedItems.filter((item) => {
    if (filter === 'ALL') return true;
    return item.status === filter;
  });

  const handleActionClick = (item) => {
    if (item.isReal) {
      if (item.status === 'SHOWING') {
        onBookMovie(item.movie || { id: item.id });
      } else {
        showToast(`Đã bật theo dõi cho phim: ${item.title}. Bạn sẽ nhận thông báo khi có lịch chiếu sớm.`);
      }
    } else {
      if (item.id === 'ban-giao-huong') {
        onBookMovie(item.movie || { id: item.id });
      } else if (item.status === 'UPCOMING') {
        showToast(`Đặt nhắc hẹn khởi chiếu phim "${item.title}" thành công!`);
      } else {
        showToast(`Phim "${item.title}" đã được trải nghiệm ở rạp CINEPREMIER.`);
      }
    }
  };

  const handleSubscribe = (e) => {
    e.preventDefault();
    if (!emailInput) return;
    setIsSubscribed(true);
    showToast(`Đăng ký tin tức VIP qua email: ${emailInput} khả dụng thành công. Hưởng trọn ưu đãi thảm đỏ sắp tới!`);
    setEmailInput('');
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-12">

      {/* HEADER ROW WITH LỌC THEO DROPDOWN */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">

        {/* BIG STYLED BANNER HEADINGS matches screenshot 3 */}
        <div className="space-y-4 max-w-xl text-left">
          <div>
            <h1 className="whitespace-nowrap text-2xl sm:text-5xl font-black leading-none uppercase tracking-wide">
              <span className="font-mono text-white">DANH SÁCH </span>
              <span className="font-serif italic text-transparent bg-clip-text bg-gradient-to-r from-rose-500 to-amber-400">THEO DÕI</span>
            </h1>
          </div>
          <p className="text-xs text-neutral-400 leading-relaxed font-sans max-w-lg font-light">
            Nơi lưu giữ những tuyệt phẩm điện ảnh bạn hằng mong đợi. Nhận thông báo tức thì khi màn bạc bắt đầu lung linh.
          </p>
        </div>

        {/* FILTER CONTROL PANEL DROPDOWN */}
        <div className="relative self-start md:self-end">
          <button
            onClick={() => setShowFilterDropdown(!showFilterDropdown)}
            className="flex items-center gap-2 border border-white/10 bg-neutral-950 px-4 py-2.5 text-[10px] font-sans font-bold uppercase tracking-[0.15em] text-neutral-400 hover:text-white hover:border-white transition-all rounded-none"
            id="filter-button"
          >
            <SlidersHorizontal className="h-3.5 w-3.5" />
            LỌC THEO: <span className="text-white font-mono font-black">{filter}</span>
          </button>

          {showFilterDropdown && (
            <div className="absolute right-0 mt-2 w-48 border border-white/15 bg-black p-1 shadow-2xl z-50 text-left font-sans">
              {[
                { label: 'TẤT CẢ PHIM', val: 'ALL' },
                { label: 'SẮP KHỞI CHIẾU', val: 'UPCOMING' },
                { label: 'ĐANG TRÌNH CHIẾU', val: 'SHOWING' },
                { label: 'ĐÃ TRẢI NGHIỆM', val: 'WATCHED' }
              ].map((opt) => (
                <button
                  key={opt.val}
                  onClick={() => {
                    setFilter(opt.val);
                    setShowFilterDropdown(false);
                  }}
                  className={`flex w-full items-center px-4 py-2 text-[9.5px] uppercase tracking-wide font-black border-l-2 ${filter === opt.val
                    ? 'border-white text-white bg-neutral-950'
                    : 'border-transparent text-neutral-400 hover:bg-neutral-900 hover:text-white'
                    } transition`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          )}
        </div>

      </div>

      {/* ITEMS DECK CARDS GRID SCREENSHOT 3 STYLE */}
      {filteredItems.length === 0 ? (
        <div className="border border-dashed border-white/10 bg-black px-6 py-14 text-center">
          <Film className="mx-auto h-9 w-9 text-neutral-600" />
          <h2 className="text-xl sm:text-2xl font-sans font-bold text-white">
            Danh sách yêu thích đang trống
          </h2>
          <p className="mx-auto mt-3 max-w-md text-xs leading-6 text-neutral-500">
            Hãy mở trang khám phá phim và bấm biểu tượng trái tim trên poster hoặc trong trang chi tiết để lưu phim vào wishlist.
          </p>
          <button
            type="button"
            onClick={() => navigate('/movies')}
            className="mt-6 border border-white bg-white px-6 py-3 text-[10px] font-black uppercase tracking-[0.2em] text-black transition hover:bg-black hover:text-white"
          >
            Khám phá phim
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6" id="watchlist-deck-grid">
          {filteredItems.map((item, index) => (
            <div
              key={`${item.id}-${index}`}
              className="group border border-white/5 bg-[#050505] overflow-hidden flex flex-col justify-between relative transition duration-300 hover:border-white/15"
            >

              {/* Poster image container */}
              <div className="relative aspect-[3/4.2] overflow-hidden bg-neutral-950">

                <img
                  src={item.posterUrl}
                  alt={item.title}
                  className="w-full h-full object-cover transition duration-700 ease-out group-hover:scale-105"
                  referrerPolicy="no-referrer"
                />

                {/* Black subtle overlay gradient */}
                <div className="absolute inset-0 bg-gradient-to-t from-neutral-950 via-transparent to-transparent opacity-80"></div>

                {/* Top left text status badge */}
                <div className="absolute left-2.5 top-2.5">
                  <span className={`inline-block border text-[7.5px] font-black px-2 py-0.5 tracking-wider rounded-none uppercase bg-black text-white border-white/20`}>
                    {item.badge}
                  </span>
                </div>

                {/* Optional top-right absolute icon badge shown in third card */}
                {item.badgeTopRightIcon === 'bell' && (
                  <div className="absolute right-2.5 top-2.5 bg-black border border-white/20 p-1 text-white">
                    <Bell className="h-3 w-3 fill-current text-purple-400" />
                  </div>
                )}

                {/* Optional absolute announcement layer over image - e.g. Card 3 "Sẽ ra mắt vào tháng 12" */}
                {item.id === 'anh-sang' && (
                  <div className="absolute inset-0 flex items-center justify-center p-3 bg-neutral-950/75 text-center pointer-events-none">
                    <span className="text-[10px] font-serif italic font-bold uppercase text-neutral-400 tracking-widest border-t border-b border-white/10 py-1.5 block w-full">
                      Sẽ ra mắt vào tháng 12
                    </span>
                  </div>
                )}

                {/* Mini deletion float trigger for real items */}
                {item.isReal && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleWatchlist(item.movie || { id: item.id, backendId: item.backendId });
                    }}
                    className="absolute right-2 bottom-2 bg-black/60 border border-white/5 hover:border-white hover:bg-neutral-950 p-1.5 text-neutral-500 hover:text-white transition rounded-none z-10"
                    title="Xóa khỏi Watchlist"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                )}

              </div>

              {/* Bottom details block */}
              <div className="p-3.5 space-y-3.5 bg-neutral-950 text-left">
                <div className="space-y-0.5 min-h-[44px] flex flex-col justify-center">
                  <h4 className="text-xs sm:text-[12.5px] font-sans font-bold text-white truncate">
                    {item.title}
                  </h4>
                  <p className="text-[7.5px] font-sans font-bold uppercase tracking-widest text-[#888888] truncate">
                    {item.genreSubtitle}
                  </p>
                </div>

                {/* Red-ish styled action buttons representing screenshot 3 */}
                <button
                  onClick={() => handleActionClick(item)}
                  className={`w-full py-2 text-[8px] font-sans font-black uppercase tracking-[0.2em] transition-all duration-300 text-center flex items-center justify-center gap-1.5 ${item.id === 'anh-sang'
                    ? 'bg-neutral-900 border border-neutral-800 text-neutral-500 cursor-not-allowed'
                    : item.actionIcon === 'bell'
                      ? 'bg-purple-900 border border-purple-500 hover:bg-black hover:text-white text-white'
                      : item.actionIcon === 'ticket'
                        ? 'bg-white hover:bg-neutral-200 text-black'
                        : 'border border-white/10 text-neutral-400 hover:text-white hover:border-white'
                    }`}
                  disabled={item.id === 'anh-sang'}
                  style={item.actionIcon === 'bell' ? { background: 'linear-gradient(90deg, #8E2DE2 0%, #4A00E0 100%)' } : {}}
                >
                  {item.actionIcon === 'bell' && <Bell className="h-3 w-3" />}
                  {item.actionIcon === 'ticket' && <Ticket className="h-3 w-3" />}
                  {item.actionIcon === 'check' && <Check className="h-3 w-3" />}
                  {item.actionLabel}
                </button>
              </div>

            </div>
          ))}
        </div>
      )}

      {/* FOOTER NEWSLETTER BANNER SCREENSHOT 3 PROMO MOCKUP */}
      <div
        className="border border-white/15 bg-black p-8 sm:p-10 relative overflow-hidden"
        style={{ background: 'linear-gradient(135deg, #120509 0%, #050106 100%)' }}
      >

        {/* Glow dots backgrounds */}
        <div className="absolute right-10 top-0 bottom-0 w-1/3 opacity-20 pointer-events-none select-none">
          <svg className="w-full h-full text-white/10" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="40" fill="none" stroke="currentColor" strokeWidth="1" strokeDasharray="3 3" />
            <circle cx="50" cy="50" r="25" fill="none" stroke="currentColor" strokeWidth="1" />
          </svg>
        </div>

        <div className="max-w-2xl text-center mx-auto space-y-6 relative z-10 font-sans">

          <div className="space-y-1.5">
            <h2 className="text-xl sm:text-2xl font-sans font-bold text-white">
              Đừng bỏ lỡ bất kỳ khoảnh khắc nào
            </h2>
            <p className="text-[10px] sm:text-xs text-neutral-400 leading-relaxed font-light uppercase tracking-wide">
              Đăng ký nhận tin để được ưu tiên đặt vé cho những suất chiếu đặc biệt và sự kiện thảm đỏ CINEPREMIER.
            </p>
          </div>

          <form onSubmit={handleSubscribe} className="flex flex-col sm:flex-row gap-3 max-w-md mx-auto">
            <input
              type="email"
              required
              placeholder="Email của bạn..."
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)}
              className="flex-1 border border-white/10 bg-neutral-950/80 p-3 text-xs text-white placeholder-neutral-700 tracking-wider focus:outline-none focus:border-white rounded-none uppercase"
            />
            <button
              type="submit"
              className="bg-[#D32F2F] text-white hover:bg-white hover:text-black font-semibold text-[10px] uppercase tracking-[0.2em] px-6 py-3.5 transition duration-300 rounded-none shrink-0"
              style={{ background: 'linear-gradient(90deg, #F00000 0%, #BE123C 100%)' }}
            >
              THAM GIA NGAY
            </button>
          </form>

          <p className="text-[8px] text-neutral-600 uppercase tracking-widest font-black">
            CINEPREMIER SỰ KIỆN MÀN ẢNH VÀ QUÀ TẶNG THƯỜNG NIÊN
          </p>

        </div>

      </div>

    </div>
  );
}
