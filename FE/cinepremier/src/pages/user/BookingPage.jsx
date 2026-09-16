import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { AnimatePresence, motion, useReducedMotion } from 'motion/react';
import { ChevronRight, ChevronLeft, ArrowLeft, Ticket, ShoppingBag, Plus, Minus, CheckCircle, XCircle, Loader2, Check, ShieldCheck, CircleAlert, Calendar, Clock, ArrowRight } from 'lucide-react';
import { expireAuthSession, getStoredAuth } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { paymentService } from '../../services/paymentService';
import { loyaltyService } from '../../services/loyaltyService';
import { useMovies } from '../../stores/useMovieStore';
import { useUiStore } from '../../stores/useUiStore';

const TICKET_TYPE_META = {
  adult: { apiType: 'ADULT', age: 22, label: 'Người lớn', short: 'NL' },
  child: { apiType: 'CHILD', age: 10, label: 'Trẻ em', short: 'TE' },
  student: { apiType: 'STUDENT', age: 18, label: 'Sinh viên', short: 'SV' }
};

const CHILD_TICKET_MAX_AGE = 12;
const DEFAULT_LOYALTY_CONFIG = {
  earningRatePercent: 1,
  redemptionPoints: 1000,
  redemptionValueVnd: 1000,
  expiryMonth: 12,
  expiryDay: 31,
  expiryTime: '23:59:59',
  lastExpiredAt: null,
  lastResetAt: null,
  lastResetSource: null
};

const normalizeSeatTypeForPricing = (seatType) => {
  const normalized = String(seatType || 'STANDARD').toUpperCase();
  if (normalized === 'NORMAL') return 'STANDARD';
  if (normalized === 'COUPLE') return 'COUPLE';
  if (normalized === 'VIP') return 'VIP';
  return 'STANDARD';
};

const getAgeRatingMinimum = (ageRating) => {
  if (!ageRating) return 0;
  const match = String(ageRating).match(/\d+/);
  return match ? Number(match[0]) : 0;
};

const moneyValue = (value) => {
  if (value === undefined || value === null || value === '') return null;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
};

const formatVnd = (value) => `${Number(value || 0).toLocaleString()}đ`;

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

const normalizeSeatTypeKey = (seatType) => {
  const normalized = String(seatType || 'STANDARD').toUpperCase();
  if (normalized === 'VIP') return 'vip';
  if (normalized === 'COUPLE') return 'couple';
  return 'standard';
};

const MAX_TICKETS = 8;

const getCoupleSeatPair = (seat, allSeats) => {
  if (!seat || normalizeSeatTypeKey(seat.type) !== 'couple') return seat ? [seat] : [];
  const rowSeats = allSeats
    .filter(candidate => candidate.row === seat.row && normalizeSeatTypeKey(candidate.type) === 'couple')
    .sort((a, b) => (a.displayColumn - b.displayColumn) || (a.col - b.col));
  const seatIndex = rowSeats.findIndex(candidate => candidate.id === seat.id);
  if (seatIndex < 0) return [seat];
  const pairStart = seatIndex % 2 === 0 ? seatIndex : seatIndex - 1;
  return rowSeats.slice(pairStart, pairStart + 2);
};

const ticketPriceField = (ticketType, seatType) => {
  const ticketPrefix = ticketType === 'child' ? 'child' : ticketType === 'student' ? 'student' : 'adult';
  const seatKey = normalizeSeatTypeKey(seatType);
  const seatSuffix = seatKey === 'vip' ? 'Vip' : seatKey === 'couple' ? 'Couple' : 'Standard';
  return `${ticketPrefix}${seatSuffix}Price`;
};

const getShowtimeTicketPrice = (showtime, ticketType, seatType) => {
  if (!showtime) return null;
  const surcharge = moneyValue(showtime.surchargeAmount) || 0;
  const configured = moneyValue(showtime[ticketPriceField(ticketType, seatType)]);
  if (configured !== null) return configured + surcharge;

  if (ticketType !== 'adult') return null;
  const seatKey = normalizeSeatTypeKey(seatType);
  const fallback = seatKey === 'vip'
    ? moneyValue(showtime.vipPrice)
    : seatKey === 'couple'
      ? moneyValue(showtime.couplePrice)
      : moneyValue(showtime.basePrice);
  return fallback === null ? null : fallback + surcharge;
};

const getShowtimeRoomKey = (showtime) => {
  const roomId = showtime?.roomId ?? showtime?.room?.id;
  if (roomId !== undefined && roomId !== null && roomId !== '') return `room-${roomId}`;
  return `name-${showtime?.roomName || 'unknown'}`;
};

const CONCESSIONS_PAGE_SIZE = 6;
const HOLD_DURATION_SECONDS = 3 * 60;
const FOOD_STATUS_META = {
  ACTIVE: { label: 'Mở bán', className: 'text-emerald-400' },
  LOW_STOCK: { label: 'Sắp hết', className: 'text-amber-400' },
  OUT_OF_STOCK: { label: 'Hết', className: 'text-rose-400' },
  INACTIVE: { label: 'Hết', className: 'text-rose-400' },
};

const getFoodStatusMeta = (status) => FOOD_STATUS_META[status] || FOOD_STATUS_META.OUT_OF_STOCK;
const isFoodSelectable = (item) => item?.status === 'ACTIVE' || item?.status === 'LOW_STOCK';

const sortShowtimes = (showtimes) => [...showtimes].sort((a, b) => {
  const timeDiff = new Date(a.startTime || 0) - new Date(b.startTime || 0);
  if (timeDiff !== 0) return timeDiff;
  return String(a.roomName || '').localeCompare(String(b.roomName || ''), 'vi');
});

