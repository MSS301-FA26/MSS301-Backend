import React, { useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronLeft, ChevronRight, RefreshCw, Ticket, Wallet, X } from 'lucide-react';
import { adminService } from '../../../services/adminService';

// Trạng thái vé còn có thể hủy. Vé PAID khi hủy sẽ được backend hoàn tiền về CineWallet.
const CANCELLABLE_STATUSES = ['PAID', 'HOLDING', 'PENDING_PAYMENT'];

const STATUS_FILTERS = ['ALL', 'PAID', 'USED', 'HOLDING', 'PENDING_PAYMENT', 'EXPIRED', 'CANCELLED', 'REFUNDED'];

const STATUS_META = {
  PAID: { label: 'Đã thanh toán', className: 'border-emerald-500/30 bg-emerald-950/30 text-emerald-300' },
  USED: { label: 'Đã check-in', className: 'border-sky-500/30 bg-sky-950/30 text-sky-300' },
  HOLDING: { label: 'Đang giữ', className: 'border-amber-500/30 bg-amber-950/30 text-amber-300' },
  PENDING_PAYMENT: { label: 'Chờ thanh toán', className: 'border-amber-500/30 bg-amber-950/30 text-amber-300' },
  EXPIRED: { label: 'Hết hạn', className: 'border-neutral-600 bg-neutral-900 text-neutral-200' },
  CANCELLED: { label: 'Đã hủy', className: 'border-rose-500/30 bg-rose-950/30 text-rose-300' },
  REFUNDED: { label: 'Đã hoàn tiền', className: 'border-purple-500/30 bg-purple-950/30 text-purple-300' },
};

const TICKET_TYPE_LABELS = { ADULT: 'Người lớn', CHILD: 'Trẻ em', STUDENT: 'Sinh viên' };

