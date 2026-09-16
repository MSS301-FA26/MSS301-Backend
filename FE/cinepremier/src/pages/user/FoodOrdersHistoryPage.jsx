/*
 * Hallmark · component: food-order history cards · genre: editorial · theme: CinePremier dark
 * States: default · hover · focus · active · disabled · loading · error · success
 * Pre-emit critique: P5 · H5 · E5 · S5 · R5 · V5
 */
import React, { useEffect, useMemo, useState } from 'react';
import { Clock3, Loader2, Popcorn, ReceiptText, ShoppingBag, X } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { useNavigate } from 'react-router-dom';
import { getStoredAuth } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { paymentService } from '../../services/paymentService';
import { useUiStore } from '../../stores/useUiStore';

const formatVnd = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`;
const labels = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  PICKED_UP: 'Đã nhận món',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Đã hết hạn',
};

const countdown = (expiresAt, now) => Math.max(0, Math.ceil((new Date(expiresAt).getTime() - now) / 1000));
const countdownLabel = (seconds) => `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
const formatDateTime = (value) => (value ? new Date(value).toLocaleString('vi-VN', {
  hour: '2-digit',
  minute: '2-digit',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
}) : '');

export default function FoodOrdersHistoryPage() {
  const navigate = useNavigate();
  const showToast = useUiStore((state) => state.showToast);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState(null);
  const [now, setNow] = useState(Date.now());

  const loadOrders = async () => {
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    try {
      const result = await bookingService.getMyFoodOrders(accessToken);
      setOrders((Array.isArray(result) ? result : []).filter((order) => !order.bookingId));
    } catch {
      setOrders([]);
      showToast('Không thể tải lịch sử bắp nước.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const sortedOrders = useMemo(() => [...orders].sort((a, b) => (
    new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
  )), [orders]);

  const groupedOrders = useMemo(() => sortedOrders.reduce((groups, order) => {
    const seconds = countdown(order.expiresAt, now);
    const status = order.status === 'PENDING_PAYMENT' && seconds === 0 ? 'EXPIRED' : order.status;
    if (status === 'PENDING_PAYMENT') groups.pending.push(order);
    else if (status === 'PAID') groups.ready.push(order);
    else groups.history.push(order);
    return groups;
  }, { pending: [], ready: [], history: [] }), [sortedOrders, now]);

  const retryPayment = async (order) => {
    const { accessToken } = getStoredAuth();
    if (!accessToken || actionId) return;
    setActionId(order.id);
    try {
      const payment = await paymentService.createVnpayFoodOrderPayment(accessToken, order.id);
      const url = payment?.paymentUrl ?? payment?.payment_url;
      if (!url) throw new Error('Cổng VNPay không trả về đường dẫn thanh toán.');
      window.location.href = url;
    } catch (error) {
      showToast(error?.message || 'Không thể tiếp tục thanh toán.');
      await loadOrders();
      setActionId(null);
    }
  };

  const cancelOrder = async (order) => {
    if (!window.confirm(`Hủy đơn ${order.orderCode}? Thao tác này không thể hoàn tác.`)) return;
    const { accessToken } = getStoredAuth();
    if (!accessToken || actionId) return;
    setActionId(order.id);
    try {
      await bookingService.cancelFoodOrder(accessToken, order.id);
      showToast('Đã hủy đơn bắp nước.');
      await loadOrders();
    } catch (error) {
      showToast(error?.message || 'Không thể hủy đơn.');
    } finally {
      setActionId(null);
    }
  };

  const renderOrderCard = (order) => {
    const seconds = countdown(order.expiresAt, now);
    const status = order.status === 'PENDING_PAYMENT' && seconds === 0 ? 'EXPIRED' : order.status;
    const pending = status === 'PENDING_PAYMENT';
    const paid = status === 'PAID';
    const pickedUp = status === 'PICKED_UP';
    const inactiveMessage = status === 'EXPIRED'
      ? 'Đã quá thời hạn thanh toán 15 phút.'
      : status === 'CANCELLED'
        ? 'Đơn đã được hủy và không còn hiệu lực thanh toán.'
        : '';

    return (
      <article key={order.id} className={`h-full border p-4 sm:p-5 ${pending ? 'border-amber-400/35 bg-amber-400/[0.04]' : paid ? 'border-emerald-400/25 bg-emerald-400/[0.035]' : 'border-white/10 bg-neutral-950'}`}>
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0"><p className="break-all font-mono text-sm font-black text-white">{order.orderCode}</p><p className="mt-2 text-[11px] leading-4 text-neutral-400">{(order.items || []).map((item) => `${item.name} ×${item.quantity}`).join(' · ')}</p></div>
          <span className={`shrink-0 border px-2.5 py-1.5 text-[9px] font-black uppercase tracking-widest ${pending ? 'border-amber-400/30 text-amber-300' : paid ? 'border-emerald-400/30 text-emerald-300' : 'border-white/10 text-neutral-400'}`}>{labels[status] || status}</span>
        </div>
        <div className="mt-5 flex flex-wrap items-end justify-between gap-4 border-t border-white/10 pt-4">
          <div>
            <p className="font-mono text-2xl font-black text-white">{formatVnd(order.totalAmount)}</p>
            {pending ? <p className="mt-1.5 flex items-center gap-1.5 font-mono text-[10px] font-bold text-amber-300"><Clock3 className="h-3.5 w-3.5" /> Còn {countdownLabel(seconds)}</p> : paid ? <p className="mt-1.5 text-[10px] font-medium text-emerald-300">Sẵn sàng nhận món tại quầy</p> : <p className="mt-1.5 max-w-sm text-[10px] leading-4 text-neutral-500">{inactiveMessage}</p>}
            {!pending && !paid && order.createdAt && <p className="mt-1 font-mono text-[9px] text-neutral-600">Tạo lúc {formatDateTime(order.createdAt)}</p>}
          </div>
          {pending && <div className="flex w-full gap-2 sm:w-auto"><button type="button" onClick={() => cancelOrder(order)} disabled={Boolean(actionId)} aria-busy={actionId === order.id} className="flex min-h-11 flex-1 items-center justify-center gap-1.5 whitespace-nowrap border border-white/15 px-4 text-[9px] font-black uppercase tracking-wider text-neutral-300 transition-colors hover:border-red-400/40 hover:text-red-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white active:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40 sm:flex-none"><X className="h-3.5 w-3.5" /> Hủy</button><button type="button" onClick={() => retryPayment(order)} disabled={Boolean(actionId)} aria-busy={actionId === order.id} className="min-h-11 flex-1 whitespace-nowrap bg-amber-400 px-5 text-[9px] font-black uppercase tracking-wider text-black transition-colors hover:bg-amber-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white active:bg-amber-500 disabled:cursor-not-allowed disabled:opacity-40 sm:flex-none">{actionId === order.id ? 'Đang chuyển...' : 'Thanh toán lại'}</button></div>}
        </div>
        {paid && order.qrCode && (
          <div className="mt-5 grid min-w-0 gap-4 border-t border-emerald-400/15 pt-5 sm:grid-cols-[112px_minmax(0,1fr)] sm:items-center">
            <div className="w-fit bg-white p-2">
              <QRCodeSVG value={order.qrCode} size={96} level="M" title={`Mã nhận món ${order.orderCode}`} />
            </div>
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.18em] text-emerald-300">QR nhận món một lần</p>
              <p className="mt-2 max-w-lg text-xs leading-5 text-neutral-300">Đưa mã này cho nhân viên tại quầy. QR sẽ hết hiệu lực ngay sau khi món được giao.</p>
              <p className="mt-3 font-mono text-[10px] font-bold text-white">{order.orderCode}</p>
            </div>
          </div>
        )}
        {pickedUp && (
          <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-white/10 pt-4 text-[10px] text-neutral-500">
            <span>Đơn đã hoàn tất, QR nhận món không còn hiệu lực.</span>
            {order.pickedUpAt && <span className="shrink-0 font-mono">{new Date(order.pickedUpAt).toLocaleString('vi-VN')}</span>}
          </div>
        )}
      </article>
    );
  };

  if (loading) {
    return <div className="flex min-h-64 items-center justify-center gap-2 border border-white/10 bg-neutral-950 text-xs text-neutral-500"><Loader2 className="h-4 w-4 animate-spin" /> Đang tải đơn bắp nước...</div>;
  }

  if (sortedOrders.length === 0) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center border border-dashed border-white/15 bg-neutral-950/60 px-6 text-center">
        <span className="flex h-14 w-14 items-center justify-center border border-purple-500/25 bg-purple-500/10 text-purple-300"><ShoppingBag className="h-6 w-6" /></span>
        <h2 className="mt-5 text-sm font-black uppercase tracking-[0.16em] text-white">Chưa có đơn bắp nước</h2>
        <p className="mt-2 max-w-md text-xs leading-5 text-neutral-500">Các đơn mua riêng, trạng thái thanh toán và mã nhận món sẽ xuất hiện tại đây.</p>
        <button type="button" onClick={() => navigate('/concessions')} className="mt-6 min-h-11 whitespace-nowrap bg-purple-600 px-6 text-[10px] font-black uppercase tracking-widest text-white transition-colors hover:bg-purple-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white active:bg-purple-700">Đặt bắp nước</button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/10 pb-3">
        <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-neutral-400"><ReceiptText className="h-4 w-4 text-purple-400" /> {sortedOrders.length} đơn mua riêng</div>
        <button type="button" onClick={() => navigate('/concessions')} className="flex min-h-11 items-center gap-2 whitespace-nowrap border border-purple-400/30 px-4 text-[9px] font-black uppercase tracking-widest text-purple-300 transition-colors hover:bg-purple-500 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-purple-300 active:bg-purple-700"><Popcorn className="h-4 w-4" /> Đặt thêm bắp nước</button>
      </div>

      <div className="space-y-8">
        {groupedOrders.pending.length > 0 && (
          <section aria-labelledby="food-orders-pending">
            <div className="mb-3 flex items-center justify-between border-b border-amber-400/20 pb-2">
              <h2 id="food-orders-pending" className="text-[10px] font-black uppercase tracking-[0.16em] text-amber-300">Cần hoàn tất</h2>
              <span className="font-mono text-[9px] text-neutral-500">{groupedOrders.pending.length} đơn</span>
            </div>
            <div className="grid items-stretch gap-4 lg:grid-cols-2">{groupedOrders.pending.map(renderOrderCard)}</div>
          </section>
        )}

        {groupedOrders.ready.length > 0 && (
          <section aria-labelledby="food-orders-ready">
            <div className="mb-3 flex items-center justify-between border-b border-emerald-400/20 pb-2">
              <h2 id="food-orders-ready" className="text-[10px] font-black uppercase tracking-[0.16em] text-emerald-300">Sẵn sàng nhận món</h2>
              <span className="font-mono text-[9px] text-neutral-500">{groupedOrders.ready.length} đơn</span>
            </div>
            <div className="grid items-stretch gap-4 lg:grid-cols-2">{groupedOrders.ready.map(renderOrderCard)}</div>
          </section>
        )}

        {groupedOrders.history.length > 0 && (
          <section aria-labelledby="food-orders-history">
            <div className="mb-3 flex items-center justify-between border-b border-white/10 pb-2">
              <h2 id="food-orders-history" className="text-[10px] font-black uppercase tracking-[0.16em] text-neutral-400">Lịch sử</h2>
              <span className="font-mono text-[9px] text-neutral-500">{groupedOrders.history.length} đơn</span>
            </div>
            <div className="grid items-stretch gap-4 lg:grid-cols-2">{groupedOrders.history.map(renderOrderCard)}</div>
          </section>
        )}
      </div>
    </div>
  );
}
