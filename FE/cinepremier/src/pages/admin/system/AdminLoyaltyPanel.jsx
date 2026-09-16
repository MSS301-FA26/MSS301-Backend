import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { toast } from 'sonner';
import {
  AlertTriangle,
  Calendar,
  CalendarClock,
  Clock,
  Eye,
  FileText,
  RefreshCw,
  RotateCcw,
  Save,
  Search,
  X
} from 'lucide-react';
import { adminService } from '../../../services/adminService';

const DEFAULT_CONFIG = {
  earningRatePercent: 1,
  redemptionPoints: 1000,
  redemptionValueVnd: 1000,
  expiryMonth: 12,
  expiryDay: 31,
  expiryTime: '23:59:59'
};

const formatNumber = (value) => Number(value || 0).toLocaleString('vi-VN');

const formatDateTime = (value) => {
  if (!value) return 'Chua co du lieu';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('vi-VN');
};

const toDateInputValue = (date) => {
  const next = new Date(date);
  next.setMinutes(next.getMinutes() - next.getTimezoneOffset());
  return next.toISOString().slice(0, 10);
};

const getTodayDateInputValue = () => toDateInputValue(new Date());

const normalizeExpiryTime = (value) => {
  if (!value) return '23:59:59';
  const parts = String(value).split(':');
  const hour = parts[0] || '23';
  const minute = parts[1] || '59';
  const second = parts[2] || '00';
  return `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}:${second.padStart(2, '0')}`;
};

const getExpiryDateValue = (config = DEFAULT_CONFIG) => {
  if (config.expiryDate) return config.expiryDate;
  const now = new Date();
  const month = Math.max(1, Math.min(12, Number(config.expiryMonth || 12)));
  const lastDay = new Date(now.getFullYear(), month, 0).getDate();
  const day = Math.max(1, Math.min(lastDay, Number(config.expiryDay || 31)));
  const [hour, minute, second] = normalizeExpiryTime(config.expiryTime).split(':').map(Number);
  const candidate = new Date(now.getFullYear(), month - 1, day, hour, minute, second || 0);
  if (candidate.getTime() < now.getTime()) candidate.setFullYear(candidate.getFullYear() + 1);
  return toDateInputValue(candidate);
};

const MIN_EXPIRY_LEAD_MINUTES = 15;

const isExpiryDateTimeTooSoon = (expiryDate, expiryTime) => {
  const normalizedTime = normalizeExpiryTime(expiryTime);
  const value = new Date(`${expiryDate}T${normalizedTime}`);
  return Number.isNaN(value.getTime()) || value.getTime() < Date.now() + MIN_EXPIRY_LEAD_MINUTES * 60 * 1000;
};

const transactionMeta = {
  EARN: { label: 'Tích điểm', className: 'border-emerald-500/30 bg-emerald-950/20 text-emerald-300' },
  REDEEM: { label: 'Dùng điểm', className: 'border-amber-500/30 bg-amber-950/20 text-amber-300' },
  EXPIRE: { label: 'Hết hạn', className: 'border-rose-500/30 bg-rose-950/20 text-rose-300' },
  ADJUST: { label: 'Điểu chỉnh', className: 'border-sky-500/30 bg-sky-950/20 text-sky-300' },
  RESTORE: { label: 'Hoàn điểm', className: 'border-cyan-500/30 bg-cyan-950/20 text-cyan-300' },
  REVOKE: { label: 'Thu hồi', className: 'border-fuchsia-500/30 bg-fuchsia-950/20 text-fuchsia-300' }
};

const Field = ({ label, children }) => (
  <label className="space-y-1.5">
    <span className="block text-[10px] font-black uppercase tracking-[0.14em] text-neutral-300">{label}</span>
    {children}
  </label>
);

const inputClass = 'w-full border border-white/[0.06] bg-black px-3 py-2.5 text-xs text-white outline-none transition focus:border-amber-500/70';

