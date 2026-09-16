import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Ticket, Calendar, MapPin, Star, CheckCircle, Clock, Loader2, MoreVertical, ScanLine, XCircle, MessageSquare, Pencil, Trash2, Save, X, ChevronLeft, ChevronRight, EyeOff } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { getStoredAuth } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { reviewService } from '../../services/reviewService';
import { useMovies } from '../../stores/useMovieStore';
import { useAuthStore } from '../../stores/useAuthStore';
import { useUiStore } from '../../stores/useUiStore';

export default function MyTicketsView({ embedded = false }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const highlightBookingId = searchParams.get('highlightBookingId') || searchParams.get('bookingId');
  const [highlightedId, setHighlightedId] = useState(null);
  const hasProcessedHighlightRef = useRef(null);
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const showToast = useUiStore((state) => state.showToast);
  const setShowOTP = useUiStore((state) => state.setShowOTP);
  const { publicCinema, moviesList = [] } = useMovies();
  const onSelectMovie = (id) => navigate(`/movies/${id}`);
  const onOpenOTP = () => setShowOTP(true);
  const [reviewContent, setReviewContent] = useState('');
  const [reviewRating, setReviewRating] = useState(5);
  const [selectedReviewMovie, setSelectedReviewMovie] = useState('');
  const [reviewsList, setReviewsList] = useState([]);
  const [realBookings, setRealBookings] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [cancellingBookingId, setCancellingBookingId] = useState('');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [editingReviewId, setEditingReviewId] = useState('');
  const [editReviewContent, setEditReviewContent] = useState('');
  const [editReviewRating, setEditReviewRating] = useState(5);
  const [savingReviewId, setSavingReviewId] = useState('');
  const [deletingReviewId, setDeletingReviewId] = useState('');
  const [ticketPage, setTicketPage] = useState(1);
  const [historyPage, setHistoryPage] = useState(1);
  const reviewSectionRef = useRef(null);

  const TICKET_PAGE_SIZE = 5;
  const HISTORY_PAGE_SIZE = 10;

  const canOrderMoreFood = (t) => (
    ['PAID', 'USED'].includes(t?.status) || Boolean(t?.isWatching)
  );

  const loadBookings = () => {
    if (!isLoggedIn) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    setIsLoading(true);
    return bookingService.getMyBookings(accessToken)
      .then(data => setRealBookings(Array.isArray(data) ? data : []))
      .catch(() => setRealBookings([]))
      .finally(() => setIsLoading(false));
  };

  const loadMyReviews = () => {
    if (!isLoggedIn) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    return reviewService.getMyReviews(accessToken)
      .then(data => setReviewsList(Array.isArray(data) ? data : []))
      .catch(() => setReviewsList([]));
  };

  useEffect(() => {
    loadBookings();
    loadMyReviews();
  }, [isLoggedIn]);

  // Đồng hồ 1s cho countdown giữ ghế của booking chờ thanh toán
  const [clockTick, setClockTick] = useState(Date.now());
  const expiredResyncRef = useRef(new Set());
  useEffect(() => {
    const hasActiveHold = realBookings.some(
      (b) => ['HOLDING', 'PENDING_PAYMENT'].includes(b.status) && b.holdExpiresAt
    );
    if (!hasActiveHold) return undefined;
    const intervalId = window.setInterval(() => setClockTick(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, [realBookings]);

  // Hết giờ giữ ghế → chờ BE scheduler dọn rồi tải lại danh sách 1 lần để đồng bộ EXPIRED
  useEffect(() => {
    realBookings.forEach((b) => {
      if (!['HOLDING', 'PENDING_PAYMENT'].includes(b.status) || !b.holdExpiresAt) return;
      if (new Date(b.holdExpiresAt).getTime() > clockTick) return;
      const key = String(b.id);
      if (expiredResyncRef.current.has(key)) return;
      expiredResyncRef.current.add(key);
      window.setTimeout(() => loadBookings(), 10000);
    });
  }, [clockTick, realBookings]);

  const formatCountdown = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const getBookingBadge = (status) => {
    switch (status) {
      case 'PAID':
        return 'ĐÃ THANH TOÁN';
      case 'USED':
        return 'ĐÃ SỬ DỤNG';
      case 'PENDING_PAYMENT':
        return 'CHỜ THANH TOÁN';
      case 'HOLDING':
        return 'ĐANG GIỮ';
      case 'EXPIRED':
        return 'ĐÃ HẾT HẠN';
      case 'CANCELLED':
        return 'ĐÃ HỦY';
      case 'REFUNDED':
        return 'ĐÃ HOÀN TIỀN';
      default:
        return status || 'KHÔNG RÕ';
    }
  };

  const getBookingBadgeColor = (status) => {
    switch (status) {
      case 'PAID':
        return 'bg-emerald-950/30 text-emerald-400 border-emerald-500/20';
      case 'USED':
        return 'bg-neutral-900 text-neutral-400 border-neutral-700';
      case 'PENDING_PAYMENT':
        return 'bg-sky-950/25 text-sky-300 border-sky-500/20';
      case 'HOLDING':
        return 'bg-amber-950/20 text-amber-400 border-amber-500/20';
      case 'CANCELLED':
        return 'bg-rose-950/25 text-rose-300 border-rose-500/20';
      case 'EXPIRED':
        return 'bg-zinc-900 text-zinc-500 border-zinc-700';
      default:
        return 'bg-neutral-900 text-neutral-400 border-neutral-700';
    }
  };

  const getBookingHelperText = (booking) => {
    if (booking.status === 'PAID') return 'Sẵn sàng quét / Đưa mã cho nhân viên soát vé';
    if (booking.status === 'PENDING_PAYMENT') return 'Có thể tiếp tục thanh toán khi thời gian giữ ghế còn hiệu lực';
    if (booking.status === 'HOLDING') {
      return `Ghế giữ đến ${booking.holdExpiresAt ? new Date(booking.holdExpiresAt).toLocaleTimeString('vi-VN') : ''}`;
    }
    return 'Đã sử dụng';
  };

  const handleCancelBooking = async (booking) => {
    const isCancellable = booking?.bookingId
      && ['HOLDING', 'PENDING_PAYMENT'].includes(booking.status)
      && (!booking.holdExpiresAt || new Date(booking.holdExpiresAt).getTime() > Date.now());
    if (!isCancellable) return;

    const confirmed = window.confirm(
      'Hủy đặt vé này? Ghế đang giữ sẽ được giải phóng ngay để bạn có thể chọn lại số ghế hoặc thông tin vé.'
    );
    if (!confirmed) return;

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 4500, null, 'sad');
      return;
    }

    setCancellingBookingId(String(booking.bookingId));
    try {
      await bookingService.cancelBooking(accessToken, booking.bookingId);
      showToast('Đã hủy đặt vé và giải phóng ghế. Bạn có thể đặt lại ngay.');
      await loadBookings();
    } catch (error) {
      showToast(error.message || 'Không thể hủy đặt vé này.', 4500, null, 'sad');
    } finally {
      setCancellingBookingId('');
    }
  };

  const normalizeMovieTitle = (value) => String(value || '').trim().toLowerCase();

  const findMovieForBooking = (booking) => {
    const title = normalizeMovieTitle(booking.movieTitle || booking.showtime?.movieTitle);
    if (!title) return null;
    return moviesList.find((movie) => (
      normalizeMovieTitle(movie.title || movie.movieTitle || movie.name) === title
    )) || null;
  };

  const isPendingBooking = (booking) => ['HOLDING', 'PENDING_PAYMENT'].includes(booking.status);
  const hasHoldExpired = (booking) => Boolean(
    isPendingBooking(booking)
    && booking.holdExpiresAt
    && new Date(booking.holdExpiresAt).getTime() <= clockTick
  );

  // Đơn chờ thanh toán chỉ nằm trong "Vé của tôi" khi session giữ ghế còn hiệu lực.
  const activeTickets = useMemo(() => {
    return realBookings
      .filter((booking) => (
        booking.status === 'PAID'
        || booking.status === 'USED'
        || (isPendingBooking(booking) && !hasHoldExpired(booking))
      ))
      .map(b => {
        // Trạng thái giữ ghế realtime: đếm ngược tới holdExpiresAt; hết giờ thì
        // hiển thị "ĐÃ HẾT HẠN" ngay cả khi BE scheduler (60s/lần) chưa kịp đổi status
        const isPendingStatus = ['HOLDING', 'PENDING_PAYMENT'].includes(b.status);
        const holdSecondsLeft = isPendingStatus && b.holdExpiresAt
          ? Math.max(0, Math.ceil((new Date(b.holdExpiresAt).getTime() - clockTick) / 1000))
          : null;
        const isClientExpired = isPendingStatus && b.holdExpiresAt && holdSecondsLeft === 0;
        const isHoldActive = isPendingStatus && !isClientExpired;
        // USED nhưng suất CHƯA chiếu xong = vừa check-in (đang xem); xem xong mới là "đã sử dụng"
        const isWatching = b.status === 'USED' && b.showtimeEnd
          && Date.now() < new Date(b.showtimeEnd).getTime();

        // Gộp bắp nước đã đặt (cả chọn lúc đặt vé lẫn đặt thêm qua VNPay) theo tên
        // để hiển thị gọn trên vé của khách. BE chỉ trả các món đã thanh toán (food order PAID).
        const foods = Array.isArray(b.foods)
          ? Object.values(b.foods.reduce((acc, f) => {
            const key = f.name || f.foodItemId || f.foodComboId;
            if (!acc[key]) acc[key] = { name: f.name || 'Bắp nước', quantity: 0, totalPrice: 0 };
            acc[key].quantity += Number(f.quantity || 0);
            acc[key].totalPrice += Number(f.totalPrice || 0);
            return acc;
          }, {}))
          : [];

        return {
          bookingId: b.id,
          id: b.bookingCode || String(b.id),
          paidAt: b.paidAt || null,
          createdAt: b.createdAt || null,
          status: b.status,
          movieId: b.movieId || b.showtime?.movieId || findMovieForBooking(b)?.backendId || findMovieForBooking(b)?.id,
          title: b.movieTitle || b.showtime?.movieTitle || 'Phim',
          englishTitle: b.bookingCode || '',
          time: b.showtimeStart ? new Date(b.showtimeStart).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '—',
          date: b.showtimeStart ? new Date(b.showtimeStart).toLocaleDateString('vi-VN') : '—',
          room: b.roomName || b.showtime?.roomName || '—',
          location: b.cinemaName || publicCinema?.name || 'Rạp chưa được cấu hình',
          seats: b.seats?.map(s => `${s.rowLabel}${s.seatNumber}`).join(', ') || '—',
          code: b.bookingCode || String(b.id),
          qrCode: b.qrCode || '',
          badge: isClientExpired ? 'ĐÃ HẾT HẠN' : isWatching ? 'ĐÃ CHECK-IN' : getBookingBadge(b.status),
          badgeColor: isClientExpired
            ? getBookingBadgeColor('EXPIRED')
            : isWatching
              ? 'bg-emerald-950/30 text-emerald-400 border-emerald-500/20'
              : getBookingBadgeColor(b.status),
          helperText: isClientExpired
            ? 'Hết thời gian giữ ghế — ghế đã được nhả cho khách khác'
            : isWatching
              ? 'Đã vào rạp — chúc bạn xem phim vui vẻ 🍿'
              : getBookingHelperText(b),
          isWatching,
          holdExpiresAt: b.holdExpiresAt,
          holdSecondsLeft,
          isClientExpired,
          isHoldActive,
          foods,
          totalAmount: b.totalAmount,
          seatDetails: Array.isArray(b.seats) ? b.seats : [],
          showtimeStart: b.showtimeStart || null,
          showtimeEnd: b.showtimeEnd || null,
          poster: b.posterUrl || b.moviePosterUrl || b.showtime?.posterUrl || b.showtime?.moviePosterUrl || findMovieForBooking(b)?.posterUrl || 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10',
          isReal: true
        };
      })
      .sort((a, b) => {
        // Vé mua/đặt mới nhất (ID đơn lớn nhất) được đưa lên đầu tiên
        const idA = Number(a.bookingId || a.id || 0);
        const idB = Number(b.bookingId || b.id || 0);
        if (idA !== idB && idA > 0 && idB > 0) {
          return idB - idA;
        }
        const timeA = a.paidAt ? new Date(a.paidAt).getTime() : (a.createdAt ? new Date(a.createdAt).getTime() : 0);
        const timeB = b.paidAt ? new Date(b.paidAt).getTime() : (b.createdAt ? new Date(b.createdAt).getTime() : 0);
        return timeB - timeA;
      });
  }, [realBookings, clockTick, publicCinema, moviesList]);

  const totalActiveTickets = activeTickets.length;
  const totalTicketPages = Math.ceil(totalActiveTickets / TICKET_PAGE_SIZE) || 1;
  const safeTicketPage = Math.min(Math.max(1, ticketPage), totalTicketPages);

  const paginatedActiveTickets = useMemo(() => {
    const start = (safeTicketPage - 1) * TICKET_PAGE_SIZE;
    return activeTickets.slice(start, start + TICKET_PAGE_SIZE);
  }, [activeTickets, safeTicketPage]);

  useEffect(() => {
    if (!highlightBookingId || activeTickets.length === 0) return;
    if (hasProcessedHighlightRef.current === highlightBookingId) return;

    const targetIdx = activeTickets.findIndex(
      (t) => String(t.bookingId) === String(highlightBookingId)
        || String(t.id) === String(highlightBookingId)
        || String(t.code) === String(highlightBookingId)
    );
    if (targetIdx !== -1) {
      hasProcessedHighlightRef.current = highlightBookingId;
      const targetPage = Math.floor(targetIdx / TICKET_PAGE_SIZE) + 1;
      setTicketPage(targetPage);
      const matchedTicket = activeTickets[targetIdx];
      const matchId = String(matchedTicket.bookingId || matchedTicket.id);
      setHighlightedId(matchId);

      setTimeout(() => {
        const el = document.getElementById(`ticket-card-${matchId}`);
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 400);

      // Tự động tắt hẳn viền phát sáng (glow) sau đúng 5 giây
      setTimeout(() => {
        setHighlightedId(null);
      }, 5000);
    }
  }, [highlightBookingId, activeTickets]);

  // HOLDING/PENDING_PAYMENT chỉ chuyển xuống lịch sử sau đúng thời điểm holdExpiresAt.
  const bookingHistory = useMemo(() => {
    return realBookings
      .filter((booking) => !isPendingBooking(booking) || hasHoldExpired(booking))
      .map((booking) => {
        const isExpiredHold = hasHoldExpired(booking);
        const isWatching = booking.status === 'USED' && booking.showtimeEnd
          && Date.now() < new Date(booking.showtimeEnd).getTime();
        return {
          bookingId: booking.id,
          id: booking.id,
          paidAt: booking.paidAt || null,
          createdAt: booking.createdAt || null,
          movie: booking.movieTitle || booking.showtime?.movieTitle || 'Phim',
          date: booking.showtimeStart ? new Date(booking.showtimeStart).toLocaleDateString('vi-VN') : '—',
          location: booking.cinemaName || publicCinema?.name || 'Rạp chưa được cấu hình',
          seats: booking.seats?.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(', ') || '—',
          status: isExpiredHold ? 'ĐÃ HẾT HẠN' : isWatching ? 'ĐÃ CHECK-IN' : getBookingBadge(booking.status),
          statusColor: isExpiredHold
            ? getBookingBadgeColor('EXPIRED')
            : isWatching
              ? 'bg-emerald-950/30 text-emerald-400 border-emerald-500/20'
              : getBookingBadgeColor(booking.status)
        };
      })
      .sort((a, b) => {
        const idA = Number(a.bookingId || a.id || 0);
        const idB = Number(b.bookingId || b.id || 0);
        if (idA !== idB && idA > 0 && idB > 0) {
          return idB - idA;
        }
        const timeA = a.paidAt ? new Date(a.paidAt).getTime() : (a.createdAt ? new Date(a.createdAt).getTime() : 0);
        const timeB = b.paidAt ? new Date(b.paidAt).getTime() : (b.createdAt ? new Date(b.createdAt).getTime() : 0);
        return timeB - timeA;
      });
  }, [realBookings, clockTick, publicCinema]);

  const totalHistoryItems = bookingHistory.length;
  const totalHistoryPages = Math.ceil(totalHistoryItems / HISTORY_PAGE_SIZE) || 1;
  const safeHistoryPage = Math.min(Math.max(1, historyPage), totalHistoryPages);

  const paginatedHistory = useMemo(() => {
    const start = (safeHistoryPage - 1) * HISTORY_PAGE_SIZE;
    return bookingHistory.slice(start, start + HISTORY_PAGE_SIZE);
  }, [bookingHistory, safeHistoryPage]);

  const reviewedBookingIds = useMemo(() => new Set(
    reviewsList
      .map((review) => review.bookingId)
      .filter((bookingId) => bookingId !== null && bookingId !== undefined)
      .map(String)
  ), [reviewsList]);

  const visibleReviews = useMemo(
    () => reviewsList
      .slice()
      .sort((a, b) => {
        const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        if (timeA !== timeB) return timeB - timeA;
        return Number(b.id || 0) - Number(a.id || 0);
      }),
    [reviewsList]
  );

  const isReviewInEditWindow = (review) => {
    if (!review?.createdAt) return false;
    return Date.now() - new Date(review.createdAt).getTime() <= 24 * 60 * 60 * 1000;
  };

  const reviewableBookings = useMemo(() => {
    return realBookings
      .filter((booking) => booking.status === 'USED')
      .map((booking) => {
        const matchedMovie = findMovieForBooking(booking);
        const movieId = booking.movieId || booking.showtime?.movieId || matchedMovie?.backendId || matchedMovie?.id;
        return {
          ...booking,
          movieId,
          movieTitle: booking.movieTitle || booking.showtime?.movieTitle || matchedMovie?.title || 'Phim',
          posterUrl: booking.posterUrl || booking.moviePosterUrl || booking.showtime?.posterUrl || booking.showtime?.moviePosterUrl || matchedMovie?.posterUrl,
        };
      })
      .filter((booking) => {
        return booking.id && booking.movieId;
      });
  }, [realBookings, moviesList]);

  const pendingReviewBookings = useMemo(
    () => reviewableBookings.filter((booking) => !reviewedBookingIds.has(String(booking.id))),
    [reviewableBookings, reviewedBookingIds]
  );
  const selectedReviewBooking = reviewableBookings.find((booking) => String(booking.id) === String(selectedReviewMovie))
    || pendingReviewBookings[0]
    || reviewableBookings[0]
    || null;

  useEffect(() => {
    const nextBooking = pendingReviewBookings[0] || null;
    if (!nextBooking) {
      if (selectedReviewMovie) setSelectedReviewMovie('');
      return;
    }
    if (!selectedReviewMovie || reviewedBookingIds.has(String(selectedReviewMovie))) {
      setSelectedReviewMovie(String(nextBooking.id));
    }
  }, [pendingReviewBookings, reviewedBookingIds, selectedReviewMovie]);

  const handleReviewSubmit = async (e) => {
    e.preventDefault();
    const comment = reviewContent.trim();
    const movieId = selectedReviewBooking?.movieId;
    if (!comment || !movieId) return;
    if (selectedReviewBooking?.id && reviewedBookingIds.has(String(selectedReviewBooking.id))) {
      showToast('Bạn đã đánh giá vé này rồi nên không thể gửi thêm đánh giá.', 4500, null, 'sad');
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast('Vui lòng đăng nhập lại để gửi đánh giá.', 4500, null, 'sad');
      return;
    }

    setIsSubmittingReview(true);
    try {
      const savedReview = await reviewService.createReview(accessToken, movieId, {
        bookingId: selectedReviewBooking?.id,
        rating: reviewRating,
        comment
      });
      setReviewsList((current) => [savedReview, ...current.filter((review) => String(review.id) !== String(savedReview.id))]);
      setReviewContent('');
      showToast(`Đã gửi đánh giá cho phim ${selectedReviewBooking?.movieTitle || 'này'}.`);
      await loadMyReviews();
    } catch (error) {
      showToast(error.message || 'Không thể gửi đánh giá phim.', 4500, null, 'sad');
    } finally {
      setIsSubmittingReview(false);
    }
  };

  const scrollToReviewSection = () => {
    reviewSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const handleGoToReviewFromTicket = (ticket) => {
    const bookingId = ticket?.bookingId;
    if (bookingId && !reviewedBookingIds.has(String(bookingId))) {
      setSelectedReviewMovie(String(bookingId));
    }
    scrollToReviewSection();
    if (bookingId && reviewedBookingIds.has(String(bookingId))) {
      showToast('Bạn đã đánh giá vé này. Có thể sửa hoặc xóa trong 24 giờ sau khi gửi.');
    }
  };

  const handleStartEditReview = (review) => {
    setEditingReviewId(String(review.id));
    setEditReviewRating(review.rating || 5);
    setEditReviewContent(review.comment || review.content || '');
  };

  const handleCancelEditReview = () => {
    setEditingReviewId('');
    setEditReviewRating(5);
    setEditReviewContent('');
  };

  const handleUpdateReview = async (review) => {
    const comment = editReviewContent.trim();
    if (!comment) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast('Vui lòng đăng nhập lại để sửa đánh giá.', 4500, null, 'sad');
      return;
    }

    setSavingReviewId(String(review.id));
    try {
      const updatedReview = await reviewService.updateReview(accessToken, review.id, {
        rating: editReviewRating,
        comment
      });
      setReviewsList((current) => current.map((item) => (
        String(item.id) === String(updatedReview.id) ? updatedReview : item
      )));
      handleCancelEditReview();
      showToast('Đã cập nhật đánh giá.');
    } catch (error) {
      showToast(error.message || 'Không thể cập nhật đánh giá.', 4500, null, 'sad');
    } finally {
      setSavingReviewId('');
    }
  };

  const handleDeleteReview = async (review) => {
    const confirmed = window.confirm('Xóa đánh giá này? Thao tác chỉ hợp lệ trong 24 giờ sau khi gửi.');
    if (!confirmed) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast('Vui lòng đăng nhập lại để xóa đánh giá.', 4500, null, 'sad');
      return;
    }

    setDeletingReviewId(String(review.id));
    try {
      await reviewService.deleteReview(accessToken, review.id);
      setReviewsList((current) => current.map((item) => (
        String(item.id) === String(review.id) ? { ...item, status: 'DELETED' } : item
      )));
      if (editingReviewId === String(review.id)) handleCancelEditReview();
      showToast('Đã xóa đánh giá.');
    } catch (error) {
      showToast(error.message || 'Không thể xóa đánh giá.', 4500, null, 'sad');
    } finally {
      setDeletingReviewId('');
    }
  };

  return (
    <div className={embedded ? 'space-y-12' : 'mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-12'}>

      {!isLoggedIn && (
        <div className="border border-white/10 bg-[#0E0E0E] p-6 text-center space-y-4 max-w-sm mx-auto my-12" id="guest-tickets-gate">
          <Ticket className="h-10 w-10 text-white mx-auto animate-pulse" />
          <h3 className="text-base font-serif font-light italic text-white uppercase tracking-wider">CinePremier Vé Đặc Quyền</h3>
          <p className="text-xs text-neutral-400 font-sans leading-relaxed">
            Vui lòng đăng nhập tài khoản Cinephile VIP để hiển thị thông tin vé, kiểm nghiệm mã vạch thông minh, và tiến hành rà soát lịch chiếu cá nhân.
          </p>
          <button
            onClick={onOpenOTP}
            className="border border-white bg-white hover:bg-black hover:text-white text-black px-6 py-2.5 text-xs font-sans font-bold tracking-widest uppercase transition-all cursor-pointer"
          >
            Đăng nhập ngay
          </button>
        </div>
      )}

      {isLoggedIn && (
        <>
          {!embedded && <div className="space-y-1 text-center sm:text-left">
            <h1 className="text-3xl font-serif font-light text-white tracking-widest leading-none uppercase italic">
              Vé của tôi
            </h1>
            <p className="text-xs font-sans tracking-[0.2em] text-neutral-400 uppercase">
              Quản lý trải nghiệm điện ảnh cao cấp của bạn
            </p>
          </div>}

          {/* SECTION: VÉ HIỆN TẠI */}
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-white/5 pb-2">
              <div className="flex items-center gap-2">
                <Ticket className="h-4 w-4 text-white" />
                <span className="text-[10px] uppercase font-sans font-black tracking-widest text-neutral-400">
                  🎫 Vé của tôi {isLoggedIn && !isLoading && `(${activeTickets.length})`}
                </span>
              </div>
              {isLoading && <Loader2 className="h-4 w-4 text-amber-500 animate-spin" />}
            </div>

            {/* Dynamic / Styled tickets flex grid matching secondary screenshot */}
            <div className="grid grid-cols-1 gap-8" id="tickets-current-grid">
              {paginatedActiveTickets.map((t) => {
                const cardId = String(t.bookingId || t.id);
                const isHighlighted = highlightedId && (
                  String(t.bookingId) === String(highlightedId)
                  || String(t.id) === String(highlightedId)
                  || String(t.code) === String(highlightedId)
                );
                const isEnded = Boolean(t.showtimeEnd && new Date(t.showtimeEnd).getTime() <= Date.now());
                const isCheckedIn = t.status === 'USED' || t.status === 'COMPLETED' || (Array.isArray(t.seatDetails) && t.seatDetails.some((s) => s.status === 'CHECKED_IN'));
                const hasShowtimePassed = isEnded || Boolean(t.showtimeStart && new Date(t.showtimeStart).getTime() <= Date.now());
                const canReview = isCheckedIn && hasShowtimePassed;
                const hasAlreadyReviewed = Boolean(t.bookingId && reviewedBookingIds.has(String(t.bookingId)));
                const showReviewButton = canReview || hasAlreadyReviewed;

                return (
                  <div
                    key={t.id}
                    id={`ticket-card-${cardId}`}
                    className={`group border bg-gradient-to-br from-[#0e0e11] via-[#07070a] to-[#020203] transition-all duration-300 flex flex-col md:flex-row relative overflow-hidden shadow-2xl rounded-sm md:min-h-[260px] ${
                      isHighlighted
                        ? 'border-2 border-white ring-2 ring-white ring-offset-2 ring-offset-black shadow-[0_0_35px_rgba(255,255,255,0.9)] animate-pulse'
                        : isEnded
                          ? 'border-neutral-850 bg-neutral-950/95 hover:border-neutral-700/80'
                          : 'border-neutral-800 hover:border-neutral-700/80'
                    }`}
                  >

                  {/* Semicircle paper ticket notch punches for authentic cinema pass feel */}
                  <div className="absolute -top-3 right-[220px] w-6 h-6 bg-black border border-neutral-850 rounded-full translate-x-1/2 z-20 hidden md:block"></div>
                  <div className="absolute -bottom-3 right-[220px] w-6 h-6 bg-black border border-neutral-850 rounded-full translate-x-1/2 z-20 hidden md:block"></div>

                  {/* Vertical line indicator representing tear ticket area */}
                  <div className="absolute right-[220px] top-0 bottom-0 border-r border-dashed border-neutral-850 hidden md:block pointer-events-none z-10 w-0"></div>

                  {/* POSTER CARD LEFT COMPONENT - Compact Fixed width on desktop */}
                  <div className="w-full md:w-[190px] h-52 md:min-h-[260px] relative bg-neutral-950 flex-shrink-0 overflow-hidden">
                    <img
                      src={t.poster}
                      alt={t.title}
                      className="w-full h-full object-cover grayscale group-hover:grayscale-0 transition duration-500 group-hover:scale-105"
                      referrerPolicy="no-referrer"
                    />

                    {/* Tech Badge on poster */}
                    <div className={`absolute left-2.5 top-2.5 border text-[9px] font-black tracking-widest px-2 py-0.5 uppercase shadow-sm ${
                      isEnded
                        ? 'bg-neutral-950/95 border-neutral-700 text-neutral-400'
                        : t.isClientExpired
                          ? 'bg-zinc-900/95 border-zinc-600 text-zinc-400'
                          : t.isWatching
                            ? 'bg-emerald-950/90 border-emerald-500/50 text-emerald-300'
                            : 'bg-red-950/90 border-red-500/50 text-white/90'
                      }`}>
                      {isEnded ? 'SUẤT CHIẾU ĐÃ KẾT THÚC' : t.badge}
                    </div>
                  </div>

                  {/* DETAILS CONTENT MIDDLE COMPONENT - Cozy spacing, fits perfectly without huge gaps */}
                  <div className="flex-1 p-4 md:py-3.5 md:px-4 flex flex-col justify-between min-w-0 md:h-full select-none">

                    {/* Upper texts: Movie title, subtitles, dates & times */}
                    <div className="space-y-1">
                      <div className="flex justify-between items-start gap-3">
                        <h3 className="text-sm sm:text-base font-serif font-black text-amber-50 tracking-wide uppercase italic leading-tight truncate max-w-[65%]">
                          {t.title}
                        </h3>
                        <span className="text-[10px] font-mono font-black text-yellow-400 bg-yellow-950/40 border border-yellow-500/20 px-2.5 py-0.5 shrink-0 rounded-sm shadow-sm tracking-wider">
                          {t.time}
                        </span>
                      </div>
                      <div className="flex justify-between items-center text-[10px] text-neutral-400 tracking-wider">
                        <p className="truncate uppercase font-sans font-medium max-w-[60%]">{t.englishTitle}</p>
                        <p className="font-mono font-extrabold text-zinc-300 border-b border-white/5 pb-0.5 uppercase tracking-wide shrink-0">{t.date}</p>
                      </div>
                    </div>

                    {/* Grid stats layout - Clean, tight capsules aligning with division line */}
                    <div className="grid grid-cols-3 gap-2 border-t border-b border-neutral-900/60 py-2.5 my-1.5 font-sans">
                      {/* PHÒNG CHIẾU */}
                      <div className="min-w-0 pr-1">
                        <span className="block text-[8px] text-neutral-500 font-black uppercase tracking-widest mb-1">PHÒNG:</span>
                        <span className="text-[10px] sm:text-[11px] font-extrabold text-white bg-neutral-900/60 border border-neutral-850 px-1.5 py-0.5 rounded-sm block truncate text-center font-mono">
                          {t.room}
                        </span>
                      </div>

                      {/* GHẾ */}
                      <div className="min-w-0 px-1">
                        <span className="block text-[8px] text-amber-500 font-extrabold uppercase tracking-widest mb-1 text-center font-mono">SỐ GHẾ:</span>
                        <span className="text-[10px] sm:text-[11px] font-extrabold text-amber-400 bg-amber-950/15 border border-amber-500/20 px-1.5 py-0.5 rounded-sm block truncate text-center font-mono">
                          {t.seats}
                        </span>
                      </div>

                      {/* MÃ ĐẶT CHỖ */}
                      <div className="min-w-0 pl-2">
                        <span className="block text-[8px] text-neutral-500 font-black uppercase tracking-widest mb-1 text-center font-mono">MÃ SỐ VÉ:</span>
                        <span className="text-[10px] sm:text-[11px] font-mono font-extrabold text-white bg-neutral-900/60 border border-neutral-850 px-1.5 py-0.5 rounded-sm block truncate text-center select-all">
                          {t.code}
                        </span>
                      </div>
                    </div>

                    {/* Sub scanning prompt */}
                    <div className="flex items-center justify-between gap-2 pt-0.5">

                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className={`flex h-9 w-9 shrink-0 items-center justify-center border ${
                          isEnded
                            ? 'border-neutral-800 bg-neutral-900/40 text-neutral-600'
                            : 'border-emerald-500/20 bg-emerald-500/5 text-emerald-400'
                        }`}>
                          <ScanLine className="h-4 w-4" />
                        </div>

                        <div className="min-w-0">
                          {t.isHoldActive && t.holdSecondsLeft !== null ? (
                            <p className={`text-[10px] font-mono font-black tracking-wider leading-tight ${t.holdSecondsLeft <= 30 ? 'text-red-400 animate-pulse' : 'text-amber-400'
                              }`}>
                              ⏳ Còn {formatCountdown(t.holdSecondsLeft)} để thanh toán
                            </p>
                          ) : (
                            <p className={`text-[8.5px] uppercase tracking-wide font-bold leading-tight truncate ${
                              isEnded ? 'text-neutral-500 font-normal' : t.isClientExpired ? 'text-zinc-500' : 'text-zinc-400'
                            }`}>
                              {isEnded ? 'Suất chiếu đã kết thúc — vé không còn hiệu lực qua cửa' : t.helperText}
                            </p>
                          )}
                          <span className="text-[7.5px] font-mono text-neutral-500 block leading-none truncate uppercase mt-0.5">
                            {t.location || 'CINEPREMIER VIP RẠP'}
                          </span>
                        </div>
                      </div>

                      {/* Context trigger option */}
                      <div className="flex items-center space-x-1 shrink-0">
                        {t.isHoldActive && t.movieId && (
                          <button
                            type="button"
                            onClick={() => navigate(`/movies/${t.movieId}/book?resumeBookingId=${t.bookingId}`)}
                            className="inline-flex items-center gap-1.5 bg-amber-400 px-3 py-1.5 text-[8px] font-black uppercase tracking-widest text-black transition hover:bg-amber-300"
                            title="Quay lại màn thanh toán với ghế đang được giữ"
                          >
                            <Clock className="h-3.5 w-3.5" />
                            Tiếp tục thanh toán
                          </button>
                        )}
                        {t.isHoldActive && (
                          <button
                            type="button"
                            onClick={() => handleCancelBooking(t)}
                            disabled={cancellingBookingId === String(t.bookingId)}
                            className="inline-flex items-center gap-1.5 border border-rose-500/30 bg-rose-950/20 px-2.5 py-1.5 text-[8px] font-black uppercase tracking-widest text-rose-300 transition hover:bg-rose-500 hover:text-white disabled:cursor-wait disabled:opacity-60"
                            title="Hủy đặt vé và giải phóng ghế để đặt lại"
                          >
                            {cancellingBookingId === String(t.bookingId) ? (
                              <Loader2 className="h-3.5 w-3.5 animate-spin" />
                            ) : (
                              <XCircle className="h-3.5 w-3.5" />
                            )}
                            Hủy đặt vé
                          </button>
                        )}
                        {canOrderMoreFood(t) && (() => {
                          const isCheckedIn = t.status === 'USED' || Boolean(t.isWatching);
                          const isEnded = Boolean(t.showtimeEnd && new Date(t.showtimeEnd).getTime() <= Date.now());
                          const isDisabled = isCheckedIn || isEnded;
                          const buttonTitle = isCheckedIn
                            ? "Vé đã check-in không thể đặt thêm bắp nước"
                            : isEnded
                              ? "Suất chiếu đã kết thúc — không thể đặt thêm bắp nước"
                              : "Đặt thêm bắp nước cho vé này";
                          return (
                            <button
                              type="button"
                              disabled={isDisabled}
                              onClick={() => !isDisabled && navigate(`/concessions?bookingId=${t.bookingId}`)}
                              className="inline-flex items-center gap-1.5 border border-emerald-500/30 bg-emerald-950/20 px-2.5 py-1.5 text-[8px] font-black uppercase tracking-widest text-emerald-300 transition hover:bg-emerald-500 hover:text-black disabled:cursor-not-allowed disabled:border-neutral-800 disabled:bg-neutral-900/40 disabled:text-neutral-500 disabled:opacity-40 disabled:hover:bg-neutral-900/40 disabled:hover:text-neutral-500"
                              title={buttonTitle}
                            >
                              🍿 Đặt thêm bắp nước
                            </button>
                          );
                        })()}
                        {showReviewButton && (
                          <button
                            type="button"
                            onClick={() => handleGoToReviewFromTicket(t)}
                            className="inline-flex items-center gap-1.5 border border-amber-400/80 bg-amber-500/20 px-3 py-1.5 text-[9px] font-black uppercase tracking-widest text-amber-300 transition hover:bg-amber-400 hover:text-black shadow-[0_0_14px_rgba(251,191,36,0.35)] cursor-pointer rounded-sm"
                            title={hasAlreadyReviewed ? 'Xem hoặc sửa đánh giá' : 'Đánh giá phim đã xem'}
                          >
                            <MessageSquare className="h-3.5 w-3.5 text-amber-400" />
                            {hasAlreadyReviewed ? 'Xem đánh giá' : 'Đánh giá'}
                          </button>
                        )}
                        <button
                          onClick={() => showToast(`Mã rạp chiếu kĩ thuật số: ${t.code} đã được gửi lên hệ thống. Đưa mã này khi nhận vé bắp nước combo VIP.`)}
                          className="p-1.5 hover:bg-neutral-900 text-neutral-500 hover:text-white transition rounded-full shrink-0"
                          title="Chi tiết vé"
                        >
                          <MoreVertical className="h-4 w-4" />
                        </button>
                      </div>

                    </div>

                    {/* BẮP NƯỚC ĐÃ ĐẶT (đã thanh toán) */}
                    {Array.isArray(t.foods) && t.foods.length > 0 && (
                      <div className="mt-2 pt-2 border-t border-neutral-850">
                        <p className="text-[8px] font-black uppercase tracking-widest text-emerald-400/80 mb-1.5">🍿 Bắp nước đã đặt</p>
                        <div className="flex flex-wrap gap-1.5">
                          {t.foods.map((f, i) => (
                            <span
                              key={`${t.bookingId}-food-${i}`}
                              className="inline-flex items-center gap-1 border border-emerald-500/20 bg-emerald-950/20 px-2 py-0.5 text-[9px] font-mono text-emerald-200"
                            >
                              {f.name} <span className="text-emerald-400/60">×{f.quantity}</span>
                            </span>
                          ))}
                        </div>
                      </div>
                    )}

                  </div>

                  {/* MỘT QR CHUNG CHO CẢ ĐƠN — bên trong chứa danh sách mã vé từng ghế */}
                  <div className="flex w-full shrink-0 flex-col items-center justify-center gap-3 border-t border-dashed border-neutral-800 bg-black/35 p-5 md:w-[220px] md:border-l md:border-t-0">
                    {isEnded ? (
                      <div className="flex flex-col items-center justify-center text-center p-3 space-y-2.5 opacity-80">
                        <div className="flex h-11 w-11 items-center justify-center rounded-full bg-neutral-900 border border-neutral-800 text-neutral-500">
                          <ScanLine className="h-5 w-5" />
                        </div>
                        <p className="text-[9px] font-black uppercase tracking-[0.16em] text-neutral-400">Mã QR tạm thời không có</p>
                        <p className="text-[8px] text-neutral-500 font-sans leading-relaxed">Suất chiếu đã kết thúc</p>
                      </div>
                    ) : t.qrCode && t.seatDetails.some((s) => s.ticketCode) ? (
                      <>
                        <div className="bg-white p-3 shadow-[0_0_28px_rgba(255,255,255,0.16)]">
                          <QRCodeSVG
                            value={t.qrCode}
                            size={148}
                            level="M"
                            marginSize={1}
                            bgColor="#ffffff"
                            fgColor="#000000"
                            title={`QR vé ${t.code}`}
                          />
                        </div>
                        <p className="text-[9px] font-black uppercase tracking-[0.2em] text-emerald-400">
                          QR đặt chỗ · Bao gồm {t.seatDetails.length} ghế.
                        </p>
                        <div className="w-full space-y-1">
                          {t.seatDetails.map((seat) => (
                            <div key={seat.ticketCode || seat.seatId} className="flex items-center gap-2 border border-white/8 bg-neutral-950 px-2 py-1.5">
                              <span className="w-7 shrink-0 font-mono text-[10px] font-black text-white">{seat.rowLabel}{seat.seatNumber}</span>
                              <span className={`shrink-0 text-[7px] font-sans font-black uppercase tracking-widest ${seat.ticketType === 'STUDENT' ? 'text-sky-400' : seat.ticketType === 'CHILD' ? 'text-amber-400' : 'text-neutral-400'
                                }`}>
                                {seat.ticketType === 'STUDENT' ? 'Sinh viên' : seat.ticketType === 'CHILD' ? 'Trẻ em' : 'Người lớn'}
                              </span>
                              <span className="ml-auto shrink-0">
                                {seat.status === 'CHECKED_IN' ? (
                                  <span className="border border-emerald-500/30 bg-emerald-950/30 px-1 py-0.5 text-[7px] font-black uppercase tracking-widest text-emerald-400">✓ Đã vào</span>
                                ) : (
                                  <span className="text-[7px] font-black uppercase tracking-widest text-neutral-600">Chưa vào</span>
                                )}
                              </span>
                            </div>
                          ))}
                        </div>
                      </>
                    ) : t.qrCode ? (
                      <>
                        <div className="bg-white p-3 shadow-[0_0_28px_rgba(255,255,255,0.16)]">
                          <QRCodeSVG
                            value={t.qrCode}
                            size={148}
                            level="M"
                            marginSize={1}
                            bgColor="#ffffff"
                            fgColor="#000000"
                            title={`QR vé ${t.code}`}
                          />
                        </div>
                        <div className="text-center">
                          <p className="text-[9px] font-black uppercase tracking-[0.2em] text-emerald-400">
                            QR Check-in
                          </p>
                          <p className="mt-1 text-[8px] leading-relaxed text-neutral-500">
                            Nhân viên quét mã để lấy thông tin vé
                          </p>
                        </div>
                      </>
                    ) : (
                      <div className="space-y-3 text-center text-neutral-600">
                        <ScanLine className="mx-auto h-12 w-12" />
                        <p className="text-[9px] font-black uppercase tracking-[0.18em]">QR được cấp sau thanh toán</p>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}

              {paginatedActiveTickets.length === 0 && (
                <div className="border border-white/10 bg-[#070707] p-8 text-center text-xs text-neutral-500 font-mono">
                  Bạn chưa có vé hiện tại nào.
                </div>
              )}
            </div>

            {/* Active Tickets Pagination Controls */}
            {totalActiveTickets > 0 && (
              <div className="flex flex-col gap-3 border border-white/10 bg-[#050505] p-3 text-[10px] font-mono uppercase text-neutral-400 sm:flex-row sm:items-center sm:justify-between">
                <span>
                  Hiển thị {Math.min((safeTicketPage - 1) * TICKET_PAGE_SIZE + 1, totalActiveTickets)}-{Math.min(safeTicketPage * TICKET_PAGE_SIZE, totalActiveTickets)} / {totalActiveTickets} vé · Trang {safeTicketPage}/{totalTicketPages}
                </span>
                {totalTicketPages > 1 && (
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      disabled={safeTicketPage <= 1}
                      onClick={() => setTicketPage((p) => Math.max(1, p - 1))}
                      className="flex items-center gap-1 border border-neutral-800 bg-black px-2.5 py-1.5 text-white transition hover:border-amber-400/50 hover:text-amber-300 disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      <ChevronLeft className="h-3.5 w-3.5" /> Trước
                    </button>
                    {Array.from({ length: totalTicketPages }, (_, i) => i + 1).map((p) => {
                      const isNear = Math.abs(p - safeTicketPage) <= 1 || p === 1 || p === totalTicketPages;
                      if (!isNear && Math.abs(p - safeTicketPage) === 2) {
                        return <span key={`ellipsis-${p}`} className="px-1 text-neutral-600 select-none">…</span>;
                      }
                      if (!isNear) return null;
                      return (
                        <button
                          key={p}
                          type="button"
                          onClick={() => setTicketPage(p)}
                          className={`h-7 min-w-7 border px-2 py-1 text-[10px] font-black transition ${p === safeTicketPage
                            ? 'border-amber-400 bg-amber-400 font-mono text-black'
                            : 'border-neutral-800 bg-black text-neutral-300 hover:border-amber-400/50 hover:text-white'
                            }`}
                        >
                          {p}
                        </button>
                      );
                    })}
                    <button
                      type="button"
                      disabled={safeTicketPage >= totalTicketPages}
                      onClick={() => setTicketPage((p) => Math.min(totalTicketPages, p + 1))}
                      className="flex items-center gap-1 border border-neutral-800 bg-black px-2.5 py-1.5 text-white transition hover:border-amber-400/50 hover:text-amber-300 disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      Sau <ChevronRight className="h-3.5 w-3.5" />
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* SECTION: LỊCH SỬ ĐẶT VÉ TABLE */}
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-white/5 pb-2">
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-white" />
                <span className="text-[10px] uppercase font-sans font-black tracking-widest text-neutral-400">
                  ⏰ Lịch sử đặt vé
                </span>
              </div>
              {totalHistoryItems > 0 && (
                <span className="text-[9px] font-mono text-neutral-500 uppercase tracking-wider">
                  Tổng {totalHistoryItems} đơn
                </span>
              )}
            </div>

            {/* Responsive layout list or elegant table matching the layout */}
            <div className="overflow-x-auto border border-white/10 bg-[#050505] rounded-none">
              <table className="min-w-full divide-y divide-white/10 text-left font-sans text-[11px]">
                <thead className="bg-[#0B0B0B] text-[8px] uppercase tracking-[0.15em] text-neutral-500 font-bold">
                  <tr>
                    <th scope="col" className="px-4 py-2">PHIM</th>
                    <th scope="col" className="px-4 py-2">NGÀY CHIẾU</th>
                    <th scope="col" className="px-4 py-2">ĐỊA ĐIỂM</th>
                    <th scope="col" className="px-4 py-2">SỐ GHẾ</th>
                    <th scope="col" className="px-4 py-2">TRẠNG THÁI</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-neutral-300 font-light">
                  {paginatedHistory.map((row, idx) => (
                    <tr key={idx} className="hover:bg-white/5 transition duration-150">
                      <td className="px-4 py-2 whitespace-nowrap font-serif italic text-white font-bold">{row.movie}</td>
                      <td className="px-4 py-2 whitespace-nowrap font-mono">{row.date}</td>
                      <td className="px-4 py-2 whitespace-nowrap text-neutral-400">{row.location}</td>
                      <td className="px-4 py-2 whitespace-nowrap font-mono">{row.seats}</td>
                      <td className="px-4 py-2 whitespace-nowrap">
                        <span className={`inline-block border text-[8px] font-bold px-2 py-0.5 tracking-wider uppercase rounded-sm ${row.statusColor}`}>
                          {row.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {paginatedHistory.length === 0 && (
                    <tr>
                      <td colSpan={5} className="px-4 py-6 text-center text-xs text-neutral-500">
                        Chưa có lịch sử đặt vé.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {totalHistoryItems > 0 && (
              <div className="flex flex-col gap-3 border border-white/10 bg-[#050505] p-3 text-[10px] font-mono uppercase text-neutral-400 sm:flex-row sm:items-center sm:justify-between">
                <span>
                  Hiển thị {Math.min((safeHistoryPage - 1) * HISTORY_PAGE_SIZE + 1, totalHistoryItems)}-{Math.min(safeHistoryPage * HISTORY_PAGE_SIZE, totalHistoryItems)} / {totalHistoryItems} đơn · Trang {safeHistoryPage}/{totalHistoryPages}
                </span>
                {totalHistoryPages > 1 && (
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      disabled={safeHistoryPage <= 1}
                      onClick={() => setHistoryPage((p) => Math.max(1, p - 1))}
                      className="flex items-center gap-1 border border-neutral-800 bg-black px-2.5 py-1.5 text-white transition hover:border-amber-400/50 hover:text-amber-300 disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      <ChevronLeft className="h-3.5 w-3.5" /> Trước
                    </button>
                    {Array.from({ length: totalHistoryPages }, (_, i) => i + 1).map((p) => {
                      const isNear = Math.abs(p - safeHistoryPage) <= 1 || p === 1 || p === totalHistoryPages;
                      if (!isNear && Math.abs(p - safeHistoryPage) === 2) {
                        return <span key={`ellipsis-${p}`} className="px-1 text-neutral-600 select-none">…</span>;
                      }
                      if (!isNear) return null;
                      return (
                        <button
                          key={p}
                          type="button"
                          onClick={() => setHistoryPage(p)}
                          className={`h-7 min-w-7 border px-2 py-1 text-[10px] font-black transition ${p === safeHistoryPage
                            ? 'border-amber-400 bg-amber-400 font-mono text-black'
                            : 'border-neutral-800 bg-black text-neutral-300 hover:border-amber-400/50 hover:text-white'
                            }`}
                        >
                          {p}
                        </button>
                      );
                    })}
                    <button
                      type="button"
                      disabled={safeHistoryPage >= totalHistoryPages}
                      onClick={() => setHistoryPage((p) => Math.min(totalHistoryPages, p + 1))}
                      className="flex items-center gap-1 border border-neutral-800 bg-black px-2.5 py-1.5 text-white transition hover:border-amber-400/50 hover:text-amber-300 disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      Sau <ChevronRight className="h-3.5 w-3.5" />
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* SECTION: ĐÁNH GIÁ PHIM ĐÃ XEM FEEDBACK AREA */}
          <div ref={reviewSectionRef} className="space-y-4 scroll-mt-24">
            <div className="flex items-center gap-2 border-b border-white/5 pb-2">
              <Star className="h-4 w-4 text-white" />
              <span className="text-[13px] uppercase font-sans font-black tracking-widest text-neutral-400">
                💬 Đánh giá phim đã xem
              </span>
            </div>

            {/* Dual card form split matches screenshot perfectly */}
            <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-start">

              {/* Left watched indicator poster matching picture */}
              <div className="md:col-span-4 border border-white/10 bg-black p-5 flex flex-col items-center justify-center text-center space-y-4">
                <div className="w-28 aspect-[3/4] overflow-hidden border border-white/5 bg-neutral-900 shadow-xl relative">
                  <img
                    src={selectedReviewBooking?.posterUrl || 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRb30EroFOo6S_-d49SOIyTINg8t7Vpmm_lpcJ1zZ2xNA&s=10'}
                    alt={selectedReviewBooking?.movieTitle || 'Phim đã xem'}
                    className="w-full h-full object-cover grayscale"
                    referrerPolicy="no-referrer"
                  />
                </div>

                <div className="space-y-1">
                  <h4 className="text-sm font-serif font-bold italic text-white uppercase tracking-wider">{selectedReviewBooking?.movieTitle || 'Chưa có vé đã sử dụng'}</h4>
                  <p className="text-[9px] uppercase tracking-widest text-[#888888] font-sans">
                    {selectedReviewBooking?.showtimeStart
                      ? `Đã xem ngày ${new Date(selectedReviewBooking.showtimeStart).toLocaleDateString('vi-VN')}`
                      : 'Chỉ vé đã sử dụng mới được đánh giá'}
                  </p>
                </div>

                {/* Simulated star ratings click selector */}
                <div className="flex space-x-1 justify-center">
                  {[1, 2, 3, 4, 5].map((st) => (
                    <button
                      key={st}
                      type="button"
                      onClick={() => setReviewRating(st)}
                      className="text-white hover:scale-110 transition"
                    >
                      <Star className={`h-4.5 w-4.5 ${reviewRating >= st ? 'text-white fill-current' : 'text-neutral-800'}`} />
                    </button>
                  ))}
                </div>
              </div>

              {/* Right Text Feedback Area matches screenshot */}
              <div className="md:col-span-8 border border-white/10 bg-black p-5 space-y-4 text-left">
                <span className="text-[10px] uppercase font-sans font-bold tracking-[0.2em] text-neutral-400">
                  Chia sẻ cảm nghĩ của bạn
                </span>

                <form onSubmit={handleReviewSubmit} className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <span className="text-[9px] uppercase tracking-wider text-neutral-500 font-sans block">Chọn phim đã xem:</span>
                      <select
                        value={selectedReviewMovie}
                        onChange={(e) => setSelectedReviewMovie(e.target.value)}
                        disabled={pendingReviewBookings.length === 0}
                        className="w-full border border-white/10 bg-neutral-950 p-2 text-xs text-white uppercase tracking-wider focus:outline-none focus:border-white rounded-none disabled:opacity-50"
                      >
                        {pendingReviewBookings.length === 0 ? (
                          <option value="">Không có phim đủ điều kiện</option>
                        ) : pendingReviewBookings.map((booking) => (
                          <option key={booking.id} value={String(booking.id)}>
                            {booking.movieTitle} - {booking.seats?.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(', ') || booking.bookingCode || `Vé #${booking.id}`} ({booking.showtimeStart ? new Date(booking.showtimeStart).toLocaleDateString('vi-VN') : 'Đã sử dụng'})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="space-y-1">
                      <span className="text-[9px] uppercase tracking-wider text-neutral-500 font-sans block">Xếp hạng:</span>
                      <div className="flex items-center h-9 font-bold text-xs text-white font-mono bg-neutral-950 px-3 border border-white/10">
                        <span>ĐỘ HÀI LÒNG: {reviewRating}/5 SAO</span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <span className="text-[9px] uppercase tracking-wider text-neutral-500 font-sans block">Nội dung phê bình:</span>
                    <textarea
                      required
                      rows={4}
                      maxLength={300}
                      placeholder="Nhập nhận xét của bạn về bộ phim này..."
                      value={reviewContent}
                      onChange={(e) => setReviewContent(e.target.value)}
                      disabled={pendingReviewBookings.length === 0 || isSubmittingReview}
                      className="w-full border border-white/10 bg-[#090909] p-3 text-xs text-white placeholder-neutral-700 font-sans focus:outline-none focus:border-white rounded-none leading-relaxed"
                    />
                  </div>

                  <div className="flex justify-end pt-1">
                    <button
                      type="submit"
                      disabled={pendingReviewBookings.length === 0 || isSubmittingReview}
                      className="bg-purple-900 duration-250 border border-purple-500/30 hover:bg-neutral-900 hover:text-white px-6 py-2.5 text-xs text-white font-sans font-bold tracking-widest uppercase transition rounded-none flex items-center gap-1.5"
                      style={{ background: 'linear-gradient(270deg, #D4145A 0%, #FBB03B 100%)' }}
                    >
                      {isSubmittingReview ? 'ĐANG GỬI...' : 'GỬI ĐÁNH GIÁ'}
                    </button>
                  </div>
                </form>
              </div>

            </div>

            {/* User reviews state displays if submitted */}
            {visibleReviews.length > 0 && (
              <div className="space-y-3 pt-3">
                <span className="text-[8px] tracking-wider text-neutral-500 font-sans block uppercase">ĐÁNH GIÁ VỪA KHAI THÁC CỦA BẠN:</span>
                <div className="space-y-3">
                  {visibleReviews.map((rev) => {
                    const isEditing = editingReviewId === String(rev.id);
                    const isHiddenByAdmin = rev.status === 'HIDDEN' || rev.status === 'DELETED';
                    const canChangeReview = (!rev.status || rev.status === 'VISIBLE') && isReviewInEditWindow(rev);
                    return (
                      <div key={rev.id} className={`border p-4 font-sans text-xs space-y-2.5 ${
                        isHiddenByAdmin ? 'border-rose-500/30 bg-rose-950/15' : 'border-white/5 bg-[#0a0a0a]'
                      }`}>
                        <div className="flex justify-between items-center">
                          <span className="font-serif italic text-white font-bold">{rev.movieTitle || rev.movie || 'Phim'}</span>
                          <span className="text-neutral-500 font-mono text-[9px]">{rev.createdAt ? new Date(rev.createdAt).toLocaleDateString('vi-VN') : rev.date}</span>
                        </div>

                        {isHiddenByAdmin && (
                          <div className="flex items-center gap-2 border border-rose-500/40 bg-rose-950/30 px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-rose-300">
                            <EyeOff className="h-4 w-4 text-rose-400 shrink-0" />
                            <span>Bình luận đã bị ẩn bởi Quản trị viên</span>
                          </div>
                        )}

                        {isEditing ? (
                          <div className="space-y-3">
                            <div className="flex space-x-1">
                              {[1, 2, 3, 4, 5].map((star) => (
                                <button
                                  key={star}
                                  type="button"
                                  onClick={() => setEditReviewRating(star)}
                                  className="text-white"
                                >
                                  <Star className={`h-3.5 w-3.5 ${editReviewRating >= star ? 'fill-current text-white' : 'text-neutral-800'}`} />
                                </button>
                              ))}
                            </div>
                            <textarea
                              rows={3}
                              maxLength={300}
                              value={editReviewContent}
                              onChange={(e) => setEditReviewContent(e.target.value)}
                              className="w-full border border-white/10 bg-black p-3 text-xs text-white placeholder-neutral-700 font-sans focus:outline-none focus:border-white rounded-none leading-relaxed"
                            />
                            <div className="flex flex-wrap justify-end gap-2">
                              <button
                                type="button"
                                onClick={handleCancelEditReview}
                                className="inline-flex items-center gap-1.5 border border-white/10 px-3 py-1.5 text-[9px] font-bold uppercase tracking-widest text-neutral-300 hover:bg-white hover:text-black"
                              >
                                <X className="h-3.5 w-3.5" />
                                Hủy
                              </button>
                              <button
                                type="button"
                                onClick={() => handleUpdateReview(rev)}
                                disabled={savingReviewId === String(rev.id)}
                                className="inline-flex items-center gap-1.5 border border-emerald-500/30 bg-emerald-950/20 px-3 py-1.5 text-[9px] font-bold uppercase tracking-widest text-emerald-300 hover:bg-emerald-500 hover:text-black disabled:cursor-wait disabled:opacity-60"
                              >
                                {savingReviewId === String(rev.id) ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Save className="h-3.5 w-3.5" />}
                                Lưu
                              </button>
                            </div>
                          </div>
                        ) : (
                          <>
                            <div className="flex space-x-0.5">
                              {Array.from({ length: 5 }, (_, idx) => (
                                <Star key={idx} className={`h-3 w-3 ${idx < rev.rating ? 'fill-current text-white' : 'text-neutral-800'}`} />
                              ))}
                            </div>
                            <p className="text-neutral-400 font-light leading-relaxed">"{rev.comment || rev.content || ''}"</p>
                            <div className="flex flex-wrap items-center justify-between gap-2 border-t border-white/5 pt-2">
                              <span className="text-[9px] uppercase tracking-widest text-neutral-500">
                                {isHiddenByAdmin
                                  ? 'Đã bị ẩn bởi Quản trị viên'
                                  : canChangeReview
                                    ? 'Có thể sửa/xóa trong 24 giờ sau khi gửi'
                                    : 'Đã hết hạn sửa/xóa sau 24 giờ'}
                              </span>
                              {canChangeReview && (
                                <div className="flex gap-2">
                                  <button
                                    type="button"
                                    onClick={() => handleStartEditReview(rev)}
                                    className="inline-flex items-center gap-1.5 border border-sky-500/30 bg-sky-950/20 px-3 py-1.5 text-[9px] font-bold uppercase tracking-widest text-sky-300 hover:bg-sky-500 hover:text-black"
                                  >
                                    <Pencil className="h-3.5 w-3.5" />
                                    Sửa
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => handleDeleteReview(rev)}
                                    disabled={deletingReviewId === String(rev.id)}
                                    className="inline-flex items-center gap-1.5 border border-rose-500/30 bg-rose-950/20 px-3 py-1.5 text-[9px] font-bold uppercase tracking-widest text-rose-300 hover:bg-rose-500 hover:text-white disabled:cursor-wait disabled:opacity-60"
                                  >
                                    {deletingReviewId === String(rev.id) ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Trash2 className="h-3.5 w-3.5" />}
                                    Xóa
                                  </button>
                                </div>
                              )}
                            </div>
                          </>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

          </div>
        </>
      )}

    </div>
  );
}