const formatVnd = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`;

/**
 * Panel quản lý vé — xem toàn bộ booking, ghế, loại vé, bắp nước kèm theo.
 * Row có thể expand để xem vé từng ghế (ticketCode), bắp nước, và flag dữ liệu thiếu.
 * Bộ lọc trạng thái + phân trang 15 đơn/trang.
 *
 * @param {{ getAdminToken: () => string|null, showToast: (msg: string) => void }} ctx
 */
export default function AdminTicketsPanel({ ctx }) {
  const { getAdminToken, showToast } = ctx;
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [bookings, setBookings] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [expandedId, setExpandedId] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [isCancelling, setIsCancelling] = useState(false);

  const loadBookings = async (nextPage = 0, status = statusFilter) => {
    const token = getAdminToken();
    if (!token) return;
    setIsLoading(true);
    try {
      const params = { page: nextPage, size: 15 };
      if (status !== 'ALL') params.status = status;
      const data = await adminService.getAdminBookings(token, params);
      setBookings(data.items || []);
      setPage(data.page || 0);
      setTotalPages(data.totalPages || 1);
      setTotalItems(data.totalItems || 0);
      setExpandedId(null);
    } catch (err) {
      showToast(err?.message || 'Không thể tải danh sách vé.');
      setBookings([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadBookings(0); }, []);

  const openCancel = (booking) => { setCancelTarget(booking); setCancelReason(''); };
  const closeCancel = () => { if (!isCancelling) { setCancelTarget(null); setCancelReason(''); } };

  const handleConfirmCancel = async () => {
    if (!cancelTarget) return;
    const token = getAdminToken();
    if (!token) return;
    setIsCancelling(true);
    try {
      await adminService.cancelBookingAdmin(token, cancelTarget.id, cancelReason.trim());
      const wasPaid = cancelTarget.status === 'PAID';
      showToast(wasPaid
        ? `Đã hủy vé ${cancelTarget.bookingCode} và hoàn ${formatVnd(cancelTarget.totalAmount)} về ví CineWallet của khách.`
        : `Đã hủy vé ${cancelTarget.bookingCode}.`);
      setCancelTarget(null);
      setCancelReason('');
      loadBookings(page);
    } catch (err) {
      showToast(err?.message || 'Không thể hủy vé.');
    } finally {
      setIsCancelling(false);
    }
  };

  const pageStats = useMemo(() => {
    const seats = bookings.reduce((sum, b) => sum + (b.seats?.length || 0), 0);
    const foods = bookings.reduce((sum, b) => sum + (b.foods || []).reduce((s, f) => s + (f.quantity || 0), 0), 0);
    const revenue = bookings
      .filter((b) => ['PAID', 'USED'].includes(b.status))
      .reduce((sum, b) => sum + Number(b.totalAmount || 0), 0);
    return { seats, foods, revenue };
  }, [bookings]);

  const missingDataFlags = (b) => {
    const flags = [];
    if (['PAID', 'USED'].includes(b.status)) {
      if (!b.qrCode) flags.push('Thiếu QR');
      if (!(b.seats || []).some((s) => s.ticketCode)) flags.push('Thiếu mã vé ghế');
      if (!(b.tickets || []).length) flags.push('Thiếu loại vé');
      if (!b.paymentAccount) flags.push('Thiếu TK thanh toán');
    }
    if (!b.customerName) flags.push('Thiếu tên KH');
    if (!b.customerPhone) flags.push('Thiếu SĐT');
    return flags;
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[9px] font-sans font-black uppercase tracking-[0.25em] text-neutral-300">Quản lý rạp</p>
          <h2 className="mt-1 flex items-center gap-2 text-sm font-sans font-black uppercase tracking-wide text-neutral-200">
            <Ticket className="h-5 w-5 text-amber-500" /> Quản Lý Vé
          </h2>
          <p className="mt-1 text-xs text-neutral-300">Vé đã bán gồm những gì, thiếu dữ liệu gì, kèm số lượng bắp nước đi theo từng đơn.</p>
        </div>
        <button
          onClick={() => loadBookings(page)}
          disabled={isLoading}
          className="flex items-center gap-2 border border-white/10 bg-black px-4 py-2.5 text-[10px] font-sans font-black uppercase tracking-widest text-neutral-300 transition hover:border-amber-500/50 hover:text-white disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} /> Làm mới
        </button>
      </div>

      {/* Stat tiles của trang hiện tại */}
      <div className="grid grid-cols-3 gap-3">
        <div className="border border-white/[0.12] bg-white/[0.04] p-4">
          <p className="text-[9px] font-black uppercase tracking-widest text-neutral-300">Vé (ghế) trang này</p>
          <p className="mt-1 text-xl font-black font-mono text-white">{pageStats.seats}</p>
        </div>
        <div className="border border-white/[0.12] bg-white/[0.04] p-4">
          <p className="text-[9px] font-black uppercase tracking-widest text-neutral-300">Bắp nước bán kèm</p>
          <p className="mt-1 text-xl font-black font-mono text-amber-400">{pageStats.foods}</p>
        </div>
        <div className="border border-white/[0.12] bg-white/[0.04] p-4">
          <p className="text-[9px] font-black uppercase tracking-widest text-neutral-300">Doanh thu (đã trả)</p>
          <p className="mt-1 text-xl font-black font-mono text-emerald-400">{formatVnd(pageStats.revenue)}</p>
        </div>
      </div>

      {/* Filter chips theo trạng thái */}
      <div className="flex flex-wrap gap-2">
        {STATUS_FILTERS.map((status) => (
          <button
            key={status}
            onClick={() => { setStatusFilter(status); loadBookings(0, status); }}
            className={`border px-3 py-1.5 text-[9px] font-sans font-black uppercase tracking-widest transition ${
              statusFilter === status
                ? 'border-amber-500/60 bg-amber-500/15 text-amber-300'
                : 'border-white/10 bg-black text-neutral-200 hover:border-white/30 hover:text-white'
            }`}
          >
            {status === 'ALL' ? 'Tất cả' : (STATUS_META[status]?.label || status)}
          </button>
        ))}
      </div>

      <div className="overflow-x-auto border border-white/10 bg-[#050505]">
        <table className="min-w-full divide-y divide-white/10 text-left text-xs font-sans">
          <thead className="bg-[#0B0B0B] text-[9px] uppercase tracking-[0.15em] text-neutral-300 font-bold">
            <tr>
              <th className="px-4 py-3.5">Mã đơn</th>
              <th className="px-4 py-3.5">Phim / Suất</th>
              <th className="px-4 py-3.5">Ghế · Loại vé</th>
              <th className="px-4 py-3.5">Người mua</th>
              <th className="px-4 py-3.5">🍿</th>
              <th className="px-4 py-3.5">Tổng tiền</th>
              <th className="px-4 py-3.5">Trạng thái</th>
              <th className="px-4 py-3.5" />
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5 text-neutral-300">
            {isLoading ? (
              <tr><td colSpan={8} className="px-4 py-12 text-center text-neutral-300">Đang tải danh sách vé…</td></tr>
            ) : bookings.length === 0 ? (
              <tr><td colSpan={8} className="px-4 py-12 text-center text-neutral-300">Không có vé nào khớp bộ lọc.</td></tr>
            ) : bookings.map((b) => {
              // USED + suất chưa kết thúc = vừa check-in (đang xem); kết thúc rồi = đã sử dụng
              const isWatching = b.status === 'USED' && b.showtimeEnd && Date.now() < new Date(b.showtimeEnd).getTime();
              const statusMeta = b.status === 'USED'
                ? (isWatching
                  ? { label: 'Đã check-in', className: 'border-sky-500/30 bg-sky-950/30 text-sky-300' }
                  : { label: 'Đã sử dụng', className: 'border-neutral-600 bg-neutral-900 text-neutral-200' })
                : (STATUS_META[b.status] || { label: b.status, className: 'border-white/10 bg-neutral-900 text-neutral-300' });
              const foodQty = (b.foods || []).reduce((s, f) => s + (f.quantity || 0), 0);
              const isExpanded = expandedId === b.id;
              const flags = missingDataFlags(b);
              return (
                <React.Fragment key={b.id}>
                  <tr className="cursor-pointer transition hover:bg-white/5" onClick={() => setExpandedId(isExpanded ? null : b.id)}>
                    <td className="whitespace-nowrap px-4 py-3.5 font-mono text-[11px] font-bold text-white">{b.bookingCode}</td>
                    <td className="px-4 py-3.5">
                      <p className="max-w-[180px] truncate font-bold text-white">{b.movieTitle}</p>
                      <p className="text-[10px] text-neutral-300">
                        {b.roomName} · {b.showtimeStart ? new Date(b.showtimeStart).toLocaleString('vi-VN') : '—'}
                      </p>
                    </td>
                    <td className="px-4 py-3.5">
                      <p className="font-mono text-[11px] text-amber-400">{(b.seats || []).map((s) => `${s.rowLabel}${s.seatNumber}`).join(', ') || '—'}</p>
                      <p className="text-[10px] text-neutral-300">
                        {(b.tickets || []).map((t) => `${TICKET_TYPE_LABELS[t.ticketType] || t.ticketType} ×${t.quantity}`).join(', ') || 'Chưa ghi loại vé'}
                      </p>
                    </td>
                    <td className="px-4 py-3.5">
                      <p className="max-w-[150px] truncate font-bold text-white">{b.customerName || '—'}</p>
                      <p className="max-w-[150px] truncate text-[10px] text-neutral-300">{b.userEmail || b.customerPhone || '—'}</p>
                    </td>
                    <td className="px-4 py-3.5 font-mono font-bold text-amber-400">{foodQty || '—'}</td>
                    <td className="whitespace-nowrap px-4 py-3.5 font-mono font-bold text-white">{formatVnd(b.totalAmount)}</td>
                    <td className="px-4 py-3.5">
                      <span className={`inline-block border px-2 py-1 text-[9px] font-black uppercase tracking-wider ${statusMeta.className}`}>{statusMeta.label}</span>
                    </td>
                    <td className="px-4 py-3.5"><ChevronDown className={`h-4 w-4 text-neutral-300 transition-transform ${isExpanded ? 'rotate-180' : ''}`} /></td>
                  </tr>
                  {isExpanded && (
                    <tr className="bg-black/40">
                      <td colSpan={8} className="px-6 py-4">
                        <div className="grid gap-4 lg:grid-cols-3">
                          <div>
                            <p className="mb-2 text-[9px] font-black uppercase tracking-widest text-neutral-300">Vé từng ghế</p>
                            <div className="space-y-1">
                              {(b.seats || []).map((s) => (
                                <div key={s.seatId} className="flex items-center justify-between gap-2 text-[11px]">
                                  <span className="font-mono text-white">{s.rowLabel}{s.seatNumber}</span>
                                  <span className="text-neutral-300">{TICKET_TYPE_LABELS[s.ticketType] || '—'}</span>
                                  <span className="font-mono text-neutral-200">{s.ticketCode || 'chưa có mã'}</span>
                                  <span className="font-mono text-white">{formatVnd(s.unitPrice)}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                          <div>
                            <p className="mb-2 text-[9px] font-black uppercase tracking-widest text-neutral-300">Bắp nước</p>
                            {(b.foods || []).length === 0 ? (
                              <p className="text-[11px] text-neutral-200">Không mua bắp nước</p>
                            ) : (b.foods || []).map((f, i) => (
                              <div key={i} className="flex items-center justify-between text-[11px]">
                                <span className="truncate text-neutral-300">{f.name} ×{f.quantity}</span>
                                <span className="font-mono text-neutral-200">{formatVnd(f.totalPrice)}</span>
                              </div>
                            ))}
                            <p className="mt-3 text-[10px] text-neutral-300">
                              Thanh toán: {b.paidAt ? new Date(b.paidAt).toLocaleString('vi-VN') : '—'}
                              {b.paymentAccount ? ` · ${b.paymentAccount}` : ''}
                              {b.checkedInAt ? ` · Check-in: ${new Date(b.checkedInAt).toLocaleString('vi-VN')}` : ''}
                            </p>
                          </div>
                          <div>
                            <p className="mb-2 text-[9px] font-black uppercase tracking-widest text-neutral-300">Dữ liệu vé</p>
                            {flags.length === 0 ? (
                              <span className="inline-block border border-emerald-500/30 bg-emerald-950/30 px-2 py-1 text-[9px] font-black uppercase tracking-wider text-emerald-300">✓ Đầy đủ dữ liệu</span>
                            ) : (
                              <div className="flex flex-wrap gap-1.5">
                                {flags.map((flag) => (
                                  <span key={flag} className="border border-rose-500/30 bg-rose-950/30 px-2 py-1 text-[9px] font-black uppercase tracking-wider text-rose-300">{flag}</span>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                        {CANCELLABLE_STATUSES.includes(b.status) && (
                          <div className="mt-4 flex items-center justify-end border-t border-white/10 pt-3">
                            <button
                              onClick={(e) => { e.stopPropagation(); openCancel(b); }}
                              className={`flex items-center gap-2 border px-4 py-2 text-[10px] font-sans font-black uppercase tracking-widest transition ${
                                b.status === 'PAID'
                                  ? 'border-purple-500/40 bg-purple-950/30 text-purple-300 hover:border-purple-400 hover:text-white'
                                  : 'border-rose-500/40 bg-rose-950/30 text-rose-300 hover:border-rose-400 hover:text-white'
                              }`}
                            >
                              {b.status === 'PAID'
                                ? (<><Wallet className="h-3.5 w-3.5" /> Hủy &amp; hoàn về ví</>)
                                : (<><X className="h-3.5 w-3.5" /> Hủy vé</>)}
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-[10px] font-sans font-black uppercase tracking-widest text-neutral-300">
        <span>{totalItems} đơn · Trang {page + 1}/{totalPages}</span>
        <div className="flex gap-2">
          <button
            disabled={page <= 0 || isLoading}
            onClick={() => loadBookings(page - 1)}
            className="flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-amber-500/50 disabled:opacity-30"
          >
            <ChevronLeft className="h-3.5 w-3.5" /> Trước
          </button>
          <button
            disabled={page >= totalPages - 1 || isLoading}
            onClick={() => loadBookings(page + 1)}
            className="flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-amber-500/50 disabled:opacity-30"
          >
            Sau <ChevronRight className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {cancelTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={closeCancel}>
          <div className="w-full max-w-md border border-white/[0.12] bg-[#0d0d0d] p-6" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-start justify-between">
              <div>
                <p className="text-[9px] font-black uppercase tracking-[0.25em] text-neutral-300">Quản lý vé</p>
                <h3 className="mt-1 text-lg font-sans font-black uppercase tracking-wide text-white">
                  {cancelTarget.status === 'PAID' ? 'Hủy & hoàn tiền' : 'Hủy vé'}
                </h3>
              </div>
              <button onClick={closeCancel} disabled={isCancelling} className="text-neutral-300 transition hover:text-white disabled:opacity-40">
                <X className="h-5 w-5" />
              </button>
            </div>

            <p className="mt-3 text-xs text-neutral-200">
              Vé <span className="font-mono font-bold text-white">{cancelTarget.bookingCode}</span> · {formatVnd(cancelTarget.totalAmount)}
            </p>

            {cancelTarget.status === 'PAID' ? (
              <div className="mt-3 flex items-start gap-2 border border-purple-500/30 bg-purple-950/20 p-3 text-[11px] text-purple-200">
                <Wallet className="mt-0.5 h-4 w-4 shrink-0 text-purple-300" />
                <span>Số tiền {formatVnd(cancelTarget.totalAmount)} sẽ được hoàn vào ví CineWallet của khách. Khách có thể rút về ngân hàng từ ví.</span>
              </div>
            ) : (
              <p className="mt-3 text-[11px] text-neutral-300">Vé chưa thanh toán — hủy sẽ giải phóng ghế, không phát sinh hoàn tiền.</p>
            )}

            <label className="mt-4 block text-[9px] font-black uppercase tracking-widest text-neutral-300">Lý do (tùy chọn)</label>
            <textarea
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              rows={3}
              maxLength={500}
              placeholder="Ví dụ: khách yêu cầu hủy, đặt nhầm suất…"
              className="mt-1.5 w-full resize-none border border-white/10 bg-black px-3 py-2 text-xs text-white outline-none transition focus:border-amber-500/50"
            />

            <div className="mt-5 flex justify-end gap-2">
              <button
                onClick={closeCancel}
                disabled={isCancelling}
                className="border border-white/10 bg-black px-4 py-2.5 text-[10px] font-sans font-black uppercase tracking-widest text-neutral-300 transition hover:border-white/30 hover:text-white disabled:opacity-40"
              >
                Đóng
              </button>
              <button
                onClick={handleConfirmCancel}
                disabled={isCancelling}
                className={`flex items-center gap-2 border px-4 py-2.5 text-[10px] font-sans font-black uppercase tracking-widest transition disabled:opacity-40 ${
                  cancelTarget.status === 'PAID'
                    ? 'border-purple-500/50 bg-purple-500/15 text-purple-200 hover:border-purple-400 hover:text-white'
                    : 'border-rose-500/50 bg-rose-500/15 text-rose-200 hover:border-rose-400 hover:text-white'
                }`}
              >
                {isCancelling
                  ? 'Đang xử lý…'
                  : (cancelTarget.status === 'PAID' ? 'Xác nhận hoàn tiền' : 'Xác nhận hủy')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
