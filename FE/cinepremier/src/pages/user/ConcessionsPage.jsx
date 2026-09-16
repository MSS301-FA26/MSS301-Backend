import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  CircleAlert,
  Clock3,
  Coins,
  Loader2,
  Minus,
  Popcorn,
  Plus,
  ReceiptText,
  ShoppingBag,
  Store,
  Ticket,
  X,
} from 'lucide-react';
import { getStoredAuth } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { paymentService } from '../../services/paymentService';
import { loyaltyService } from '../../services/loyaltyService';
import { useMovies } from '../../stores/useMovieStore';
import { useUiStore } from '../../stores/useUiStore';

const MAX_ITEM_QUANTITY = 3;

const formatVnd = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`;

const formatDateTime = (value) => {
  if (!value) return 'Chưa xác định';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Chưa xác định';
  return date.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const canAttachToBooking = (booking) => (
  booking?.status === 'PAID'
  && (!booking?.showtimeEnd || new Date(booking.showtimeEnd).getTime() > Date.now())
);

const isFoodAvailable = (item) => (
  !item?.status || ['ACTIVE', 'LOW_STOCK'].includes(String(item.status).toUpperCase())
);

const orderStatusLabel = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Đã hết hạn',
};

const remainingSeconds = (expiresAt, now) => Math.max(
  0,
  Math.ceil((new Date(expiresAt).getTime() - now) / 1000)
);

const formatCountdown = (seconds) => `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;