export default function AdminLoyaltyPanel({ ctx }) {
  const { activeTab, getAdminToken, showToast } = ctx;
  const [config, setConfig] = useState(DEFAULT_CONFIG);
  const [keyword, setKeyword] = useState('');
  const [transactions, setTransactions] = useState({ items: [], page: 0, totalPages: 1, totalItems: 0 });
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [loading, setLoading] = useState(false);
  const [savingConfig, setSavingConfig] = useState(false);
  const [loadingBooking, setLoadingBooking] = useState(false);
  const [resettingPoints, setResettingPoints] = useState(false);

  const loadAll = async (page = 0) => {
    if (activeTab !== 'loyalty') return;
    const token = getAdminToken?.();
    if (!token) return;
    setLoading(true);
    try {
      const [nextConfig, nextTransactions] = await Promise.all([
        adminService.getLoyaltyConfiguration(token),
        adminService.getLoyaltyTransactions(token, {
          keyword: keyword.trim() || undefined,
          page,
          size: 10
        })
      ]);
      setConfig({ ...DEFAULT_CONFIG, ...nextConfig, expiryDate: getExpiryDateValue(nextConfig) });
      setTransactions({
        items: Array.isArray(nextTransactions?.items) ? nextTransactions.items : [],
        page: Number(nextTransactions?.page || 0),
        totalPages: Math.max(1, Number(nextTransactions?.totalPages || 1)),
        totalItems: Number(nextTransactions?.totalItems || 0)
      });
    } catch (error) {
      showToast?.(error.message || 'Không thể tải dữ liệu điểm.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll(0);
  }, [activeTab]);

  if (activeTab !== 'loyalty') return null;

  const updateConfig = (field, value) => {
    setConfig((prev) => ({ ...prev, [field]: value }));
  };

  const updateExpiryDate = (value) => {
    const date = new Date(`${value}T${normalizeExpiryTime(config.expiryTime)}`);
    setConfig((prev) => ({
      ...prev,
      expiryDate: value,
      expiryMonth: Number.isNaN(date.getTime()) ? prev.expiryMonth : date.getMonth() + 1,
      expiryDay: Number.isNaN(date.getTime()) ? prev.expiryDay : date.getDate()
    }));
  };

  const saveConfig = async (event) => {
    event.preventDefault();
    const token = getAdminToken?.();
    if (!token) return;
    const expiryDate = getExpiryDateValue(config);
    const expiryTime = normalizeExpiryTime(config.expiryTime);
    const earningRatePercent = Number(config.earningRatePercent);
    if (!Number.isFinite(earningRatePercent) || earningRatePercent < 0 || earningRatePercent > 100) {
      showToast?.('Tỷ lệ tích điểm phải nằm trong khoảng 0% đến 100%.', 'error');
      return;
    }
    const redemptionPoints = Number(config.redemptionPoints);
    if (!Number.isInteger(redemptionPoints) || redemptionPoints < 1) {
      showToast?.('Số điểm quy đổi phải là số nguyên lớn hơn hoặc bằng 1.', 'error');
      return;
    }
    const redemptionValueVnd = Number(config.redemptionValueVnd);
    if (!Number.isFinite(redemptionValueVnd) || redemptionValueVnd <= 0) {
      showToast?.('Giá trị giảm phải lớn hơn 0 VND.', 'error');
      return;
    }
    if (isExpiryDateTimeTooSoon(expiryDate, expiryTime)) {
      showToast?.('Ngày và giờ reset điểm phải sau thời điểm hiện tại ít nhất 15 phút.', 'error');
      return;
    }
    setSavingConfig(true);
    try {
      const selectedDate = new Date(`${expiryDate}T${expiryTime}`);
      const payload = {
        earningRatePercent,
        redemptionPoints,
        redemptionValueVnd,
        expiryMonth: selectedDate.getMonth() + 1,
        expiryDay: selectedDate.getDate(),
        expiryDate,
        expiryTime
      };
      const saved = await adminService.updateLoyaltyConfiguration(token, payload);
      setConfig({ ...DEFAULT_CONFIG, ...saved, expiryDate: getExpiryDateValue(saved) });
      showToast?.('Đã lưu cấu hình điểm.', 'success');
    } catch (error) {
      showToast?.(error.message || 'Không thể lưu cấu hình điểm.', 'error');
    } finally {
      setSavingConfig(false);
    }
  };

  const resetPoints = async (toastId) => {
    toast.dismiss(toastId);
    const token = getAdminToken?.();
    if (!token) return;
    setResettingPoints(true);
    try {
      const affected = await adminService.expireLoyaltyPointsNow(token);
      showToast?.(`Đã reset điểm cho ${formatNumber(affected)} tài khoản.`, 'success');
      await loadAll(0);
    } catch (error) {
      showToast?.(error.message || 'Không thể reset điểm.', 'error');
    } finally {
      setResettingPoints(false);
    }
  };

  const showResetConfirmToast = () => {
    toast.custom((toastId) => (
      <div className="loyalty-reset-toast w-full  border border-amber-500/25 bg-[#080808]/95 p-4 text-white shadow-[0_24px_80px_rgba(0,0,0,0.72)] backdrop-blur-xl sm:p-5">
        <div className="flex items-start gap-3">
          <div className="mt-0.5 grid h-10 w-10 shrink-0 place-items-center rounded-full border border-amber-400/30 bg-amber-500/10 text-amber-300 shadow-[0_0_28px_rgba(245,158,11,0.2)]">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <div className="min-w-0 flex-1">
            <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">Xác nhận reset điểm</h3>
            <p className="mt-2 text-xs leading-relaxed text-neutral-300">
              Thao tác này sẽ đặt toàn bộ điểm hiện tại về 0. Hành động này không thể hoàn tác.
            </p>
          </div>
        </div>
        <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={() => toast.dismiss(toastId)}
            className="inline-flex items-center justify-center  border border-white/[0.08] bg-neutral-900 px-4 py-2.5 text-[11px] font-black uppercase tracking-[0.14em] text-neutral-300 transition hover:border-white/[0.15] hover:bg-white/[0.04] hover:text-white"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={() => resetPoints(toastId)}
            className="inline-flex items-center justify-center  border border-red-500/50 bg-red-600 px-4 py-2.5 text-[11px] font-black uppercase tracking-[0.14em] text-white shadow-[0_10px_26px_rgba(220,38,38,0.28)] transition duration-200 hover:-translate-y-0.5 hover:scale-[1.02] hover:bg-red-500 hover:shadow-[0_16px_34px_rgba(220,38,38,0.4)] active:translate-y-0 active:scale-100"
          >
            Reset ngay
          </button>
        </div>
      </div>
    ), { duration: Infinity });
  };

  const openBooking = async (bookingId) => {
    if (!bookingId) return;
    const token = getAdminToken?.();
    if (!token) return;
    setLoadingBooking(true);
    try {
      const booking = await adminService.getAdminBooking(token, bookingId);
      setSelectedBooking(booking);
    } catch (error) {
      showToast?.(error.message || 'Không thể tải chi tiết đơn hàng.');
    } finally {
      setLoadingBooking(false);
    }
  };

  return (
    <motion.div
      key="panel-loyalty"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.2 }}
      className="space-y-5"
    >
      <div className="flex flex-col gap-3 border border-white/[0.05] bg-black p-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">QUẢN LÝ ĐIỂM THÀNH VIÊN</h3>
          <p className="mt-1 break-words text-[10px] leading-snug text-neutral-300">Cấu hình tích/tiêu điểm, audit trail và báo cáo điểm</p>
        </div>
        <button
          type="button"
          onClick={() => loadAll(transactions.page)}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 border border-amber-500/35 bg-amber-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-wider text-amber-300 transition hover:bg-amber-500 hover:text-black disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
          LÀM MỚI
        </button>
      </div>

      <div className="grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
        <form onSubmit={saveConfig} noValidate className="border border-white/[0.05] bg-neutral-950 p-4">
          <div className="mb-4 flex items-center justify-between border-b border-white/[0.05] pb-3">
            <div>
              <h4 className="text-[11px] font-black uppercase tracking-[0.16em] text-white">Cấu hình vận hành</h4>

            </div>
            <CalendarClock className="h-4 w-4 text-amber-400" />
          </div>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
            <Field label="Tỷ lệ tích điểm (%)">
              <input className={inputClass} type="number" min="0" max="100" maxLength={3} step="0.01" value={config.earningRatePercent} onChange={(e) => updateConfig('earningRatePercent', e.target.value)} />
            </Field>
            <Field label="Số điểm quy đổi">
              <input className={inputClass} type="number" min="1" step="1" value={config.redemptionPoints} onChange={(e) => updateConfig('redemptionPoints', e.target.value)} />
            </Field>
            <Field label="Giá trị giảm (VND)">
              <input className={inputClass} type="number" min="1" step="1" value={config.redemptionValueVnd} onChange={(e) => updateConfig('redemptionValueVnd', e.target.value)} />
            </Field>
            <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-1">
              <Field label="Ngày reset điểm">
                <div className="relative">
                  <Calendar className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-amber-400" />
                  <input
                    className={`${inputClass} pl-9`}
                    type="date"
                    min={getTodayDateInputValue()}
                    value={getExpiryDateValue(config)}
                    onChange={(e) => updateExpiryDate(e.target.value)}
                  />
                </div>
              </Field>
              <Field label="Giờ reset điểm">
                <div className="relative">
                  <Clock className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-amber-400" />
                  <input
                    className={`${inputClass} pl-9`}
                    type="time"
                    step="1"
                    value={normalizeExpiryTime(config.expiryTime)}
                    onChange={(e) => updateConfig('expiryTime', normalizeExpiryTime(e.target.value))}
                  />
                </div>
              </Field>
            </div>
          </div>
          <div className="mt-4 flex flex-col gap-2 sm:flex-row">
            <button type="submit" disabled={savingConfig} className="inline-flex flex-1 items-center justify-center gap-2 border border-emerald-500/35 bg-emerald-500/10 px-3 py-2.5 text-[10px] font-black uppercase tracking-wider text-emerald-300 transition hover:bg-emerald-500 hover:text-black disabled:opacity-50">
              <Save className="h-3.5 w-3.5" />
              Lưu cấu hình
            </button>
            <button type="button" onClick={showResetConfirmToast} disabled={resettingPoints} className="inline-flex items-center justify-center gap-2 border border-rose-500/35 bg-rose-500/10 px-3 py-2.5 text-[10px] font-black uppercase tracking-wider text-rose-300 transition hover:bg-rose-500 hover:text-black disabled:opacity-50">
              <RotateCcw className="h-3.5 w-3.5" />
              Reset ngay
            </button>
          </div>
          {config.lastExpiredAt && (
            <p className="mt-3 text-[10px] text-neutral-400">Lần reset gần nhất: {config.lastExpiredAt}</p>
          )}
        </form>

        <div className="space-y-4">
          <div className="border border-white/[0.05] bg-neutral-950">
            <div className="flex flex-col gap-3 border-b border-white/[0.05] bg-black p-4 md:flex-row md:items-center md:justify-between">
              <div>
                <h4 className="text-[11px] font-black uppercase tracking-[0.16em] text-white">Audit trail điểm</h4>
                <p className="mt-1 break-words text-[10px] leading-snug text-neutral-300">Tìm theo mã khách hàng, số điện thoại, email hoăc mã đơn hàng.</p>
              </div>
              <form onSubmit={(event) => { event.preventDefault(); loadAll(0); }} className="flex min-w-0 flex-1 gap-2 md:max-w-sm">
                <div className="relative min-w-0 flex-1">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-neutral-300" />
                  <input className={`${inputClass} pl-9`} value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Customer ID, phone, email, booking..." />
                </div>
                <button type="submit" className="inline-flex items-center justify-center border border-amber-500/35 bg-amber-500/10 px-3 text-[10px] font-black uppercase tracking-wider text-amber-300 hover:bg-amber-500 hover:text-black">
                  Lọc
                </button>
              </form>
            </div>
            <div className="overflow-hidden">
              <table className="w-full table-fixed border-collapse text-left">
                <colgroup>
                  <col className="w-[78px]" />
                  <col className="w-[140px]" />
                  <col />
                  <col className="w-[96px]" />
                  <col className="w-[100px]" />
                  <col className="w-[124px]" />
                </colgroup>
                <thead>
                  <tr className="border-b border-white/[0.05] bg-neutral-950 text-[9.5px] font-black uppercase tracking-wider text-neutral-300">
                    <th className="px-3 py-3">Tx ID</th>
                    <th className="px-3 py-3">Booking</th>
                    <th className="px-3 py-3">Tài khoản</th>
                    <th className="px-3 py-3">Loại</th>
                    <th className="px-3 py-3 text-right">Biến động</th>
                    <th className="px-3 py-3">Thời gian</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/[0.03] text-xs">
                  {transactions.items.map((tx) => {
                    const meta = transactionMeta[tx.type] || transactionMeta.ADJUST;
                    return (
                      <tr key={tx.pointTransactionId} className="hover:bg-white/[0.02]">
                        <td className="px-3 py-3 font-mono text-neutral-300">#{tx.pointTransactionId}</td>
                        <td className="px-3 py-3">
                          {tx.bookingId ? (
                            <button type="button" onClick={() => openBooking(tx.bookingId)} className="inline-flex max-w-full items-center gap-1.5 font-mono text-amber-300 hover:text-amber-100">
                              <Eye className="h-3.5 w-3.5" />
                              <span className="whitespace-nowrap">{tx.bookingCode || `#${tx.bookingId}`}</span>
                            </button>
                          ) : (
                            <span className="text-neutral-200">Hệ thống</span>
                          )}
                          {tx.movieTitle && <div className="mt-1 break-words text-[10px] leading-snug text-neutral-300">{tx.movieTitle}</div>}
                        </td>
                        <td className="px-3 py-3">
                          <div className="break-words font-bold leading-snug text-white">{tx.customerName || tx.customerEmail}</div>
                          <div className="break-all text-[10px] leading-snug text-neutral-300">{tx.customerPhone || 'Khong co SĐT'} | {tx.customerEmail}</div>
                        </td>
                        <td className="px-3 py-3">
                          <span className={`inline-flex border px-1.5 py-1 text-[8.5px] font-black uppercase tracking-wide ${meta.className}`}>{meta.label}</span>
                        </td>
                        <td className={`px-3 py-3 text-right font-mono font-black ${tx.pointsDelta >= 0 ? 'text-emerald-300' : 'text-rose-300'}`}>
                          {tx.pointsDelta >= 0 ? '+' : ''}{formatNumber(tx.pointsDelta)}
                          <div className="text-[10px] font-normal text-neutral-300">So du: {formatNumber(tx.balanceAfter)}</div>
                        </td>
                        <td className="px-3 py-3 font-mono text-[10.5px] leading-snug text-neutral-200">{formatDateTime(tx.occurredAt)}</td>
                      </tr>
                    );
                  })}
                  {!transactions.items.length && (
                    <tr>
                      <td colSpan="6" className="px-4 py-10 text-center text-xs text-neutral-300">Chưa có giao dịch phù hợp</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between border-t border-white/[0.05] px-4 py-3 text-[10px] text-neutral-300">
              <span>{formatNumber(transactions.totalItems)} giao dịch</span>
              <div className="flex items-center gap-2">
                <button type="button" disabled={transactions.page <= 0 || loading} onClick={() => loadAll(transactions.page - 1)} className="border border-white/[0.06] px-3 py-1.5 font-bold uppercase text-neutral-300 disabled:opacity-40">Truoc</button>
                <span className="font-mono">{transactions.page + 1}/{transactions.totalPages}</span>
                <button type="button" disabled={transactions.page + 1 >= transactions.totalPages || loading} onClick={() => loadAll(transactions.page + 1)} className="border border-white/[0.06] px-3 py-1.5 font-bold uppercase text-neutral-300 disabled:opacity-40">Sau</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <AnimatePresence>
        {(selectedBooking || loadingBooking) && (
          <motion.div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/75 p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <motion.div className="w-full max-w-2xl border border-white/[0.06] bg-neutral-950 shadow-2xl" initial={{ scale: 0.98, y: 12 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.98, y: 12 }}>
              <div className="flex items-center justify-between border-b border-white/[0.05] bg-black p-4">
                <div>
                  <h4 className="text-[11px] font-black uppercase tracking-[0.16em] text-white">Chi tiết đơn hàng</h4>
                  <p className="mt-1 font-mono text-[10px] text-amber-300">{selectedBooking?.bookingCode || 'Đang tải...'}</p>
                </div>
                <button type="button" onClick={() => setSelectedBooking(null)} className="border border-white/[0.06] p-2 text-neutral-200 hover:border-rose-500/50 hover:text-rose-300">
                  <X className="h-4 w-4" />
                </button>
              </div>
              {selectedBooking && (
                <div className="grid gap-3 p-4 text-xs text-neutral-300 sm:grid-cols-2">
                  <Detail label="Phim" value={selectedBooking.movieTitle} />
                  <Detail label="Suat chieu" value={formatDateTime(selectedBooking.showtimeStart)} />
                  <Detail label="Rap / phong" value={`${selectedBooking.cinemaName || ''} ${selectedBooking.roomName || ''}`} />
                  <Detail label="Trang thai" value={selectedBooking.status} />
                  <Detail label="Tam tinh" value={`${formatNumber(selectedBooking.subtotal)} VND`} />
                  <Detail label="Diem da tieu" value={formatNumber(selectedBooking.loyaltyPointsRedeemed)} />
                  <Detail label="Giam gia" value={`${formatNumber(selectedBooking.discountAmount)} VND`} />
                  <Detail label="Thanh toan" value={`${formatNumber(selectedBooking.totalAmount)} VND`} />
                  <div className="sm:col-span-2 border border-white/[0.05] bg-black/40 p-3">
                    <div className="mb-2 flex items-center gap-2 text-[10px] font-black uppercase tracking-wider text-neutral-300">
                      <FileText className="h-3.5 w-3.5" />
                      Ghe / bap nuoc
                    </div>
                    <p className="text-neutral-300">
                      {(selectedBooking.seats || []).map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(', ') || 'Khong co ghe'}
                    </p>
                    {!!selectedBooking.foods?.length && (
                      <p className="mt-2 text-neutral-300">{selectedBooking.foods.map((food) => `${food.name} x${food.quantity}`).join(', ')}</p>
                    )}
                  </div>
                </div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

function Detail({ label, value }) {
  return (
    <div className="border border-white/[0.05] bg-black/40 p-3">
      <div className="text-[9px] font-black uppercase tracking-wider text-neutral-300">{label}</div>
      <div className="mt-1 font-medium text-white">{value || 'Chưa có dữ liệu'}</div>
    </div>
  );
}