export default function BookingView() {
  const navigate = useNavigate();
  const shouldReduceMotion = useReducedMotion();
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const draftKey = `cp:bookingDraft:${id}`;
  // Draft đọc từ sessionStorage khi mở trang (khôi phục tiến trình sau refresh)
  const pendingDraftRef = useRef(undefined);
  // Trì hoãn dọn draft để React Strict Mode có thể hủy lần cleanup mô phỏng.
  const draftCleanupTimerRef = useRef(null);
  // Ghế chờ áp lại sau khi seat map tải xong
  const pendingSeatRestoreRef = useRef(null);
  const resumeAppliedRef = useRef(false);
  const audienceDialogRef = useRef(null);
  const audienceConfirmButtonRef = useRef(null);
  const audienceReturnFocusRef = useRef(null);
  const { moviesList, foodCatalog, fetchPublicFoodCatalog } = useMovies();
  const showToast = useUiStore((state) => state.showToast);
  const movie = moviesList.find(m => String(m.id) === String(id) || String(m.backendId) === String(id));
  // Cho phép đặt vé khi phim NOW_SHOWING hoặc SCHEDULED (có thể có suất chiếu mở từ trang Lịch chiếu)
  const isMovieBookable = movie?.status === 'NOW_SHOWING'

    || (!movie?.status && !movie?.isUpcoming);
  const childTicketsAllowed = getAgeRatingMinimum(movie?.ageRating) <= CHILD_TICKET_MAX_AGE;
  const onBack = () => navigate(-1);
  const onConfirmBooking = (booking) => {
    navigate('/tickets');
    showToast(`Đặt vé thành công! Mã booking: ${booking.bookingCode || ''}`.trim());
  };
  const seatScrollRef = useRef(null);
  const scrollSeats = (direction) => {
    seatScrollRef.current?.scrollBy({ left: direction === 'left' ? -180 : 180, behavior: 'smooth' });
  };

  const concessions = foodCatalog;

  useEffect(() => {
    fetchPublicFoodCatalog();
  }, []);

  useEffect(() => {
    if (draftCleanupTimerRef.current !== null) {
      window.clearTimeout(draftCleanupTimerRef.current);
      draftCleanupTimerRef.current = null;
    }

    return () => {
      draftCleanupTimerRef.current = window.setTimeout(() => {
        try { sessionStorage.removeItem(draftKey); } catch { /* ignore */ }
        draftCleanupTimerRef.current = null;
      }, 0);
    };
  }, [draftKey]);

  useEffect(() => {
    // Chỉ redirect khi phim thực sự chưa mở bán (UPCOMING rõ ràng), không redirect SCHEDULED
    if (!movie || isMovieBookable) return;
    if (movie.isUpcoming && movie.status !== 'SCHEDULED' && movie.status !== 'NOW_SHOWING') {
      showToast('Phim sắp chiếu chưa mở bán vé.');
      navigate(`/movies/${movie.id}`, { replace: true });
    }
  }, [movie?.id, movie?.status, movie?.isUpcoming, isMovieBookable]);

  // Showtime & seat map from BE
  const [showtimesList, setShowtimesList] = useState([]);
  const [isLoadingShowtimes, setIsLoadingShowtimes] = useState(false);
  const [selectedShowtime, setSelectedShowtime] = useState(null);
  const [seatMapData, setSeatMapData] = useState(null);
  const [isLoadingSeatMap, setIsLoadingSeatMap] = useState(false);
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedRoomKey, setSelectedRoomKey] = useState('');

  // Ticket type counts
  const [adultCount, setAdultCount] = useState(1);
  const [childCount, setChildCount] = useState(0);
  const [studentCount, setStudentCount] = useState(0);
  const totalTickets = adultCount + childCount + studentCount;

  // Seats track ticketType: adult | child | student. Seat type is priced by BE.
  const [selectedSeats, setSelectedSeats] = useState([]);

  useEffect(() => {
    if (childTicketsAllowed) return;
    if (childCount > 0) setChildCount(0);
    setSelectedSeats(prev => prev.some(seat => seat.ticketType === 'child')
      ? prev.filter(seat => seat.ticketType !== 'child')
      : prev);
  }, [childTicketsAllowed, childCount]);

  // Food
  const [selectedCombos, setSelectedCombos] = useState({});
  const [concessionsPage, setConcessionsPage] = useState(1);
  const concessionsTotalPages = Math.max(1, Math.ceil(concessions.length / CONCESSIONS_PAGE_SIZE));
  const safeConcessionsPage = Math.min(concessionsPage, concessionsTotalPages);
  const concessionsStartIndex = (safeConcessionsPage - 1) * CONCESSIONS_PAGE_SIZE;
  const paginatedConcessions = concessions.slice(concessionsStartIndex, concessionsStartIndex + CONCESSIONS_PAGE_SIZE);
  const concessionsDisplayStart = concessions.length === 0 ? 0 : concessionsStartIndex + 1;
  const concessionsDisplayEnd = Math.min(concessionsStartIndex + CONCESSIONS_PAGE_SIZE, concessions.length);

  useEffect(() => {
    setConcessionsPage((page) => Math.min(page, concessionsTotalPages));
  }, [concessionsTotalPages]);

  // Tính năng giảm giá/khuyến mãi đã được gỡ khỏi nghiệp vụ.
  const appliedPromo = null;

  // Booking & payment flow
  const [bookingStep, setBookingStep] = useState('seats');
  const [holdBookingId, setHoldBookingId] = useState(null);
  const [holdExpiresAt, setHoldExpiresAt] = useState(null);
  const [holdSecondsLeft, setHoldSecondsLeft] = useState(null);
  const [seatClockTick, setSeatClockTick] = useState(Date.now());
  const [isHolding, setIsHolding] = useState(false);
  const [paymentState, setPaymentState] = useState('booking');
  const [showAudienceConfirmation, setShowAudienceConfirmation] = useState(false);
  const [ticketPriceValidation, setTicketPriceValidation] = useState(null);
  const [loyaltyPoints, setLoyaltyPoints] = useState(0);
  const [loyaltyPointsInput, setLoyaltyPointsInput] = useState('');
  const [loyaltyConfig, setLoyaltyConfig] = useState(DEFAULT_LOYALTY_CONFIG);
  const [heldPaymentSummary, setHeldPaymentSummary] = useState(null);
  const [paidBooking, setPaidBooking] = useState(null);
  const [activeHoldingBookings, setActiveHoldingBookings] = useState([]);
  const [isCancellingExistingHold, setIsCancellingExistingHold] = useState(false);
  const heldSeatIdsRef = useRef(null);
  // Lưu số điểm GỐC trước khi trừ tạm khi hold ghế.
  // Khi quay lại: SET tuyệt đối về giá trị này — không cộng tích luỹ.
  const loyaltyPointsBeforeHoldRef = useRef(null);

  // Sau khi thanh toán thành công: tải booking để lấy QR thật + mã vé từng ghế
  useEffect(() => {
    if (paymentState !== 'payment_success' || !holdBookingId) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    let cancelled = false;
    bookingService.getMyBooking(accessToken, holdBookingId)
      .then((booking) => { if (!cancelled) setPaidBooking(booking); })
      .catch(() => { /* QR hiển thị fallback trong My Tickets */ });
    return () => { cancelled = true; };
  }, [paymentState, holdBookingId]);

  useEffect(() => {
    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      setLoyaltyPoints(0);
      setLoyaltyConfig(DEFAULT_LOYALTY_CONFIG);
      return;
    }
    let cancelled = false;
    Promise.all([
      loyaltyService.getMyLoyalty(accessToken),
      loyaltyService.getConfiguration(accessToken)
    ])
      .then(([loyalty, config]) => {
        if (cancelled) return;
        setLoyaltyPoints(Number(loyalty?.points ?? 0));
        setLoyaltyConfig({ ...DEFAULT_LOYALTY_CONFIG, ...(config || {}) });
      })
      .catch(() => {
        if (!cancelled) {
          setLoyaltyPoints(0);
          setLoyaltyConfig(DEFAULT_LOYALTY_CONFIG);
        }
      });
    return () => { cancelled = true; };
  }, []);

  // Booking đang giữ là nguồn dữ liệu chính khi khách quay lại đúng suất chiếu.
  useEffect(() => {
    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      setActiveHoldingBookings([]);
      return;
    }

    let cancelled = false;
    bookingService.getMyBookings(accessToken)
      .then((bookings) => {
        if (cancelled) return;
        const now = Date.now();
        setActiveHoldingBookings((Array.isArray(bookings) ? bookings : []).filter((booking) => (
          ['HOLDING', 'PENDING_PAYMENT'].includes(booking.status)
          && booking.holdExpiresAt
          && new Date(booking.holdExpiresAt).getTime() > now
        )));
      })
      .catch(() => {
        if (!cancelled) setActiveHoldingBookings([]);
      });

    return () => { cancelled = true; };
  }, []);

  // Load showtimes for this movie
  useEffect(() => {
    if (!isMovieBookable) return;
    if (!movie?.backendId && !movie?.id) return;
    const movieId = movie.backendId || movie.id;
    if (isNaN(Number(movieId))) return;

    if (pendingDraftRef.current === undefined) {
      try {
        const raw = sessionStorage.getItem(draftKey);
        pendingDraftRef.current = raw ? JSON.parse(raw) : null;
      } catch {
        pendingDraftRef.current = null;
      }
    }

    setIsLoadingShowtimes(true);
    bookingService.getShowtimes({ movieId: Number(movieId) })
      .then(data => {
        // BE trả PageResponse { items, page, ... } — không phải mảng thuần.
        const rawList = Array.isArray(data) ? data : (data?.items ?? data?.content ?? []);
        // Ẩn suất đã qua giờ bắt đầu — không cho đặt vé suất đang/đã chiếu
        const upcoming = rawList.filter(st => new Date(st.startTime) > new Date());
        const list = sortShowtimes(upcoming);
        setShowtimesList(list);
        if (list.length > 0) {
          // Ưu tiên suất từ deep-link (?showtimeId=) rồi tới draft đã lưu, cuối cùng là suất đầu tiên
          const draft = pendingDraftRef.current;
          const preferId = searchParams.get('showtimeId') || draft?.showtimeId;
          const preferred = preferId ? list.find(st => String(st.id) === String(preferId)) : null;
          const first = preferred || list[0];
          const date = first.startTime?.split('T')[0] || '';
          setSelectedDate(date);
          setSelectedRoomKey(getShowtimeRoomKey(first));
          setSelectedShowtime(first);
          if (draft && preferred && String(draft.showtimeId) === String(preferred.id) && !searchParams.get('resumeBookingId')) {
            setAdultCount(draft.counts?.adult ?? 1);
            setChildCount(draft.counts?.child ?? 0);
            setStudentCount(draft.counts?.student ?? 0);
            if (draft.combos) setSelectedCombos(draft.combos);
            if (draft.bookingStep) setBookingStep(draft.bookingStep);
            if (Array.isArray(draft.seats) && draft.seats.length > 0) {
              pendingSeatRestoreRef.current = { showtimeId: preferred.id, seats: draft.seats, allowHeld: false };
            }
          }
          pendingDraftRef.current = null;
        } else {
          setSelectedDate('');
          setSelectedRoomKey('');
          setSelectedShowtime(null);
        }
      })
      .catch(() => {
        setShowtimesList([]);
        setSelectedDate('');
        setSelectedRoomKey('');
        setSelectedShowtime(null);
      })
      .finally(() => setIsLoadingShowtimes(false));
  }, [movie?.backendId, movie?.id]);

  // Load seat map when showtime changes
  useEffect(() => {
    if (!selectedShowtime?.id) { setSeatMapData(null); return; }
    setIsLoadingSeatMap(true);
    setSelectedSeats([]);
    bookingService.getSeatMap(selectedShowtime.id)
      .then(data => setSeatMapData(data))
      .catch(() => setSeatMapData(null))
      .finally(() => setIsLoadingSeatMap(false));
  }, [selectedShowtime?.id]);

  // Derived: group showtimes by date
  const dateOptions = [...new Set(showtimesList.map(st => st.startTime?.split('T')[0]))].filter(Boolean);
  const showtimesForDate = showtimesList.filter(st => st.startTime?.split('T')[0] === selectedDate);
  const roomOptionsForDate = showtimesForDate.reduce((rooms, st) => {
    const key = getShowtimeRoomKey(st);
    if (!rooms.some(room => room.key === key)) {
      rooms.push({
        key,
        name: st.roomName || st.room?.name || 'Chưa rõ phòng',
        roomType: st.roomType || st.room?.roomType || ''
      });
    }
    return rooms;
  }, []);
  const showtimesForSelectedRoom = selectedRoomKey
    ? showtimesForDate.filter(st => getShowtimeRoomKey(st) === selectedRoomKey)
    : showtimesForDate;

  const handleSelectDate = (date) => {
    setSelectedDate(date);
    const first = showtimesList.find(st => st.startTime?.split('T')[0] === date);
    if (first) {
      setSelectedRoomKey(getShowtimeRoomKey(first));
      setSelectedShowtime(first);
    }
    setSelectedSeats([]);
    setSelectedCombos({});
  };

  const handleSelectRoom = (roomKey) => {
    setSelectedRoomKey(roomKey);
    const first = showtimesForDate.find(st => getShowtimeRoomKey(st) === roomKey);
    if (first) setSelectedShowtime(first);
    setSelectedSeats([]);
    setSelectedCombos({});
  };

  const handleSelectShowtime = (st) => {
    setSelectedRoomKey(getShowtimeRoomKey(st));
    setSelectedShowtime(st);
    setSelectedSeats([]);
    setSelectedCombos({});
  };

  // Seat selection
  const handleSelectSeat = (seat) => {
    if (matchingHoldingBooking) {
      showToast('Bạn đã có đơn đang giữ ghế cho suất chiếu này. Hãy tiếp tục thanh toán hoặc hủy đơn hiện tại trước khi chọn ghế khác.');
      return;
    }
    if (seat.isBooked) return;
    const seatGroup = getCoupleSeatPair(seat, seats);
    const isCoupleSeat = normalizeSeatTypeKey(seat.type) === 'couple';
    const isCoupleSelection = isCoupleSeat && seatGroup.length === 2;
    if (isCoupleSeat && (seatGroup.length !== 2 || seatGroup.some(groupSeat => groupSeat.isBooked))) {
      showToast('Ghế đôi phải chọn trọn cặp, hiện có ghế trong cặp không khả dụng.');
      return;
    }

    const selectedSeatIds = new Set(selectedSeats.map(s => s.id));
    const alreadySelected = seatGroup.some(groupSeat => selectedSeatIds.has(groupSeat.id));
    if (alreadySelected) {
      setSelectedSeats(prev => prev.filter(s => !seatGroup.some(groupSeat => groupSeat.id === s.id)));
    } else {
      if (selectedSeats.length + seatGroup.length > totalTickets) {
        if (isCoupleSelection) {
          showToast('Ghế đôi cần chọn đủ 2 vé. Vui lòng tăng tổng số lượng vé trước khi chọn cặp ghế này.');
          return;
        }
        showToast(`Bạn đã chọn đủ ${totalTickets} vé. Tăng số lượng vé để chọn thêm ghế.`);
        return;
      }
      // Ticket type is selected independently from the seat type.
      setSelectedSeats(prev => {
        const next = [...prev];
        seatGroup.forEach(groupSeat => {
          const currentAdults = next.filter(s => s.ticketType === 'adult').length;
          const currentChildren = next.filter(s => s.ticketType === 'child').length;
          const ticketType = currentAdults < adultCount
            ? 'adult'
            : currentChildren < childCount
              ? 'child'
              : 'student';
          next.push({ ...groupSeat, ticketType });
        });
        return next;
      });
    }
  };

  // Build seats from seatMapData
  const seats = seatMapData
    ? seatMapData.seats.map(s => ({
      id: `${s.rowLabel}${s.seatNumber}`,
      seatId: s.seatId,
      row: s.rowLabel,
      col: s.seatNumber,
      displayOrder: s.displayOrder ?? 0,
      displayColumn: s.displayColumn ?? s.seatNumber,
      startColumn: s.startColumn ?? 1,
      type: s.seatType?.toLowerCase(),
      price: moneyValue(s.unitPrice) ?? 0,
      runtimeStatus: s.runtimeStatus,
      holdExpiresAt: s.holdExpiresAt || null,
      isBooked: s.runtimeStatus === 'HOLDING' || s.runtimeStatus === 'BOOKED' || s.runtimeStatus === 'CHECKED_IN' || s.seatStatus !== 'AVAILABLE'
    })).sort((a, b) => (a.displayOrder - b.displayOrder) || (a.displayColumn - b.displayColumn) || (a.col - b.col))
    : [];

  // Đổi số lượng vé nhưng GIỮ ghế đã chọn: chỉ cắt bớt khi vượt tổng vé mới,
  // và gán lại ticketType cho ghế nào không còn quota loại cũ.
  const changeTicketCount = (type, delta) => {
    const next = {
      adult: adultCount,
      child: childCount,
      student: studentCount,
    };
    next[type] += delta;
    const newTotal = next.adult + next.child + next.student;
    if (next[type] < 0 || newTotal > MAX_TICKETS) return;

    setAdultCount(next.adult);
    setChildCount(next.child);
    setStudentCount(next.student);

    setSelectedSeats(prev => {
      // Cắt từ cuối nếu vượt tổng vé mới — ghế đôi phải bỏ trọn cặp
      let kept = [...prev];
      while (kept.length > newTotal) {
        const last = kept[kept.length - 1];
        const pairIds = new Set(getCoupleSeatPair(last, seats).map(s => s.id));
        kept = kept.filter(s => s.id !== last.id && !pairIds.has(s.id));
      }
      // Giữ loại vé cũ nếu còn quota, phần dư gán lại theo thứ tự adult → child → student
      const capacity = { ...next };
      const marked = kept.map(s => {
        if (capacity[s.ticketType] > 0) {
          capacity[s.ticketType] -= 1;
          return s;
        }
        return { ...s, ticketType: null };
      });
      return marked.map(s => {
        if (s.ticketType) return s;
        const t = capacity.adult > 0 ? 'adult' : capacity.child > 0 ? 'child' : 'student';
        capacity[t] -= 1;
        return { ...s, ticketType: t };
      });
    });
  };

  useEffect(() => {
    const hasActiveSeatHold = seatMapData?.seats?.some(seat => Boolean(seat.holdExpiresAt));
    if (!hasActiveSeatHold) return undefined;

    const intervalId = window.setInterval(() => setSeatClockTick(Date.now()), 1000);
    return () => window.clearInterval(intervalId);
  }, [seatMapData]);

  // Áp lại ghế đã lưu (draft/resume) sau khi seat map tải xong; bỏ ghế đã bị người khác chiếm
  useEffect(() => {
    const pending = pendingSeatRestoreRef.current;
    if (!pending || !seatMapData || !selectedShowtime) return;
    if (String(pending.showtimeId) !== String(selectedShowtime.id)) {
      pendingSeatRestoreRef.current = null;
      return;
    }
    pendingSeatRestoreRef.current = null;
    const restored = pending.seats
      .map((saved) => {
        const live = seats.find(s => String(s.seatId) === String(saved.seatId));
        if (!live) return pending.allowHeld ? saved : null;
        if (live.isBooked && !pending.allowHeld) return null;
        return { ...live, ticketType: saved.ticketType || 'adult' };
      })
      .filter(Boolean);
    if (restored.length > 0) setSelectedSeats(restored);
  }, [seatMapData]);

  // Lưu tiến trình đặt vé vào sessionStorage để refresh không mất bước đang làm
  useEffect(() => {
    if (paymentState === 'payment_success') {
      try { sessionStorage.removeItem(draftKey); } catch { /* ignore */ }
      return;
    }
    if (!selectedShowtime?.id) return;
    const draft = {
      showtimeId: selectedShowtime.id,
      counts: { adult: adultCount, child: childCount, student: studentCount },
      seats: selectedSeats.map(({ seatId, ticketType }) => ({ seatId, ticketType })),
      combos: selectedCombos,
      bookingStep
    };
    try { sessionStorage.setItem(draftKey, JSON.stringify(draft)); } catch { /* ignore */ }
  }, [selectedShowtime?.id, adultCount, childCount, studentCount, selectedSeats, selectedCombos, bookingStep, paymentState]);

  // "Tiếp tục thanh toán" từ My Tickets: khôi phục booking đang giữ và vào thẳng màn thanh toán
  useEffect(() => {
    const resumeBookingId = searchParams.get('resumeBookingId');
    if (!resumeBookingId || resumeAppliedRef.current || showtimesList.length === 0) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    resumeAppliedRef.current = true;
    bookingService.getMyBooking(accessToken, resumeBookingId)
      .then((booking) => {
        const stillHeld = ['HOLDING', 'PENDING_PAYMENT'].includes(booking.status)
          && booking.holdExpiresAt
          && new Date(booking.holdExpiresAt).getTime() > Date.now();
        if (!stillHeld) {
          showToast('Booking đã hết thời gian giữ ghế. Vui lòng đặt lại từ đầu.');
          return;
        }
        const st = showtimesList.find(s => String(s.id) === String(booking.showtimeId));
        if (st) {
          setSelectedDate(st.startTime?.split('T')[0] || '');
          setSelectedRoomKey(getShowtimeRoomKey(st));
          setSelectedShowtime(st);
        }
        const restoredSeats = (booking.seats || []).map((seat) => ({
          id: `${seat.rowLabel}${seat.seatNumber}`,
          seatId: seat.seatId,
          type: 'standard',
          ticketType: String(seat.ticketType || 'ADULT').toLowerCase(),
          resumeUnitPrice: Number(seat.unitPrice || 0)
        }));
        const restoredCombos = (booking.foods || []).reduce((result, food) => {
          const foodId = food.foodItemId
            ? `item-${food.foodItemId}`
            : food.foodComboId
              ? `combo-${food.foodComboId}`
              : null;
          if (foodId) result[foodId] = Number(food.quantity || 0);
          return result;
        }, {});
        setAdultCount(restoredSeats.filter(s => s.ticketType === 'adult').length);
        setChildCount(restoredSeats.filter(s => s.ticketType === 'child').length);
        setStudentCount(restoredSeats.filter(s => s.ticketType === 'student').length);
        setSelectedSeats(restoredSeats);
        setSelectedCombos(restoredCombos);
        pendingSeatRestoreRef.current = { showtimeId: booking.showtimeId, seats: restoredSeats, allowHeld: true };
        heldSeatIdsRef.current = restoredSeats.map(s => s.seatId);
        setHoldBookingId(booking.id);
        setHoldExpiresAt(booking.holdExpiresAt);
        setHeldPaymentSummary({
          subtotal: Number(booking.subtotal || 0),
          discountAmount: Number(booking.discountAmount || 0),
          totalAmount: Number(booking.totalAmount || 0),
          loyaltyPointsRedeemed: Number(booking.loyaltyPointsRedeemed || 0)
        });
        setBookingStep('combos');
        setPaymentState('payment_method');
      })
      .catch(() => showToast('Không thể khôi phục booking đang giữ.'));
  }, [showtimesList]);

  const buildTicketSelections = () => {
    return selectedSeats.map((seat) => {
      const meta = TICKET_TYPE_META[seat.ticketType] || TICKET_TYPE_META.adult;
      return {
        seatId: seat.seatId,
        ticketType: meta.apiType,
        seatType: normalizeSeatTypeForPricing(seat.type),
        viewerAge: meta.age,
        quantity: 1
      };
    });
  };

  const buildFoodRequests = () => Object.entries(selectedCombos)
    .map(([id, quantity]) => {
      const item = concessions.find(i => i.id === id);
      if (!item) return null;
      return {
        foodItemId: item.foodItemId ?? null,
        foodComboId: item.foodComboId ?? null,
        quantity
      };
    })
    .filter(Boolean);

  useEffect(() => {
    if (!selectedShowtime?.id || selectedSeats.length === 0) {
      setTicketPriceValidation(null);
      return;
    }
    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      setTicketPriceValidation(null);
      return;
    }
    const tickets = buildTicketSelections();
    let cancelled = false;
    bookingService.validateTicketPrice(accessToken, {
      showtimeId: selectedShowtime.id,
      holiday: false,
      tickets
    })
      .then((result) => {
        if (!cancelled) setTicketPriceValidation(result);
      })
      .catch(() => {
        if (!cancelled) setTicketPriceValidation(null);
      });
    return () => { cancelled = true; };
  }, [selectedShowtime?.id, selectedSeats]);

  const validationLines = Array.isArray(ticketPriceValidation?.tickets) ? ticketPriceValidation.tickets : [];
  const getSeatValidationLine = (seat) => {
    const index = selectedSeats.findIndex(item => item.id === seat.id);
    return index >= 0 ? validationLines[index] : null;
  };
  const getSeatUnitPrice = (seat) => {
    const line = getSeatValidationLine(seat);
    return moneyValue(line?.unitPrice)
      ?? moneyValue(seat.resumeUnitPrice)
      ?? getShowtimeTicketPrice(selectedShowtime, seat.ticketType || 'adult', seat.type)
      ?? moneyValue(seat.price)
      ?? 0;
  };
  const getSeatLineTotal = (seat) => {
    const line = getSeatValidationLine(seat);
    return moneyValue(line?.lineTotal) ?? getSeatUnitPrice(seat);
  };
  const getSelectedSeatTotal = (ticketType) => selectedSeats
    .filter(seat => seat.ticketType === ticketType)
    .reduce((total, seat) => total + getSeatLineTotal(seat), 0);
  const getTicketStartingPrice = (ticketType) => {
    const prices = [...new Set(seats.map(seat => seat.type))]
      .map(seatType => getShowtimeTicketPrice(selectedShowtime, ticketType, seatType))
      .filter(value => value !== null && Number.isFinite(value));
    return prices.length ? Math.min(...prices) : null;
  };
  const firstTicketError = validationLines.find(line => line && line.eligible === false)?.message;

  // Food combos
  const handleModifyCombo = (id, operator) => {
    const product = concessions.find((item) => item.id === id);
    const maxQuantity = isFoodSelectable(product) ? 3 : 0;
    setSelectedCombos(prev => {
      const qty = prev[id] || 0;
      let next = operator === '+' ? Math.min(maxQuantity, qty + 1) : Math.max(0, qty - 1);
      if (operator === '+' && qty >= 3) showToast('Tối đa 3 phần mỗi sản phẩm.');
      if (operator === '+' && maxQuantity === 0) showToast('Món này đang hết.');
      const updated = { ...prev };
      if (next === 0) delete updated[id]; else updated[id] = next;
      return updated;
    });
  };

  // Nếu khách quay lại đúng suất đang giữ, hiển thị bill từ booking backend thay vì draft cục bộ.
  const matchingHoldingBooking = activeHoldingBookings.find((booking) => (
    String(booking.showtimeId) === String(selectedShowtime?.id)
    && new Date(booking.holdExpiresAt).getTime() > seatClockTick
  )) || null;
  const isViewingExistingHold = Boolean(matchingHoldingBooking);

  useEffect(() => {
    if (!matchingHoldingBooking) return;
    // Backend booking is the source of truth. Discard any stale local draft so
    // the customer cannot accidentally build a second order for this showtime.
    setSelectedSeats([]);
    setSelectedCombos({});
    setBookingStep('seats');
    setTicketPriceValidation(null);
  }, [matchingHoldingBooking?.id]);
  const heldTicketRows = isViewingExistingHold
    ? (matchingHoldingBooking.seats || []).map((seat) => {
      const ticketType = String(seat.ticketType || 'ADULT').toLowerCase();
      const unitPrice = Number(seat.unitPrice || 0);
      return {
        id: seat.seatId,
        label: `Ghế ${seat.rowLabel}${seat.seatNumber}`,
        ticketType,
        unitPrice,
        lineTotal: unitPrice
      };
    })
    : [];
  const localFoodRows = Object.entries(selectedCombos)
    .map(([id, quantity]) => {
      const item = concessions.find(candidate => candidate.id === id);
      if (!item) return null;
      return {
        id,
        name: item.name,
        unitPrice: Number(item.price || 0),
        quantity: Number(quantity || 0),
        lineTotal: Number(item.price || 0) * Number(quantity || 0)
      };
    })
    .filter(Boolean);
  const heldFoodRows = isViewingExistingHold
    ? (matchingHoldingBooking.foods || []).map((food, index) => ({
      id: food.foodItemId ? `item-${food.foodItemId}` : food.foodComboId ? `combo-${food.foodComboId}` : `held-food-${index}`,
      name: food.name || 'Bắp nước',
      unitPrice: Number(food.unitPrice || 0),
      quantity: Number(food.quantity || 0),
      lineTotal: Number(food.totalPrice ?? (Number(food.unitPrice || 0) * Number(food.quantity || 0)))
    }))
    : [];

  // Financial calculations
  const fallbackTicketPrice = selectedSeats.reduce((total, s) => total + getSeatLineTotal(s), 0);
  const heldTicketPrice = heldTicketRows.reduce((total, row) => total + row.lineTotal, 0);
  const priceTickets = Number(ticketPriceValidation?.finalAmount ?? ticketPriceValidation?.ticketSubtotal ?? fallbackTicketPrice);
  const displayTicketPrice = isViewingExistingHold ? heldTicketPrice : priceTickets;
  const receiptTicketRows = isViewingExistingHold
    ? heldTicketRows
    : selectedSeats.map(seat => ({
      id: seat.id,
      label: `Ghế ${seat.id}`,
      ticketType: seat.ticketType || 'adult',
      unitPrice: getSeatUnitPrice(seat),
      lineTotal: getSeatLineTotal(seat)
    }));
  const receiptFoodRows = isViewingExistingHold ? heldFoodRows : localFoodRows;
  const priceCombos = receiptFoodRows.reduce((total, row) => total + row.lineTotal, 0);
  const subTotal = displayTicketPrice + priceCombos;
  const requestedLoyaltyPoints = Math.max(0, Number(String(loyaltyPointsInput || '').replace(/\D/g, '')) || 0);
  const earningRatePercent = Math.max(0, Number(loyaltyConfig.earningRatePercent ?? DEFAULT_LOYALTY_CONFIG.earningRatePercent) || 0);
  const redemptionPoints = Math.max(1, Number(loyaltyConfig.redemptionPoints ?? DEFAULT_LOYALTY_CONFIG.redemptionPoints) || DEFAULT_LOYALTY_CONFIG.redemptionPoints);
  const redemptionValueVnd = Math.max(1, Number(loyaltyConfig.redemptionValueVnd ?? DEFAULT_LOYALTY_CONFIG.redemptionValueVnd) || DEFAULT_LOYALTY_CONFIG.redemptionValueVnd);
  const loyaltyResetScheduleLabel = formatDateTimeVi(buildLoyaltyExpiryDateTime(loyaltyConfig));
  const loyaltyAdminResetAt = loyaltyConfig.lastResetSource === 'ADMIN'
    ? formatDateTimeVi(loyaltyConfig.lastResetAt || loyaltyConfig.lastExpiredAt)
    : '';
  const maxRedeemablePointsForSubtotal = Math.max(0, Math.floor(Math.floor(subTotal) * redemptionPoints / redemptionValueVnd));
  const loyaltyPointsToRedeem = Math.min(requestedLoyaltyPoints, loyaltyPoints, maxRedeemablePointsForSubtotal);
  const loyaltyDiscountAmount = Math.min(
    Math.floor(loyaltyPointsToRedeem * redemptionValueVnd / redemptionPoints),
    Math.max(0, Math.floor(subTotal))
  );
  const earnedPointsPreview = Math.floor(Math.max(0, subTotal - loyaltyDiscountAmount) * earningRatePercent / 100);
  const discountAmount = loyaltyDiscountAmount;
  const totalAmount = Math.max(0, subTotal - discountAmount);
  const loyaltyValidationMessage = requestedLoyaltyPoints > loyaltyPoints
    ? `Số điểm muốn dùng không được vượt quá ${loyaltyPoints.toLocaleString('vi-VN')} điểm hiện có.`
    : requestedLoyaltyPoints > maxRedeemablePointsForSubtotal
      ? `Số điểm muốn dùng vượt quá mức giảm tối đa của hóa đơn (${maxRedeemablePointsForSubtotal.toLocaleString('vi-VN')} điểm).`
      : '';

  const existingHoldDiscount = isViewingExistingHold ? Number(matchingHoldingBooking.discountAmount || 0) : null;
  const existingHoldTotal = isViewingExistingHold ? Number(matchingHoldingBooking.totalAmount || subTotal) : null;
  const gatewayDiscountAmount = Number(heldPaymentSummary?.discountAmount ?? existingHoldDiscount ?? discountAmount);
  const gatewayTotalAmount = heldPaymentSummary
    ? Number(heldPaymentSummary.totalAmount)
    : existingHoldTotal ?? totalAmount;
  const gatewayRedeemedPoints = Number(heldPaymentSummary?.loyaltyPointsRedeemed
    ?? (isViewingExistingHold ? matchingHoldingBooking.loyaltyPointsRedeemed : null)
    ?? loyaltyPointsToRedeem);
  const audienceMinimumAge = getAgeRatingMinimum(movie?.ageRating);
  const selectedShowtimeEnd = (() => {
    const explicitEnd = selectedShowtime?.endTime ? new Date(selectedShowtime.endTime) : null;
    if (explicitEnd && !Number.isNaN(explicitEnd.getTime())) return explicitEnd;
    const start = selectedShowtime?.startTime ? new Date(selectedShowtime.startTime) : null;
    if (!start || Number.isNaN(start.getTime())) return null;
    return new Date(start.getTime() + Number(movie?.duration || 0) * 60 * 1000);
  })();
  const isLateNightShow = (() => {
    if (!selectedShowtimeEnd || !selectedShowtime?.startTime) return false;
    const start = new Date(selectedShowtime.startTime);
    if (Number.isNaN(start.getTime())) return false;
    const lateNightCutoff = new Date(start);
    lateNightCutoff.setHours(23, 0, 0, 0);
    return selectedShowtimeEnd.getTime() > lateNightCutoff.getTime();
  })();

  useEffect(() => {
    if (!showAudienceConfirmation) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    audienceConfirmButtonRef.current?.focus();
    const handleDialogKeyDown = (event) => {
      if (event.key === 'Escape') {
        setShowAudienceConfirmation(false);
        return;
      }
      if (event.key !== 'Tab') return;
      const focusable = audienceDialogRef.current?.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])');
      if (!focusable?.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    window.addEventListener('keydown', handleDialogKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleDialogKeyDown);
      audienceReturnFocusRef.current?.focus?.();
    };
  }, [showAudienceConfirmation]);

  const formatHoldSeconds = (seconds) => {
    if (seconds === null || seconds === undefined) return '--:--';
    const normalizedSeconds = Math.max(0, Math.floor(Number(seconds) || 0));
    return `${String(Math.floor(normalizedSeconds / 60)).padStart(2, '0')}:${String(normalizedSeconds % 60).padStart(2, '0')}`;
  };
  const holdCountdownLabel = holdSecondsLeft === null
    ? '--:--'
    : formatHoldSeconds(holdSecondsLeft);
  const holdProgressPercent = holdSecondsLeft === null ? 100 : Math.max(0, Math.min(100, (holdSecondsLeft / HOLD_DURATION_SECONDS) * 100));

  const handleLoyaltyPointsInputChange = (event) => {
    const nextValue = event.target.value.replace(/\D/g, '').slice(0, 9);
    setLoyaltyPointsInput(nextValue);
  };

  const applyMaxLoyaltyPoints = () => {
    const maxPoints = Math.min(loyaltyPoints, maxRedeemablePointsForSubtotal);
    setLoyaltyPointsInput(String(maxPoints));
  };

  const clearLoyaltyPoints = () => setLoyaltyPointsInput('');

  const renderLoyaltyRedeemPanel = () => (
    <div className="border border-emerald-500/20 bg-emerald-950/10 p-3 space-y-3">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-[10px] font-black uppercase tracking-widest text-emerald-300">CinePoints</p>
          <p className="mt-1 text-[11px] font-mono text-neutral-500">
            {loyaltyPoints.toLocaleString('vi-VN')} điểm khả dụng · {redemptionPoints.toLocaleString('vi-VN')} điểm = {redemptionValueVnd.toLocaleString('vi-VN')}đ
          </p>
          {loyaltyResetScheduleLabel && (
            <p className="mt-1 text-[10px] font-mono text-neutral-500">
              Ngày reset điểm: {loyaltyResetScheduleLabel}
            </p>
          )}
          {loyaltyAdminResetAt && Number(loyaltyPoints || 0) === 0 && (
            <p className="mt-1 border-l border-amber-400/40 pl-2 text-[10px] font-bold leading-snug text-amber-300">
              Quản trị viên đã reset điểm lúc {loyaltyAdminResetAt}.
            </p>
          )}
        </div>
        <div className="text-right">
          <p className="text-[11px] uppercase tracking-widest text-neutral-500">Dự kiến cộng</p>
          <p className="font-mono text-xs font-black text-emerald-300">+{earnedPointsPreview.toLocaleString('vi-VN')}</p>
        </div>
      </div>
      <div className="grid grid-cols-[minmax(0,1fr)_auto_auto] gap-2">
        <input
          type="text"
          inputMode="numeric"
          value={loyaltyPointsInput}
          onChange={handleLoyaltyPointsInputChange}
          placeholder="Điểm muốn dùng"
          disabled={loyaltyPoints <= 0 || subTotal <= 0}
          className="min-w-0 border border-white/10 bg-black px-3 py-2 text-xs font-mono font-bold text-white outline-none focus:border-emerald-400 disabled:cursor-not-allowed disabled:opacity-50"
        />
        <button
          type="button"
          onClick={applyMaxLoyaltyPoints}
          disabled={loyaltyPoints <= 0 || subTotal <= 0}
          className="border border-emerald-500/30 px-3 py-2 text-[10px] font-black uppercase tracking-wider text-emerald-300 hover:bg-emerald-500/10 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Tối đa
        </button>
        <button
          type="button"
          onClick={clearLoyaltyPoints}
          disabled={!loyaltyPointsInput}
          className="border border-white/10 px-3 py-2 text-[10px] font-black uppercase tracking-wider text-neutral-400 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          Xóa
        </button>
      </div>
      {loyaltyDiscountAmount > 0 && (
        <div className="flex justify-between border-t border-white/5 pt-2 text-[10px] font-mono uppercase tracking-wider text-emerald-300">
          <span>Đang dùng</span>
          <span>-{loyaltyDiscountAmount.toLocaleString('vi-VN')}đ ({loyaltyPointsToRedeem.toLocaleString('vi-VN')} điểm)</span>
        </div>
      )}
    </div>
  );

  const refreshSeatMap = async () => {
    if (!selectedShowtime?.id) return;
    try {
      const data = await bookingService.getSeatMap(selectedShowtime.id);
      setSeatMapData(data);
    } catch {
      // A later explicit seat-map load will recover this state.
    }
  };

  const refreshLoyaltyBalance = async () => {
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    try {
      const loyalty = await loyaltyService.getMyLoyalty(accessToken);
      setLoyaltyPoints(Number(loyalty?.points ?? 0));
    } catch {
      // Profile page will refresh the balance later if this lightweight sync fails.
    }
  };

  const releaseHeldBooking = async () => {
    const bookingId = holdBookingId;
    if (!bookingId) return;

    const { accessToken } = getStoredAuth();
    heldSeatIdsRef.current = null;
    setHoldBookingId(null);
    setHoldExpiresAt(null);
    setHoldSecondsLeft(null);
    setTicketPriceValidation(null);
    setHeldPaymentSummary(null);

    if (!accessToken) return;
    try {
      await bookingService.cancelBooking(accessToken, bookingId);
    } catch (err) {
      console.warn('[releaseHeldBooking] unable to cancel held booking:', err);
    } finally {
      refreshSeatMap();
      refreshLoyaltyBalance();
    }
  };

  useEffect(() => {
    // Đếm ngược mọi lúc còn hold (kể cả khi quay lại bước bắp nước)
    if (!holdExpiresAt || !holdBookingId || ['payment_success'].includes(paymentState)) return undefined;

    const tick = () => {
      const expiresAtMs = new Date(holdExpiresAt).getTime();
      if (Number.isNaN(expiresAtMs)) {
        setHoldSecondsLeft(null);
        return;
      }
      const secondsLeft = Math.max(0, Math.ceil((expiresAtMs - Date.now()) / 1000));
      setHoldSecondsLeft(secondsLeft);

      if (secondsLeft === 0) {
        showToast('Thời gian giữ ghế đã hết. Vui lòng chọn ghế lại.');
        setPaymentState('booking');
        setBookingStep('seats');
        releaseHeldBooking();
      }
    };

    tick();
    const intervalId = window.setInterval(tick, 1000);
    return () => window.clearInterval(intervalId);
  }, [holdExpiresAt, paymentState]);

  const isHoldActive = () => {
    if (!holdBookingId) {
      showToast('Không tìm thấy booking. Vui lòng chọn ghế lại.');
      setPaymentState('booking');
      return false;
    }
    if (holdExpiresAt && new Date(holdExpiresAt).getTime() <= Date.now()) {
      showToast('Thời gian giữ ghế đã hết. Vui lòng chọn ghế lại.');
      setPaymentState('booking');
      setBookingStep('seats');
      releaseHeldBooking();
      return false;
    }
    return true;
  };

  const isSeatHeldByCurrentUser = (seat) => Boolean(
    holdBookingId
    && holdSecondsLeft !== null
    && holdSecondsLeft > 0
    && selectedSeats.some((selectedSeat) => selectedSeat.id === seat.id)
  );

  const getSeatHoldSecondsLeft = (seat) => {
    if (isSeatHeldByCurrentUser(seat)) {
      return holdSecondsLeft;
    }
    if (!seat?.holdExpiresAt) {
      return null;
    }
    const expiresAtMs = new Date(seat.holdExpiresAt).getTime();
    if (Number.isNaN(expiresAtMs)) {
      return null;
    }
    return Math.max(0, Math.ceil((expiresAtMs - seatClockTick) / 1000));
  };

  const renderSeatHoldCountdown = (seat) => {
    const secondsLeft = getSeatHoldSecondsLeft(seat);
    if (secondsLeft === null || secondsLeft <= 0) {
      return null;
    }
    const isMine = isSeatHeldByCurrentUser(seat);
    return (
      <span className={`pointer-events-none absolute -bottom-3 left-1/2 z-20 min-w-[32px] -translate-x-1/2 border px-1 py-0.5 text-center text-[7px] font-black leading-none shadow-lg ${secondsLeft <= 15
        ? 'border-red-400 bg-red-500 text-white'
        : isMine
          ? 'border-emerald-300 bg-emerald-500 text-black'
          : 'border-amber-300 bg-amber-400 text-black'
        }`}>
        {formatHoldSeconds(secondsLeft)}
      </span>
    );
  };

  useEffect(() => {
    const hasExpiredVisibleHold = seats.some(seat => seat.holdExpiresAt && getSeatHoldSecondsLeft(seat) === 0);
    if (!hasExpiredVisibleHold) return undefined;

    const timeoutId = window.setTimeout(() => refreshSeatMap(), 500);
    return () => window.clearTimeout(timeoutId);
  }, [seatClockTick, seatMapData]);

  const hasPartialCoupleSelection = () => {
    const selectedSeatIds = new Set(selectedSeats.map(seat => seat.id));
    return selectedSeats.some(seat => {
      if (normalizeSeatTypeKey(seat.type) !== 'couple') return false;
      const pair = getCoupleSeatPair(seat, seats);
      return pair.length !== 2 || pair.some(pairSeat => !selectedSeatIds.has(pairSeat.id));
    });
  };

  // Quay lại bước bắp nước từ màn thanh toán mà KHÔNG hủy hold ghế
  const handleBackToBooking = () => {
    setPaymentState('booking');
    setBookingStep('combos');
    // Nếu có điểm gốc được lưu trong ref (tức là đã hold lần đầu với deduct),
    // restore TUYỆT ĐỐI về giá trị đó — tránh cộng tích luỹ mỗi lần quay lại.
    // Update path (holdStillActive) không deduct nên ref = null → không đổi gì.
    if (loyaltyPointsBeforeHoldRef.current !== null) {
      setLoyaltyPoints(loyaltyPointsBeforeHoldRef.current);
      loyaltyPointsBeforeHoldRef.current = null;
    }
    // Xóa heldPaymentSummary cũ để tổng hóa đơn tính lại thực thời theo bắp nước mới thêm
    setHeldPaymentSummary(null);
    if (concessions.length === 0) fetchPublicFoodCatalog({ force: true });
  };

  // Đổi ghế thì phải hủy hold và chọn lại từ đầu (dùng ở màn payment_failed)
  const handleBackToSeats = async () => {
    setIsHolding(true);
    setPaymentState('booking');
    setBookingStep('seats');
    try {
      await releaseHeldBooking();
    } finally {
      setIsHolding(false);
    }
  };

  const canMovePastSeats = () => {
    if (selectedSeats.length === 0) { showToast('Vui lòng chọn ít nhất một ghế.'); return false; }
    if (!selectedShowtime) { showToast('Vui lòng chọn suất chiếu.'); return false; }
    if (selectedSeats.length !== totalTickets) {
      showToast(`Vui lòng chọn đủ ${totalTickets} ghế theo số lượng vé.`);
      return false;
    }
    if (hasPartialCoupleSelection()) {
      showToast('Ghế đôi phải chọn đủ cả cặp.');
      return false;
    }
    return true;
  };

  const handleContinueToCombos = () => {
    if (!canMovePastSeats()) return;
    audienceReturnFocusRef.current = document.activeElement;
    setShowAudienceConfirmation(true);
  };

  const handleConfirmAudience = () => {
    setShowAudienceConfirmation(false);
    setBookingStep('combos');
    if (concessions.length === 0) fetchPublicFoodCatalog({ force: true });
  };

  // Proceed: hold seats on BE first
  const handleProceedToPayment = async () => {
    if (hasPartialCoupleSelection()) {
      showToast('Ghế đôi phải chọn đủ cả cặp.');
      return;
    }
    if (selectedSeats.length === 0) { showToast("Vui lòng chọn ít nhất một ghế."); return; }
    if (!selectedShowtime) { showToast("Vui lòng chọn suất chiếu."); return; }
    if (selectedSeats.length !== totalTickets) {
      showToast(`Vui lòng chọn đủ ${totalTickets} ghế theo số lượng vé.`);
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.");
      expireAuthSession();
      return;
    }

    if (loyaltyValidationMessage) {
      showToast(loyaltyValidationMessage);
      return;
    }

    setIsHolding(true);
    try {
      // Hold còn hiệu lực + ghế không đổi → chỉ cập nhật bắp nước/loyalty, giữ nguyên hold
      const holdStillActive = holdBookingId
        && holdExpiresAt
        && new Date(holdExpiresAt).getTime() > Date.now();
      const seatsUnchanged = heldSeatIdsRef.current
        && heldSeatIdsRef.current.length === selectedSeats.length
        && selectedSeats.every((seat) => heldSeatIdsRef.current.includes(seat.seatId));
      if (holdStillActive && seatsUnchanged) {
        const updated = await bookingService.updateHoldingBooking(accessToken, holdBookingId, {
          foods: buildFoodRequests(),
          loyaltyPointsToRedeem
        });
        setHeldPaymentSummary({
          subtotal: Number(updated.subtotal ?? subTotal),
          discountAmount: Number(updated.discountAmount ?? discountAmount),
          totalAmount: Number(updated.totalAmount ?? totalAmount),
          loyaltyPointsRedeemed: Number(updated.loyaltyPointsRedeemed ?? loyaltyPointsToRedeem)
        });
        setHoldExpiresAt(updated.holdExpiresAt || holdExpiresAt);
        setPaymentState('payment_method');
        setIsHolding(false);
        return;
      }

      if (holdBookingId) {
        await releaseHeldBooking();
      }

      const validation = await bookingService.validateTicketPrice(accessToken, {
        showtimeId: selectedShowtime.id,
        holiday: false,
        tickets: buildTicketSelections()
      });
      setTicketPriceValidation(validation);
      if (validation?.eligible === false) {
        const message = validation.tickets?.find(line => line.eligible === false)?.message;
        showToast(message || 'Lựa chọn vé không hợp lệ với suất chiếu này.');
        setIsHolding(false);
        return;
      }

      const holdResult = await bookingService.holdSeats(accessToken, {
        showtimeId: selectedShowtime.id,
        seatIds: selectedSeats.map(s => s.seatId),
        holiday: false,
        tickets: buildTicketSelections(),
        foods: buildFoodRequests(),
        loyaltyPointsToRedeem
      });
      console.log('[holdSeats] result:', holdResult);
      setHoldBookingId(holdResult.id);
      heldSeatIdsRef.current = selectedSeats.map((seat) => seat.seatId);
      setHeldPaymentSummary({
        subtotal: Number(holdResult.subtotal ?? subTotal),
        discountAmount: Number(holdResult.discountAmount ?? discountAmount),
        totalAmount: Number(holdResult.totalAmount ?? totalAmount),
        loyaltyPointsRedeemed: Number(holdResult.loyaltyPointsRedeemed ?? loyaltyPointsToRedeem)
      });
      setHoldExpiresAt(holdResult.holdExpiresAt || holdResult.expiresAt || null);
      setHoldSecondsLeft(holdResult.holdExpiresAt
        ? Math.max(0, Math.ceil((new Date(holdResult.holdExpiresAt).getTime() - Date.now()) / 1000))
        : HOLD_DURATION_SECONDS);
      if (loyaltyPointsToRedeem > 0) {
        // Lưu điểm gốc trước khi trừ để có thể restore tuyệt đối khi quay lại.
        loyaltyPointsBeforeHoldRef.current = loyaltyPoints;
        setLoyaltyPoints(points => Math.max(0, points - loyaltyPointsToRedeem));
      }

      setPaymentState('payment_method');
    } catch (err) {
      showToast(err.message || 'Không thể đặt vé. Vui lòng thử lại.');
      setPaymentState('booking');
    } finally {
      setIsHolding(false);
    }
  };

  const handleReceiptAction = () => {
    if (bookingStep === 'seats') {
      handleContinueToCombos();
      return;
    }
    handleProceedToPayment();
  };

  // Payment via VNPAY
  const handleVnpayPayment = async () => {
    if (!isHoldActive()) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      showToast('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');
      expireAuthSession();
      return;
    }
    setPaymentState('payment_processing');
    try {
      // Kiểm tra nguồn dữ liệu backend ngay trước khi mở VNPay để tránh dùng booking stale.
      const latestBooking = await bookingService.getMyBooking(accessToken, holdBookingId);
      const isPayable = ['HOLDING', 'PENDING_PAYMENT'].includes(latestBooking?.status);
      const isExpired = latestBooking?.holdExpiresAt
        && new Date(latestBooking.holdExpiresAt).getTime() <= Date.now();
      if (!isPayable || isExpired) {
        throw new Error(isExpired
          ? 'Thời gian giữ ghế đã hết. Vui lòng chọn lại ghế.'
          : 'Đơn này không còn ở trạng thái chờ thanh toán.');
      }

      setHoldExpiresAt(latestBooking.holdExpiresAt || holdExpiresAt);
      const result = await paymentService.createVnpayPayment(accessToken, holdBookingId);
      const paymentUrl = result?.paymentUrl ?? result?.payment_url;
      if (paymentUrl) {
        window.location.href = paymentUrl;
        return;
      }
      throw new Error('Cổng VNPay không trả về đường dẫn thanh toán.');
    } catch (err) {
      const message = err?.message || 'Không thể kết nối tới cổng VNPay.';
      showToast(`${message} Đơn vẫn được giữ nếu đồng hồ chưa hết.`);
      setPaymentState('payment_failed');
    }
  };

  const cancelExistingHoldAndUnlockSeats = async () => {
    if (!matchingHoldingBooking || isCancellingExistingHold) return;
    const confirmed = window.confirm(
      'Hủy đơn đang giữ ghế? Ghế và bắp nước trong đơn hiện tại sẽ được giải phóng để bạn chọn lại.'
    );
    if (!confirmed) return;

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      navigate('/login');
      return;
    }

    setIsCancellingExistingHold(true);
    try {
      await bookingService.cancelBooking(accessToken, matchingHoldingBooking.id);
      setActiveHoldingBookings((bookings) => bookings.filter(
        (booking) => String(booking.id) !== String(matchingHoldingBooking.id)
      ));
      setSelectedSeats([]);
      setSelectedCombos({});
      setBookingStep('seats');
      await refreshSeatMap();
      await refreshLoyaltyBalance();
      showToast('Đã hủy đơn cũ. Bạn có thể chọn ghế mới cho suất chiếu này.');
    } catch (error) {
      showToast(error?.message || 'Không thể hủy đơn đang giữ ghế. Vui lòng thử lại.');
    } finally {
      setIsCancellingExistingHold(false);
    }
  };

  const handleFinalSuccessSubmit = () => {
    onConfirmBooking({ bookingCode: holdBookingId ? `#${holdBookingId}` : '' });
  };

  if (!movie) return <div className="min-h-screen bg-black flex items-center justify-center text-white">Đang tải...</div>;

  // If we are in any simulated payment state (payment_method, processing, success, failed),
  // we render a beautiful, responsive, fully dedicated billing gateway UI
  if (paymentState !== 'booking') {
    return (
      <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8 space-y-8 pb-24">

        {/* Gateway Security Header banner */}
        <div className="flex items-center justify-between border-b border-white/10 pb-5">
          <div className="flex items-center space-x-3">
            <div className="h-2.5 w-2.5 rounded-full bg-emerald-500 animate-pulse"></div>
            <div>
              <h1 className="text-lg font-mono text-zinc-300 uppercase tracking-widest font-black flex items-center gap-2">
                CỔNG THANH TOÁN AN TOÀN • CINEPREMIER GATEWAY
              </h1>
              <p className="text-[9px] text-zinc-500 uppercase tracking-wider">Hệ thống mã hoá bảo mật SSL 256-bit được chứng thực</p>
            </div>
          </div>

          {paymentState === 'payment_method' && (
            <button
              onClick={handleBackToBooking}
              className="text-[10px] text-zinc-400 hover:text-white border border-white/10 hover:border-white px-3 py-1.5 uppercase font-mono tracking-wider transition bg-neutral-950"
              title="Quay lại chọn thêm bắp nước — ghế vẫn được giữ"
            >
              ← Quay lại bắp nước (giữ ghế)
            </button>
          )}
        </div>

        {/* ⏳ SUB-VIEW: PAYMENT ONGOING PROCESSING LOADER */}
        {paymentState === 'payment_processing' && (
          <div className="border border-white/10 bg-black p-12 text-center my-12 space-y-6 max-w-xl mx-auto flex flex-col items-center justify-center">
            <Loader2 className="h-12 w-12 text-amber-500 animate-spin" />
            <div className="space-y-2">
              <h3 className="text-sm font-mono text-white tracking-widest uppercase font-black">Mã hoá & Xử lý Giao dịch...</h3>
              <p className="text-[11px] text-zinc-500 leading-normal max-w-xs mx-auto">Vui lòng không tắt trình duyệt hoặc tải lại trang trong khi hệ thống bảo mật kết nối với dịch vụ thanh toán ngân hàng.</p>
            </div>
            <div className="w-48 bg-neutral-900 h-1.5 overflow-hidden rounded-full border border-white/5 relative">
              <div className="bg-amber-500 h-full w-2/3 rounded-full absolute left-0 top-0 animate-infinite-loading"></div>
            </div>
          </div>
        )}

        {/* ❌ SUB-VIEW: PAYMENT FAILED SCREEN */}
        {paymentState === 'payment_failed' && (
          <div className="border border-red-500/20 bg-neutral-950 p-8 my-8 max-w-xl mx-auto text-center space-y-6" id="payment-failed-layout">
            <div className="flex justify-center">
              <div className="h-16 w-16 rounded-full bg-red-950/40 border border-red-500/40 flex items-center justify-center text-red-500 shadow-[0_0_20px_rgba(239,68,68,0.2)]">
                <XCircle className="h-10 w-10" />
              </div>
            </div>

            <div className="space-y-2">
              <h3 className="text-lg font-sans text-red-500 uppercase tracking-wide font-bold">Thanh Toán Thất Bại</h3>
              <p className="text-[11px] text-zinc-400 max-w-sm mx-auto leading-relaxed uppercase tracking-wider">
                Giao dịch của bạn bị huỷ hoặc không thể kết nối tới tài khoản nguồn ngân hàng liên kết.
              </p>
            </div>

            <div className="bg-black border border-white/5 p-4 rounded-none text-left space-y-2">
              <span className="text-[9px] font-mono font-bold text-red-400 uppercase tracking-widest block mb-1">CÁC LÝ DO PHỔ BIẾN:</span>
              <ul className="text-[10px] text-zinc-500 list-disc pl-4 space-y-1.5 font-sans">
                <li>Tài khoản ví điện tử hoặc số dư thẻ ngân hàng không đủ thanh toán.</li>
                <li>Xác thực mật khẩu ứng dụng OTP quá thời gian quy định (Timeout).</li>
                <li>Quá trình truyền thông bị gián đoạn giữa trình duyệt và Gateway.</li>
                <li>Thao tác từ chối giao dịch chủ động thực hiện bởi chủ tài khoản.</li>
              </ul>
            </div>

            <div className="grid grid-cols-2 gap-4 pt-4">
              <button
                onClick={() => setPaymentState('payment_method')}
                className="w-full bg-white text-black hover:bg-neutral-200 py-3.5 text-xs font-bold uppercase tracking-widest font-sans transition"
              >
                Thử thanh toán lại
              </button>
              <button
                onClick={handleBackToSeats}
                className="w-full border border-white/20 hover:border-white text-white hover:bg-neutral-900 py-3.5 text-xs font-bold uppercase tracking-widest font-sans transition"
              >
                Hủy đơn & chọn ghế khác
              </button>
            </div>
          </div>
        )}

        {/* PAYMENT SUCCESS */}
        {paymentState === 'payment_success' && (
          <div className="max-w-2xl mx-auto space-y-6">

            {/* Header */}
            <div className="text-center space-y-3">
              <div className="flex justify-center">
                <div className="relative">
                  <div className="h-20 w-20 rounded-full bg-emerald-950/40 border-2 border-emerald-500/60 flex items-center justify-center text-emerald-400 shadow-[0_0_40px_rgba(16,185,129,0.4)]">
                    <CheckCircle className="h-10 w-10" />
                  </div>
                  <div className="absolute inset-0 rounded-full bg-emerald-500/10 animate-ping" />
                </div>
              </div>
              <div>
                <p className="text-[10px] font-mono tracking-[0.3em] text-emerald-400 uppercase font-black">Thanh toán thành công</p>
                <h2 className="text-2xl font-sans text-white uppercase tracking-wide font-bold mt-1">Đặt Vé Thành Công!</h2>
                <p className="text-xs text-zinc-500 mt-1">Vé đã được xác nhận và lưu vào tài khoản của bạn</p>
              </div>
            </div>

            {/* Ticket Card */}
            <div className="border border-white/10 bg-black overflow-hidden shadow-2xl">

              {/* Top strip */}
              <div className="h-1.5 bg-gradient-to-r from-emerald-500 via-amber-400 to-emerald-500" />

              {/* Movie header */}
              <div className="flex gap-5 p-6 border-b border-white/10">
                <img
                  src={movie.posterUrl}
                  alt={movie.title}
                  className="h-24 w-16 object-cover border border-white/10 shrink-0"
                  referrerPolicy="no-referrer"
                />
                <div className="space-y-2 min-w-0">
                  <span className="text-[8px] tracking-widest bg-red-950 text-red-400 px-2 py-0.5 border border-red-500/30 font-bold inline-block">{movie.ageRating}</span>
                  <h3 className="text-base font-sans text-white uppercase font-black leading-tight">{movie.title}</h3>
                  <p className="text-[10px] text-zinc-400 uppercase tracking-widest truncate">{movie.englishTitle}</p>
                </div>
              </div>

              {/* Ticket details */}
              <div className="p-6 grid grid-cols-2 sm:grid-cols-4 gap-5 border-b border-dashed border-white/10">
                <div>
                  <p className="text-[8px] text-zinc-600 uppercase tracking-widest font-sans mb-1">Phòng chiếu</p>
                  <p className="text-white font-bold font-mono text-sm">{selectedShowtime?.roomName || '—'}</p>
                </div>
                <div>
                  <p className="text-[8px] text-zinc-600 uppercase tracking-widest font-sans mb-1">Ngày chiếu</p>
                  <p className="text-white font-bold font-mono text-sm">
                    {selectedShowtime ? new Date(selectedShowtime.startTime).toLocaleDateString('vi-VN') : '—'}
                  </p>
                </div>
                <div>
                  <p className="text-white font-bold font-mono text-sm">
                    {selectedShowtime
                      ? `${new Date(selectedShowtime.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}${selectedShowtimeEnd ? ` - ${selectedShowtimeEnd.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}` : ''}`
                      : '—'}
                  </p>
                </div>
                <div>
                  <p className="text-[8px] text-zinc-600 uppercase tracking-widest font-sans mb-1">Ghế</p>
                  <p className="text-amber-400 font-bold font-mono text-sm">{selectedSeats.map(s => s.id).join(', ')}</p>
                </div>
              </div>

              {/* Seat breakdown */}
              <div className="px-6 py-4 space-y-2 border-b border-white/5">
                {selectedSeats.filter(s => s.ticketType === 'adult').length > 0 && (
                  <div className="flex justify-between text-[11px] font-mono text-zinc-400">
                    <span>Người lớn ×{selectedSeats.filter(s => s.ticketType === 'adult').length}</span>
                    <span className="text-white">{formatVnd(getSelectedSeatTotal('adult'))}</span>
                  </div>
                )}
                {selectedSeats.filter(s => s.ticketType === 'child').length > 0 && (
                  <div className="flex justify-between text-[11px] font-mono text-amber-400">
                    <span>Trẻ em ×{selectedSeats.filter(s => s.ticketType === 'child').length}</span>
                    <span>{formatVnd(getSelectedSeatTotal('child'))}</span>
                  </div>
                )}
                {selectedSeats.filter(s => s.ticketType === 'student').length > 0 && (
                  <div className="flex justify-between text-[11px] font-mono text-sky-400">
                    <span>Sinh viên ×{selectedSeats.filter(s => s.ticketType === 'student').length}</span>
                    <span>{formatVnd(getSelectedSeatTotal('student'))}</span>
                  </div>
                )}
                {Object.entries(selectedCombos).map(([id, q]) => {
                  const it = concessions.find(i => i.id === id);
                  if (!it) return null;
                  return (
                    <div key={id} className="flex justify-between text-[11px] font-mono text-zinc-500">
                      <span>{it.name} ×{q}</span>
                      <span>{(it.price * q).toLocaleString()}đ</span>
                    </div>
                  );
                })}
                {gatewayDiscountAmount > 0 && (
                  <div className="flex justify-between text-[11px] font-mono text-emerald-400">
                    <span>Giảm CinePoints</span>
                    <span>-{gatewayDiscountAmount.toLocaleString()}đ</span>
                  </div>
                )}
              </div>

              {/* Total + QR */}
              <div className="flex items-center justify-between p-6">
                <div>
                  <p className="text-[9px] text-zinc-600 uppercase tracking-widest mb-1">Tổng thanh toán</p>
                  <p className="text-2xl font-black text-emerald-400 font-mono">{gatewayTotalAmount.toLocaleString()}đ</p>
                  {paidBooking?.bookingCode && (
                    <p className="text-[10px] text-zinc-500 font-mono mt-1">Mã đơn: {paidBooking.bookingCode}</p>
                  )}
                </div>
                {paidBooking?.qrCode ? (
                  <div className="bg-white p-2 border border-white/10">
                    <QRCodeSVG value={paidBooking.qrCode} size={72} level="M" />
                  </div>
                ) : (
                  <div className="bg-white/5 border border-white/10 w-[88px] h-[88px] flex items-center justify-center">
                    <Loader2 className="h-5 w-5 text-zinc-500 animate-spin" />
                  </div>
                )}
              </div>

              {/* Danh sách mã vé từng ghế nằm TRONG mã QR chung ở trên */}
              {Array.isArray(paidBooking?.seats) && paidBooking.seats.some((s) => s.ticketCode) && (
                <div className="px-6 pb-6 border-t border-dashed border-white/10 pt-4">
                  <p className="text-[9px] text-zinc-600 uppercase tracking-widest mb-3">
                    {paidBooking.seats.length} vé trong mã QR — quét một mã là đủ
                  </p>
                  <div className="space-y-1.5">
                    {paidBooking.seats.map((seat) => (
                      <div key={seat.ticketCode || seat.seatId} className="flex items-center gap-3 border border-white/10 bg-neutral-950 px-3 py-2">
                        <span className="w-8 shrink-0 font-mono text-xs font-black text-white">{seat.rowLabel}{seat.seatNumber}</span>
                        <span className={`shrink-0 text-[8px] font-sans font-black uppercase tracking-widest ${seat.ticketType === 'STUDENT' ? 'text-sky-400' : seat.ticketType === 'CHILD' ? 'text-amber-400' : 'text-zinc-400'
                          }`}>
                          {seat.ticketType === 'STUDENT' ? 'Sinh viên' : seat.ticketType === 'CHILD' ? 'Trẻ em' : 'Người lớn'}
                        </span>
                        <span className="ml-auto truncate font-mono text-[10px] text-zinc-500" title={seat.ticketCode}>
                          {seat.ticketCode}
                        </span>
                      </div>
                    ))}
                  </div>
                  <p className="mt-2 text-[8px] text-zinc-600 leading-relaxed">
                    Nhân viên quét mã QR phía trên sẽ thấy đủ danh sách vé và xác nhận từng ghế khi vào phòng.
                  </p>
                </div>
              )}

            </div>

            {/* Actions */}
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={handleFinalSuccessSubmit}
                className="flex items-center justify-center gap-2 bg-white text-black hover:bg-neutral-200 py-4 text-xs font-black uppercase tracking-widest transition"
              >
                <Check className="h-4 w-4" />
                Lưu vé của tôi
              </button>
              <button
                onClick={onBack}
                className="flex items-center justify-center gap-2 border border-white/20 hover:border-white text-white hover:bg-neutral-900 py-4 text-xs font-bold uppercase tracking-widest transition"
              >
                Về trang chủ
              </button>
            </div>

          </div>
        )}

        {/* 💳 SUB-VIEW: SELECT PAYMENT METHOD VIEW */}
        {paymentState === 'payment_method' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">

            {/* Left Block: Receipt Summary */}
            <div className="lg:col-span-5 border border-white/10 bg-black p-6 space-y-5 shadow-xl">
              <span className="text-[10px] font-sans tracking-[0.2em] font-black text-amber-500 block uppercase">THÔNG TIN HOÁ ĐƠN THANH TOÁN</span>

              <div className="flex items-start gap-3.5 border-b border-white/5 pb-4">
                <img
                  src={movie.posterUrl}
                  alt={movie.title}
                  className="h-16 w-11 object-cover border border-white/15 flex-shrink-0"
                  referrerPolicy="no-referrer"
                />
                <div className="space-y-1">
                  <span className="text-[8px] tracking-widest bg-red-950 text-red-400 px-1 py-0.5 border border-red-500/30 font-bold">{movie.ageRating}</span>
                  <h4 className="text-xs font-sans text-white uppercase font-black tracking-wide leading-tight">{movie.title}</h4>
                  <p className="text-[10px] text-zinc-300 font-bold uppercase tracking-wider truncate">{movie.englishTitle}</p>
                </div>
              </div>

              {/* List booking details */}
              <div className="space-y-3.5 text-[10px] uppercase tracking-wider font-mono text-zinc-400">
                <div className="flex justify-between border-b border-white/5 pb-2">
                  <span className="text-white">Phòng chiếu:</span>
                  <span className="text-white font-bold">{(selectedShowtime?.roomName || 'Chưa chọn')}</span>
                </div>
                <div className="flex justify-between border-b border-white/5 pb-2">
                  <span className="text-white">Ngày chiếu:</span>
                  <span className="text-white font-bold">{selectedDate ? new Date(selectedDate).toLocaleDateString('vi-VN') : ''}</span>
                </div>
                <div className="flex justify-between border-b border-white/5 pb-2">
                  <span className="text-white">Giờ chiếu:</span>
                  <span className="text-white font-bold">
                    {selectedShowtime
                      ? `${new Date(selectedShowtime.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}${selectedShowtimeEnd ? ` - ${selectedShowtimeEnd.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}` : ''}`
                      : ''}
                  </span>
                </div>
                <div className="flex justify-between border-b border-white/5 pb-2">
                  <span className="text-white">Số ghế đã chọn ({selectedSeats.length}):</span>
                  <span className="text-amber-400 font-bold">{selectedSeats.map(s => s.id).join(', ')}</span>
                </div>
                <div className="flex justify-between border-b border-white/5 pb-2">
                  <span className="text-white">Giữ ghế còn lại:</span>
                  <span className={`font-black ${holdSecondsLeft !== null && holdSecondsLeft <= 15 ? 'text-red-400' : 'text-emerald-400'}`}>{holdCountdownLabel}</span>
                </div>

              </div>

              {/* Detailed receipt — mirrors the booking sidebar */}
              <div className="border-t border-white/10 pt-4 font-sans">
                <div className="mb-3 flex items-center justify-between gap-3">
                  <p className="text-[9px] font-black uppercase tracking-[0.2em] text-sky-400">Chi tiết đơn hàng</p>
                  <span className="text-[8px] font-mono text-neutral-400">#{selectedSeats.length} vé</span>
                </div>

                <div className="mb-1 grid grid-cols-[minmax(0,1fr)_auto_auto_auto] gap-x-3 border-b border-white/5 pb-1.5 text-[8px] font-black uppercase tracking-widest text-neutral-400">
                  <span>Hạng mục</span>
                  <span className="text-right">Đơn giá</span>
                  <span className="text-right">SL</span>
                  <span className="text-right">Thành tiền</span>
                </div>

                {receiptTicketRows.length > 0 && (
                  <div className="mb-1 space-y-0.5">
                    <div className="flex items-center gap-1.5 py-1 text-[8px] font-bold uppercase tracking-widest text-neutral-400">
                      <Ticket className="h-3 w-3 text-amber-400" /> Vé xem phim
                    </div>
                    {receiptTicketRows.map(row => {
                      const typeMeta = TICKET_TYPE_META[row.ticketType] || TICKET_TYPE_META.adult;
                      const typeClass = row.ticketType === 'student'
                        ? 'border-sky-500/30 bg-sky-500/10 text-sky-400'
                        : row.ticketType === 'child'
                          ? 'border-amber-500/30 bg-amber-500/10 text-amber-400'
                          : 'border-white/15 bg-white/5 text-neutral-300';
                      return (
                        <div key={row.id} className="grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-center gap-x-3 border border-white/10 bg-white/[0.025] px-2 py-2">
                          <div className="min-w-0">
                            <span className="block truncate text-[10px] font-bold text-white">{row.label}</span>
                            <span className={`mt-0.5 inline-flex border px-1 py-0.5 text-[7px] font-black uppercase ${typeClass}`}>{typeMeta.label}</span>
                          </div>
                          <span className="text-right font-mono text-[9px] tabular-nums text-neutral-400">{formatVnd(row.unitPrice)}</span>
                          <span className="text-right font-mono text-[9px] text-neutral-400">×1</span>
                          <span className="text-right font-mono text-[10px] font-black tabular-nums text-white">{formatVnd(row.lineTotal)}</span>
                        </div>
                      );
                    })}
                    <div className="grid grid-cols-[minmax(0,1fr)_auto] gap-3 px-2 py-1.5 text-[8px] font-bold uppercase tracking-wider text-neutral-400">
                      <span>Tổng vé</span>
                      <span className="text-right font-mono text-[10px] font-black tabular-nums text-neutral-300">{formatVnd(priceTickets)}</span>
                    </div>
                  </div>
                )}

                {receiptFoodRows.length > 0 && (
                  <div className="mb-1 space-y-0.5">
                    <div className="flex items-center gap-1.5 py-1 text-[8px] font-bold uppercase tracking-widest text-neutral-400">
                      <ShoppingBag className="h-3 w-3 text-rose-400" /> Bắp nước
                    </div>
                    {receiptFoodRows.map(row => (
                      <div key={row.id} className="grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-center gap-x-3 border border-amber-900/20 bg-amber-950/5 px-2 py-2">
                        <span className="min-w-0 truncate text-[10px] font-bold text-white" title={row.name}>{row.name}</span>
                        <span className="text-right font-mono text-[9px] tabular-nums text-neutral-400">{formatVnd(row.unitPrice)}</span>
                        <span className="text-right font-mono text-[9px] text-neutral-400">×{row.quantity}</span>
                        <span className="text-right font-mono text-[10px] font-black tabular-nums text-amber-300">{formatVnd(row.lineTotal)}</span>
                      </div>
                    ))}
                    <div className="grid grid-cols-[minmax(0,1fr)_auto] gap-3 px-2 py-1.5 text-[8px] font-bold uppercase tracking-wider text-neutral-400">
                      <span>Tổng bắp nước</span>
                      <span className="text-right font-mono text-[10px] font-black tabular-nums text-amber-300">{formatVnd(priceCombos)}</span>
                    </div>
                  </div>
                )}

                {gatewayDiscountAmount > 0 && (
                  <div className="flex items-center justify-between gap-3 border-t border-emerald-500/20 px-2 py-2 text-[9px] font-bold uppercase tracking-wider text-emerald-400">
                    <span>Giảm bằng CinePoints</span>
                    <span className="font-mono font-black tabular-nums">-{formatVnd(gatewayDiscountAmount)}</span>
                  </div>
                )}

                <div className="mt-2 flex items-end justify-between gap-4 border-y border-white/10 py-3">
                  <div>
                    <span className="block text-[10px] font-bold uppercase tracking-widest text-white">Tổng cộng hóa đơn</span>
                    <span className="mt-0.5 block text-[8px] text-neutral-400">{selectedSeats.length} vé · {receiptFoodRows.reduce((sum, row) => sum + row.quantity, 0)} món</span>
                  </div>
                  <span className="shrink-0 border border-white/15 bg-[#0e0e0e] p-2 font-mono text-xl font-black leading-none tracking-tight text-white">{formatVnd(gatewayTotalAmount)}</span>
                </div>
              </div>

            </div>

            {/* Right Block: VNPAY Payment */}
            <div className="lg:col-span-7 border border-white/10 bg-neutral-950 p-6 space-y-6 flex flex-col justify-between">

              <div className="space-y-4">
                <div>
                  <h3 className="text-sm font-mono text-white tracking-widest uppercase font-black">Thanh toán qua VNPAY</h3>
                  <p className="text-[11px] text-zinc-500 font-sans mt-1">Hệ thống sẽ chuyển bạn sang cổng thanh toán VNPAY để hoàn tất giao dịch an toàn.</p>
                </div>

                {/* VNPAY info */}
                <div className="border border-emerald-500/20 bg-emerald-950/10 p-5 space-y-3">
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 bg-emerald-950/30 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                      <CheckCircle className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-white uppercase tracking-wider">Thanh toán bảo mật</p>
                      <p className="text-[10px] text-zinc-500">Hỗ trợ thanh toán nhanh chóng và an toàn</p>
                    </div>
                  </div>
                  <div className="border-t border-white/5 pt-3 grid grid-cols-2 gap-2 text-[10px] font-mono text-zinc-400">
                    <div><span className="text-zinc-600 block text-[12px]">SỐ TIỀN:</span><span className="text-white font-bold text-sm">{gatewayTotalAmount.toLocaleString()}đ</span></div>
                    <div><span className="text-zinc-600 block text-[12px]">GHẾ:</span><span className="text-amber-400 font-bold">{selectedSeats.map(s => s.id).join(', ')}</span></div>
                  </div>
                  <div className="border-t border-white/5 pt-3">
                    <div className="flex items-center justify-between text-[10px] font-mono uppercase tracking-wider">
                      <span className="text-zinc-500">Thời gian giữ ghế</span>
                      <span className={`font-black ${holdSecondsLeft !== null && holdSecondsLeft <= 15 ? 'text-red-400' : 'text-emerald-400'}`}>{holdCountdownLabel}</span>
                    </div>
                    <div className="mt-2 h-1.5 overflow-hidden bg-neutral-900">
                      <div
                        className={`h-full transition-all duration-500 ${holdSecondsLeft !== null && holdSecondsLeft <= 15 ? 'bg-red-500' : 'bg-emerald-500'}`}
                        style={{ width: `${holdProgressPercent}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                <button
                  onClick={handleVnpayPayment}
                  className="w-full flex items-center justify-center gap-2 bg-emerald-600 hover:bg-emerald-500 text-white font-sans font-black uppercase tracking-widest text-sm py-5 transition shadow-[0_0_20px_rgba(16,185,129,0.2)]"
                >
                  <CheckCircle className="h-5 w-5" />
                  <span>Xác nhận & Thanh toán qua VNPAY</span>
                </button>
                <p className="text-[9px] text-zinc-600 text-center font-mono uppercase tracking-wider">
                  Bạn sẽ được chuyển sang cổng VNPAY · Ghế được giữ trong 3 phút
                </p>
              </div>

            </div>

          </div>
        )}

      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8 space-y-10 pb-24">

      {/* Upper header controls */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/10 pb-6">
        <div className="flex items-center space-x-3 mt-1">
          <button
            onClick={onBack}
            className="border border-white/20 hover:border-white bg-black hover:bg-neutral-900 text-white p-2.5 transition"
            id="booking-back-button"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <h1 className="text-xl font-sans text-white uppercase tracking-wide font-black">Đặt Vé Trực Tuyến</h1>
            <p className="text-[10px] text-neutral-500 uppercase tracking-widest mt-1">TÁC PHẨM: {movie.title} • {movie.ageRating}</p>
          </div>
        </div>

        {/* Process progress indicators */}
        <div className="flex items-center space-x-2 text-[10px] font-sans uppercase tracking-[0.2em]">
          <button
            onClick={() => setBookingStep('seats')}
            className={`px-3 py-1.5 transition ${bookingStep === 'seats' ? 'text-white border-b border-white font-bold' : 'text-neutral-500 hover:text-white'}`}
          >
            1. CHỖ NGỒI
          </button>
          <ChevronRight className="h-3 w-3 text-neutral-700" />
          <button
            disabled={selectedSeats.length === 0}
            onClick={() => {
              if (bookingStep === 'seats') handleContinueToCombos();
            }}
            className={`px-3 py-1.5 transition ${bookingStep === 'combos' ? 'text-white border-b border-white font-bold' : 'text-neutral-500 hover:text-white disabled:opacity-30'}`}
          >
            2. BẮP NƯỚC
          </button>
        </div>
      </div>

      {/* Primary columns split */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">

        {/* Left Block: Seat Map or F&B selection */}
        <div className="lg:col-span-8 space-y-8">

          {/* Schedule options layout */}
          <div className="border border-white/10 bg-black p-5 space-y-5">
            <span className="text-[9px] font-sans tracking-[0.2em] font-bold text-neutral-400 block uppercase">CHỌN NGÀY & GIỜ CHIẾU</span>

            {isLoadingShowtimes ? (
              <div className="flex items-center gap-2 text-neutral-500 text-xs py-2">
                <Loader2 className="h-3.5 w-3.5 animate-spin" /> Đang tải lịch chiếu...
              </div>
            ) : showtimesList.length === 0 ? (
              <p className="text-xs text-neutral-500 py-2">Không có suất chiếu nào cho phim này.</p>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 pt-1">

                {/* Dates Row */}
                <div className="space-y-3">
                  <span className="text-[10px] uppercase tracking-widest text-neutral-500 font-sans">Chọn ngày:</span>
                  <div className="flex flex-wrap gap-2">
                    {dateOptions.map(date => (
                      <button
                        key={date}
                        onClick={() => handleSelectDate(date)}
                        className={`px-3 py-2 text-[10px] font-sans font-bold tracking-wider uppercase border transition-all ${selectedDate === date ? 'bg-white text-black border-white' : 'bg-black text-neutral-400 border-white/10 hover:text-white hover:border-white/30'
                          }`}
                      >
                        {new Date(date).toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' })}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Rooms Row */}
                <div className="space-y-3">
                  <span className="text-[10px] uppercase tracking-widest text-neutral-500 font-sans">Chọn phòng:</span>
                  <div className="flex flex-wrap gap-2">
                    {roomOptionsForDate.map(room => (
                      <button
                        key={room.key}
                        onClick={() => handleSelectRoom(room.key)}
                        className={`px-3 py-2 text-[10px] font-sans font-bold tracking-wider uppercase border transition-all ${selectedRoomKey === room.key ? 'bg-white text-black border-white' : 'bg-black text-neutral-400 border-white/10 hover:text-white hover:border-white/30'
                          }`}
                      >
                        <span className="block">{room.name}</span>
                        {room.roomType && <span className="block mt-0.5 text-[8px] opacity-60">{room.roomType}</span>}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Showtimes Row */}
                <div className="space-y-3">
                  <span className="text-[10px] uppercase tracking-widest text-neutral-500 font-sans">Chọn giờ:</span>
                  <div className="flex flex-wrap gap-2">
                    {showtimesForSelectedRoom.map(st => (
                      <button
                        key={st.id}
                        onClick={() => handleSelectShowtime(st)}
                        className={`px-3 py-2 text-[10px] font-mono font-bold border transition ${selectedShowtime?.id === st.id ? 'bg-white text-black border-white' : 'bg-black text-neutral-400 border-white/10 hover:text-white hover:border-white/30'
                          }`}
                      >
                        {new Date(st.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                      </button>
                    ))}
                  </div>
                </div>

              </div>
            )}
          </div>

          {/* Render Step 1: SEATS SCREEN */}
          {bookingStep === 'seats' ? (
            <div className="border border-white/10 bg-black p-4 sm:p-6 space-y-8 overflow-hidden relative">

              {/* Loading seat map */}
              {isLoadingSeatMap && (
                <div className="flex items-center justify-center gap-2 py-16 text-neutral-500 text-xs">
                  <Loader2 className="h-5 w-5 animate-spin text-amber-500" /> Đang tải sơ đồ ghế...
                </div>
              )}
              {!isLoadingSeatMap && !selectedShowtime && (
                <p className="text-center text-neutral-600 text-xs py-16">Chọn suất chiếu để xem sơ đồ ghế.</p>
              )}
              {!isLoadingSeatMap && selectedShowtime && seats.length === 0 && (
                <p className="text-center text-neutral-600 text-xs py-16">Không có dữ liệu ghế cho suất chiếu này.</p>
              )}

              {/* Ticket type selector */}
              {!isLoadingSeatMap && seats.length > 0 && (
                <div className="border border-white/10 bg-neutral-950 p-4 space-y-3">
                  <span className="text-[9px] font-sans tracking-[0.2em] font-bold text-neutral-400 block uppercase">Chọn số lượng vé</span>
                  <div className="flex flex-wrap gap-6">
                    {/* Adult */}
                    <div className="flex items-center gap-3">
                      <div>
                        <p className="text-[10px] font-bold text-white uppercase tracking-wider">Người lớn</p>
                        <p className="text-[9px] text-neutral-500 font-mono">Từ {getTicketStartingPrice('adult') !== null ? formatVnd(getTicketStartingPrice('adult')) : '—'}/vé</p>
                      </div>
                      <div className="flex items-center gap-2 border border-white/10 px-2 py-1 bg-black">
                        <button onClick={() => changeTicketCount('adult', -1)} className="text-neutral-400 hover:text-white w-5 text-center font-bold">−</button>
                        <span className="text-white font-mono font-bold w-5 text-center text-sm">{adultCount}</span>
                        <button onClick={() => changeTicketCount('adult', 1)} className="text-neutral-400 hover:text-white w-5 text-center font-bold">+</button>
                      </div>
                    </div>
                    {/* Child */}
                    {childTicketsAllowed && (
                      <div className="flex items-center gap-3">
                        <div>
                          <p className="text-[10px] font-bold text-amber-400 uppercase tracking-wider">Trẻ em</p>
                          <p className="text-[9px] text-neutral-500 font-mono">
                            {childTicketsAllowed
                              ? `Từ ${getTicketStartingPrice('child') !== null ? formatVnd(getTicketStartingPrice('child')) : '—'}/vé`
                              : `Không bán cho phim ${movie?.ageRating || '16+'}`}
                          </p>
                        </div>
                        <div className="flex items-center gap-2 border border-amber-500/20 px-2 py-1 bg-black">
                          <button disabled={!childTicketsAllowed} onClick={() => changeTicketCount('child', -1)} className="text-neutral-400 hover:text-white w-5 text-center font-bold disabled:cursor-not-allowed disabled:hover:text-neutral-400">−</button>
                          <span className="text-amber-400 font-mono font-bold w-5 text-center text-sm">{childCount}</span>
                          <button disabled={!childTicketsAllowed} onClick={() => changeTicketCount('child', 1)} className="text-neutral-400 hover:text-amber-400 w-5 text-center font-bold disabled:cursor-not-allowed disabled:hover:text-neutral-400">+</button>
                        </div>
                      </div>
                    )}

                    {/* Student */}
                    <div className="flex items-center gap-3">
                      <div>
                        <p className="text-[10px] font-bold text-sky-400 uppercase tracking-wider">Sinh viên</p>
                        <p className="text-[9px] text-neutral-500 font-mono">Từ {getTicketStartingPrice('student') !== null ? formatVnd(getTicketStartingPrice('student')) : '—'}/vé</p>
                      </div>
                      <div className="flex items-center gap-2 border border-sky-500/20 px-2 py-1 bg-black">
                        <button onClick={() => changeTicketCount('student', -1)} className="text-neutral-400 hover:text-white w-5 text-center font-bold">−</button>
                        <span className="text-sky-400 font-mono font-bold w-5 text-center text-sm">{studentCount}</span>
                        <button onClick={() => changeTicketCount('student', 1)} className="text-neutral-400 hover:text-sky-400 w-5 text-center font-bold">+</button>
                      </div>
                    </div>

                    {/* Couple — chỉ hiển thị nếu có ghế đôi trong phòng */}

                    {/* Total indicator */}
                    <div className="flex items-center">
                      <div className="border border-white/10 bg-black px-3 py-1.5 text-center">
                        <p className="text-[10px] text-neutral-450 uppercase tracking-widest">Tổng ghế cần chọn</p>
                        <p className="text-white font-mono font-black text-base">{totalTickets}</p>
                      </div>
                    </div>
                  </div>
                  {selectedSeats.length > 0 && (
                    <div className="flex flex-wrap gap-2 pt-1 border-t border-white/5">
                      {selectedSeats.map(s => (
                        <span key={s.id} className={`text-[10px] font-mono font-bold px-2 py-0.5 border ${s.ticketType === 'adult' ? 'border-white/30 text-white' :
                          s.ticketType === 'student' ? 'border-sky-500/40 text-sky-400' :
                            'border-amber-500/40 text-amber-400'
                          }`}>
                          {s.id} · {TICKET_TYPE_META[s.ticketType]?.short || 'NL'} · {formatVnd(getSeatLineTotal(s))}
                        </span>
                      ))}
                    </div>
                  )}
                  {ticketPriceValidation?.eligible === false && (
                    <p className="text-[10px] font-bold uppercase tracking-wider text-rose-400">
                      {firstTicketError || 'Lựa chọn vé không hợp lệ với suất chiếu này.'}
                    </p>
                  )}
                </div>
              )}

              {/* Seat map only shows when data available */}
              {!isLoadingSeatMap && seats.length > 0 && <>

                {/* BACKLIGHT PROJECTION REFLECTION SHINING ONTO THE SEATS */}
                {/* Cinematic projector light cone streaming down directly towards the seat rows */}
                <div className="absolute top-[80px] left-1/2 -translate-x-1/2 w-[110%] h-[100%] bg-[conic-gradient(from_150deg_at_50%_0%,transparent_0deg,rgba(255,255,255,0.15)_20deg,rgba(255,255,255,0.2)_30deg,rgba(255,255,255,0.15)_40deg,transparent_60deg)] opacity-60 pointer-events-none z-0 mix-blend-screen blur-xl animate-pulse" style={{ animationDuration: '6s' }}></div>
                {/* Radial ambient glow focused directly on the rows of seat map */}
                <div className="absolute top-[100px] left-1/2 -translate-x-1/2 w-[90%] h-[80%] bg-[radial-gradient(ellipse_at_top,rgba(255,255,255,0.12)_0%,rgba(255,255,255,0.03)_50%,transparent_90%)] pointer-events-none z-0"></div>

                {/* Curved Glowing screen representer */}
                <div className="relative text-center mx-auto max-w-xl mb-6 pt-4 z-10" id="cinema-screen">
                  {/* Simulated curved ambient cinema screen with realistic glow directed downwards toward seats */}
                  <div className="relative h-5 w-full rounded-[50%] border-t-[4px] border-white/95 shadow-[0_18px_35px_rgba(255,255,255,0.85),0_3px_12px_rgba(255,255,255,0.45)] bg-gradient-to-b from-white/20 to-transparent"></div>
                  {/* Cinematic light shine beam projection simulation shining downwards */}
                  <div className="absolute top-5 left-1/2 -translate-x-1/2 w-[92%] h-32 bg-gradient-to-b from-white/25 via-white/5 to-transparent blur-xl pointer-events-none rounded-b-xl opacity-95"></div>
                  <span className="text-[10.5px] font-sans font-black uppercase tracking-[0.45em] block mt-5 text-zinc-300 animate-pulse">
                    MÀN HÌNH CHÍNH • CINEPREMIER IMAX VIP VIEW
                  </span>
                </div>

                {/* Responsive Scroll and Navigation Helper */}
                <div className="flex items-center justify-center gap-2 text-[10.5px] bg-[#12121c] border border-violet-500/30 text-violet-300 font-extrabold tracking-wider px-4 py-2.5 rounded max-w-md mx-auto relative z-10 animate-pulse">
                  <span className="text-sm">⇆</span>
                  <span>VUỐT TRÁI / PHẢI HOẶC DÙNG MŨI TÊN ĐỂ DI CHUYỂN PHÒNG GHẾ</span>
                </div>

                {/* Seats Matrix Layout with Scroll Buttons Container */}
                <div className="relative z-10 group/seats select-none">

                  {/* Floating Left Navigation Button */}
                  <button
                    type="button"
                    onClick={() => scrollSeats('left')}
                    className="absolute -left-2 top-1/2 -translate-y-1/2 z-30 bg-black/90 hover:bg-white text-white hover:text-black border border-white/20 rounded-full p-2.5 shadow-[0_4px_12px_rgba(0,0,0,0.8)] transition-all cursor-pointer opacity-80 hover:opacity-100 flex items-center justify-center scale-90 sm:scale-100 active:scale-95"
                    title="Di chuyển sang trái"
                  >
                    <ChevronLeft className="h-5 w-5" />
                  </button>

                  {/* Main Overflow Scroll Container */}
                  <div
                    ref={seatScrollRef}
                    className="overflow-x-auto pb-4 max-w-full scrollbar-thin scroll-smooth"
                    style={{ WebkitOverflowScrolling: 'touch' }}
                  >
                    <div className="w-max mx-auto flex flex-col items-start space-y-3.5 px-10 md:px-14 pb-2" id="seats-map-grid">

                      {/* Pre-compute max column so all rows share the same width */}
                      {(() => { const _maxCol = seats.reduce((m, s) => Math.max(m, s.displayColumn ?? s.col ?? 0), 0); return null; })()}

                      {/* Rows Mapping — dynamic from BE data */}
                      {[...new Map(seats.map(s => [s.row, s])).values()]
                        .sort((a, b) => (a.displayOrder - b.displayOrder) || String(a.row).localeCompare(String(b.row)))
                        .map((rowSeed) => {
                          const rowLetter = rowSeed.row;
                          const rowSeats = seats
                            .filter(s => s.row === rowLetter)
                            .sort((a, b) => (a.displayColumn - b.displayColumn) || (a.col - b.col));
                          const isCoupleRow = rowSeats.every(s => s.type === 'couple');
                          const isVipRow = !isCoupleRow && rowSeats.every(s => s.type === 'vip');
                          const rowTypeLabel = isCoupleRow ? 'ĐÔI' : isVipRow ? 'VIP' : null;
                          const rowTypeBadgeClass = isCoupleRow
                            ? 'text-rose-400 border-rose-500/30 bg-rose-950/20'
                            : 'text-yellow-400 border-yellow-500/30 bg-yellow-950/20';

                          return (
                            <div key={rowLetter} className="flex items-center space-x-5">

                              {/* Left: row letter + type label */}
                              <div className="flex flex-col items-center gap-0.5 shrink-0 w-10">
                                <span className="w-8 h-8 sm:w-10 sm:h-10 flex items-center justify-center rounded-sm bg-[#121212] border border-white/10 text-xs sm:text-sm font-black font-mono text-zinc-300">{rowLetter}</span>
                                <span className={`text-[7px] font-black uppercase tracking-wider border px-1 w-full text-center ${isCoupleRow ? 'text-rose-400 border-rose-500/30 bg-rose-950/20'
                                  : isVipRow ? 'text-yellow-400 border-yellow-500/30 bg-yellow-950/20'
                                    : 'text-neutral-600 border-neutral-800 bg-transparent'
                                  }`}>
                                  {isCoupleRow ? 'ĐÔI' : isVipRow ? 'VIP' : 'STD'}
                                </span>
                              </div>

                              {isCoupleRow ? (
                                // Couple row — same inter-seat gap as STD rows; paired seats share a rose border
                                (() => {
                                  const firstSeat1Col = rowSeats.length > 0 ? (rowSeats[0].displayColumn ?? rowSeats[0].col ?? 1) : 1;
                                  // Leading placeholder slots = columns before the first seat of the first pair
                                  const leadingPlaceholders = firstSeat1Col - 1;
                                  return (
                                    <div className="flex items-center gap-2 sm:gap-2.5">
                                      {/* Leading placeholder cells to align pairs under correct columns */}
                                      {Array.from({ length: leadingPlaceholders }, (_, i) => (
                                        <div key={`lead-${i}`} className="h-9 w-9 sm:h-10 sm:w-10 shrink-0" />
                                      ))}
                                      {Array.from({ length: Math.ceil(rowSeats.length / 2) }).map((_, pairIdx) => {
                                        const seat1 = rowSeats[pairIdx * 2];
                                        const seat2 = rowSeats[pairIdx * 2 + 1];
                                        if (!seat1 || !seat2) return null;

                                        const s1Selected = !!selectedSeats.find(s => s.id === seat1.id);
                                        const s2Selected = !!selectedSeats.find(s => s.id === seat2.id);
                                        const isAnySelected = s1Selected || s2Selected;
                                        const isPairBooked = seat1.isBooked || seat2.isBooked;
                                        const isPairHeld = getSeatHoldSecondsLeft(seat1) > 0 || getSeatHoldSecondsLeft(seat2) > 0;

                                        const seatBtn = (seat, isSelected) => (
                                          <button
                                            key={seat.id}
                                            disabled={isPairBooked || Boolean(matchingHoldingBooking)}
                                            onClick={() => handleSelectSeat(seat)}
                                            title={matchingHoldingBooking ? 'Hủy đơn đang giữ ghế trước khi chọn ghế khác' : `Ghế đôi ${seat.col}`}
                                            className={`relative h-9 w-9 sm:h-10 sm:w-10 shrink-0 text-[11.5px] sm:text-[14.5px] font-black font-mono flex items-center justify-center transition-all duration-150 rounded-none border-0 ${isPairBooked
                                              ? isPairHeld
                                                ? 'bg-amber-950/30 text-amber-300 cursor-not-allowed'
                                                : 'bg-neutral-950 text-neutral-800 cursor-not-allowed line-through'
                                              : matchingHoldingBooking
                                                ? 'bg-rose-950/10 text-rose-900 cursor-not-allowed opacity-40'
                                                : isSelected
                                                  ? 'bg-rose-500 text-white font-black'
                                                  : 'bg-rose-950/20 text-rose-400 hover:bg-rose-950/50 hover:text-rose-200'
                                              }`}
                                          >
                                            {renderSeatHoldCountdown(seat)}
                                            {seat.col}
                                          </button>
                                        );

                                        return (
                                          // Pair wrapper: p-0.5 padding + matching gap-2/gap-2.5 so spacing matches standard columns
                                          <div
                                            key={pairIdx}
                                            className={`flex items-center sm:gap-0.5 p-0.5 border transition-all rounded-sm ${isPairBooked
                                              ? isPairHeld
                                                ? 'border-amber-500/40'
                                                : 'border-neutral-800'
                                              : isAnySelected
                                                ? 'border-rose-500 shadow-[0_0_10px_rgba(244,63,94,0.5)]'
                                                : 'border-rose-500/50 hover:border-rose-400'
                                              }`}
                                          >
                                            {seatBtn(seat1, s1Selected)}
                                            {/* 1px internal divider between paired seats */}
                                            <div className="w-px self-stretch bg-rose-500/30 shrink-0" />
                                            {seatBtn(seat2, s2Selected)}
                                          </div>
                                        );
                                      })}
                                    </div>
                                  );
                                })()
                              ) : (
                                // Standard & VIP row — render slots for ALL columns so seats sit at their real positions
                                (() => {
                                  const maxCol = seats.reduce((m, s) => Math.max(m, s.displayColumn ?? s.col ?? 0), 0);
                                  const seatByCol = Object.fromEntries(rowSeats.map(s => [s.displayColumn ?? s.col, s]));
                                  return (
                                    <div className="flex items-center space-x-2 sm:space-x-2.5">
                                      {Array.from({ length: maxCol }, (_, i) => i + 1).map((colNum) => {
                                        const seat = seatByCol[colNum];
                                        if (!seat) {
                                          // Transparent placeholder to preserve column position
                                          return <div key={`empty-${colNum}`} className="h-9 w-9 sm:h-10 sm:w-10 shrink-0" />;
                                        }
                                        const selectedSeat = selectedSeats.find(s => s.id === seat.id);
                                        const isSelected = !!selectedSeat;
                                        const ticketMeta = selectedSeat ? TICKET_TYPE_META[selectedSeat.ticketType] : null;
                                        const isVip = seat.type === 'vip';
                                        const isChild = selectedSeat?.ticketType === 'child';
                                        const isStudent = selectedSeat?.ticketType === 'student';
                                        const isHeldOnActiveBooking = getSeatHoldSecondsLeft(seat) > 0;
                                        const displayPrice = isSelected ? getSeatLineTotal(selectedSeat) : (getShowtimeTicketPrice(selectedShowtime, 'adult', seat.type) ?? seat.price);

                                        return (
                                          <button
                                            key={seat.id}
                                            disabled={seat.isBooked || Boolean(matchingHoldingBooking)}
                                            onClick={() => handleSelectSeat(seat)}
                                            title={matchingHoldingBooking
                                              ? 'Hủy đơn đang giữ ghế trước khi chọn ghế khác'
                                              : isSelected ? `${ticketMeta?.label || 'Người lớn'} · ${formatVnd(displayPrice)}` : `${formatVnd(displayPrice)}`}
                                            className={`relative h-9 w-9 sm:h-10 sm:w-10 shrink-0 text-[11.5px] sm:text-[14.5px] font-black font-mono transition-all duration-150 rounded-none border ${seat.isBooked
                                              ? isHeldOnActiveBooking
                                                ? 'bg-amber-950/30 border-amber-500/40 text-amber-300 cursor-not-allowed'
                                                : 'bg-neutral-950 border-neutral-900 text-neutral-800 cursor-not-allowed line-through'
                                              : matchingHoldingBooking
                                                ? 'border-white/5 bg-neutral-950 text-neutral-700 cursor-not-allowed opacity-40'
                                                : isSelected && isChild
                                                  ? 'bg-amber-400 text-black border-amber-400 font-black scale-110 shadow-[0_0_12px_rgba(251,191,36,0.6)]'
                                                  : isSelected && isStudent
                                                    ? 'bg-sky-400 text-black border-sky-400 font-black scale-110 shadow-[0_0_12px_rgba(56,189,248,0.6)]'
                                                    : isSelected
                                                      ? 'bg-white text-black border-white font-black scale-110 shadow-[0_0_12px_rgba(255,255,255,0.6)]'
                                                      : isVip
                                                        ? 'border-yellow-500/40 bg-yellow-950/30 text-yellow-400 hover:border-yellow-300 hover:bg-yellow-950/50 hover:scale-105'
                                                        : 'border-white/15 bg-[#121212] text-neutral-200 hover:border-white/50 hover:text-white hover:scale-105'
                                              }`}
                                          >
                                            {renderSeatHoldCountdown(seat)}
                                            {seat.col}
                                          </button>
                                        );
                                      })}
                                    </div>
                                  );
                                })()
                              )}

                              {/* Right Row Label */}
                              <div className="flex flex-col items-center gap-0.5 shrink-0 w-10">
                                <span className="w-8 h-8 sm:w-10 sm:h-10 flex items-center justify-center rounded-sm bg-[#121212] border border-white/10 text-xs sm:text-sm font-black font-mono text-zinc-300">{rowLetter}</span>
                                <span className={`text-[7px] font-black uppercase tracking-wider border px-1 w-full text-center ${isCoupleRow ? 'text-rose-400 border-rose-500/30 bg-rose-950/20'
                                  : isVipRow ? 'text-yellow-400 border-yellow-500/30 bg-yellow-950/20'
                                    : 'text-neutral-600 border-neutral-800 bg-transparent'
                                  }`}>
                                  {isCoupleRow ? 'ĐÔI' : isVipRow ? 'VIP' : 'STD'}
                                </span>
                              </div>

                            </div>
                          );
                        })}

                    </div>
                  </div>

                  {/* Floating Right Navigation Button */}
                  <button
                    type="button"
                    onClick={() => scrollSeats('right')}
                    className="absolute -right-2 top-1/2 -translate-y-1/2 z-30 bg-black/90 hover:bg-white text-white hover:text-black border border-white/20 rounded-full p-2.5 shadow-[0_4px_12px_rgba(0,0,0,0.8)] transition-all cursor-pointer opacity-80 hover:opacity-100 flex items-center justify-center scale-90 sm:scale-100 active:scale-95"
                    title="Di chuyển sang phải"
                  >
                    <ChevronRight className="h-5 w-5" />
                  </button>

                </div>

                {/* Legends — giá lấy theo bảng giá thật của suất chiếu */}
                {(() => {
                  const seatTypes = new Set(seats.map(s => normalizeSeatTypeKey(s.type)));
                  const typeConfig = [
                    { key: 'standard', label: 'STANDARD', color: 'border-white/20', bg: 'bg-black', textClass: 'text-white' },
                    { key: 'vip', label: 'VIP', color: 'border-yellow-500/40', bg: 'bg-yellow-950/20', textClass: 'text-yellow-400' },
                    { key: 'couple', label: 'ĐÔI', color: 'border-rose-500/40', bg: 'bg-rose-950/10', textClass: 'text-rose-400' },
                  ].filter(t => seatTypes.has(t.key));
                  return (
                    <div className="border border-white/5 p-4 space-y-3">
                      <span className="text-[9px] font-sans tracking-[0.2em] font-bold text-neutral-500 block uppercase">Bảng giá vé</span>
                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-[10px] text-neutral-400">
                        {typeConfig.map(({ key, label, color, bg, textClass }) => {
                          const adultPrice = getShowtimeTicketPrice(selectedShowtime, 'adult', key);
                          const childPrice = getShowtimeTicketPrice(selectedShowtime, 'child', key);
                          const studentPrice = getShowtimeTicketPrice(selectedShowtime, 'student', key);
                          return (
                            <div key={key} className={`border ${color} ${bg} p-2.5 space-y-1`}>
                              <p className={`font-black text-[9px] uppercase tracking-widest ${textClass}`}>{label}</p>
                              <p className="text-neutral-300 font-mono font-bold">Người lớn: {adultPrice !== null ? formatVnd(adultPrice) : '—'}</p>
                              {childTicketsAllowed && <p className="text-amber-400 font-mono text-[9px]">Trẻ em: {childPrice !== null ? formatVnd(childPrice) : '—'}</p>}
                              <p className="text-sky-400 font-mono text-[9px]">SV: {studentPrice !== null ? formatVnd(studentPrice) : '—'}</p>
                            </div>
                          );
                        })}
                        <div className="border border-neutral-800 bg-neutral-900 p-2.5 space-y-1">
                          <p className="text-neutral-500 font-black text-[9px] uppercase tracking-widest">ĐÃ ĐẶT</p>
                          <div className="h-4 w-4 bg-neutral-950 border border-neutral-800 text-neutral-800 line-through text-[9px] flex items-center justify-center font-mono">×</div>
                        </div>
                      </div>
                    </div>
                  );
                })()}

                {/* Fully interactive, gorgeous gourmet inline F&B Concessions grid */}
              </> /* end seat map */}

              <div className="border border-white/10 bg-neutral-950 p-6 space-y-6" id="inline-concessions-section">

                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-white/5 pb-4">
                  <div className="space-y-1.5">
                    <h3 className="text-sm font-sans text-white uppercase tracking-wide font-black flex items-center gap-2">
                      <ShoppingBag className="h-4 w-4 text-amber-500 animate-pulse" /> THỰC ĐƠN BẮP NƯỚC CINEPREMIER
                    </h3>
                    <p className="text-[10px] text-zinc-500 leading-normal">
                      Sử dụng bắp bơ thượng hạng và các vị sốt nhập khẩu. Ưu đãi đặt trước trực tuyến tiết kiệm đến 15% so với mua tại quầy.
                    </p>
                  </div>

                  <div className="inline-flex items-center gap-2 border border-amber-500/20 bg-amber-950/10 px-3.5 py-2 text-[10px] text-amber-400 font-mono tracking-wider font-bold">
                    <span>GIÁ TRỊ CONCESSIONS: {priceCombos.toLocaleString()}đ</span>
                  </div>
                </div>

                {/* Grid layout of actual product items */}
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4" id="inline-combos-grid">
                  {paginatedConcessions.map((item) => {
                    const qty = selectedCombos[item.id] || 0;
                    const statusMeta = getFoodStatusMeta(item.status);
                    const maxQuantity = isFoodSelectable(item) ? 3 : 0;

                    return (
                      <div
                        key={item.id}
                        className={`group flex flex-col justify-between border p-3.5 bg-black transition-all duration-300 ${qty > 0
                          ? 'border-amber-400/40 bg-[#0e0a05] shadow-[0_0_15px_rgba(245,158,11,0.06)]'
                          : 'border-white/5 hover:border-white/15'
                          }`}
                      >
                        <div className="space-y-3">

                          {/* Rich graphic display container */}
                          <div className="h-28 w-full overflow-hidden bg-neutral-950 border border-white/5 relative">
                            <img
                              src={item.imageUrl}
                              alt={item.name}
                              className="h-full w-full object-cover group-hover:scale-105 transition duration-500 pointer-events-none"
                              referrerPolicy="no-referrer"
                            />

                            {/* Circular badge indicators */}
                            {qty > 0 && (
                              <div className="absolute top-2 right-2 h-5 w-5 rounded-full bg-amber-400 text-black text-[10px] font-mono font-black flex items-center justify-center animate-bounce">
                                {qty}
                              </div>
                            )}

                            {/* Elegant tag showing concessions type */}
                            <span className="absolute bottom-2 left-2 text-[8px] font-mono tracking-widest uppercase bg-black/8 w-fit text-zinc-400 px-1.5 py-0.5 border border-white/10">
                              {item.category === 'combo' ? 'Trọn gói' : item.category === 'item' ? 'Món lẻ' : item.category === 'popcorn' ? 'Bắp ngô' : item.category === 'snack' ? 'Ăn vặt' : 'Đồ uống'}
                            </span>
                          </div>

                          {/* Concessions labels */}
                          <div className="space-y-1">
                            <span className="text-[7.5px] font-mono text-zinc-600 block tracking-widest uppercase">CINE-CONCESSION</span>
                            <h4 className="text-[11.5px] font-sans font-bold text-white group-hover:text-amber-300 transition-colors leading-tight line-clamp-1">{item.name}</h4>
                            <p className="text-[9px] text-[#8e8e8e] leading-snug line-clamp-2 h-7">{item.description}</p>
                            <p className={`text-[8px] font-mono uppercase tracking-widest ${statusMeta.className}`}>
                              {statusMeta.label}
                            </p>
                          </div>

                        </div>

                        {/* Interactive operations row */}
                        <div className="flex items-center justify-between pt-3 mt-3 border-t border-white/5">
                          <span className="text-[11px] font-mono font-bold text-white group-hover:text-amber-300 transition-colors">
                            {item.price.toLocaleString()}đ
                          </span>

                          <div className="flex items-center space-x-2.5 bg-neutral-950 px-2.5 py-1.5 border border-white/10">
                            <button
                              disabled={qty === 0}
                              onClick={() => handleModifyCombo(item.id, '-')}
                              className="text-zinc-500 hover:text-white disabled:opacity-20 transition-colors"
                            >
                              <Minus className="h-2.5 w-2.5" />
                            </button>

                            <span className="w-3 text-center text-[10.5px] font-mono font-bold text-white">{qty}</span>

                            <button
                              disabled={qty >= maxQuantity}
                              onClick={() => handleModifyCombo(item.id, '+')}
                              className="text-zinc-500 hover:text-amber-400 disabled:opacity-20 transition-colors"
                            >
                              <Plus className="h-2.5 w-2.5" />
                            </button>
                          </div>
                        </div>

                      </div>
                    );
                  })}
                </div>
                <div className="flex flex-col gap-3 border-t border-white/5 pt-4 text-[10px] font-mono font-bold uppercase tracking-widest text-zinc-500 sm:flex-row sm:items-center sm:justify-between">
                  <span>
                    Hiển thị {concessionsDisplayStart}-{concessionsDisplayEnd}/{concessions.length} món - Trang {safeConcessionsPage}/{concessionsTotalPages}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      disabled={safeConcessionsPage <= 1}
                      onClick={() => setConcessionsPage((page) => Math.max(1, page - 1))}
                      className="inline-flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-amber-400 disabled:opacity-30"
                    >
                      <ChevronLeft className="h-3.5 w-3.5" /> Trước
                    </button>
                    <button
                      type="button"
                      disabled={safeConcessionsPage >= concessionsTotalPages}
                      onClick={() => setConcessionsPage((page) => Math.min(concessionsTotalPages, page + 1))}
                      className="inline-flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-amber-400 disabled:opacity-30"
                    >
                      Sau <ChevronRight className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>

              </div>

            </div>
          ) : (

            // Render Step 2: DYNAMIC COMBO ITEMS F&B LIST SCREEN
            <div className="space-y-6">

              <div className="flex items-center justify-between border-b border-white/10 pb-3">
                <div>
                  <h3 className="text-sm font-sans text-white uppercase tracking-wide font-black flex items-center gap-2">
                    <ShoppingBag className="h-4 w-4 text-white" /> CinePremier Concessions
                  </h3>
                  <p className="text-[11px] text-neutral-500 font-sans mt-0.5">Bắp sấy bơ Pháp nóng giòn và thức uống ga mạnh mát lạnh sảng khoái.</p>
                </div>

                <button
                  onClick={() => setBookingStep('seats')}
                  className="border border-white/20 hover:border-white bg-black hover:bg-neutral-900 text-white text-[10px] uppercase tracking-wider px-4 py-2 transition"
                >
                  SƠ ĐỒ GHẾ
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6" id="combos-list">
                {paginatedConcessions.map((item) => {
                  const qty = selectedCombos[item.id] || 0;
                  const statusMeta = getFoodStatusMeta(item.status);
                  const maxQuantity = isFoodSelectable(item) ? 3 : 0;

                  return (
                    <div
                      key={item.id}
                      className="group flex gap-4 border border-white/5 bg-[#0a0a0a] p-4 hover:border-white/10 transition"
                    >
                      <div className="h-20 w-20 overflow-hidden flex-shrink-0 bg-neutral-950 border border-white/5">
                        <img
                          src={item.imageUrl}
                          alt={item.name}
                          className="h-full w-full object-cover group-hover:scale-105 transition duration-500"
                          referrerPolicy="no-referrer"
                        />
                      </div>

                      <div className="flex-1 flex flex-col justify-between">
                        <div className="space-y-1">
                          <h4 className="text-xs font-sans font-bold text-white group-hover:text-zinc-300 transition-colors leading-snug">{item.name}</h4>
                          <p className="text-[10px] text-neutral-500 leading-normal line-clamp-2">{item.description}</p>
                          <p className={`text-[8px] font-mono uppercase tracking-widest ${statusMeta.className}`}>
                            {statusMeta.label}
                          </p>
                        </div>

                        <div className="flex items-center justify-between pt-2">
                          <span className="text-xs text-white font-mono font-bold">
                            {item.price.toLocaleString()}đ
                          </span>

                          {/* Control increase / decrease qty buttons */}
                          <div className="flex items-center space-x-3 bg-black px-2.5 py-1.5 border border-white/10">
                            <button
                              disabled={qty === 0}
                              onClick={() => handleModifyCombo(item.id, '-')}
                              className="text-neutral-500 hover:text-white disabled:opacity-20 transition"
                            >
                              <Minus className="h-3 w-3" />
                            </button>

                            <span className="w-4 text-center text-[11px] font-mono font-bold text-white">{qty}</span>

                            <button
                              disabled={qty >= maxQuantity}
                              onClick={() => handleModifyCombo(item.id, '+')}
                              className="text-neutral-500 hover:text-white disabled:opacity-20 transition"
                            >
                              <Plus className="h-3 w-3" />
                            </button>
                          </div>
                        </div>
                      </div>

                    </div>
                  );
                })}
              </div>
              <div className="flex flex-col gap-3 border-t border-white/5 pt-4 text-[10px] font-mono font-bold uppercase tracking-widest text-zinc-500 sm:flex-row sm:items-center sm:justify-between">
                <span>
                  Hiển thị {concessionsDisplayStart}-{concessionsDisplayEnd}/{concessions.length} món - Trang {safeConcessionsPage}/{concessionsTotalPages}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={safeConcessionsPage <= 1}
                    onClick={() => setConcessionsPage((page) => Math.max(1, page - 1))}
                    className="inline-flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-white disabled:opacity-30"
                  >
                    <ChevronLeft className="h-3.5 w-3.5" /> Trước
                  </button>
                  <button
                    type="button"
                    disabled={safeConcessionsPage >= concessionsTotalPages}
                    onClick={() => setConcessionsPage((page) => Math.min(concessionsTotalPages, page + 1))}
                    className="inline-flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-white disabled:opacity-30"
                  >
                    Sau <ChevronRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>

            </div>
          )}

        </div>

        {/* Right Block: Receipt Sticky Booking summary card */}
        <div className="lg:col-span-4 border border-white/10 bg-black p-6 space-y-6 shadow-2xl relative" id="booking-receipt-sidebar">

          <div className="flex items-start gap-4 border-b border-white/5 pb-4">
            <img
              src={movie.posterUrl}
              alt={movie.title}
              className="h-20 w-14 object-cover flex-shrink-0 border border-white/5"
              referrerPolicy="no-referrer"
            />
            <div className="min-w-0 space-y-1.5 pt-0.5">
              <span className="text-[8px] tracking-[0.1em] border border-red-500/50 bg-red-950/20 px-1.5 py-0.5 text-red-400 font-bold">{movie.ageRating}</span>
              <h3 className="text-sm font-sans font-black text-white truncate uppercase">{movie.title}</h3>
              <p className="text-[9.5px] text-neutral-300 font-bold truncate leading-none uppercase tracking-widest">{movie.englishTitle}</p>
            </div>
          </div>

          {/* Ticket Information specs list */}
          <div className="space-y-3 text-[11px] uppercase tracking-wider font-sans text-neutral-400 border-b border-white/5 pb-4">

            <div className="flex justify-between items-center">
              <span>Phòng chiếu:</span>
              <span className="text-white font-bold truncate max-w-[150px] text-right">{(selectedShowtime?.roomName || 'Chưa chọn') || 'IMAX VIP 03'}</span>
            </div>

            <div className="flex justify-between items-center">
              <span className="flex items-center gap-1.5 text-neutral-300">
                <Calendar className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                Ngày chiếu:
              </span>
              <span className="text-white font-mono font-bold text-right">
                {selectedDate ? selectedDate.split(/[-/]/).reverse().join('/') : '—'}
              </span>
            </div>

            {/* Showtime Range Card with Icons */}
            <div className="border border-white/10 bg-white/[0.03] p-3 space-y-1.5 my-1.5">
              <div className="flex items-center justify-between text-[9px] text-neutral-400 font-mono tracking-wider">
                <span className="flex items-center gap-1 font-bold text-neutral-300">
                  <Clock className="w-3 h-3 text-amber-400 shrink-0" /> BẮT ĐẦU
                </span>
                <span className="flex items-center gap-1 font-bold text-neutral-300">
                  <Clock className="w-3 h-3 text-amber-400 shrink-0" /> KẾT THÚC
                </span>
              </div>
              <div className="flex items-center justify-between font-mono text-sm font-black">
                <span className="text-white">
                  {selectedShowtime ? new Date(selectedShowtime.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '--:--'}
                </span>
                <ArrowRight className="w-3.5 h-3.5 text-neutral-500 shrink-0 mx-1" />
                <span className="text-amber-400">
                  {selectedShowtimeEnd ? selectedShowtimeEnd.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '--:--'}
                </span>
              </div>
            </div>

            <div className="flex justify-between items-start">
              <span>Vị trí ghế:</span>
              <span className="text-white font-mono font-bold max-w-[140px] break-words text-right text-xs">
                {receiptTicketRows.length > 0
                  ? receiptTicketRows.map(row => row.label.replace(/^Ghế\s+/i, '')).join(', ')
                  : 'Chưa chọn'
                }
              </span>
            </div>

            {/* Show selected combos details */}
            {receiptFoodRows.length > 0 && (
              <div className="space-y-2 pt-2 border-t border-white/5 text-[9px] lowercase tracking-wide text-neutral-400">
                <span className="text-[11px] uppercase tracking-[0.15em] text-neutral-600 block font-sans">Dịch vụ đi kèm</span>
                {receiptFoodRows.map(row => (
                  <div key={row.id} className="flex justify-between uppercase">
                    <span className="truncate max-w-[140px] font-sans">{row.name} <span className="text-white font-bold font-mono">x{row.quantity}</span></span>
                    <span className="text-neutral-300 font-mono">{formatVnd(row.lineTotal)}</span>
                  </div>
                ))}
              </div>
            )}

          </div>

          {isViewingExistingHold ? (
            <div className="border border-amber-400/30 bg-amber-400/[0.07] p-4">
              <div className="flex items-start gap-3">
                <span className="mt-0.5 inline-flex h-8 w-8 shrink-0 items-center justify-center border border-amber-400/40 bg-amber-400/10 text-amber-300">
                  <Ticket className="h-4 w-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-[10px] font-black uppercase tracking-[0.16em] text-amber-300">Bạn có đơn đang chờ thanh toán</p>
                  <p className="mt-1 text-[9px] leading-relaxed text-neutral-400">
                    Ghế của suất chiếu này đang được giữ trong đơn {matchingHoldingBooking.bookingCode || `#${matchingHoldingBooking.id}`}.
                  </p>
                  <p className="mt-2 text-[8px] font-bold uppercase tracking-[0.12em] text-neutral-500">
                    Tiếp tục tại nút cuối hóa đơn
                  </p>
                </div>
              </div>
            </div>
          ) : renderLoyaltyRedeemPanel()}

          {/* Checkout receipts final value indicator */}
          <div className="pt-4 border-t border-white/5 font-sans">
            <div className="mb-3 flex items-center justify-between gap-3">
              <p className="text-[9px] font-black uppercase tracking-[0.2em] text-sky-400">Chi tiết đơn hàng</p>
              <span className="text-[8px] font-mono text-neutral-400">#{receiptTicketRows.length} vé</span>
            </div>

            <div className="mb-1 grid grid-cols-[minmax(0,1fr)_auto_auto_auto] gap-x-3 border-b border-white/5 pb-1.5 text-[8px] font-black uppercase tracking-widest text-neutral-400">
              <span>Hạng mục</span>
              <span className="text-right">Đơn giá</span>
              <span className="text-right">SL</span>
              <span className="text-right">Thành tiền</span>
            </div>

            {receiptTicketRows.length > 0 && (
              <div className="mb-1 space-y-0.5">
                <div className="flex items-center gap-1.5 py-1 text-[8px] font-bold uppercase tracking-widest text-neutral-400">
                  <Ticket className="h-3 w-3 text-amber-400" /> Vé xem phim
                </div>
                {receiptTicketRows.map(row => {
                  const typeMeta = TICKET_TYPE_META[row.ticketType] || TICKET_TYPE_META.adult;
                  const typeClass = row.ticketType === 'student'
                    ? 'border-sky-500/30 bg-sky-500/10 text-sky-400'
                    : row.ticketType === 'child'
                      ? 'border-amber-500/30 bg-amber-500/10 text-amber-400'
                      : 'border-neutral-700 bg-neutral-900 text-neutral-400';
                  return (
                    <div key={row.id} className="grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-center gap-x-3 border border-neutral-900 bg-neutral-950 px-2 py-2">
                      <div className="flex min-w-0 flex-wrap items-center gap-1.5">
                        <span className="font-mono text-[11px] font-black text-white">{row.label}</span>
                        <span className={`border px-1 py-0.5 text-[8px] font-bold uppercase ${typeClass}`}>
                          {typeMeta.label}
                        </span>
                      </div>
                      <span className="text-right font-mono text-[9px] tabular-nums text-neutral-400">{formatVnd(row.unitPrice)}</span>
                      <span className="text-right font-mono text-[9px] text-neutral-400">×1</span>
                      <span className="text-right font-mono text-[10px] font-black tabular-nums text-white">{formatVnd(row.lineTotal)}</span>
                    </div>
                  );
                })}
                <div className="grid grid-cols-[1fr_auto] items-center gap-x-3 px-2 py-1.5">
                  <span className="text-[8px] font-bold uppercase tracking-widest text-neutral-400">Tổng vé</span>
                  <span className="text-right font-mono text-[10px] font-black tabular-nums text-neutral-300">{formatVnd(displayTicketPrice)}</span>
                </div>
              </div>
            )}

            {receiptFoodRows.length > 0 && (
              <div className="mb-1 space-y-0.5">
                <div className="flex items-center gap-1.5 py-1 text-[8px] font-bold uppercase tracking-widest text-neutral-400">
                  <ShoppingBag className="h-3 w-3 text-rose-400" /> Bắp nước
                </div>
                {receiptFoodRows.map(row => (
                  <div key={row.id} className="grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-center gap-x-3 border border-amber-900/20 bg-amber-950/5 px-2 py-2">
                    <span className="min-w-0 truncate text-[10px] font-bold text-white" title={row.name}>{row.name}</span>
                    <span className="text-right font-mono text-[9px] tabular-nums text-neutral-400">{formatVnd(row.unitPrice)}</span>
                    <span className="text-right font-mono text-[9px] text-neutral-400">×{row.quantity}</span>
                    <span className="text-right font-mono text-[10px] font-black tabular-nums text-amber-300">{formatVnd(row.lineTotal)}</span>
                  </div>
                ))}
                <div className="grid grid-cols-[1fr_auto] items-center gap-x-3 px-2 py-1.5">
                  <span className="text-[8px] font-bold uppercase tracking-widest text-neutral-400">Tổng bắp nước</span>
                  <span className="text-right font-mono text-[10px] font-black tabular-nums text-amber-300">{formatVnd(priceCombos)}</span>
                </div>
              </div>
            )}

            {gatewayDiscountAmount > 0 && (
              <div className="flex justify-between border-t border-white/5 py-2 text-[9px] font-bold uppercase tracking-wider text-emerald-400">
                <span>Giảm bằng CinePoints</span>
                <span className="font-mono">-{formatVnd(gatewayDiscountAmount)}</span>
              </div>
            )}

            <div className="mt-2 flex items-end justify-between gap-4 border-y border-white/10 py-3">
              <div>
                <span className="block text-[10px] font-bold uppercase tracking-widest text-white">Tổng cộng hóa đơn</span>
                <span className="mt-0.5 block text-[8px] text-neutral-400">{receiptTicketRows.length} vé · {receiptFoodRows.reduce((sum, row) => sum + row.quantity, 0)} món</span>
              </div>
              <span className="shrink-0 font-mono text-xl font-black leading-none tracking-tight text-white">{formatVnd(gatewayTotalAmount)}</span>
            </div>
          </div>

          {/* CTA Proceed triggers */}
          {isViewingExistingHold ? (
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-[0.9fr_1.1fr]">
              <button
                type="button"
                onClick={cancelExistingHoldAndUnlockSeats}
                disabled={isCancellingExistingHold}
                className="flex items-center justify-center gap-2 border border-rose-500/50 bg-rose-950/10 py-4 font-sans text-[10px] font-bold uppercase tracking-[0.14em] text-rose-300 transition hover:border-rose-400 hover:bg-rose-950/30 disabled:cursor-wait disabled:opacity-50"
                id="cancel-existing-booking"
              >
                {isCancellingExistingHold ? <Loader2 className="h-4 w-4 animate-spin" /> : <XCircle className="h-4 w-4" />}
                <span>{isCancellingExistingHold ? 'Đang hủy đơn...' : 'Hủy đơn & chọn ghế khác'}</span>
              </button>
              <button
                type="button"
                onClick={() => navigate('/tickets')}
                disabled={isCancellingExistingHold}
                className="flex items-center justify-center gap-2 border border-amber-400 bg-amber-400 py-4 font-sans text-[10px] font-bold uppercase tracking-[0.14em] text-black transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-50"
                id="continue-existing-payment"
              >
                <Ticket className="h-4 w-4" />
                <span>Tiếp tục thanh toán</span>
              </button>
            </div>
          ) : (
            <button
              onClick={handleReceiptAction}
              disabled={selectedSeats.length === 0 || isHolding || !selectedShowtime}
              className={`w-full flex items-center justify-center space-x-2 py-4 text-xs font-bold font-sans uppercase tracking-[0.2em] transition-all border ${selectedSeats.length === 0 || !selectedShowtime
                ? 'bg-neutral-900 border-neutral-800 text-neutral-600 cursor-not-allowed opacity-30'
                : 'bg-white hover:bg-black hover:text-white border-white text-black'
                }`}
              id="proceed-payment-submit"
            >
              {isHolding ? <Loader2 className="h-4 w-4 animate-spin" /> : bookingStep === 'seats' ? <ShoppingBag className="h-4 w-4" /> : <Ticket className="h-4 w-4" />}
              <span>
                {isHolding ? 'ĐANG GIỮ GHẾ...' : selectedSeats.length === 0 ? 'CHƯA CHỌN GHẾ' : bookingStep === 'seats' ? 'TIẾP TỤC CHỌN BẮP NƯỚC' : 'TIẾP TỤC THANH TOÁN'}
              </span>
            </button>
          )}

        </div>

      </div>

      {/* Hallmark · component: confirmation-dialog · genre: modern-minimal · theme: CinePremier OLED · critique: P4 H5 E5 S5 R5 V4 */}
      <AnimatePresence>
        {showAudienceConfirmation && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: shouldReduceMotion ? 0.1 : 0.18, ease: 'easeOut' }}
            className="fixed inset-0 z-[140] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
            onMouseDown={(event) => {
              if (event.target === event.currentTarget) setShowAudienceConfirmation(false);
            }}
            role="presentation"
          >
            <motion.section
              ref={audienceDialogRef}
              initial={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, y: 12, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, y: 8, scale: 0.985 }}
              transition={{ duration: shouldReduceMotion ? 0.1 : 0.22, ease: 'easeOut' }}
              role="dialog"
              aria-modal="true"
              aria-labelledby="audience-confirmation-title"
              aria-describedby="audience-confirmation-description"
              className="max-h-[calc(100dvh-2rem)] w-full max-w-lg overflow-y-auto border border-white/15 bg-neutral-950 shadow-2xl"
            >
              <div className="h-1 bg-amber-400" aria-hidden="true" />

              <div className="border-b border-white/10 p-5 sm:px-6">
                <div className="flex items-start gap-4">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center border border-amber-400/40 bg-amber-400/10 text-amber-300" aria-hidden="true">
                    <ShieldCheck className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-[10px] font-black uppercase tracking-[0.2em] text-amber-300">Xác nhận điều kiện</p>
                    <h2 id="audience-confirmation-title" className="mt-1 min-w-0 text-xl font-black text-white [overflow-wrap:anywhere]">Độ tuổi người xem</h2>
                    <p className="mt-1 truncate text-xs text-neutral-400">
                      {movie?.title} · {selectedSeats.length} ghế đã chọn
                    </p>
                  </div>
                  <span className="shrink-0 border border-amber-400/40 bg-amber-400/10 px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-amber-300">
                    {movie?.ageRating || 'Phân loại phim'}
                  </span>
                </div>
              </div>

              <div id="audience-confirmation-description" className="space-y-4 p-5 text-sm leading-6 text-neutral-200 sm:px-6">
                <p>
                  Tôi xác nhận mua vé cho người xem{' '}
                  <strong className="font-black text-white">
                    {audienceMinimumAge > 0
                      ? `từ đủ ${audienceMinimumAge} tuổi trở lên`
                      : `phù hợp với phân loại ${movie?.ageRating || 'của phim'}`}
                  </strong>{' '}
                  và đồng ý cung cấp giấy tờ tùy thân để xác thực độ tuổi khi được yêu cầu.
                </p>

                {isLateNightShow && (
                  <div className="flex gap-3 border-l-2 border-amber-400 bg-amber-400/5 px-4 py-3 text-amber-100">
                    <CircleAlert className="mt-1 h-4 w-4 shrink-0 text-amber-300" aria-hidden="true" />
                    <div>
                      <p className="text-xs font-black uppercase tracking-wider text-amber-300">Lưu ý suất chiếu muộn</p>
                      <p className="mt-1 text-sm leading-6">Suất chiếu kết thúc sau 23:00. Rạp không phục vụ khách hàng dưới 16 tuổi cho suất chiếu này.</p>
                    </div>
                  </div>
                )}

                <div className="flex gap-3 border-t border-white/10 pt-4">
                  <Ticket className="mt-0.5 h-4 w-4 shrink-0 text-neutral-300" aria-hidden="true" />
                  <p className="text-sm leading-6 text-neutral-300">
                    Vé không được hoàn nếu người xem không đáp ứng quy định về độ tuổi tại thời điểm kiểm tra.
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-3 border-t border-white/10 bg-black/30 p-4 sm:grid-cols-[minmax(0,0.75fr)_minmax(0,1.25fr)]">
                <button
                  type="button"
                  onClick={() => setShowAudienceConfirmation(false)}
                  className="min-h-12 cursor-pointer border border-white/15 px-5 text-xs font-black uppercase tracking-[0.16em] text-neutral-300 outline-none transition-colors hover:border-white/30 hover:bg-white/5 hover:text-white focus-visible:ring-2 focus-visible:ring-white active:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Hủy
                </button>
                <button
                  ref={audienceConfirmButtonRef}
                  type="button"
                  onClick={handleConfirmAudience}
                  className="flex min-h-12 cursor-pointer items-center justify-center gap-2 whitespace-nowrap bg-amber-400 px-5 text-xs font-black uppercase tracking-[0.16em] text-black outline-none transition-colors hover:bg-amber-300 focus-visible:ring-2 focus-visible:ring-white active:bg-amber-500 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Check className="h-4 w-4" aria-hidden="true" />
                  Xác nhận & tiếp tục
                </button>
              </div>
            </motion.section>
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}