export default function ConcessionsPage() {
  const [searchParams] = useSearchParams();
  const requestedBookingId = searchParams.get('bookingId');
  const showToast = useUiStore((state) => state.showToast);
  const { foodCatalog = [], fetchPublicFoodCatalog, publicCinema } = useMovies();

  const [linkedBooking, setLinkedBooking] = useState(null);
  const [isLoadingContext, setIsLoadingContext] = useState(Boolean(requestedBookingId));
  const [quantities, setQuantities] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [checkoutError, setCheckoutError] = useState('');
  const [loyaltyPoints, setLoyaltyPoints] = useState(0);
  const [earningRatePercent, setEarningRatePercent] = useState(1);
  const [foodOrders, setFoodOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [orderActionId, setOrderActionId] = useState(null);
  const [now, setNow] = useState(Date.now());

  const refreshFoodOrders = useCallback(async () => {
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    try {
      const orders = await bookingService.getMyFoodOrders(accessToken);
      setFoodOrders(Array.isArray(orders) ? orders : []);
    } catch {
      setFoodOrders([]);
    } finally {
      setOrdersLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPublicFoodCatalog();
  }, [fetchPublicFoodCatalog]);

  useEffect(() => {
    refreshFoodOrders();
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [refreshFoodOrders]);

  useEffect(() => {
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    Promise.all([
      loyaltyService.getMyLoyalty(accessToken).catch(() => null),
      loyaltyService.getConfiguration(accessToken).catch(() => null),
    ]).then(([loyalty, config]) => {
      setLoyaltyPoints(Number(loyalty?.points ?? 0));
      setEarningRatePercent(Math.max(0, Number(config?.earningRatePercent ?? 1) || 0));
    });
  }, []);

  useEffect(() => {
    if (!requestedBookingId) {
      setLinkedBooking(null);
      setIsLoadingContext(false);
      return undefined;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      setIsLoadingContext(false);
      return undefined;
    }

    let cancelled = false;
    setIsLoadingContext(true);
    bookingService.getMyBooking(accessToken, requestedBookingId)
      .then((booking) => {
        if (cancelled) return;
        if (canAttachToBooking(booking)) setLinkedBooking(booking);
        else showToast('Vé này không còn nhận thêm món. Đơn mới sẽ được tạo riêng tại quầy.');
      })
      .catch(() => {
        if (!cancelled) {
          setLinkedBooking(null);
          showToast('Không thể liên kết vé. Bạn vẫn có thể đặt bắp nước riêng.');
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoadingContext(false);
      });

    return () => { cancelled = true; };
  }, [requestedBookingId, showToast]);

  const availableFoods = useMemo(
    () => foodCatalog.filter(isFoodAvailable),
    [foodCatalog]
  );

  const selectedRows = useMemo(() => Object.entries(quantities)
    .map(([id, quantity]) => {
      const item = availableFoods.find((candidate) => String(candidate.id) === String(id));
      if (!item || quantity <= 0) return null;
      return { ...item, quantity, lineTotal: Number(item.price || 0) * quantity };
    })
    .filter(Boolean), [availableFoods, quantities]);

  const totalItems = selectedRows.reduce((sum, row) => sum + row.quantity, 0);
  const totalAmount = selectedRows.reduce((sum, row) => sum + row.lineTotal, 0);
  const earnedPointsPreview = Math.floor(totalAmount * earningRatePercent / 100);
  const standaloneOrders = useMemo(
    () => foodOrders.filter((order) => !order.bookingId),
    [foodOrders]
  );
  const activeStandaloneOrder = standaloneOrders.find((order) => (
    order.status === 'PENDING_PAYMENT' && remainingSeconds(order.expiresAt, now) > 0
  ));

  const changeQuantity = (item, delta) => {
    if (!isFoodAvailable(item)) return;
    setCheckoutError('');
    setQuantities((current) => {
      const key = String(item.id);
      const nextQuantity = Math.max(0, Math.min(MAX_ITEM_QUANTITY, (current[key] || 0) + delta));
      const next = { ...current };
      if (nextQuantity === 0) delete next[key];
      else next[key] = nextQuantity;
      return next;
    });
  };

  const focusFirstProduct = () => {
    const firstAddButton = document.querySelector('[data-concession-add]');
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    firstAddButton?.scrollIntoView({ behavior: prefersReducedMotion ? 'auto' : 'smooth', block: 'center' });
    firstAddButton?.focus({ preventScroll: true });
  };

  const retryFoodOrderPayment = async (order) => {
    const { accessToken } = getStoredAuth();
    if (!accessToken || orderActionId) return;
    setOrderActionId(order.id);
    try {
      const payment = await paymentService.createVnpayFoodOrderPayment(accessToken, order.id);
      const paymentUrl = payment?.paymentUrl ?? payment?.payment_url;
      if (!paymentUrl) throw new Error('Cổng VNPay không trả về đường dẫn thanh toán.');
      window.location.href = paymentUrl;
    } catch (error) {
      showToast(error?.message || 'Không thể tiếp tục thanh toán đơn này.');
      await refreshFoodOrders();
      setOrderActionId(null);
    }
  };

  const cancelFoodOrder = async (order) => {
    if (!window.confirm(`Hủy đơn ${order.orderCode}? Thao tác này không thể hoàn tác.`)) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken || orderActionId) return;
    setOrderActionId(order.id);
    try {
      await bookingService.cancelFoodOrder(accessToken, order.id);
      showToast('Đã hủy đơn bắp nước.');
      await refreshFoodOrders();
    } catch (error) {
      showToast(error?.message || 'Không thể hủy đơn bắp nước.');
    } finally {
      setOrderActionId(null);
    }
  };

  const handleCheckout = async () => {
    if (selectedRows.length === 0) {
      showToast('Vui lòng chọn ít nhất một món bắp nước.');
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken || isSubmitting) return;

    const body = {
      foods: selectedRows.map((item) => ({
        foodItemId: item.foodItemId ?? null,
        foodComboId: item.foodComboId ?? null,
        quantity: item.quantity,
      })),
    };

    setCheckoutError('');
    setIsSubmitting(true);
    try {
      const order = linkedBooking
        ? await bookingService.createFoodOrder(accessToken, linkedBooking.id, body)
        : await bookingService.createStandaloneFoodOrder(accessToken, body);
      await refreshFoodOrders();
      const payment = await paymentService.createVnpayFoodOrderPayment(accessToken, order.id);
      const paymentUrl = payment?.paymentUrl ?? payment?.payment_url;
      if (!paymentUrl) throw new Error('Cổng VNPay không trả về đường dẫn thanh toán.');
      window.location.href = paymentUrl;
    } catch (error) {
      const backendMessage = String(error?.message || '');
      const message = !backendMessage || backendMessage.toLowerCase().includes('lỗi hệ thống')
        ? 'Chưa thể khởi tạo thanh toán. Vui lòng thử lại sau khi dịch vụ được kết nối lại.'
        : backendMessage;
      setCheckoutError(message);
      showToast(message);
      await refreshFoodOrders();
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-black px-4 py-10 text-white sm:px-6 lg:px-8">
      <div className="mx-auto max-w-[1500px]">
        <header className="border-b border-white/10 pb-7">
          <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-end">
            <div>
              <div className="mb-3 flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.24em] text-purple-400">
                <Popcorn className="h-4 w-4" /> CinePremier Concessions
              </div>
              <h1 className="min-w-0 text-3xl font-black uppercase tracking-tight [overflow-wrap:anywhere] sm:text-5xl">Đặt bắp nước</h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-neutral-400">
                Chọn món yêu thích, thanh toán qua VNPay và nhận trực tiếp tại quầy. Không cần mua vé xem phim.
              </p>
            </div>
            <div className="flex items-center gap-3 border border-white/10 bg-neutral-950 px-4 py-3 text-[10px] uppercase tracking-wider text-neutral-400">
              <span className="flex h-8 w-8 items-center justify-center border border-purple-500/30 bg-purple-500/10 text-purple-300">
                <Store className="h-4 w-4" />
              </span>
              <span>Nhận tại quầy <strong className="block text-white">{publicCinema?.name || 'CinePremier'}</strong></span>
            </div>
          </div>
        </header>

        {(ordersLoading || activeStandaloneOrder) && <section id="my-food-orders" className="scroll-mt-36 border-b border-white/10 py-6">
          <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <div>
              <div className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.16em] text-white">
                <ReceiptText className="h-4 w-4 text-purple-400" /> Đơn đang chờ thanh toán
              </div>
              <p className="mt-1 text-[10px] leading-5 text-neutral-500">
                Hoàn tất hoặc hủy đơn này trước khi tạo một đơn mua riêng mới.
              </p>
            </div>
            <span className="font-mono text-[9px] uppercase tracking-wider text-neutral-500">
              {activeStandaloneOrder ? 1 : 0} đơn
            </span>
          </div>

          {ordersLoading ? (
            <div className="flex min-h-20 items-center gap-2 border border-white/10 bg-neutral-950 px-4 text-xs text-neutral-500">
              <Loader2 className="h-4 w-4 animate-spin" /> Đang tải đơn bắp nước...
            </div>
          ) : !activeStandaloneOrder ? (
            <div className="flex min-h-20 items-center gap-3 border border-dashed border-white/10 bg-neutral-950/60 px-4 text-xs text-neutral-500">
              <ShoppingBag className="h-5 w-5 text-neutral-700" /> Không có đơn đang chờ thanh toán.
            </div>
          ) : (
            <div className="grid gap-3 xl:grid-cols-2">
              {[activeStandaloneOrder].filter(Boolean).map((order) => {
                const seconds = remainingSeconds(order.expiresAt, now);
                const isPending = order.status === 'PENDING_PAYMENT' && seconds > 0;
                const isPaid = order.status === 'PAID';
                const displayStatus = order.status === 'PENDING_PAYMENT' && seconds === 0
                  ? 'EXPIRED'
                  : order.status;
                return (
                  <article key={order.id} className={`border p-4 ${isPending ? 'border-amber-400/35 bg-amber-400/[0.05]' : isPaid ? 'border-emerald-400/25 bg-emerald-400/[0.04]' : 'border-white/10 bg-neutral-950'}`}>
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="font-mono text-[10px] font-black text-white">{order.orderCode}</p>
                        <p className="mt-1 text-[9px] text-neutral-500">
                          {(order.items || []).map((item) => `${item.name} ×${item.quantity}`).join(' · ')}
                        </p>
                      </div>
                      <span className={`border px-2 py-1 text-[8px] font-black uppercase tracking-widest ${isPending ? 'border-amber-400/30 text-amber-300' : isPaid ? 'border-emerald-400/30 text-emerald-300' : 'border-white/10 text-neutral-500'}`}>
                        {orderStatusLabel[displayStatus] || displayStatus}
                      </span>
                    </div>
                    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-white/10 pt-3">
                      <div>
                        <p className="font-mono text-sm font-black text-white">{formatVnd(order.totalAmount)}</p>
                        {isPending ? (
                          <p className="mt-1 flex items-center gap-1.5 font-mono text-[9px] text-amber-300"><Clock3 className="h-3 w-3" /> Còn {formatCountdown(seconds)}</p>
                        ) : isPaid ? (
                          <p className="mt-1 text-[9px] text-emerald-300">Đưa mã này cho nhân viên tại quầy</p>
                        ) : (
                          <p className="mt-1 text-[9px] text-neutral-600">Đơn không còn hiệu lực thanh toán</p>
                        )}
                      </div>
                      {isPending ? (
                        <div className="flex gap-2">
                          <button type="button" onClick={() => cancelFoodOrder(order)} disabled={Boolean(orderActionId)} className="flex min-h-10 items-center gap-1.5 border border-white/15 px-3 text-[8px] font-black uppercase tracking-wider text-neutral-300 hover:border-red-400/50 hover:text-red-300 disabled:opacity-40">
                            <X className="h-3.5 w-3.5" /> Hủy đơn
                          </button>
                          <button type="button" onClick={() => retryFoodOrderPayment(order)} disabled={Boolean(orderActionId)} className="flex min-h-10 items-center gap-2 bg-amber-400 px-4 text-[8px] font-black uppercase tracking-wider text-black hover:bg-amber-300 disabled:opacity-40">
                            {orderActionId === order.id ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Clock3 className="h-3.5 w-3.5" />} Thanh toán lại
                          </button>
                        </div>
                      ) : null}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>}

        {isLoadingContext ? (
          <div className="my-6 flex items-center gap-2 border border-white/10 bg-neutral-950 p-4 text-xs text-neutral-500">
            <Loader2 className="h-4 w-4 animate-spin" /> Đang kiểm tra vé liên kết...
          </div>
        ) : linkedBooking ? (
          <div className="my-6 flex flex-col justify-between gap-3 border border-purple-500/25 bg-purple-500/[0.06] p-4 sm:flex-row sm:items-center">
            <div className="flex items-center gap-3">
              <span className="flex h-9 w-9 items-center justify-center border border-purple-400/40 text-purple-300"><Ticket className="h-4 w-4" /></span>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-purple-300">Đặt thêm cho vé {linkedBooking.bookingCode}</p>
                <p className="mt-1 text-xs text-white">{linkedBooking.movieTitle} · {formatDateTime(linkedBooking.showtimeStart)}</p>
              </div>
            </div>
            <button type="button" onClick={() => setLinkedBooking(null)} className="min-h-11 whitespace-nowrap px-2 text-[9px] font-black uppercase tracking-wider text-neutral-400 transition-colors duration-150 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-400 active:text-purple-300">
              Chuyển thành đơn mua riêng
            </button>
          </div>
        ) : null}

        <div className="grid items-start gap-7 py-7 lg:grid-cols-[minmax(0,1fr)_390px]">
          <section>
            <div className="mb-4 flex items-center gap-3">
              <span className="flex h-7 w-7 items-center justify-center bg-white text-xs font-black text-black">1</span>
              <div>
                <h2 className="text-sm font-black uppercase tracking-[0.15em]">Chọn món</h2>
                <p className="mt-1 text-[10px] text-neutral-500">Tối đa {MAX_ITEM_QUANTITY} phần cho mỗi sản phẩm.</p>
              </div>
            </div>

            {availableFoods.length === 0 ? (
              <div className="flex min-h-60 items-center justify-center gap-2 border border-white/10 bg-neutral-950 text-xs text-neutral-500">
                <Loader2 className="h-4 w-4 animate-spin" /> Đang tải thực đơn...
              </div>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2">
                {availableFoods.map((item) => {
                  const quantity = quantities[String(item.id)] || 0;
                  return (
                    <article key={item.id} className={`grid min-h-40 grid-cols-[112px_minmax(0,1fr)] overflow-hidden border bg-neutral-950 transition-colors duration-200 sm:grid-cols-[120px_minmax(0,1fr)] ${quantity > 0 ? 'border-purple-400/60' : 'border-white/10 hover:border-white/25'}`}>
                      <div className="relative bg-neutral-900">
                        {item.imageUrl ? <img src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" referrerPolicy="no-referrer" /> : <div className="flex h-full items-center justify-center text-purple-400"><Popcorn className="h-10 w-10" /></div>}
                        <span className="absolute left-2 top-2 bg-black/85 px-2 py-1 text-[7px] font-black uppercase tracking-widest text-emerald-400">{item.category === 'combo' ? 'Combo' : 'Món lẻ'}</span>
                      </div>
                      <div className="flex min-w-0 flex-col justify-between p-4">
                        <div>
                          <h3 className="truncate text-sm font-black text-white" title={item.name}>{item.name}</h3>
                          <p className="mt-2 line-clamp-2 text-[10px] leading-4 text-neutral-500">{item.description || 'Sẵn sàng phục vụ tại quầy CinePremier.'}</p>
                        </div>
                        <div className="mt-4 flex items-end justify-between gap-3">
                          <span className="font-mono text-base font-black text-amber-300">{formatVnd(item.price)}</span>
                          <div className="flex h-11 items-center border border-white/10 bg-black">
                            <button type="button" aria-label={`Giảm ${item.name}`} onClick={() => changeQuantity(item, -1)} disabled={quantity === 0} className="flex h-11 w-11 items-center justify-center text-neutral-400 transition-colors duration-150 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-400 disabled:cursor-not-allowed disabled:text-neutral-800"><Minus className="h-3.5 w-3.5" /></button>
                            <span className="w-8 text-center font-mono text-xs font-black text-white">{quantity}</span>
                            <button data-concession-add type="button" aria-label={`Thêm ${item.name}`} onClick={() => changeQuantity(item, 1)} disabled={quantity >= MAX_ITEM_QUANTITY} className="flex h-11 w-11 items-center justify-center text-neutral-400 transition-colors duration-150 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-400 disabled:cursor-not-allowed disabled:text-neutral-800"><Plus className="h-3.5 w-3.5" /></button>
                          </div>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>

          <aside className="border border-white/10 bg-[#070707] lg:sticky lg:top-36">
            <div className="border-b border-white/10 p-5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.16em] text-white"><ReceiptText className="h-4 w-4 text-purple-400" /> Hóa đơn bắp nước</div>
                <span className="font-mono text-[9px] text-neutral-500">{totalItems} món</span>
              </div>
            </div>

            <div className="space-y-5 p-5">
              <div className="space-y-2" aria-live="polite">
                {selectedRows.length === 0 ? (
                  <div className="border-y border-white/10 bg-neutral-950 px-5 py-6">
                    <div className="flex items-start gap-4">
                      <span className="flex h-10 w-10 shrink-0 items-center justify-center border border-purple-500/30 bg-purple-500/10 text-purple-300">
                        <ShoppingBag className="h-4 w-4" aria-hidden="true" />
                      </span>
                      <div className="min-w-0">
                        <p className="text-[11px] font-black uppercase tracking-[0.14em] text-white">Hóa đơn đang trống</p>
                        <p className="mt-2 text-[10px] leading-5 text-neutral-400">
                          Chọn món bằng nút cộng ở thực đơn. Giá và CinePoints sẽ được cập nhật ngay tại đây.
                        </p>
                      </div>
                    </div>
                    <div className="mt-5 flex items-center justify-between border-t border-white/10 pt-4">
                      <span className="font-mono text-[9px] uppercase tracking-wider text-neutral-400">0 món · 0đ</span>
                      <button type="button" onClick={focusFirstProduct} className="min-h-11 whitespace-nowrap border border-white/15 px-3 text-[9px] font-black uppercase tracking-[0.12em] text-white transition-colors duration-150 hover:border-purple-400 hover:text-purple-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-400 active:bg-white/5">
                        Chọn món đầu tiên
                      </button>
                    </div>
                  </div>
                ) : selectedRows.map((row) => (
                  <div key={row.id} className="grid grid-cols-[minmax(0,1fr)_auto] gap-3 border-b border-white/5 py-2.5">
                    <div className="min-w-0"><p className="truncate text-[11px] font-bold text-white">{row.name}</p><p className="mt-1 font-mono text-[9px] text-neutral-500">{formatVnd(row.price)} × {row.quantity}</p></div>
                    <span className="font-mono text-[11px] font-black text-amber-300">{formatVnd(row.lineTotal)}</span>
                  </div>
                ))}
              </div>

              <div className="flex items-center justify-between gap-4 border-y border-emerald-500/20 bg-emerald-500/[0.05] px-3 py-3">
                <div className="flex items-center gap-2">
                  <Coins className="h-4 w-4 text-emerald-400" />
                  <div>
                    <p className="text-[9px] font-black uppercase tracking-widest text-emerald-300">CinePoints</p>
                    <p className="mt-1 text-[8px] text-neutral-500">Đang có {loyaltyPoints.toLocaleString('vi-VN')} điểm</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="font-mono text-xs font-black text-emerald-400">+{earnedPointsPreview.toLocaleString('vi-VN')}</p>
                  <p className="mt-1 text-[8px] uppercase text-neutral-500">Sau thanh toán</p>
                </div>
              </div>

              <div className="flex items-end justify-between border-y border-white/10 py-4">
                <div><p className="text-[10px] font-black uppercase tracking-widest text-white">Tổng thanh toán</p><p className="mt-1 text-[8px] text-neutral-500">{totalItems} sản phẩm · Thanh toán VNPay</p></div>
                <span className="font-mono text-2xl font-black text-white">{formatVnd(totalAmount)}</span>
              </div>

              {!linkedBooking && activeStandaloneOrder ? (
                <div className="flex items-start gap-3 border border-amber-400/25 bg-amber-400/[0.06] p-3 text-amber-100">
                  <Clock3 className="mt-0.5 h-4 w-4 shrink-0 text-amber-300" />
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-[0.12em]">Bạn đang có một đơn chờ thanh toán</p>
                    <p className="mt-1 text-[10px] leading-4 text-amber-100/65">Hoàn tất hoặc hủy đơn {activeStandaloneOrder.orderCode} ở phía trên trước khi tạo đơn mới.</p>
                  </div>
                </div>
              ) : null}

              {checkoutError ? (
                <div role="alert" className="flex items-start gap-3 border border-red-500/30 bg-red-500/[0.07] p-3 text-red-100">
                  <CircleAlert className="mt-0.5 h-4 w-4 shrink-0 text-red-400" aria-hidden="true" />
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-[0.12em]">Thanh toán chưa được tạo</p>
                    <p className="mt-1 text-[10px] leading-4 text-red-100/70">{checkoutError} Kiểm tra kết nối rồi bấm thử lại.</p>
                  </div>
                </div>
              ) : null}

              <button type="button" onClick={handleCheckout} disabled={totalItems === 0 || isSubmitting || (!linkedBooking && Boolean(activeStandaloneOrder))} aria-describedby="checkout-help" className="flex min-h-14 w-full items-center justify-center gap-2 bg-purple-600 px-4 py-4 text-[11px] font-black uppercase tracking-[0.16em] text-white transition-colors duration-200 hover:bg-purple-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white active:bg-purple-700 disabled:cursor-not-allowed disabled:bg-neutral-900 disabled:text-neutral-600">
                {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <ShoppingBag className="h-4 w-4" />}
                {isSubmitting ? 'Đang chuyển đến VNPay...' : (!linkedBooking && activeStandaloneOrder) ? 'Hoàn tất đơn đang chờ phía trên' : checkoutError ? 'Thử thanh toán lại' : 'Xác nhận & thanh toán'}
              </button>

              <p id="checkout-help" className="text-center text-[9px] leading-4 text-neutral-400">
                {totalItems === 0
                  ? 'Chọn ít nhất một món để mở thanh toán.'
                  : 'Sau thanh toán, dùng mã đơn để nhận món tại quầy CinePremier.'}
              </p>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
}
