import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertCircle,
  Camera,
  CameraOff,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Clock3,
  Printer,
  QrCode,
  RefreshCw,
  ScanLine,
  Search,
  ShieldCheck,
  Ticket,
  Wallet,
  XCircle,
} from 'lucide-react';
import jsQR from 'jsqr';
import { motion } from 'motion/react';
import { getStoredAuth } from '../../services/authService';
import { staffService } from '../../services/staffService';
import { useAuthStore } from '../../stores/useAuthStore';
import StaffWalletPanel from './StaffWalletPanel';

const FOOD_PAGE_SIZE = 10;
const BOOKINGS_PAGE_SIZE = 8;
const RECENT_BOOKINGS_LIMIT = 50;
const CHECK_IN_LEAD_MINUTES = 30;

const FOOD_STATUS_META = {
  ACTIVE: { label: 'Mở bán', className: 'bg-emerald-400/10 text-emerald-300' },
  LOW_STOCK: { label: 'Sắp hết', className: 'bg-purple-500/10 text-purple-300' },
  OUT_OF_STOCK: { label: 'Hết', className: 'bg-rose-500/10 text-rose-300' },
  INACTIVE: { label: 'Hết', className: 'bg-rose-500/10 text-rose-300' },
};

const getFoodStatusMeta = (status) => FOOD_STATUS_META[status] || FOOD_STATUS_META.OUT_OF_STOCK;

const normalizeSearchText = (value) => String(value || '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .toLowerCase()
  .trim();

const formatDateTime = (value) => {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('vi-VN');
};

const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')}d`;

const formatSeats = (booking) => {
  const seats = booking?.seats || [];
  if (!seats.length) return 'Chưa có ghế';
  return seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(', ');
};

const getCheckInOpenAt = (booking) => {
  if (!booking?.showtimeStart) return null;
  const showtimeStart = new Date(booking.showtimeStart);
  if (Number.isNaN(showtimeStart.getTime())) return null;
  return new Date(showtimeStart.getTime() - CHECK_IN_LEAD_MINUTES * 60 * 1000);
};

const isShowtimeOver = (booking) => {
  if (!booking?.showtimeEnd) return false;
  const endTime = new Date(booking.showtimeEnd);
  if (Number.isNaN(endTime.getTime())) return false;
  return Date.now() > endTime.getTime();
};

const isBookingCheckInOpen = (booking) => {
  if (String(booking?.status || '').toUpperCase() !== 'PAID') return false;
  if (isShowtimeOver(booking)) return false;
  const checkInOpenAt = getCheckInOpenAt(booking);
  return !checkInOpenAt || Date.now() >= checkInOpenAt.getTime();
};

const getCheckInWindowMessage = (booking) => {
  if (isShowtimeOver(booking)) {
    const endTime = booking.showtimeEnd ? new Date(booking.showtimeEnd).toLocaleString('vi-VN') : '';
    return `Suất chiếu đã kết thúc${endTime ? ` lúc ${endTime}` : ''}. Không thể check-in.`;
  }
  const checkInOpenAt = getCheckInOpenAt(booking);
  if (!checkInOpenAt) return 'Check-in chỉ mở trong vòng 30 phút trước giờ chiếu.';
  return `Check-in chỉ mở từ ${checkInOpenAt.toLocaleString('vi-VN')} (30 phút trước giờ chiếu).`;
};

const parseQrOrBookingCode = (value) => {
  const trimmed = String(value || '').trim();
  if (!trimmed) return { qrCode: '', bookingCode: '' };
  if (trimmed.toUpperCase().startsWith('CINEAI:')) return { qrCode: trimmed, bookingCode: '' };
  return { qrCode: '', bookingCode: trimmed };
};

const isFoodPickupCode = (value) => /^(CINEAI:FOOD:|FO[A-Z0-9]{6,}$)/i.test(String(value || '').trim());

const getBookingStatusMeta = (status = '') => {
  const normalized = String(status).toUpperCase();
  if (normalized === 'PAID') {
    return {
      label: 'Sẵn sàng check-in',
      className: 'border-purple-400/40 bg-purple-400/10 text-purple-300',
    };
  }
  if (normalized === 'USED') {
    return {
      label: 'Đã check-in',
      className: 'border-emerald-400/40 bg-emerald-400/10 text-emerald-300',
    };
  }
  return {
    label: normalized || 'Không hợp lệ',
    className: 'border-rose-400/40 bg-rose-500/10 text-rose-300',
  };
};

function StatusBadge({ status }) {
  const meta = getBookingStatusMeta(status);
  return (
    <span className={`inline-flex items-center gap-1.5 border px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${meta.className}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {meta.label}
    </span>
  );
}

const TICKET_TYPE_BADGES = {
  STUDENT: { label: 'SINH VIÊN — KIỂM TRA THẺ', className: 'border-amber-400/50 bg-amber-400/10 text-amber-300' },
  CHILD: { label: 'TRẺ EM', className: 'border-sky-400/40 bg-sky-400/10 text-sky-300' },
  ADULT: { label: 'NGƯỜI LỚN', className: 'border-neutral-700 bg-neutral-900 text-neutral-300' },
};

const getTicketTypeBadge = (ticketType) => TICKET_TYPE_BADGES[String(ticketType || 'ADULT').toUpperCase()] || TICKET_TYPE_BADGES.ADULT;

function ResultCard({ result, selectedTicketCodes = [], onToggleSeat, onConfirmSeats, onConfirmFood, isCheckingIn, onOpenCounterSale }) {
  if (!result) {
    return (
      <div className="flex min-h-[280px] flex-col items-center justify-center rounded-2xl border border-dashed border-white/15 bg-gradient-to-b from-[#0c0e12] to-[#050608] p-6 text-center shadow-xl">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl border border-emerald-400/20 bg-emerald-500/10 text-emerald-400 shadow-[0_0_20px_rgba(16,185,129,0.15)]">
          <ShieldCheck className="h-7 w-7 text-emerald-400" />
        </div>
        <p className="mt-4 text-xs font-black uppercase tracking-widest text-neutral-300">Chưa có kết quả</p>
        <p className="mt-2 max-w-sm text-xs leading-6 text-neutral-400">
          Nhập mã QR hoặc mã booking để hệ thống kiểm tra dữ liệu thật từ backend.
        </p>
      </div>
    );
  }

  const Icon = result.type === 'success' ? CheckCircle2 : result.type === 'warning' ? AlertCircle : XCircle;
  const color = result.type === 'success' ? 'text-emerald-300' : result.type === 'warning' ? 'text-purple-300' : 'text-rose-300';
  const booking = result.booking;
  const foodOrder = result.foodOrder;
  const seats = Array.isArray(booking?.seats) ? booking.seats : [];
  const hasSeatTickets = seats.some((seat) => seat.ticketCode);
  const showtimeOver = isShowtimeOver(booking);
  const canPartialCheckIn = booking && booking.status === 'PAID' && isBookingCheckInOpen(booking) && hasSeatTickets;
  const canSellFood = booking && ['PAID', 'USED'].includes(booking.status)
    && (!booking.showtimeEnd || new Date(booking.showtimeEnd).getTime() > Date.now());

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-2xl border border-white/10 bg-gradient-to-b from-[#0c0e12] to-[#050608] p-5 shadow-2xl"
    >
      <div className="flex items-start gap-3">
        <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-white/10 bg-black/60 ${color}`}>
          <Icon className="h-5 w-5" />
        </div>
        <div className="min-w-0">
          <h3 className="text-sm font-black uppercase tracking-wider text-white">{result.title}</h3>
          <p className="mt-1 text-xs leading-6 text-neutral-400">{result.message}</p>
        </div>
      </div>

      {foodOrder && (
        <div className="mt-5 border border-purple-400/25 bg-black p-4">
          <div className="flex flex-wrap items-start justify-between gap-3 border-b border-white/10 pb-3">
            <div>
              <p className="text-[9px] font-black uppercase tracking-[0.2em] text-purple-300">Đơn bắp nước</p>
              <p className="mt-1 font-mono text-sm font-black text-white">{foodOrder.orderCode}</p>
            </div>
            <span className={`border px-2.5 py-1 text-[9px] font-black uppercase tracking-wider ${foodOrder.status === 'PICKED_UP' ? 'border-neutral-700 text-neutral-400' : 'border-emerald-400/40 bg-emerald-400/10 text-emerald-300'}`}>
              {foodOrder.status === 'PICKED_UP' ? 'Đã nhận món' : 'Sẵn sàng giao'}
            </span>
          </div>
          <div className="divide-y divide-white/5">
            {(foodOrder.items || []).map((item, index) => (
              <div key={`${item.name}-${index}`} className="grid grid-cols-[1fr_auto_auto] gap-3 py-3 text-xs">
                <span className="font-bold text-white">{item.name}</span>
                <span className="font-mono text-neutral-500">×{item.quantity}</span>
                <span className="font-mono font-black text-purple-200">{formatCurrency(item.totalPrice)}</span>
              </div>
            ))}
          </div>
          <div className="flex items-center justify-between border-t border-white/10 pt-3">
            <span className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Tổng thanh toán</span>
            <strong className="font-mono text-lg text-white">{formatCurrency(foodOrder.totalAmount)}</strong>
          </div>
          {foodOrder.pickedUpAt && <p className="mt-3 text-[10px] text-neutral-500">Đã giao lúc {formatDateTime(foodOrder.pickedUpAt)}</p>}
          {foodOrder.status === 'PAID' && (
            <button
              type="button"
              onClick={() => onConfirmFood?.(foodOrder.orderCode)}
              disabled={isCheckingIn}
              className="mt-4 flex min-h-11 w-full items-center justify-center gap-2 whitespace-nowrap bg-emerald-400 px-5 text-[10px] font-black uppercase tracking-[0.16em] text-black transition-colors hover:bg-emerald-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white active:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isCheckingIn ? <RefreshCw className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />} Xác nhận giao món
            </button>
          )}
        </div>
      )}

      {booking && (
        <div className="mt-5 space-y-3 border border-neutral-800 bg-black p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-mono text-sm font-black text-white">{booking.bookingCode}</p>
              <p className="mt-1 text-[10px] font-bold uppercase tracking-widest text-neutral-500">Booking #{booking.id}</p>
            </div>
            <StatusBadge status={booking.status} />
          </div>
          <div className="grid gap-3 text-xs text-neutral-400 sm:grid-cols-2">
            <div><span className="font-black text-white">Khách:</span> {booking.customerName || '—'}</div>
            <div><span className="font-black text-white">SĐT:</span> {booking.customerPhone || '—'}</div>
            <div><span className="font-black text-white">Phim:</span> {booking.movieTitle}</div>
            <div><span className="font-black text-white">Phòng:</span> {booking.roomName}</div>
            <div><span className="font-black text-white">Giờ chiếu:</span> {formatDateTime(booking.showtimeStart)}</div>
            <div><span className="font-black text-white">Kết thúc:</span> {booking.showtimeEnd ? formatDateTime(booking.showtimeEnd) : '—'}</div>
            <div><span className="font-black text-white">Tổng tiền:</span> {Number(booking.totalAmount || 0).toLocaleString('vi-VN')}đ</div>
          </div>

          {/* Showtime timeline bar */}
          {booking.showtimeStart && booking.showtimeEnd && (() => {
            const start = new Date(booking.showtimeStart);
            const end = new Date(booking.showtimeEnd);
            const now = Date.now();
            const totalMs = end.getTime() - start.getTime();
            const elapsedMs = Math.min(Math.max(now - start.getTime(), 0), totalMs);
            const progressPct = totalMs > 0 ? Math.round((elapsedMs / totalMs) * 100) : 0;
            const durationMin = Math.round(totalMs / 60000);
            const isOver = now > end.getTime();
            const isNotStarted = now < start.getTime();
            return (
              <div className="border-t border-white/5 pt-3">
                <div className="flex items-center justify-between text-[9px] font-bold uppercase tracking-widest text-neutral-500 mb-1.5">
                  <span className="text-purple-400">
                    {start.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                    <span className="text-neutral-600 font-normal ml-1">{start.toLocaleDateString('vi-VN')}</span>
                  </span>
                  <span className="text-neutral-600">{durationMin} phút</span>
                  <span className={isOver ? 'text-rose-400' : 'text-emerald-400'}>
                    {end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                    <span className="text-neutral-600 font-normal ml-1">{end.toLocaleDateString('vi-VN')}</span>
                  </span>
                </div>
                <div className="relative h-1.5 w-full rounded-full bg-neutral-800 overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all ${isOver ? 'bg-rose-500' : isNotStarted ? 'bg-neutral-600' : 'bg-purple-500'}`}
                    style={{ width: `${progressPct}%` }}
                  />
                </div>
                <div className="mt-1.5 text-center text-[8px] font-bold uppercase tracking-widest">
                  {isOver
                    ? <span className="text-rose-400">⛔ Đã kết thúc chiếu</span>
                    : isNotStarted
                      ? <span className="text-neutral-500">Chưa bắt đầu chiếu</span>
                      : <span className="text-purple-400">🎬 Đang chiếu · {progressPct}%</span>}
                </div>
              </div>
            );
          })()}

          {/* ── ORDER DETAIL RECEIPT ── */}
          {(() => {
            const ticketRows = seats.map((seat) => ({
              label: `Ghế ${seat.rowLabel}${seat.seatNumber}`,
              type: seat.ticketType,
              unitPrice: Number(seat.unitPrice ?? seat.price ?? 0),
              qty: 1,
            }));
            const foodRows = Array.isArray(booking?.foods) ? booking.foods.map((f) => ({
              name: f.name,
              unitPrice: Number(f.unitPrice || 0),
              qty: Number(f.quantity || 1),
              total: Number(f.totalPrice || 0),
            })) : [];
            const ticketTotal = ticketRows.reduce((s, r) => s + r.unitPrice * r.qty, 0);
            const foodTotal = foodRows.reduce((s, r) => s + r.total, 0);
            const discount = Number(booking.discountAmount || 0);
            const grandTotal = Number(booking.totalAmount || 0);
            const TICKET_TYPE_LABELS = { ADULT: 'Người lớn', STUDENT: 'Sinh viên', CHILD: 'Trẻ em' };

            return (
              <div className="border-t border-white/5 pt-3">
                {/* Header */}
                <div className="flex items-center justify-between mb-3">
                  <p className="text-[9px] font-black uppercase tracking-[0.2em] text-sky-400">📋 Chi tiết đơn hàng</p>
                  <span className="text-[8px] font-mono text-neutral-600">#{booking.bookingCode}</span>
                </div>

                {/* Column headers */}
                <div className="grid grid-cols-[1fr_auto_auto_auto] gap-x-3 text-[8px] font-black uppercase tracking-widest text-neutral-600 pb-1.5 border-b border-white/5 mb-1">
                  <span>Hạng mục</span>
                  <span className="text-right">Đơn giá</span>
                  <span className="text-right">SL</span>
                  <span className="text-right">Thành tiền</span>
                </div>

                {/* Seat ticket rows */}
                {ticketRows.length > 0 && (
                  <div className="space-y-0.5 mb-1">
                    <div className="text-[8px] font-bold uppercase tracking-widest text-neutral-500 py-1">🎫 Vé xem phim</div>
                    {ticketRows.map((row, i) => (
                      <div key={i} className="grid grid-cols-[1fr_auto_auto_auto] gap-x-3 items-center py-1.5 border border-neutral-900 bg-[#0a0a0a] px-2">
                        <div className="min-w-0">
                          <span className="text-[11px] font-black text-white font-mono">{row.label}</span>
                          <span className={`ml-1.5 text-[8px] font-bold uppercase px-1 py-0.5 border ${
                            row.type === 'STUDENT' ? 'border-sky-500/30 text-sky-400 bg-sky-500/10'
                            : row.type === 'CHILD' ? 'border-amber-500/30 text-amber-400 bg-amber-500/10'
                            : 'border-neutral-700 text-neutral-400 bg-neutral-900'
                          }`}>
                            {TICKET_TYPE_LABELS[row.type?.toUpperCase()] || 'Người lớn'}
                          </span>
                        </div>
                        <span className="text-right text-[10px] font-mono text-neutral-400 tabular-nums">
                          {row.unitPrice > 0 ? `${row.unitPrice.toLocaleString('vi-VN')}đ` : '—'}
                        </span>
                        <span className="text-right text-[10px] font-mono text-neutral-500">×{row.qty}</span>
                        <span className="text-right text-[11px] font-black font-mono text-white tabular-nums">
                          {row.unitPrice > 0 ? `${(row.unitPrice * row.qty).toLocaleString('vi-VN')}đ` : '—'}
                        </span>
                      </div>
                    ))}
                    {/* Ticket subtotal */}
                    <div className="grid grid-cols-[1fr_auto] gap-x-3 items-center px-2 py-1">
                      <span className="text-[8px] font-bold text-neutral-600 uppercase tracking-widest">Tổng vé</span>
                      <span className="text-right text-[10px] font-mono font-black text-neutral-300 tabular-nums">
                        {ticketTotal.toLocaleString('vi-VN')}đ
                      </span>
                    </div>
                  </div>
                )}

                {/* Food rows */}
                {foodRows.length > 0 && (
                  <div className="space-y-0.5 mb-1">
                    <div className="text-[8px] font-bold uppercase tracking-widest text-neutral-500 py-1">🍿 Bắp nước</div>
                    {foodRows.map((row, i) => (
                      <div key={i} className="grid grid-cols-[1fr_auto_auto_auto] gap-x-3 items-center py-1.5 border border-amber-900/20 bg-amber-950/5 px-2">
                        <span className="text-[11px] font-bold text-white truncate">{row.name}</span>
                        <span className="text-right text-[10px] font-mono text-neutral-400 tabular-nums">
                          {row.unitPrice.toLocaleString('vi-VN')}đ
                        </span>
                        <span className="text-right text-[10px] font-mono text-neutral-500">×{row.qty}</span>
                        <span className="text-right text-[11px] font-black font-mono text-amber-300 tabular-nums">
                          {row.total.toLocaleString('vi-VN')}đ
                        </span>
                      </div>
                    ))}
                    {/* Food subtotal */}
                    <div className="grid grid-cols-[1fr_auto] gap-x-3 items-center px-2 py-1">
                      <span className="text-[8px] font-bold text-neutral-600 uppercase tracking-widest">Tổng bắp nước</span>
                      <span className="text-right text-[10px] font-mono font-black text-amber-300 tabular-nums">
                        {foodTotal.toLocaleString('vi-VN')}đ
                      </span>
                    </div>
                  </div>
                )}

                {/* Discount row */}
                {discount > 0 && (
                  <div className="grid grid-cols-[1fr_auto] gap-x-3 items-center px-2 py-1.5 border border-emerald-900/20 bg-emerald-950/10 mb-1">
                    <span className="text-[9px] font-bold text-emerald-400 uppercase tracking-widest">🎁 Giảm giá / CinePoints</span>
                    <span className="text-right text-[11px] font-black font-mono text-emerald-400 tabular-nums">
                      -{discount.toLocaleString('vi-VN')}đ
                    </span>
                  </div>
                )}

                {/* Grand total */}
                <div className="flex items-center justify-between border-t border-white/10 pt-3 mt-1">
                  <div>
                    <p className="text-[9px] font-black uppercase tracking-widest text-white">Tổng cộng hoá đơn</p>
                    <p className="text-[8px] text-neutral-600 mt-0.5">{ticketRows.length} vé{foodRows.length > 0 ? ` · ${foodRows.length} món` : ''}{discount > 0 ? ' · có giảm giá' : ''}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xl font-black font-mono text-white tabular-nums tracking-tight">
                      {grandTotal.toLocaleString('vi-VN')}đ
                    </p>
                  </div>
                </div>
              </div>
            );
          })()}


          {seats.length > 0 && (
            <div className="border-t border-neutral-800 pt-3">
              <div className="flex items-center justify-between">
                <p className="text-[9px] font-black uppercase tracking-[0.2em] text-emerald-400">Check-in từng ghế</p>
                {showtimeOver && (
                  <span className="text-[8px] font-black uppercase tracking-wider text-rose-400 border border-rose-500/30 bg-rose-500/10 px-2 py-0.5">
                    ⛔ Đã quá giờ chiếu
                  </span>
                )}
              </div>
              <div className="mt-2 space-y-1.5">
                {seats.map((seat) => {
                  const badge = getTicketTypeBadge(seat.ticketType);
                  const isCheckedIn = seat.status === 'CHECKED_IN';
                  const selectable = canPartialCheckIn && !isCheckedIn && seat.ticketCode;
                  return (
                    <label
                      key={seat.ticketCode || seat.seatId}
                      className={`flex items-center gap-3 border bg-[#070707] px-3 py-2 ${
                        isCheckedIn
                          ? 'border-emerald-800/40 opacity-70'
                          : showtimeOver
                            ? 'border-rose-900/40 opacity-60 cursor-not-allowed'
                            : selectable
                              ? 'border-neutral-800 cursor-pointer hover:border-emerald-400/50'
                              : 'border-neutral-800 opacity-80'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={selectedTicketCodes.includes(seat.ticketCode)}
                        disabled={!selectable}
                        onChange={() => onToggleSeat?.(seat.ticketCode)}
                        className="h-3.5 w-3.5 accent-emerald-400"
                      />
                      <span className="w-9 font-mono text-xs font-black text-white">{seat.rowLabel}{seat.seatNumber}</span>
                      <span className={`border px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wider ${badge.className}`}>{badge.label}</span>
                      <span className="ml-auto text-[9px] font-black uppercase tracking-wider">
                        {isCheckedIn
                          ? <span className="text-emerald-300">✓ Đã vào {seat.checkedInAt ? new Date(seat.checkedInAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                          : showtimeOver
                            ? <span className="text-rose-400/70">Đã quá giờ chiếu</span>
                            : <span className="text-neutral-500">Chưa vào</span>}
                      </span>
                    </label>
                  );
                })}
              </div>
              {showtimeOver && seats.some((s) => s.status !== 'CHECKED_IN') && (
                <div className="mt-3 flex items-center gap-2 border border-rose-500/20 bg-rose-950/20 px-3 py-2">
                  <XCircle className="h-3.5 w-3.5 shrink-0 text-rose-400" />
                  <p className="text-[9px] font-bold text-rose-400">
                    Suất chiếu đã kết thúc. Không thể check-in các ghế còn lại.
                  </p>
                </div>
              )}
              {canPartialCheckIn && (
                <button
                  type="button"
                  onClick={onConfirmSeats}
                  disabled={isCheckingIn || selectedTicketCodes.length === 0}
                  className="mt-3 flex w-full items-center justify-center gap-2 bg-emerald-400 px-4 py-3 text-[9px] font-black uppercase tracking-widest text-black transition hover:bg-emerald-300 disabled:opacity-40"
                >
                  <CheckCircle2 className="h-3.5 w-3.5" /> Check-in ghế đã chọn ({selectedTicketCodes.length})
                </button>
              )}
            </div>
          )}

          {/* {canSellFood && (
            // <button
            //   type="button"
            //   onClick={onOpenCounterSale}
            //   className="flex w-full items-center justify-center gap-2 border border-purple-400/40 bg-purple-500/10 px-4 py-3 text-[9px] font-black uppercase tracking-widest text-purple-300 transition hover:bg-purple-400 hover:text-black"
            // >
            //   🍿 Bán thêm bắp nước cho booking này
            // </button>
          )} */}
        </div>
      )}

      {result.type === 'success' && (
        <button type="button" className="mt-5 flex w-full items-center justify-center gap-2 border border-emerald-400/40 bg-emerald-400 px-4 py-3 text-[9px] font-black uppercase tracking-widest text-black transition hover:bg-emerald-300">
          <Printer className="h-3.5 w-3.5" /> In vé vào cửa
        </button>
      )}
    </motion.div>
  );
}

export default function StaffCheckInPage() {
  const currentUser = useAuthStore((state) => state.currentUser);
  const [activeSection, setActiveSection] = useState('checkin'); // 'checkin' | 'wallet'
  const [qrCode, setQrCode] = useState('');
  const [bookingCode, setBookingCode] = useState('');
  const [result, setResult] = useState(null);
  const [isCheckingIn, setIsCheckingIn] = useState(false);
  const [isLookingUp, setIsLookingUp] = useState(false);
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const [scannerError, setScannerError] = useState('');
  const [scannerMessage, setScannerMessage] = useState('');
  const [showtimeId, setShowtimeId] = useState('');
  const [isLoadingShowtime, setIsLoadingShowtime] = useState(false);
  const [showtimeError, setShowtimeError] = useState('');
  const [showtimeBookings, setShowtimeBookings] = useState([]);
  const [recentBookings, setRecentBookings] = useState([]);
  const [isLoadingRecentBookings, setIsLoadingRecentBookings] = useState(false);
  const [recentBookingsError, setRecentBookingsError] = useState('');
  const [staffFoodItems, setStaffFoodItems] = useState([]);
  const [staffFoodCombos, setStaffFoodCombos] = useState([]);
  const [staffFoodError, setStaffFoodError] = useState('');
  const [isLoadingStaffFoods, setIsLoadingStaffFoods] = useState(false);
  const [savingStaffFoodKey, setSavingStaffFoodKey] = useState('');
  const [staffFoodPage, setStaffFoodPage] = useState(1);
  const [staffFoodSearch, setStaffFoodSearch] = useState('');
  const [failedRefunds, setFailedRefunds] = useState([]);
  const [failedRefundPage, setFailedRefundPage] = useState(0);
  const [failedRefundTotalPages, setFailedRefundTotalPages] = useState(1);
  const [isLoadingFailedRefunds, setIsLoadingFailedRefunds] = useState(false);
  const [failedRefundError, setFailedRefundError] = useState('');
  const [manualRefundModal, setManualRefundModal] = useState(null);
  const [manualRefundForm, setManualRefundForm] = useState({ refundMethod: 'MANUAL_BANK_TRANSFER', notes: '' });
  const [isSavingManualRefund, setIsSavingManualRefund] = useState(false);
  const [selectedTicketCodes, setSelectedTicketCodes] = useState([]);
  const [counterSaleBooking, setCounterSaleBooking] = useState(null);
  const [counterSaleQuantities, setCounterSaleQuantities] = useState({});
  const [isSavingCounterSale, setIsSavingCounterSale] = useState(false);
  const [counterSaleMessage, setCounterSaleMessage] = useState('');
  const [cashGiven, setCashGiven] = useState(''); // tiền khách đưa (trống = thu đúng số)
  const [counterSaleReceipt, setCounterSaleReceipt] = useState(null); // biên lai sau khi thu
  const [pendingWalletCount, setPendingWalletCount] = useState(0);
  const qrVideoRef = useRef(null);
  const qrCanvasRef = useRef(null);
  const qrStreamRef = useRef(null);
  const qrScanTimerRef = useRef(null);
  const lastScannedQrRef = useRef('');

  const checkPendingWallet = useCallback(async () => {
    const { accessToken } = getStoredAuth();
    if (!accessToken) return;
    try {
      const data = await staffService.getWalletDashboard(accessToken);
      if (data && typeof data.pendingWithdrawalsCount === 'number') {
        setPendingWalletCount(data.pendingWithdrawalsCount);
      }
    } catch (e) {
      // ignore silently
    }
  }, []);

  const pendingCheckinCount = useMemo(() => {
    return (failedRefunds || []).filter((r) => r.status === 'PENDING' || !r.status).length;
  }, [failedRefunds]);

  const visibleBookings = showtimeBookings.length > 0 ? showtimeBookings : recentBookings;
  const isShowingShowtimeBookings = showtimeBookings.length > 0;

  // Phân trang client-side cho bảng booking (cùng pattern với danh sách bắp nước)
  const [bookingsPage, setBookingsPage] = useState(1);
  const bookingsTotalPages = Math.max(1, Math.ceil(visibleBookings.length / BOOKINGS_PAGE_SIZE));
  const safeBookingsPage = Math.min(bookingsPage, bookingsTotalPages);
  const bookingsStartIndex = (safeBookingsPage - 1) * BOOKINGS_PAGE_SIZE;
  const paginatedBookings = visibleBookings.slice(bookingsStartIndex, bookingsStartIndex + BOOKINGS_PAGE_SIZE);
  const bookingsDisplayStart = visibleBookings.length === 0 ? 0 : bookingsStartIndex + 1;
  const bookingsDisplayEnd = Math.min(bookingsStartIndex + BOOKINGS_PAGE_SIZE, visibleBookings.length);

  useEffect(() => {
    setBookingsPage((page) => Math.min(page, bookingsTotalPages));
  }, [bookingsTotalPages]);

  useEffect(() => {
    setBookingsPage(1);
  }, [isShowingShowtimeBookings]);
  const staffFoods = useMemo(() => [
    ...staffFoodCombos.map((item) => ({ ...item, kind: 'combo' })),
    ...staffFoodItems.map((item) => ({ ...item, kind: 'item' })),
  ], [staffFoodCombos, staffFoodItems]);
  const filteredStaffFoods = useMemo(() => {
    const keyword = normalizeSearchText(staffFoodSearch);
    if (!keyword) return staffFoods;
    return staffFoods.filter((food) => normalizeSearchText(food.name).includes(keyword));
  }, [staffFoodSearch, staffFoods]);

  const foodStats = useMemo(() => {
    const outOfStock = staffFoods.filter((item) => item.status === 'OUT_OF_STOCK' || item.status === 'INACTIVE').length;
    const lowStock = staffFoods.filter((item) => item.status === 'LOW_STOCK').length;
    const active = staffFoods.filter((item) => item.status === 'ACTIVE').length;
    return { active, lowStock, outOfStock, total: staffFoods.length };
  }, [staffFoods]);
  const staffFoodTotalPages = Math.max(1, Math.ceil(filteredStaffFoods.length / FOOD_PAGE_SIZE));
  const safeStaffFoodPage = Math.min(staffFoodPage, staffFoodTotalPages);
  const staffFoodStartIndex = (safeStaffFoodPage - 1) * FOOD_PAGE_SIZE;
  const paginatedStaffFoods = filteredStaffFoods.slice(staffFoodStartIndex, staffFoodStartIndex + FOOD_PAGE_SIZE);
  const staffFoodDisplayStart = filteredStaffFoods.length === 0 ? 0 : staffFoodStartIndex + 1;
  const staffFoodDisplayEnd = Math.min(staffFoodStartIndex + FOOD_PAGE_SIZE, filteredStaffFoods.length);

  useEffect(() => {
    setStaffFoodPage((page) => Math.min(page, staffFoodTotalPages));
  }, [staffFoodTotalPages]);

  useEffect(() => {
    setStaffFoodPage(1);
  }, [staffFoodSearch]);

  const stats = useMemo(() => {
    const checked = visibleBookings.filter((booking) => booking.status === 'USED').length;
    const paid = visibleBookings.filter((booking) => booking.status === 'PAID').length;
    return { checked, paid, total: visibleBookings.length };
  }, [visibleBookings]);

  const getToken = () => {
    const { accessToken } = getStoredAuth();
    return accessToken;
  };

  const stopQrScanner = () => {
    if (qrScanTimerRef.current) {
      window.clearInterval(qrScanTimerRef.current);
      qrScanTimerRef.current = null;
    }
    if (qrStreamRef.current) {
      qrStreamRef.current.getTracks().forEach((track) => track.stop());
      qrStreamRef.current = null;
    }
    if (qrVideoRef.current) {
      qrVideoRef.current.srcObject = null;
    }
    setIsCameraOpen(false);
  };

  const startQrScanner = async () => {
    setScannerError('');
    setScannerMessage('');
    lastScannedQrRef.current = '';

    if (!navigator.mediaDevices?.getUserMedia) {
      setScannerError('Trình duyệt không hỗ trợ mở camera. Hãy dán mã QR thủ công.');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {
          facingMode: { ideal: 'environment' },
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
      });
      qrStreamRef.current = stream;
      setIsCameraOpen(true);
      setScannerMessage('Đưa QR của khách vào khung camera để hệ thống tự đọc.');

      window.requestAnimationFrame(async () => {
        const video = qrVideoRef.current;
        if (!video || qrStreamRef.current !== stream) return;
        video.srcObject = stream;
        await video.play();

        qrScanTimerRef.current = window.setInterval(async () => {
          if (video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) return;

          try {
            const canvas = qrCanvasRef.current;
            const width = video.videoWidth;
            const height = video.videoHeight;
            if (!canvas || !width || !height) return;

            canvas.width = width;
            canvas.height = height;
            const context = canvas.getContext('2d', { willReadFrequently: true });
            if (!context) return;

            context.drawImage(video, 0, 0, width, height);
            const imageData = context.getImageData(0, 0, width, height);
            const detected = jsQR(imageData.data, width, height, {
              inversionAttempts: 'attemptBoth',
            });
            const rawValue = detected?.data?.trim();
            if (!rawValue || rawValue === lastScannedQrRef.current) return;

            lastScannedQrRef.current = rawValue;
            setQrCode(rawValue);
            setScannerMessage('Đã quét QR. Đang kiểm tra mã...');
            stopQrScanner();
            void lookupBooking({ preferQr: true, qrValue: rawValue });
          } catch (error) {
            setScannerError(error.message || 'Không thể đọc QR từ camera.');
          }
        }, 350);
      });
    } catch (error) {
      stopQrScanner();
      setScannerError(error.name === 'NotAllowedError'
        ? 'Bạn cần cấp quyền camera để quét QR.'
        : error.message || 'Không thể mở camera để quét QR.');
    }
  };

  const rememberBooking = (booking) => {
    if (!booking?.id) return;
    setShowtimeBookings((current) => current.map((item) => (
      String(item.id) === String(booking.id) ? booking : item
    )));
    setRecentBookings((current) => [
      booking,
      ...current.filter((item) => String(item.id) !== String(booking.id)),
    ].slice(0, RECENT_BOOKINGS_LIMIT));
  };

  const loadRecentBookings = async (seedBooking = null) => {
    const token = getToken();
    if (!token) {
      setRecentBookingsError('Vui lòng đăng nhập bằng tài khoản STAFF.');
      return;
    }

    setIsLoadingRecentBookings(true);
    setRecentBookingsError('');
    try {
      const bookings = await staffService.getRecentStaffCheckInBookings(token, RECENT_BOOKINGS_LIMIT);
      const source = Array.isArray(bookings) ? bookings : [];
      const merged = seedBooking
        ? [seedBooking, ...source.filter((item) => String(item.id) !== String(seedBooking.id))]
        : source;
      setRecentBookings(merged.slice(0, RECENT_BOOKINGS_LIMIT));
    } catch (error) {
      if (seedBooking) rememberBooking(seedBooking);
      setRecentBookingsError(error.message || 'Không thể tải booking vừa tra cứu/check-in từ API.');
    } finally {
      setIsLoadingRecentBookings(false);
    }
  };

  const loadStaffFoods = async () => {
    const token = getToken();
    if (!token) {
      setStaffFoodError('Vui lòng đăng nhập bằng tài khoản STAFF.');
      return;
    }

    setIsLoadingStaffFoods(true);
    setStaffFoodError('');
    try {
      const [items, combos] = await Promise.all([
        staffService.getStaffFoodItems(token),
        staffService.getStaffFoodCombos(token),
      ]);
      setStaffFoodItems(Array.isArray(items) ? items : []);
      setStaffFoodCombos(Array.isArray(combos) ? combos : []);
    } catch (error) {
      setStaffFoodError(error.message || 'Không thể tải danh sách bắp nước.');
    } finally {
      setIsLoadingStaffFoods(false);
    }
  };

  const loadFailedRefunds = async (page = 0) => {
    const token = getToken();
    if (!token) {
      setFailedRefundError('Vui long dang nhap bang tai khoan STAFF.');
      return;
    }

    setIsLoadingFailedRefunds(true);
    setFailedRefundError('');
    try {
      const data = await staffService.getFailedBulkRefunds(token, { page, size: 8 });
      const items = data?.items || data?.content || (Array.isArray(data) ? data : []);
      setFailedRefunds(items);
      setFailedRefundTotalPages(data?.totalPages || 1);
      setFailedRefundPage(page);
    } catch (error) {
      setFailedRefundError(error.message || 'Khong the tai danh sach refund loi.');
    } finally {
      setIsLoadingFailedRefunds(false);
    }
  };

  const openManualRefundModal = async (refund) => {
    const token = getToken();
    if (!token) return;
    setFailedRefundError('');
    try {
      const detail = await staffService.getBulkRefundDetail(token, refund.bookingId);
      setManualRefundModal(detail);
      setManualRefundForm({ refundMethod: 'MANUAL_BANK_TRANSFER', notes: '' });
    } catch (error) {
      setFailedRefundError(error.message || 'Khong the tai chi tiet refund.');
    }
  };

  const confirmManualRefund = async () => {
    const token = getToken();
    if (!token || !manualRefundModal?.bookingId) return;
    setIsSavingManualRefund(true);
    setFailedRefundError('');
    try {
      await staffService.confirmManualBulkRefund(token, manualRefundModal.bookingId, manualRefundForm);
      setManualRefundModal(null);
      await loadFailedRefunds(failedRefundPage);
    } catch (error) {
      setFailedRefundError(error.message || 'Khong the xac nhan hoan tien ngoai.');
    } finally {
      setIsSavingManualRefund(false);
    }
  };

  const updateStaffFoodStatus = async (food, nextStatus) => {
    const token = getToken();
    if (!token) {
      setStaffFoodError('Vui lòng đăng nhập bằng tài khoản STAFF.');
      return;
    }

    const foodKey = `${food.kind}-${food.id}`;
    setSavingStaffFoodKey(foodKey);
    setStaffFoodError('');
    try {
      const saved = food.kind === 'combo'
        ? await staffService.updateStaffFoodComboStatus(token, food.id, nextStatus)
        : await staffService.updateStaffFoodItemStatus(token, food.id, nextStatus);

      if (food.kind === 'combo') {
        setStaffFoodCombos((current) => current.map((item) => (item.id === food.id ? saved : item)));
      } else {
        setStaffFoodItems((current) => current.map((item) => (item.id === food.id ? saved : item)));
      }
    } catch (error) {
      setStaffFoodError(error.message || 'Không thể đổi trạng thái món.');
    } finally {
      setSavingStaffFoodKey('');
    }
  };

  useEffect(() => {
    loadRecentBookings();
    loadStaffFoods();
    loadFailedRefunds();
    checkPendingWallet();

    const interval = setInterval(() => {
      checkPendingWallet();
    }, 15000);

    return () => clearInterval(interval);
  }, [checkPendingWallet]);

  useEffect(() => () => stopQrScanner(), []);

  const lookupBooking = async ({ preferQr = false, qrValue = '', bookingValue = '' } = {}) => {
    const token = getToken();
    const trimmedQr = String(qrValue || qrCode).trim();
    const trimmedCode = String(bookingValue || bookingCode).trim();
    if (!token) {
      setResult({ type: 'error', title: 'Phiên đăng nhập không hợp lệ.', message: 'Vui lòng đăng nhập bằng tài khoản STAFF.' });
      return null;
    }
    if (!trimmedQr && !trimmedCode) {
      setResult({ type: 'warning', title: 'Thiếu dữ liệu tra cứu.', message: 'Nhập mã QR hoặc mã booking trước khi tra cứu.' });
      return null;
    }

    const lookupValue = preferQr ? trimmedQr : trimmedCode;
    const isFoodLookup = isFoodPickupCode(lookupValue);

    setIsLookingUp(true);
    try {
      if (isFoodLookup) {
        const foodOrder = await staffService.lookupFoodOrder(token, lookupValue);
        const pickedUp = foodOrder.status === 'PICKED_UP';
        setSelectedTicketCodes([]);
        setResult({
          type: pickedUp ? 'success' : 'warning',
          title: pickedUp ? 'Đơn đã được giao trước đó.' : 'Đơn đã thanh toán, sẵn sàng giao.',
          message: pickedUp
            ? 'Không giao lại đơn này. Kiểm tra thời gian nhận món bên dưới.'
            : 'Đối chiếu món với khách, sau đó chọn “Xác nhận giao món”.',
          foodOrder,
        });
        return foodOrder;
      }
      const parsedQrInput = parseQrOrBookingCode(trimmedQr);
      const booking = await staffService.lookupStaffCheckInBooking(token, {
        qrCode: preferQr ? parsedQrInput.qrCode : '',
        bookingCode: preferQr ? parsedQrInput.bookingCode : trimmedCode,
      });
      rememberBooking(booking);
      // Quét QR của một ghế cụ thể → tự tick ghế đó để staff xác nhận nhanh
      const seatQrMatch = String(parsedQrInput.qrCode || '').match(/^CINEAI:SEAT:([^:]+):/i);
      const scannedTicketCode = seatQrMatch ? seatQrMatch[1] : null;
      setSelectedTicketCodes(
        scannedTicketCode && (booking.seats || []).some((seat) => seat.ticketCode === scannedTicketCode && seat.status !== 'CHECKED_IN')
          ? [scannedTicketCode]
          : []
      );
      const canCheckIn = isBookingCheckInOpen(booking);
      const showtimeEnded = isShowtimeOver(booking);
      const isPaid = booking.status === 'PAID';
      setResult({
        type: isPaid
          ? (canCheckIn ? 'warning' : 'error')
          : booking.status === 'USED' ? 'success' : 'error',
        title: isPaid
          ? canCheckIn
            ? 'Booking hợp lệ, chờ check-in.'
            : showtimeEnded
              ? 'Đã quá giờ chiếu.'
              : 'Chưa đến giờ check-in.'
          : booking.status === 'USED' ? 'Booking đã check-in.' : 'Booking chưa đủ điều kiện.',
        message: isPaid
          ? canCheckIn
            ? 'Có thể xác nhận check-in bằng mã QR của booking này.'
            : getCheckInWindowMessage(booking)
          : `Trạng thái hiện tại: ${booking.status}.`,
        booking,
      });
      void loadRecentBookings(booking);
      return booking;
    } catch (error) {
      setResult({
        type: 'error',
        title: isFoodLookup ? 'Không tìm thấy đơn bắp nước.' : 'Không tìm thấy booking.',
        message: error.message || (isFoodLookup ? 'Không thể tra cứu đơn bắp nước từ hệ thống.' : 'Không thể tra cứu booking từ hệ thống.'),
      });
      return null;
    } finally {
      setIsLookingUp(false);
    }
  };

  const toggleSeatSelection = (ticketCode) => {
    if (!ticketCode) return;
    setSelectedTicketCodes((current) => (
      current.includes(ticketCode)
        ? current.filter((code) => code !== ticketCode)
        : [...current, ticketCode]
    ));
  };

  const checkInSelectedSeats = async () => {
    const token = getToken();
    const booking = result?.booking;
    if (!token || !booking?.bookingCode || selectedTicketCodes.length === 0) return;

    setIsCheckingIn(true);
    try {
      const updated = await staffService.checkInStaffSeats(token, {
        bookingCode: booking.bookingCode,
        ticketCodes: selectedTicketCodes,
      });
      rememberBooking(updated);
      setSelectedTicketCodes([]);
      const remaining = (updated.seats || []).filter((seat) => seat.status !== 'CHECKED_IN').length;
      setResult({
        type: 'success',
        title: remaining === 0 ? 'Đã check-in toàn bộ ghế.' : `Đã check-in ${selectedTicketCodes.length} ghế.`,
        message: remaining === 0
          ? 'Toàn bộ ghế của booking đã vào phòng chiếu.'
          : `Còn ${remaining} ghế chưa vào. Quét tiếp hoặc chọn ghế để xác nhận.`,
        booking: updated,
      });
      void loadRecentBookings(updated);
    } catch (error) {
      setResult((current) => ({
        type: 'error',
        title: 'Không thể check-in ghế.',
        message: error.message || 'Ghế không đủ điều kiện check-in.',
        booking: current?.booking || null,
      }));
    } finally {
      setIsCheckingIn(false);
    }
  };

  const openCounterSale = () => {
    if (!result?.booking) return;
    setCounterSaleQuantities({});
    setCounterSaleMessage('');
    setCashGiven('');
    setCounterSaleReceipt(null);
    setCounterSaleBooking(result.booking);
  };

  const changeCounterSaleQuantity = (foodKey, delta) => {
    setCounterSaleQuantities((prev) => {
      const next = Math.max(0, (prev[foodKey] || 0) + delta);
      const copy = { ...prev };
      if (next === 0) delete copy[foodKey];
      else copy[foodKey] = next;
      return copy;
    });
  };

  const counterSaleTotal = useMemo(() => Object.entries(counterSaleQuantities).reduce((sum, [foodKey, qty]) => {
    const food = staffFoods.find((item) => `${item.kind}-${item.id}` === foodKey);
    return sum + (food ? Number(food.price || 0) * qty : 0);
  }, 0), [counterSaleQuantities, staffFoods]);

  const counterSaleLines = useMemo(() => Object.entries(counterSaleQuantities)
    .filter(([, qty]) => qty > 0)
    .map(([foodKey, qty]) => {
      const food = staffFoods.find((item) => `${item.kind}-${item.id}` === foodKey);
      if (!food) return null;
      const price = Number(food.price || 0);
      return { key: foodKey, name: food.name, qty, price, lineTotal: price * qty };
    })
    .filter(Boolean), [counterSaleQuantities, staffFoods]);

  // Tiền khách đưa / thối lại — nghiệp vụ thu ngân
  const cashGivenValue = cashGiven === '' ? null : Number(cashGiven) || 0;
  const cashChange = cashGivenValue !== null ? cashGivenValue - counterSaleTotal : null;
  const cashInsufficient = cashGivenValue !== null && cashGivenValue < counterSaleTotal;

  const submitCounterSale = async () => {
    const token = getToken();
    if (!token || !counterSaleBooking?.bookingCode || isSavingCounterSale) return;
    const foods = Object.entries(counterSaleQuantities)
      .map(([foodKey, quantity]) => {
        const food = staffFoods.find((item) => `${item.kind}-${item.id}` === foodKey);
        if (!food) return null;
        return food.kind === 'combo'
          ? { foodItemId: null, foodComboId: food.id, quantity }
          : { foodItemId: food.id, foodComboId: null, quantity };
      })
      .filter(Boolean);
    if (foods.length === 0) {
      setCounterSaleMessage('Chọn ít nhất một món.');
      return;
    }
    if (cashInsufficient) {
      setCounterSaleMessage('Tiền khách đưa chưa đủ — kiểm tra lại trước khi xác nhận.');
      return;
    }
    setIsSavingCounterSale(true);
    setCounterSaleMessage('');
    try {
      const order = await staffService.createStaffFoodOrder(token, counterSaleBooking.bookingCode, { foods });
      // Chuyển sang màn BIÊN LAI thay vì đóng modal — staff đối chiếu tiền ngay tại chỗ
      setCounterSaleReceipt({
        orderCode: order.orderCode,
        total: Number(order.totalAmount || 0),
        cashGiven: cashGivenValue,
        change: cashChange !== null && cashChange >= 0 ? cashChange : null,
        time: new Date().toLocaleString('vi-VN'),
        collector: currentUser?.fullName || currentUser?.name || currentUser?.email || 'STAFF',
      });
      setCounterSaleQuantities({});
      setCashGiven('');
      setResult((current) => (current ? {
        ...current,
        type: 'success',
        title: 'Đã bán bắp nước tại quầy.',
        message: `Đơn ${order.orderCode} (${Number(order.totalAmount || 0).toLocaleString('vi-VN')}đ) đã thu tiền mặt.`,
      } : current));
    } catch (error) {
      setCounterSaleMessage(error.message || 'Không thể tạo đơn bắp nước.');
    } finally {
      setIsSavingCounterSale(false);
    }
  };

  const checkInByQr = async (value = qrCode) => {
    const token = getToken();
    const trimmedQr = String(value || '').trim();
    if (!token) {
      setResult({ type: 'error', title: 'Phiên đăng nhập không hợp lệ.', message: 'Vui lòng đăng nhập bằng tài khoản STAFF.' });
      return;
    }
    if (!trimmedQr) {
      setResult({ type: 'warning', title: 'Chưa có mã QR.', message: 'Nhập hoặc quét mã QR trước khi xác nhận check-in.' });
      return;
    }

    setIsCheckingIn(true);
    try {
      if (isFoodPickupCode(trimmedQr)) {
        const foodOrder = await staffService.pickUpFoodOrder(token, trimmedQr);
        setSelectedTicketCodes([]);
        setResult({
          type: 'success',
          title: 'Đã xác nhận giao món.',
          message: `Đơn ${foodOrder.orderCode} đã hoàn tất và QR không còn hiệu lực nhận món.`,
          foodOrder,
        });
        return;
      }
      const parsedInput = parseQrOrBookingCode(trimmedQr);
      let qrForCheckIn = parsedInput.qrCode;

      if (!qrForCheckIn && parsedInput.bookingCode) {
        const foundBooking = await staffService.lookupStaffCheckInBooking(token, {
          bookingCode: parsedInput.bookingCode,
          qrCode: '',
        });
        qrForCheckIn = foundBooking?.qrCode || '';
        if (!qrForCheckIn) {
          throw new Error('Booking này chưa có QR check-in. Vui lòng kiểm tra trạng thái thanh toán.');
        }
      }

      const booking = await staffService.checkInStaffBooking(token, qrForCheckIn);
      rememberBooking(booking);
      setResult({
        type: 'success',
        title: 'Check-in thành công.',
        message: 'Booking đã được xác nhận. Có thể hướng dẫn khách vào phòng chiếu.',
        booking,
      });
      void loadRecentBookings(booking);
    } catch (error) {
      const isFoodPickup = isFoodPickupCode(trimmedQr);
      setResult({
        type: 'error',
        title: isFoodPickup ? 'Không thể xác nhận giao món.' : 'Không thể check-in.',
        message: isFoodPickup && /already been picked up/i.test(error.message || '')
          ? 'Đơn này đã được giao trước đó. Không giao món lần hai.'
          : error.message || (isFoodPickup ? 'Đơn không đủ điều kiện nhận món.' : 'Booking không đủ điều kiện check-in.'),
      });
    } finally {
      setIsCheckingIn(false);
    }
  };

  const loadShowtimeBookings = async () => {
    const token = getToken();
    const trimmedShowtimeId = showtimeId.trim();
    if (!token) {
      setShowtimeError('Vui lòng đăng nhập bằng tài khoản STAFF.');
      return;
    }
    if (!trimmedShowtimeId) {
      setShowtimeError('Nhập showtimeId trước khi tải danh sách.');
      return;
    }

    setShowtimeError('');
    setIsLoadingShowtime(true);
    try {
      const bookings = await staffService.getStaffShowtimeBookings(token, trimmedShowtimeId);
      setShowtimeBookings(Array.isArray(bookings) ? bookings : []);
    } catch (error) {
      setShowtimeBookings([]);
      setShowtimeError(error.message || 'Không thể tải danh sách booking của suất chiếu.');
    } finally {
      setIsLoadingShowtime(false);
    }
  };

  return (
    <div className="staff-page relative bg-black text-white">
      <div className="staff-orb staff-orb-one" />
      <div className="staff-orb staff-orb-two" />
      <div className="staff-grid-bg" />

      <main className="relative z-10 mx-auto max-w-[1500px] space-y-5 px-4 py-7 sm:px-6 lg:px-8 lg:py-10">

        {/* Tab Navigation */}
        <div style={{ display: 'flex', gap: 6, padding: 4, background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.07)', borderRadius: 12, width: 'fit-content' }}>
          <button
            onClick={() => setActiveSection('checkin')}
            style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 7, padding: '8px 18px', borderRadius: 9, border: 'none', cursor: 'pointer', fontSize: 11, fontWeight: 800, fontFamily: 'Inter, sans-serif', background: activeSection === 'checkin' ? 'linear-gradient(135deg, #10b981, #059669)' : 'transparent', color: activeSection === 'checkin' ? '#fff' : 'rgba(255,255,255,0.4)', boxShadow: activeSection === 'checkin' ? '0 2px 12px rgba(16,185,129,0.35)' : 'none', transition: 'all 0.2s' }}
          >
            <ScanLine size={14} /> Check-in & Vận hành
            {pendingCheckinCount > 0 && (
              <span style={{ position: 'absolute', top: -3, right: -3, display: 'flex', height: 10, width: 10 }}>
                <span style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: '#ef4444', opacity: 0.75, animation: 'ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite' }} />
                <span style={{ position: 'relative', borderRadius: '50%', height: 10, width: 10, background: '#f43f5e', border: '1.5px solid #000', boxShadow: '0 0 6px #f43f5e' }} />
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveSection('wallet')}
            style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 7, padding: '8px 18px', borderRadius: 9, border: 'none', cursor: 'pointer', fontSize: 11, fontWeight: 800, fontFamily: 'Inter, sans-serif', background: activeSection === 'wallet' ? 'linear-gradient(135deg, #f59e0b, #d97706)' : 'transparent', color: activeSection === 'wallet' ? '#fff' : 'rgba(255,255,255,0.4)', boxShadow: activeSection === 'wallet' ? '0 2px 12px rgba(245,158,11,0.35)' : 'none', transition: 'all 0.2s' }}
          >
            <Wallet size={14} /> Quản lý ví
            {pendingWalletCount > 0 && (
              <span style={{ position: 'absolute', top: -3, right: -3, display: 'flex', height: 10, width: 10 }} title={`${pendingWalletCount} đơn/yêu cầu chờ xử lý`}>
                <span style={{ position: 'absolute', inset: 0, borderRadius: '50%', background: '#ef4444', opacity: 0.75, animation: 'ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite' }} />
                <span style={{ position: 'relative', borderRadius: '50%', height: 10, width: 10, background: '#f43f5e', border: '1.5px solid #000', boxShadow: '0 0 6px #f43f5e' }} />
              </span>
            )}
          </button>
        </div>

        {/* Wallet Panel */}
        {activeSection === 'wallet' && (
          <StaffWalletPanel
            token={getToken()}
            showToast={(msg) => console.log('[Wallet]', msg)}
            onPendingCountChange={setPendingWalletCount}
          />
        )}

        {/* Check-in Content */}
        {activeSection === 'checkin' && (
          <>
            <section className="grid gap-4 xl:grid-cols-[1fr_380px]">
              <motion.div initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-r from-[#0d0f14] via-[#090b0e] to-[#050608] p-5 shadow-[0_24px_80px_rgba(0,0,0,0.45)] sm:p-6">
                <div className="absolute right-0 top-0 h-full w-1/2 bg-[radial-gradient(circle_at_top_right,rgba(16,185,129,0.16),transparent_62%)]" />
                <div className="relative">
                  <div className="mb-3 flex flex-wrap items-center gap-2">
                    <span className="rounded-full border border-purple-400/30 bg-purple-500/10 px-3 py-1 text-[9px] font-black uppercase tracking-widest text-purple-300">
                      STAFF OPERATIONS
                    </span>
                  </div>
                  <h2 className="text-2xl font-black uppercase tracking-tight text-white sm:text-3xl">Kiểm soát vé bằng QR booking</h2>
                  <p className="mt-3 max-w-2xl text-xs leading-6 text-neutral-400">
                    Mỗi ghế có một mã QR riêng như thẻ lên máy bay. Quét mã ghế để check-in từng người, hoặc quét mã booking để xác nhận cả nhóm cùng lúc.
                  </p>
                  <p className="mt-3 text-[10px] font-black uppercase tracking-[0.18em] text-neutral-500">
                    Nhân viên: {currentUser?.fullName || currentUser?.name || currentUser?.email || 'STAFF'}
                  </p>
                </div>
              </motion.div>

              <motion.div initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.08 }} className="rounded-2xl border border-white/10 bg-gradient-to-b from-[#0d0f14] to-[#050608] p-5 shadow-2xl">
                <p className="text-[9px] font-black uppercase tracking-[0.2em] text-neutral-400">Phiên hiện tại</p>
                <p className="mt-1 text-2xl font-black text-white">{stats.checked}/{stats.total}</p>
                <div className="mt-4 grid grid-cols-3 gap-2">
                  <div className="rounded-xl border border-emerald-400/30 bg-emerald-500/10 p-2.5"><p className="text-lg font-black text-emerald-300">{stats.checked}</p><p className="text-[8px] font-black uppercase tracking-widest text-emerald-200/70">Đã vào</p></div>
                  <div className="rounded-xl border border-purple-400/30 bg-purple-500/10 p-2.5"><p className="text-lg font-black text-purple-300">{stats.paid}</p><p className="text-[8px] font-black uppercase tracking-widest text-purple-200/70">Chờ vào</p></div>
                  <div className="rounded-xl border border-white/10 bg-black/60 p-2.5"><p className="text-lg font-black text-white">{stats.total}</p><p className="text-[8px] font-black uppercase tracking-widest text-neutral-400">Đã tra</p></div>
                </div>
              </motion.div>
            </section>

            <section className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(380px,0.75fr)]">
              <div className="rounded-2xl border border-white/10 bg-gradient-to-b from-[#0c0e12] to-[#050608] p-5 shadow-2xl sm:p-7">
                <div className="grid gap-6 lg:grid-cols-2">
                  <div className="space-y-4">
                    <div>
                      <p className="text-[9px] font-black uppercase tracking-[0.22em] text-emerald-400">Quét/Xác nhận QR</p>
                      <h3 className="mt-2 text-xl font-black uppercase text-white">Check-in bằng mã QR</h3>
                      <p className="mt-2 text-xs leading-6 text-neutral-400">Quét QR trên vé, hoặc dán chuỗi `CINEAI:...` / mã booking `BK...` để tra cứu và check-in.</p>
                    </div>
                    <div className="space-y-3 rounded-xl border border-white/10 bg-black/50 p-4">
                      <button
                        type="button"
                        onClick={isCameraOpen ? stopQrScanner : startQrScanner}
                        className="flex w-full items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.04] px-4 py-3 text-[9px] font-black uppercase tracking-[0.18em] text-neutral-200 transition hover:border-emerald-400 hover:bg-white/10 hover:text-white"
                      >
                        {isCameraOpen ? <CameraOff className="h-4 w-4" /> : <Camera className="h-4 w-4" />}
                        {isCameraOpen ? 'Tắt camera' : 'Mở camera quét QR'}
                      </button>
                      {isCameraOpen && (
                        <div className="relative overflow-hidden rounded-xl border border-emerald-400/30 bg-neutral-950">
                          <video
                            ref={qrVideoRef}
                            className="aspect-video w-full object-cover"
                            muted
                            playsInline
                          />
                          <canvas ref={qrCanvasRef} className="hidden" />
                          <div className="pointer-events-none absolute inset-0 grid place-items-center">
                            <div className="h-40 w-40 rounded-lg border-2 border-emerald-300/80 shadow-[0_0_0_999px_rgba(0,0,0,0.35)]" />
                          </div>
                        </div>
                      )}
                      {(scannerMessage || scannerError) && (
                        <p className={`text-[10px] font-bold leading-5 ${scannerError ? 'text-rose-400' : 'text-emerald-300'}`}>
                          {scannerError || scannerMessage}
                        </p>
                      )}
                    </div>
                    <textarea
                      value={qrCode}
                      onChange={(event) => setQrCode(event.target.value)}
                      placeholder="Dán QR vé, QR nhận bắp nước hoặc mã booking..."
                      rows={6}
                      className="w-full resize-none rounded-xl border border-white/10 bg-black/60 p-4 text-sm font-bold text-white outline-none transition placeholder:text-neutral-600 focus:border-emerald-400/70 focus:ring-1 focus:ring-emerald-400/20"
                    />
                    <div className="grid gap-3 sm:grid-cols-2">
                      <button
                        type="button"
                        onClick={() => lookupBooking({ preferQr: true })}
                        disabled={isLookingUp || !qrCode.trim()}
                        className="flex items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.04] px-5 py-3.5 text-[10px] font-black uppercase tracking-[0.18em] text-neutral-200 transition hover:border-emerald-400 hover:bg-white/10 hover:text-white disabled:opacity-50"
                      >
                        {isLookingUp ? <RefreshCw className="h-4 w-4 animate-spin" /> : <QrCode className="h-4 w-4" />} Kiểm tra mã
                      </button>
                      <button
                        type="button"
                        onClick={() => checkInByQr()}
                        disabled={isCheckingIn || !qrCode.trim()}
                        className="flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-emerald-600 px-5 py-3.5 text-[10px] font-black uppercase tracking-[0.18em] text-black font-extrabold shadow-[0_4px_20px_rgba(16,185,129,0.3)] transition hover:from-emerald-400 hover:to-emerald-500 disabled:opacity-50"
                      >
                        {isCheckingIn ? <RefreshCw className="h-4 w-4 animate-spin" /> : <ScanLine className="h-4 w-4" />} {isFoodPickupCode(qrCode) ? 'Xác nhận giao món' : 'Xác nhận check-in'}
                      </button>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <div>
                      <p className="text-[9px] font-black uppercase tracking-[0.22em] text-purple-400">Tra cứu thủ công</p>
                      <h3 className="mt-2 text-xl font-black uppercase text-white">Tìm theo mã đơn</h3>
                      <p className="mt-2 text-xs leading-6 text-neutral-400">Nhập mã booking BK... hoặc mã nhận bắp nước FO... khi khách chưa mở được QR.</p>
                    </div>
                    <div className="relative">
                      <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-500" />
                      <input
                        value={bookingCode}
                        onChange={(event) => setBookingCode(event.target.value)}
                        placeholder="VD: BKABC123... hoặc FOABC123..."
                        className="w-full rounded-xl border border-white/10 bg-black/60 py-3.5 pl-11 pr-4 text-sm font-bold text-white outline-none transition placeholder:text-neutral-600 focus:border-purple-400/70 focus:ring-1 focus:ring-purple-400/20"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={() => lookupBooking()}
                      disabled={isLookingUp || !bookingCode.trim()}
                      className="flex w-full items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.04] px-5 py-3.5 text-[10px] font-black uppercase tracking-[0.18em] text-neutral-200 transition hover:border-purple-400 hover:bg-white/10 hover:text-white disabled:opacity-50"
                    >
                      {isLookingUp ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />} {isFoodPickupCode(bookingCode) ? 'Tra cứu đơn bắp nước' : 'Tra cứu booking'}
                    </button>
                    {result?.booking?.qrCode && result.booking.status === 'PAID' && isBookingCheckInOpen(result.booking) && (
                      <button
                        type="button"
                        onClick={() => checkInByQr(result.booking.qrCode)}
                        disabled={isCheckingIn}
                        className="flex w-full items-center justify-center gap-2 rounded-xl border border-emerald-400/40 bg-emerald-500/10 px-5 py-3.5 text-[10px] font-black uppercase tracking-[0.18em] text-emerald-300 transition hover:bg-emerald-400 hover:text-black disabled:opacity-50"
                      >
                        {isCheckingIn ? <RefreshCw className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />} Check-in booking vừa tra
                      </button>
                    )}
                  </div>
                </div>
              </div>

              <ResultCard
                result={result}
                selectedTicketCodes={selectedTicketCodes}
                onToggleSeat={toggleSeatSelection}
                onConfirmSeats={checkInSelectedSeats}
                onConfirmFood={checkInByQr}
                isCheckingIn={isCheckingIn}
                onOpenCounterSale={openCounterSale}
              />
            </section>

            {/* <section className="border border-neutral-800 bg-[#070707] p-5">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div className="max-w-xl">
                  <p className="text-[9px] font-black uppercase tracking-[0.2em] text-emerald-400">Danh sách theo suất chiếu</p>
                  <h3 className="mt-1 text-lg font-black uppercase text-white">Tải booking của một showtime</h3>
                  <p className="mt-2 text-xs leading-6 text-neutral-500">
                    STAFF có thể nhập showtimeId để xem toàn bộ booking thật của suất đó, sau đó check-in trực tiếp các booking PAID.
                  </p>
                </div>
                <div className="flex w-full flex-col gap-3 sm:flex-row lg:w-auto">
                  <input
                    value={showtimeId}
                    onChange={(event) => setShowtimeId(event.target.value)}
                    placeholder="showtimeId..."
                    className="min-w-[220px] border border-neutral-800 bg-black px-4 py-3 text-sm font-bold text-white outline-none transition placeholder:text-neutral-700 focus:border-emerald-400/70"
                  />
                  <button
                    type="button"
                    onClick={loadShowtimeBookings}
                    disabled={isLoadingShowtime || !showtimeId.trim()}
                    className="flex items-center justify-center gap-2 bg-white px-5 py-3 text-[10px] font-black uppercase tracking-[0.18em] text-black transition hover:bg-emerald-400 disabled:opacity-50"
                  >
                    {isLoadingShowtime ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />} Tải danh sách
                  </button>
                  {isShowingShowtimeBookings && (
                    <button
                      type="button"
                      onClick={() => {
                        setShowtimeBookings([]);
                        setShowtimeError('');
                      }}
                      className="border border-neutral-700 bg-black px-5 py-3 text-[10px] font-black uppercase tracking-[0.18em] text-neutral-300 transition hover:border-white hover:text-white"
                    >
                      Về danh sách phiên
                    </button>
                  )}
                </div>
              </div>
              {showtimeError && <p className="mt-3 text-xs font-bold text-rose-400">{showtimeError}</p>}
            </section> */}

            {/* <section className="border border-neutral-800 bg-[#070707] p-5">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <p className="text-[9px] font-black uppercase tracking-[0.2em] text-rose-300">Bulk refund failed</p>
                  <h3 className="mt-1 text-lg font-black uppercase text-white">Xu ly hoan tien thu cong</h3>
                  <p className="mt-2 text-xs leading-6 text-neutral-500">
                    STAFF xem cac booking VNPay refund loi, hoan tien ngoai he thong, roi xac nhan de can bang diem loyalty.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => loadFailedRefunds(failedRefundPage)}
                  disabled={isLoadingFailedRefunds}
                  className="flex items-center justify-center gap-2 border border-neutral-700 bg-black px-4 py-3 text-[10px] font-black uppercase tracking-widest text-neutral-300 transition hover:border-rose-300 hover:text-white disabled:opacity-50"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${isLoadingFailedRefunds ? 'animate-spin' : ''}`} /> Lam moi
                </button>
              </div>
              {failedRefundError && <p className="mt-3 text-xs font-bold text-rose-400">{failedRefundError}</p>}
              <div className="mt-4 overflow-x-auto border border-neutral-800 bg-black">
                <table className="w-full min-w-[860px] text-left">
                  <thead className="border-b border-neutral-800 bg-[#050505] text-[8px] font-black uppercase tracking-[0.18em] text-neutral-500">
                    <tr>
                      <th className="px-4 py-3">Booking</th>
                      <th className="px-3 py-3">Khach</th>
                      <th className="px-3 py-3">Phim / suat</th>
                      <th className="px-3 py-3">So tien</th>
                      <th className="px-3 py-3">Diem NET</th>
                      <th className="px-4 py-3 text-right">Thao tac</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-900">
                    {isLoadingFailedRefunds ? (
                      <tr>
                        <td colSpan={6} className="px-5 py-10 text-center text-xs font-bold text-neutral-500">
                          Dang tai danh sach refund loi...
                        </td>
                      </tr>
                    ) : failedRefunds.length > 0 ? failedRefunds.map((refund) => (
                      <tr key={refund.bookingId} className="transition hover:bg-rose-400/5">
                        <td className="px-4 py-4">
                          <p className="font-mono text-[11px] font-black text-white">{refund.bookingCode}</p>
                          <p className="mt-1 font-mono text-[8px] text-neutral-600">#{refund.bookingId}</p>
                        </td>
                        <td className="px-3 py-4">
                          <p className="max-w-[180px] truncate text-xs font-bold text-neutral-300">{refund.customerName || '--'}</p>
                          <p className="max-w-[180px] truncate text-[10px] text-neutral-600">{refund.customerEmail}</p>
                        </td>
                        <td className="px-3 py-4">
                          <p className="max-w-[220px] truncate text-xs font-bold text-neutral-300">{refund.movieName}</p>
                          <p className="text-[10px] font-bold text-neutral-500">{formatDateTime(refund.showtimeStart)}</p>
                        </td>
                        <td className="px-3 py-4 font-mono text-xs font-black text-amber-300">{formatCurrency(refund.refundAmount)}</td>
                        <td className="px-3 py-4 font-mono text-xs font-black text-emerald-300">
                          {Number(refund.loyaltyCalculation?.netBalanceChange || 0).toLocaleString('vi-VN')}
                        </td>
                        <td className="px-4 py-4 text-right">
                          <button
                            type="button"
                            onClick={() => openManualRefundModal(refund)}
                            className="border border-amber-400/40 bg-amber-400 px-3 py-2 text-[8px] font-black uppercase tracking-widest text-black transition hover:bg-amber-300"
                          >
                            Xac nhan ngoai
                          </button>
                        </td>
                      </tr>
                    )) : (
                      <tr>
                        <td colSpan={6} className="px-5 py-10 text-center text-xs font-bold text-neutral-500">
                          Khong co booking refund loi.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
              <div className="mt-4 flex items-center justify-between text-[10px] font-black uppercase tracking-[0.18em] text-neutral-500">
                <span>Trang {failedRefundPage + 1}/{failedRefundTotalPages}</span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={failedRefundPage <= 0}
                    onClick={() => loadFailedRefunds(failedRefundPage - 1)}
                    className="border border-neutral-700 px-3 py-2 text-white transition hover:border-rose-300 disabled:opacity-30"
                  >
                    Truoc
                  </button>
                  <button
                    type="button"
                    disabled={failedRefundPage >= failedRefundTotalPages - 1}
                    onClick={() => loadFailedRefunds(failedRefundPage + 1)}
                    className="border border-neutral-700 px-3 py-2 text-white transition hover:border-rose-300 disabled:opacity-30"
                  >
                    Sau
                  </button>
                </div>
              </div>
            </section> */}

            <section className="rounded-2xl border border-white/10 bg-gradient-to-b from-[#0c0e12] to-[#050608] p-5 shadow-2xl">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <p className="text-[9px] font-black uppercase tracking-[0.2em] text-purple-400">Quầy bắp nước</p>
                  <h3 className="mt-1 text-lg font-black uppercase text-white">Trạng thái món/combo</h3>
                  <p className="mt-2 text-xs leading-6 text-neutral-400">
                    STAFF có thể đổi nhanh trạng thái bắp nước theo quầy: mở bán, sắp hết hoặc hết.
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <span className="rounded-xl border border-white/10 bg-black/60 px-3 py-2 text-[10px] font-black uppercase tracking-widest text-neutral-300">
                    {foodStats.active}/{foodStats.total} đang bán
                  </span>
                  <span className="rounded-xl border border-purple-400/30 bg-purple-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-widest text-purple-300">
                    {foodStats.lowStock} sắp hết
                  </span>
                  <span className="rounded-xl border border-purple-400/30 bg-purple-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-widest text-purple-300">
                    {foodStats.outOfStock} hết
                  </span>
                  <button
                    type="button"
                    onClick={loadStaffFoods}
                    disabled={isLoadingStaffFoods}
                    className="flex items-center gap-2 rounded-xl border border-white/15 bg-white/[0.04] px-4 py-2 text-[10px] font-black uppercase tracking-widest text-neutral-200 transition hover:border-emerald-400 hover:bg-white/10 hover:text-white disabled:opacity-50"
                  >
                    <RefreshCw className={`h-3.5 w-3.5 ${isLoadingStaffFoods ? 'animate-spin' : ''}`} /> Làm mới
                  </button>
                </div>
              </div>

              {staffFoodError && <p className="mt-3 text-xs font-bold text-rose-400">{staffFoodError}</p>}

              <div className="mt-4 flex flex-col gap-2 rounded-xl border border-white/10 bg-black/50 p-3 sm:flex-row sm:items-center">
                <div className="flex min-w-0 flex-1 items-center gap-3 rounded-lg border border-white/10 bg-[#070707] px-3 py-2.5 focus-within:border-purple-400">
                  <Search className="h-4 w-4 shrink-0 text-neutral-500" />
                  <input
                    type="search"
                    value={staffFoodSearch}
                    onChange={(event) => setStaffFoodSearch(event.target.value)}
                    placeholder="Tìm gần đúng theo tên món/combo..."
                    className="w-full bg-transparent text-xs font-bold text-white outline-none placeholder:text-neutral-600"
                  />
                </div>
                <span className="shrink-0 px-1 text-[10px] font-black uppercase tracking-[0.18em] text-neutral-500">
                  {filteredStaffFoods.length}/{staffFoods.length} kết quả
                </span>
              </div>

              <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {isLoadingStaffFoods ? (
                  <div className="rounded-xl border border-white/10 bg-black/60 p-4 text-xs font-bold text-neutral-500">Đang tải danh sách bắp nước...</div>
                ) : filteredStaffFoods.length > 0 ? paginatedStaffFoods.map((food) => {
                  const foodKey = `${food.kind}-${food.id}`;
                  const statusMeta = getFoodStatusMeta(food.status);
                  return (
                    <div key={foodKey} className="rounded-xl border border-white/10 bg-black/60 p-4 transition hover:border-purple-400/30">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-black uppercase text-white">{food.name}</p>
                          <p className="mt-1 text-[9px] font-black uppercase tracking-widest text-neutral-500">{food.kind === 'combo' ? 'Combo' : 'Món lẻ'}</p>
                        </div>
                        <span className={`rounded px-2 py-1 text-[9px] font-black uppercase ${statusMeta.className}`}>
                          {statusMeta.label}
                        </span>
                      </div>
                      <select
                        value={food.status || 'ACTIVE'}
                        onChange={(event) => updateStaffFoodStatus(food, event.target.value)}
                        disabled={savingStaffFoodKey === foodKey}
                        className="mt-4 w-full rounded-lg border border-white/10 bg-black/80 px-3 py-2.5 text-xs font-black text-white outline-none transition focus:border-purple-400 disabled:opacity-50"
                      >
                        <option value="ACTIVE">Mở bán</option>
                        <option value="LOW_STOCK">Sắp hết</option>
                        <option value="OUT_OF_STOCK">Hết</option>
                      </select>
                    </div>
                  );
                }) : (
                  <div className="rounded-xl border border-dashed border-white/10 bg-black/60 p-4 text-xs font-bold text-neutral-500">
                    {staffFoodSearch.trim() ? 'Không tìm thấy món/combo phù hợp.' : 'Chưa có món bắp nước nào.'}
                  </div>
                )}
              </div>
              <div className="mt-4 flex flex-col gap-3 rounded-xl border border-white/10 bg-black/80 p-3 text-[10px] font-black uppercase tracking-[0.18em] text-neutral-400 sm:flex-row sm:items-center sm:justify-between">
                <span>
                  Hiển thị {staffFoodDisplayStart}-{staffFoodDisplayEnd}/{filteredStaffFoods.length} món - Trang {safeStaffFoodPage}/{staffFoodTotalPages}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={safeStaffFoodPage <= 1}
                    onClick={() => setStaffFoodPage((page) => Math.max(1, page - 1))}
                    className="inline-flex items-center gap-1 rounded-lg border border-white/15 px-3 py-2 text-white transition hover:border-purple-400 disabled:opacity-30"
                  >
                    <ChevronLeft className="h-3.5 w-3.5" /> Trước
                  </button>
                  <button
                    type="button"
                    disabled={safeStaffFoodPage >= staffFoodTotalPages}
                    onClick={() => setStaffFoodPage((page) => Math.min(staffFoodTotalPages, page + 1))}
                    className="inline-flex items-center gap-1 rounded-lg border border-white/15 px-3 py-2 text-white transition hover:border-purple-400 disabled:opacity-30"
                  >
                    Sau <ChevronRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </section>

            <section className="overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-b from-[#0c0e12] to-[#050608] shadow-2xl">
              <div className="border-b border-neutral-800 p-5">
                <p className="text-[9px] font-black uppercase tracking-[0.2em] text-emerald-400">
                  {isShowingShowtimeBookings ? 'Dữ liệu showtime từ API' : 'Dữ liệu recent từ API'}
                </p>
                <h3 className="mt-1 text-lg font-black uppercase text-white">
                  {isShowingShowtimeBookings ? `Booking của showtime #${showtimeId}` : 'Booking vừa tra cứu/check-in'}
                </h3>
                {!isShowingShowtimeBookings && recentBookingsError && (
                  <p className="mt-2 text-xs font-bold text-rose-400">{recentBookingsError}</p>
                )}
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-left">
                  <thead className="border-b border-neutral-800 bg-black text-[8px] font-black uppercase tracking-[0.18em] text-neutral-500">
                    <tr>
                      <th className="px-5 py-3">Booking</th>
                      <th className="px-3 py-3">Phim</th>
                      <th className="px-3 py-3">Suất</th>
                      <th className="px-3 py-3">Ghế</th>
                      <th className="px-3 py-3">Trạng thái</th>
                      <th className="px-5 py-3 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-900">
                    {!isShowingShowtimeBookings && isLoadingRecentBookings ? (
                      <tr>
                        <td colSpan={6} className="px-5 py-12 text-center">
                          <RefreshCw className="mx-auto h-8 w-8 animate-spin text-emerald-400/70" />
                          <p className="mt-3 text-xs font-bold text-neutral-500">Đang tải booking gần đây từ API...</p>
                        </td>
                      </tr>
                    ) : visibleBookings.length > 0 ? paginatedBookings.map((booking) => {
                      const canCheckIn = isBookingCheckInOpen(booking);
                      return (
                        <tr key={booking.id} className="transition hover:bg-emerald-400/5">
                          <td className="px-5 py-4"><p className="font-mono text-[11px] font-black text-white">{booking.bookingCode}</p><p className="mt-1 font-mono text-[8px] text-neutral-600">#{booking.id}</p></td>
                          <td className="px-3 py-4 text-xs font-bold text-neutral-300">{booking.movieTitle}</td>
                          <td className="px-3 py-4 text-[10px] font-bold text-neutral-500">
                            {formatDateTime(booking.showtimeStart)}
                            {booking.status === 'PAID' && !canCheckIn && (
                              <p className="mt-1 text-[8px] font-black uppercase tracking-wider text-purple-300">Mở check-in trước 30 phút</p>
                            )}
                          </td>
                          <td className="px-3 py-4 text-xs font-black text-white">{formatSeats(booking)}</td>
                          <td className="px-3 py-4"><StatusBadge status={booking.status} /></td>
                          <td className="px-5 py-4 text-right">
                            <button
                              type="button"
                              onClick={() => checkInByQr(booking.qrCode)}
                              disabled={!canCheckIn || !booking.qrCode || isCheckingIn}
                              title={booking.status === 'PAID' && !canCheckIn ? getCheckInWindowMessage(booking) : undefined}
                              className="border border-neutral-700 px-3 py-2 text-[8px] font-black uppercase tracking-widest text-neutral-300 transition hover:border-emerald-400 hover:text-emerald-300 disabled:border-neutral-900 disabled:text-neutral-700"
                            >
                              {booking.status === 'USED' ? 'Đã xác nhận' : booking.status === 'PAID' && !canCheckIn ? 'Chưa mở' : 'Check-in'}
                            </button>
                          </td>
                        </tr>
                      );
                    }) : (
                      <tr>
                        <td colSpan={6} className="px-5 py-12 text-center">
                          <Ticket className="mx-auto h-8 w-8 text-neutral-700" />
                          <p className="mt-3 text-xs font-bold text-neutral-500">Chưa có booking nào trong phiên này.</p>
                          {isShowingShowtimeBookings && <p className="mt-1 text-[10px] font-bold text-neutral-600">Showtime này chưa có booking.</p>}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
              <div className="flex flex-col justify-between gap-3 border-t border-neutral-800 px-5 py-4 text-[9px] font-bold uppercase tracking-widest text-neutral-500 sm:flex-row sm:items-center">
                <span>
                  {visibleBookings.length > 0
                    ? `Hiển thị ${bookingsDisplayStart}-${bookingsDisplayEnd}/${visibleBookings.length} booking - Trang ${safeBookingsPage}/${bookingsTotalPages}`
                    : isShowingShowtimeBookings
                      ? 'Danh sách booking lấy trực tiếp theo showtimeId'
                      : `Hiển thị tối đa ${RECENT_BOOKINGS_LIMIT} booking gần nhất từ API staff/check-in/recent`}
                </span>
                {bookingsTotalPages > 1 && (
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      disabled={safeBookingsPage <= 1}
                      onClick={() => setBookingsPage((page) => Math.max(1, page - 1))}
                      className="inline-flex items-center gap-1 border border-neutral-700 px-3 py-2 text-white transition hover:border-emerald-400 disabled:opacity-30"
                    >
                      <ChevronLeft className="h-3.5 w-3.5" /> Trước
                    </button>
                    <button
                      type="button"
                      disabled={safeBookingsPage >= bookingsTotalPages}
                      onClick={() => setBookingsPage((page) => Math.min(bookingsTotalPages, page + 1))}
                      className="inline-flex items-center gap-1 border border-neutral-700 px-3 py-2 text-white transition hover:border-emerald-400 disabled:opacity-30"
                    >
                      Sau <ChevronRight className="h-3.5 w-3.5" />
                    </button>
                  </div>
                )}
              </div>
            </section>
            {counterSaleBooking && (
              <div className="fixed inset-0 z-[260] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
                <motion.div
                  initial={{ opacity: 0, scale: 0.96 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="w-full max-w-lg border border-purple-400/30 bg-[#070707] p-6 shadow-[0_30px_90px_rgba(0,0,0,0.55)]"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-[9px] font-black uppercase tracking-[0.2em] text-purple-300">Quầy bắp nước</p>
                      <h3 className="mt-1 text-lg font-black uppercase text-white">Bán thêm cho {counterSaleBooking.bookingCode}</h3>
                      <p className="mt-1 text-xs text-neutral-500">{counterSaleBooking.customerName || counterSaleBooking.movieTitle} · thanh toán tiền mặt tại quầy</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setCounterSaleBooking(null)}
                      className="border border-neutral-700 px-3 py-2 text-[10px] font-black uppercase text-neutral-300 hover:border-white hover:text-white"
                    >
                      Đóng
                    </button>
                  </div>

                  {counterSaleReceipt ? (
                    <div className="mt-4">
                      <div className="border border-emerald-400/40 bg-emerald-500/5 px-4 py-4 text-center">
                        <p className="text-2xl">✓</p>
                        <p className="mt-1 text-sm font-black uppercase tracking-widest text-emerald-300">Đã thu tiền mặt</p>
                        <p className="mt-1 font-mono text-xs text-neutral-400">Mã đơn: <span className="font-black text-white">{counterSaleReceipt.orderCode}</span></p>
                      </div>
                      <div className="mt-3 divide-y divide-neutral-900 border border-neutral-800 bg-black text-xs">
                        <div className="flex items-center justify-between px-4 py-2.5">
                          <span className="font-bold uppercase tracking-widest text-neutral-500">Tổng thu</span>
                          <span className="font-mono text-base font-black text-emerald-300">{formatCurrency(counterSaleReceipt.total)}</span>
                        </div>
                        {counterSaleReceipt.cashGiven !== null && (
                          <>
                            <div className="flex items-center justify-between px-4 py-2.5">
                              <span className="font-bold uppercase tracking-widest text-neutral-500">Tiền khách đưa</span>
                              <span className="font-mono font-black text-white">{formatCurrency(counterSaleReceipt.cashGiven)}</span>
                            </div>
                            <div className="flex items-center justify-between px-4 py-2.5">
                              <span className="font-bold uppercase tracking-widest text-neutral-500">Thối lại khách</span>
                              <span className="font-mono font-black text-amber-300">{formatCurrency(counterSaleReceipt.change || 0)}</span>
                            </div>
                          </>
                        )}
                        <div className="flex items-center justify-between px-4 py-2.5">
                          <span className="font-bold uppercase tracking-widest text-neutral-500">Thời gian</span>
                          <span className="font-mono text-neutral-300">{counterSaleReceipt.time}</span>
                        </div>
                        <div className="flex items-center justify-between px-4 py-2.5">
                          <span className="font-bold uppercase tracking-widest text-neutral-500">Người thu</span>
                          <span className="font-bold text-neutral-300">{counterSaleReceipt.collector}</span>
                        </div>
                      </div>
                      <div className="mt-4 grid grid-cols-2 gap-2">
                        <button
                          type="button"
                          onClick={() => setCounterSaleBooking(null)}
                          className="border border-neutral-700 px-4 py-3 text-[10px] font-black uppercase tracking-widest text-neutral-300 transition hover:border-white hover:text-white"
                        >
                          Đóng
                        </button>
                        <button
                          type="button"
                          onClick={() => { setCounterSaleReceipt(null); setCounterSaleMessage(''); }}
                          className="bg-purple-400 px-4 py-3 text-[10px] font-black uppercase tracking-widest text-black transition hover:bg-purple-300"
                        >
                          Bán đơn khác
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="mt-4 max-h-52 overflow-y-auto divide-y divide-neutral-900 border border-neutral-800 bg-black">
                        {staffFoods.filter((food) => food.status === 'ACTIVE' || food.status === 'LOW_STOCK').map((food) => {
                          const foodKey = `${food.kind}-${food.id}`;
                          return (
                            <div key={foodKey} className="flex items-center justify-between gap-3 px-4 py-3">
                              <div className="min-w-0">
                                <p className="truncate text-xs font-black uppercase text-white">{food.name}</p>
                                <p className="mt-0.5 font-mono text-[10px] text-amber-300">{formatCurrency(food.price)} · {food.kind === 'combo' ? 'Combo' : 'Món lẻ'}</p>
                              </div>
                              <div className="flex shrink-0 items-center gap-2">
                                <button
                                  type="button"
                                  onClick={() => changeCounterSaleQuantity(foodKey, -1)}
                                  className="h-7 w-7 border border-neutral-700 text-white transition hover:border-purple-400"
                                >−</button>
                                <span className="w-6 text-center font-mono text-xs font-black text-white">{counterSaleQuantities[foodKey] || 0}</span>
                                <button
                                  type="button"
                                  onClick={() => changeCounterSaleQuantity(foodKey, 1)}
                                  className="h-7 w-7 border border-neutral-700 text-white transition hover:border-purple-400"
                                >+</button>
                              </div>
                            </div>
                          );
                        })}
                        {staffFoods.length === 0 && (
                          <p className="px-4 py-6 text-center text-xs font-bold text-neutral-500">Chưa tải được danh sách bắp nước.</p>
                        )}
                      </div>

                      {counterSaleLines.length > 0 && (
                        <div className="mt-3 border border-purple-400/20 bg-purple-500/5">
                          <p className="border-b border-purple-400/20 px-4 py-2 text-[9px] font-black uppercase tracking-[0.2em] text-purple-300">Soát đơn trước khi thu</p>
                          <div className="divide-y divide-neutral-900">
                            {counterSaleLines.map((line) => (
                              <div key={line.key} className="flex items-center justify-between gap-3 px-4 py-2 text-xs">
                                <span className="min-w-0 truncate font-bold text-neutral-300">{line.name} <span className="font-mono text-neutral-500">× {line.qty}</span></span>
                                <span className="shrink-0 font-mono text-neutral-400">{formatCurrency(line.price)} = <span className="font-black text-white">{formatCurrency(line.lineTotal)}</span></span>
                              </div>
                            ))}
                          </div>
                          <div className="flex items-center justify-between border-t border-purple-400/20 px-4 py-3">
                            <span className="text-[10px] font-black uppercase tracking-widest text-neutral-400">Cần thu</span>
                            <span className="font-mono text-xl font-black text-purple-300">{formatCurrency(counterSaleTotal)}</span>
                          </div>
                        </div>
                      )}

                      <div className="mt-3">
                        <p className="text-[9px] font-black uppercase tracking-[0.2em] text-neutral-500">Tiền khách đưa (bỏ trống nếu thu đúng số)</p>
                        <div className="mt-2 flex items-center gap-2">
                          <input
                            type="number"
                            min="0"
                            step="1000"
                            value={cashGiven}
                            onChange={(event) => setCashGiven(event.target.value)}
                            placeholder="VD: 200000"
                            className="w-full border border-neutral-700 bg-black px-3 py-2.5 font-mono text-sm font-black text-white outline-none focus:border-purple-400"
                          />
                          <button
                            type="button"
                            onClick={() => setCashGiven('')}
                            className="shrink-0 border border-neutral-700 px-3 py-2.5 text-[9px] font-black uppercase text-neutral-400 transition hover:border-white hover:text-white"
                          >
                            Xóa
                          </button>
                        </div>
                        <div className="mt-2 flex flex-wrap gap-2">
                          {[50000, 100000, 200000, 500000].map((amount) => (
                            <button
                              key={amount}
                              type="button"
                              onClick={() => setCashGiven(String(amount))}
                              className="border border-neutral-700 px-3 py-1.5 font-mono text-[10px] font-black text-neutral-300 transition hover:border-purple-400 hover:text-purple-300"
                            >
                              {(amount / 1000).toLocaleString('vi-VN')}k
                            </button>
                          ))}
                        </div>
                        {cashGivenValue !== null && counterSaleTotal > 0 && (
                          cashInsufficient ? (
                            <p className="mt-2 text-xs font-black text-rose-400">Khách đưa thiếu {formatCurrency(counterSaleTotal - cashGivenValue)} — chưa thể xác nhận.</p>
                          ) : (
                            <p className="mt-2 text-xs font-bold text-neutral-300">Thối lại khách: <span className="font-mono text-base font-black text-emerald-300">{formatCurrency(cashChange)}</span></p>
                          )
                        )}
                      </div>

                      {counterSaleMessage && <p className="mt-3 text-xs font-bold text-rose-400">{counterSaleMessage}</p>}

                      <button
                        type="button"
                        onClick={submitCounterSale}
                        disabled={isSavingCounterSale || counterSaleTotal <= 0 || cashInsufficient}
                        className="mt-4 w-full bg-purple-400 px-4 py-3 text-[10px] font-black uppercase tracking-widest text-black transition hover:bg-purple-300 disabled:opacity-40"
                      >
                        {isSavingCounterSale ? 'Đang xác nhận...' : `Xác nhận đã thu ${counterSaleTotal > 0 ? formatCurrency(counterSaleTotal) : 'tiền mặt'}`}
                      </button>
                    </>
                  )}
                </motion.div>
              </div>
            )}
            {manualRefundModal && (
              <div className="fixed inset-0 z-[260] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
                <motion.div
                  initial={{ opacity: 0, scale: 0.96 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="w-full max-w-lg border border-amber-400/30 bg-[#070707] p-6 shadow-[0_30px_90px_rgba(0,0,0,0.55)]"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-[9px] font-black uppercase tracking-[0.2em] text-amber-300">Manual refund</p>
                      <h3 className="mt-1 text-lg font-black uppercase text-white">Xac nhan da hoan tien ngoai</h3>
                    </div>
                    <button
                      type="button"
                      onClick={() => setManualRefundModal(null)}
                      className="border border-neutral-700 px-3 py-2 text-[10px] font-black uppercase text-neutral-300 hover:border-white hover:text-white"
                    >
                      Dong
                    </button>
                  </div>
                  <div className="mt-5 grid gap-3 text-xs sm:grid-cols-2">
                    <div className="border border-neutral-800 bg-black p-3">
                      <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Booking</p>
                      <p className="mt-1 font-mono font-black text-white">{manualRefundModal.bookingCode}</p>
                    </div>
                    <div className="border border-neutral-800 bg-black p-3">
                      <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">So tien</p>
                      <p className="mt-1 font-mono font-black text-amber-300">{formatCurrency(manualRefundModal.refundAmount)}</p>
                    </div>
                    <div className="border border-neutral-800 bg-black p-3">
                      <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Khach hang</p>
                      <p className="mt-1 truncate font-bold text-neutral-300">{manualRefundModal.customerName || manualRefundModal.customerEmail}</p>
                    </div>
                    <div className="border border-neutral-800 bg-black p-3">
                      <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Diem NET</p>
                      <p className="mt-1 font-mono font-black text-emerald-300">
                        {Number(manualRefundModal.loyaltyCalculation?.netBalanceChange || 0).toLocaleString('vi-VN')}
                      </p>
                    </div>
                  </div>
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    <label className="space-y-1.5">
                      <span className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Phuong thuc</span>
                      <select
                        value={manualRefundForm.refundMethod}
                        onChange={(event) => setManualRefundForm((current) => ({ ...current, refundMethod: event.target.value }))}
                        className="w-full border border-neutral-800 bg-black px-3 py-3 text-xs font-bold text-white outline-none focus:border-amber-300"
                      >
                        <option value="MANUAL_BANK_TRANSFER">Bank Transfer</option>
                        <option value="CASH">Cash</option>
                      </select>
                    </label>
                    <label className="space-y-1.5">
                      <span className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Ghi chu</span>
                      <input
                        value={manualRefundForm.notes}
                        onChange={(event) => setManualRefundForm((current) => ({ ...current, notes: event.target.value }))}
                        maxLength={500}
                        placeholder="Transaction ID, STK, ca truc..."
                        className="w-full border border-neutral-800 bg-black px-3 py-3 text-xs font-bold text-white outline-none placeholder:text-neutral-700 focus:border-amber-300"
                      />
                    </label>
                  </div>
                  <div className="mt-5 flex gap-3">
                    <button
                      type="button"
                      onClick={() => setManualRefundModal(null)}
                      className="flex-1 border border-neutral-700 px-4 py-3 text-[10px] font-black uppercase tracking-widest text-neutral-300 transition hover:border-white hover:text-white"
                    >
                      Quay lai
                    </button>
                    <button
                      type="button"
                      onClick={confirmManualRefund}
                      disabled={isSavingManualRefund}
                      className="flex-1 bg-amber-400 px-4 py-3 text-[10px] font-black uppercase tracking-widest text-black transition hover:bg-amber-300 disabled:opacity-50"
                    >
                      {isSavingManualRefund ? 'Dang luu...' : 'Da hoan tien ngoai'}
                    </button>
                  </div>
                </motion.div>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
