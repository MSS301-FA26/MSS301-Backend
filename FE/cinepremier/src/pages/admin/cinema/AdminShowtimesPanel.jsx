import React, { useCallback, useEffect, useState, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Plus, Trash2, Edit3, Calendar, Clock, MapPin, Film,
  Search, RefreshCw, Eye, CheckCircle2, XCircle, AlertCircle,
  ChevronDown, ChevronLeft, ChevronRight, Layers, DollarSign,
  Play, PauseCircle, Ban, Check, X, Info, Zap
} from 'lucide-react';
import { adminService } from '../../../services/adminService';

/* bulk slot controls */
const STATUS_META = {
  SCHEDULED: { label: 'Đã lên lịch', color: 'text-blue-400', bg: 'bg-blue-950/30 border-blue-500/30', dot: 'bg-blue-400' },
  OPEN: { label: 'Đang mở bán', color: 'text-emerald-400', bg: 'bg-emerald-950/30 border-emerald-500/30', dot: 'bg-emerald-400' },
  COMPLETED: { label: 'Đã kết thúc', color: 'text-neutral-200', bg: 'bg-white/[0.02] border-white/[0.06]', dot: 'bg-neutral-500' },
  CANCELLED: { label: 'Đã hủy', color: 'text-rose-400', bg: 'bg-rose-950/30 border-rose-500/30', dot: 'bg-rose-400' },
};

const fmt = (dt) => {
  if (!dt) return '—';
  const d = new Date(dt);
  return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

const fmtPrice = (n) => n ? Number(n).toLocaleString('vi-VN') + 'đ' : '—';
const hasDetailPrice = (value) => value !== null && value !== undefined && value !== '';
const fmtDetailPrice = (value) => hasDetailPrice(value) ? `${Number(value).toLocaleString('vi-VN')}đ` : null;

const toInputDatetime = (dt) => {
  if (!dt) return '';
  const d = new Date(dt);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const toApiLocalDateTime = (value) => {
  if (!value) return '';
  return String(value).length === 16 ? `${value}:00` : String(value).slice(0, 19);
};

const toLocalDateInput = (date = new Date()) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const normalizeDateInput = (value) => {
  if (!value) return '';
  const raw = String(value).slice(0, 10);
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? '' : toLocalDateInput(parsed);
};

const formatDateInput = (value) => {
  const normalized = normalizeDateInput(value);
  return normalized ? normalized.split('-').reverse().join('/') : '--/--/----';
};

const maxDateInput = (...values) => {
  const sorted = values.filter(Boolean).sort();
  return sorted.length ? sorted[sorted.length - 1] : '';
};

const buildDateTimeOnDate = (sourceDateTime, targetDate) => {
  if (!sourceDateTime || !targetDate) return '';
  const source = new Date(sourceDateTime);
  if (Number.isNaN(source.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${targetDate}T${pad(source.getHours())}:${pad(source.getMinutes())}`;
};

const isFutureDateInput = (dateValue) => {
  if (!dateValue) return false;
  const today = toLocalDateInput();
  return dateValue > today;
};

const EMPTY_FORM = {
  movieId: '', roomId: '', startTime: '',
  basePrice: '', vipPrice: '', couplePrice: '',
  adultStandardPrice: '', childStandardPrice: '', studentStandardPrice: '',
  adultVipPrice: '', childVipPrice: '', studentVipPrice: '',
  adultCouplePrice: '', childCouplePrice: '', studentCouplePrice: '',
  weekendSurcharge: false, holidaySurcharge: false,
  lateNightSurchargeAmount: '20.000',
  status: 'SCHEDULED'
};

const EMPTY_BULK = {
  movieId: '', basePrice: '', vipPrice: '', couplePrice: '',
  adultStandardPrice: '', childStandardPrice: '', studentStandardPrice: '',
  adultVipPrice: '', childVipPrice: '', studentVipPrice: '',
  adultCouplePrice: '', childCouplePrice: '', studentCouplePrice: '',
  weekendSurcharge: false, holidaySurcharge: false,
  lateNightSurchargeAmount: '20.000',
  defaultStatus: 'SCHEDULED',
  slots: [{ roomId: '', startTime: '', selected: true }]
};

const PRICE_ROWS = [
  { key: 'adult', label: 'Người lớn' },
  { key: 'child', label: 'Trẻ em' },
  { key: 'student', label: 'Sinh viên' },
];

const PRICE_COLS = [
  { key: 'Standard', label: 'Ghế thường', addon: 0 },
  { key: 'Vip', label: 'VIP', addon: 20000 },
  { key: 'Couple', label: 'Ghế đôi', addon: 30000 },
];

const priceField = (row, col) => `${row}${col}Price`;
const priceDigits = (value) => String(value ?? '').replace(/\D/g, '');
const toPriceNumber = (value) => Number(priceDigits(value) || 0);
const hasPriceInput = (value) => priceDigits(value).length > 0;
const canAutoDeriveSeatPrice = (value) => {
  if (!hasPriceInput(value)) return false;
  const price = toPriceNumber(value);
  return price >= 10000;
};
const MIN_LATE_NIGHT_SURCHARGE = 10000;
const MAX_LATE_NIGHT_SURCHARGE = 100000;
const formatPriceInput = (value) => {
  const digits = priceDigits(value);
  return digits ? Number(digits).toLocaleString('vi-VN') : '';
};
const withDerivedSeatPrices = (next, rowKey) => {
  const standardField = priceField(rowKey, 'Standard');
  const vipField = priceField(rowKey, 'Vip');
  const coupleField = priceField(rowKey, 'Couple');
  const standard = toPriceNumber(next[standardField]);
  if (!canAutoDeriveSeatPrice(next[standardField])) {
    return {
      ...next,
      [vipField]: '',
      [coupleField]: '',
    };
  }
  return {
    ...next,
    [vipField]: formatPriceInput(standard + 20000),
    [coupleField]: formatPriceInput(standard + 30000),
  };
};

const priceOrFallback = (value, fallbackValue) => (
  hasPriceInput(value) ? toPriceNumber(value) : toPriceNumber(fallbackValue)
);

const priceOrNull = (value) => (
  hasPriceInput(value) ? toPriceNumber(value) : null
);

const seatPriceOrDerived = (value, standardValue, addon) => (
  hasPriceInput(value) && toPriceNumber(value) >= toPriceNumber(standardValue)
    ? toPriceNumber(value)
    : toPriceNumber(standardValue) + addon
);

const displaySeatPrice = (matrixValue, rowKey, colKey) => {
  const currentValue = matrixValue?.[priceField(rowKey, colKey)];
  if (colKey === 'Standard') return formatPriceInput(currentValue);
  const standardValue = matrixValue?.[priceField(rowKey, 'Standard')];
  if (!canAutoDeriveSeatPrice(standardValue)) return '';

  const derivedValue = toPriceNumber(standardValue) + (colKey === 'Vip' ? 20000 : 30000);
  if (!hasPriceInput(currentValue) || toPriceNumber(currentValue) < toPriceNumber(standardValue)) {
    return formatPriceInput(derivedValue);
  }
  return formatPriceInput(currentValue);
};

const validateStandardTicketPrice = (value, message) => (
  hasPriceInput(value) && !canAutoDeriveSeatPrice(value) ? message : null
);

const buildShowtimePayload = (formState, allowChildTickets = true) => ({
  basePrice: priceOrFallback(formState.adultStandardPrice, formState.basePrice),
  vipPrice: priceOrNull(formState.adultVipPrice),
  couplePrice: priceOrNull(formState.adultCouplePrice),
  adultStandardPrice: priceOrFallback(formState.adultStandardPrice, formState.basePrice),
  childStandardPrice: allowChildTickets ? priceOrFallback(formState.childStandardPrice, formState.basePrice) : null,
  studentStandardPrice: priceOrFallback(formState.studentStandardPrice, formState.basePrice),
  adultVipPrice: seatPriceOrDerived(formState.adultVipPrice, priceOrFallback(formState.adultStandardPrice, formState.basePrice), 20000),
  childVipPrice: allowChildTickets ? seatPriceOrDerived(formState.childVipPrice, priceOrFallback(formState.childStandardPrice, formState.basePrice), 20000) : null,
  studentVipPrice: seatPriceOrDerived(formState.studentVipPrice, priceOrFallback(formState.studentStandardPrice, formState.basePrice), 20000),
  adultCouplePrice: seatPriceOrDerived(formState.adultCouplePrice, priceOrFallback(formState.adultStandardPrice, formState.basePrice), 30000),
  childCouplePrice: allowChildTickets ? seatPriceOrDerived(formState.childCouplePrice, priceOrFallback(formState.childStandardPrice, formState.basePrice), 30000) : null,
  studentCouplePrice: seatPriceOrDerived(formState.studentCouplePrice, priceOrFallback(formState.studentStandardPrice, formState.basePrice), 30000),
  weekendSurcharge: Boolean(formState.weekendSurcharge),
  holidaySurcharge: Boolean(formState.holidaySurcharge),
  lateNightSurchargeAmount: toPriceNumber(formState.lateNightSurchargeAmount || 20000),
});

const addMinutes = (value, minutes) => {
  if (!value || !minutes) return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  d.setMinutes(d.getMinutes() + minutes);
  return d;
};

const formatTimeOnly = (value) => {
  if (!value) return '--:--';
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return '--:--';
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

const getRoomCapacity = (room) => {
  const direct = Number(room?.capacity ?? room?.totalSeats ?? room?.seatCount ?? room?.numberOfSeats);
  if (Number.isFinite(direct) && direct > 0) return direct;
  const rows = Number(room?.rows ?? room?.rowCount);
  const columns = Number(room?.columns ?? room?.cols ?? room?.columnCount);
  return Number.isFinite(rows) && Number.isFinite(columns) ? rows * columns : 0;
};

const getBulkSlotIndexFromError = (message) => {
  const normalizedMessage = String(message || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/Ä'/g, 'd')
    .replace(/Đ/g, 'D');
  const match = normalizedMessage.match(/thoi gian so\s*(\d+)/i);
  const slotNumber = match ? Number(match[1]) : 0;
  return Number.isFinite(slotNumber) && slotNumber > 0 ? slotNumber - 1 : null;
};

const getShowtimeRoomId = (showtime) => showtime?.roomId ?? showtime?.room?.id;

const intervalsOverlap = (startA, endA, startB, endB) => (
  startA < endB && startB < endA
);

const isNightSlot = (value) => {
  if (!value) return false;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return false;
  const hour = d.getHours();
  return hour >= 23 || hour < 5;
};

const getMovieOptionId = (movie) => String(movie?.backendId ?? movie?.id ?? '');

const getAgeRatingMinimum = (ageRating) => {
  if (!ageRating) return 0;
  const match = String(ageRating).match(/\d+/);
  return match ? Number(match[0]) : 0;
};

const CHILD_TICKET_MAX_AGE = 12;
const allowsChildTicketsForMovie = (movie) => getAgeRatingMinimum(movie?.ageRating) <= CHILD_TICKET_MAX_AGE;

const clearChildPrices = (state) => ({
  ...state,
  childStandardPrice: '',
  childVipPrice: '',
  childCouplePrice: '',
});

const validateLateNightSurcharge = (value) => {
  const amount = toPriceNumber(value);
  if (!amount) return 'Nhập phụ thu đêm';
  if (amount < MIN_LATE_NIGHT_SURCHARGE) return 'Phụ thu đêm tối thiểu 10.000';
  if (amount > MAX_LATE_NIGHT_SURCHARGE) return 'Phụ thu đêm tối đa 100.000';
  return '';
};

/**
 * DateTimePicker
 * Renders a compact date <input> + manual time inputs.
 * value  : "YYYY-MM-DDTHH:mm" (same as datetime-local)
 * onChange: (newValue: string) => void
 */
function DateTimePicker({ value, onChange, error, label, minDate, maxDate, helpText }) {
  const dateInputRef = React.useRef(null);
  const datePart = value ? value.split('T')[0] : '';
  const timePart = value && value.includes('T') ? value.split('T')[1].slice(0, 5) : '';
  const today = toLocalDateInput();
  const effectiveMinDate = maxDateInput(today, minDate);
  const hasValidDateRange = !maxDate || !effectiveMinDate || effectiveMinDate <= maxDate;

  const prevValueRef = React.useRef(value);

  const clampDateToRange = (dateValue) => {
    if (!dateValue) return '';
    if (effectiveMinDate && dateValue < effectiveMinDate) return effectiveMinDate;
    if (maxDate && dateValue > maxDate) return maxDate;
    return dateValue;
  };

  const handleDate = (e) => {
    const d = clampDateToRange(e.target.value);
    onChange(d ? `${d}T${timePart || '00:00'}` : '');
  };

  const handleTime = (t) => {
    if (!hasValidDateRange) return;
    const d = clampDateToRange(datePart || effectiveMinDate || toLocalDateInput());
    onChange(`${d}T${t}`);
  };

  const openDatePicker = () => {
    if (!hasValidDateRange) return;
    const input = dateInputRef.current;
    if (!input) return;
    input.focus();
    if (typeof input.showPicker === 'function') {
      input.showPicker();
    }
  };

  return (
    <div className="space-y-2">
      {label && (
        <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black block">{label}</label>
      )}
      {/* Date row */}
      <div className="relative">
        <input
          ref={dateInputRef}
          type="date"
          min={effectiveMinDate || undefined}
          max={maxDate || undefined}
          value={datePart}
          disabled={!hasValidDateRange}
          onChange={handleDate}
          className="h-11 w-full bg-black border border-white/[0.08] py-2.5 pl-3 pr-11 text-xs text-white font-mono [color-scheme:dark] focus:outline-none focus:border-amber-400 disabled:cursor-not-allowed disabled:opacity-50 [&::-webkit-calendar-picker-indicator]:opacity-0 [&::-webkit-calendar-picker-indicator]:absolute [&::-webkit-calendar-picker-indicator]:right-0 [&::-webkit-calendar-picker-indicator]:w-11 [&::-webkit-calendar-picker-indicator]:h-full [&::-webkit-calendar-picker-indicator]:cursor-pointer"
        />
        <button
          type="button"
          onClick={openDatePicker}
          disabled={!hasValidDateRange}
          title="Chọn ngày chiếu"
          className="absolute right-2 top-1/2 grid h-7 w-7 -translate-y-1/2 place-items-center border border-amber-500/30 bg-amber-500/10 text-amber-300 transition hover:bg-amber-500 hover:text-black disabled:cursor-not-allowed disabled:opacity-40 pointer-events-none"
        >
          <Calendar className="h-3.5 w-3.5" />
        </button>
      </div>
      {!hasValidDateRange ? (
        <p className="text-[9px] font-bold text-rose-300">Không có ngày hợp lệ trong thời gian phát hành.</p>
      ) : (
        helpText && <p className="text-[9px] text-neutral-300 font-bold">{helpText}</p>
      )}
      {/* Time inputs */}
      <div className="border border-white/[0.08] bg-black p-3 mt-1">
        <p className="text-[9px] uppercase tracking-widest text-neutral-200 mb-2.5 font-bold">Giờ chiếu</p>
        <div className="flex items-center gap-1.5">
          <select
            disabled={!hasValidDateRange}
            value={timePart ? timePart.split(':')[0] : '00'}
            onChange={(e) => handleTime(`${e.target.value}:${timePart ? timePart.split(':')[1] : '00'}`)}
            className="flex-1 bg-black border border-white/[0.08] text-white text-sm font-mono font-bold text-center py-2.5 focus:border-amber-400 focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed [color-scheme:dark] appearance-none cursor-pointer hover:border-white/[0.15] transition-colors"
          >
            {Array.from({ length: 24 }, (_, i) => (
              <option key={i} value={String(i).padStart(2, '0')}>{String(i).padStart(2, '0')} giờ</option>
            ))}
          </select>
          <span className="text-neutral-400 font-black text-lg shrink-0">:</span>
          <select
            disabled={!hasValidDateRange}
            value={timePart ? timePart.split(':')[1] : '00'}
            onChange={(e) => handleTime(`${timePart ? timePart.split(':')[0] : '00'}:${e.target.value}`)}
            className="flex-1 bg-black border border-white/[0.08] text-white text-sm font-mono font-bold text-center py-2.5 focus:border-amber-400 focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed [color-scheme:dark] appearance-none cursor-pointer hover:border-white/[0.15] transition-colors"
          >
            {Array.from({ length: 60 }, (_, i) => (
              <option key={i} value={String(i).padStart(2, '0')}>{String(i).padStart(2, '0')} phút</option>
            ))}
          </select>
        </div>
        {timePart && (
          <p className="text-[9px] text-amber-400 font-mono font-bold mt-2.5 text-center bg-amber-900/20 py-1.5 rounded border border-amber-900/30">
            ✓ Đã chọn: {datePart ? `${datePart.split('-').reverse().join('/')} lúc ${timePart}` : timePart}
          </p>
        )}
      </div>
      {error && <p className="text-rose-400 text-[9px]">{error}</p>}
    </div>
  );
}

function PriceMatrix({ value, onChange, errors = {}, prefix = '', allowChildTickets = true, ageRatingLabel = '' }) {
  const setPrice = (field, nextValue) => {
    const next = { ...value, [field]: formatPriceInput(nextValue) };
    const row = PRICE_ROWS.find(item => field === priceField(item.key, 'Standard'));
    onChange(row ? withDerivedSeatPrices(next, row.key) : next);
  };
  const visibleRows = PRICE_ROWS.filter(row => allowChildTickets || row.key !== 'child');

  return (
    <div className="md:col-span-2 border border-white/[0.06] bg-black/30 p-4 space-y-3">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <p className="text-[11px] uppercase tracking-widest text-amber-500 font-black">Bảng giá theo lứa tuổi</p>
          <p className="text-[10px] text-neutral-300">Nhập giá ghế thường, hệ thống tự cộng VIP +20k và ghế đôi +30k. Bạn có thể sửa từng mục.</p>
        </div>
        <div className="flex gap-3 text-[9px] uppercase tracking-wider text-neutral-200">
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input type="checkbox" checked={Boolean(value.weekendSurcharge)}
              onChange={e => onChange({ ...value, weekendSurcharge: e.target.checked })}
              className="accent-amber-500" />
            Cuối tuần +10k
          </label>
          <label className="flex items-center gap-1.5 cursor-pointer">
            <input type="checkbox" checked={Boolean(value.holidaySurcharge)}
              onChange={e => onChange({ ...value, holidaySurcharge: e.target.checked })}
              className="accent-amber-500" />
            Ngày lễ +10k
          </label>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[560px] text-[10px]">
          <thead>
            <tr className="text-left text-neutral-300 uppercase tracking-wider">
              <th className="py-2 pr-2">Loại vé</th>
              {PRICE_COLS.map(col => <th key={col.key} className="py-2 px-2">{col.label}</th>)}
            </tr>
          </thead>
          <tbody>
            {visibleRows.map(row => (
              <tr key={row.key} className="border-t border-white/[0.06]">
                <td className="py-2 pr-2 text-neutral-200 font-bold uppercase whitespace-nowrap">{row.label}</td>
                {PRICE_COLS.map(col => {
                  const field = priceField(row.key, col.key);
                  return (
                    <td key={field} className="py-2 px-2">
                      <input type="text" inputMode="numeric" value={displaySeatPrice(value, row.key, col.key)}
                        onChange={e => setPrice(field, e.target.value)}
                        placeholder={col.addon ? `+${formatPriceInput(col.addon)}` : '90.000'}
                        className="w-full bg-black border border-white/[0.08] p-2 text-xs text-white font-mono focus:outline-none focus:border-amber-400" />
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!allowChildTickets && (
        <p className="text-[11px] font-black uppercase tracking-wide text-amber-300">
          Phim {ageRatingLabel || '16+'} Không cho phép bán vé trẻ em, bảng giá mục này vô hiệu hóa!
        </p>
      )}
      <div className="grid gap-2 border border-white/[0.06] bg-black/40 p-3 sm:grid-cols-[1fr_180px] sm:items-end">
        <div>
          <p className="text-[10px] font-black uppercase tracking-widest text-orange-300">Phụ thu đêm</p>
          <p className="mt-1 text-[10px] text-neutral-300">
            Áp dụng cho suất bắt đầu từ 23:00 đến trước 05:00. Nhập trong khoảng 10.000 - 100.000.
          </p>
        </div>
        <div>
          <input
            type="text"
            inputMode="numeric"
            value={formatPriceInput(value.lateNightSurchargeAmount)}
            onChange={e => onChange({ ...value, lateNightSurchargeAmount: formatPriceInput(e.target.value) })}
            placeholder="20.000"
            className="w-full bg-black border border-white/[0.08] p-2 text-xs text-white font-mono focus:outline-none focus:border-orange-400"
          />
        </div>
      </div>
      {['adultStandardPrice', ...(allowChildTickets ? ['childStandardPrice'] : []), 'studentStandardPrice'].map(field => (
        errors[`${prefix}${field}`] ? <p key={field} className="text-rose-400 text-[9px]">{errors[`${prefix}${field}`]}</p> : null
      ))}
      {errors[`${prefix}lateNightSurchargeAmount`] && (
        <p className="text-rose-400 text-[9px]">{errors[`${prefix}lateNightSurchargeAmount`]}</p>
      )}
      <p className="text-[11px] text-neutral-200">
        Phụ thu đêm 23:00-04:59 được cộng theo mức admin nhập: {formatPriceInput(value.lateNightSurchargeAmount || 20000)}đ/vé.
      </p>
    </div>
  );
}


/* ─── main component ─────────────────────────────────────── */
export default function AdminShowtimesPanel({ ctx }) {
  const { getAdminToken, showToast, moviesList } = ctx;

  // Stable ref – ensures useEffect([]) always calls the latest getAdminToken
  const getTokenRef = React.useRef(getAdminToken);
  React.useEffect(() => { getTokenRef.current = getAdminToken; }, [getAdminToken]);

  /* state */
  const [showtimes, setShowtimes] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [filters, setFilters] = useState({ movieId: '', roomId: '', status: '', date: '' });
  const dateFilterRef = useRef(null);

  const [mode, setMode] = useState('list'); // 'list' | 'create' | 'bulk' | 'edit' | 'refunds'
  const [form, setForm] = useState(EMPTY_FORM);
  const [bulkForm, setBulkForm] = useState(EMPTY_BULK);
  const [editingBulkSlotIndex, setEditingBulkSlotIndex] = useState(0);
  const [editingId, setEditingId] = useState(null);
  const [editingStatus, setEditingStatus] = useState('');
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  // Cảnh báo trùng lịch sớm (live) cho form bulk: { slotIndex: message }
  const [liveConflicts, setLiveConflicts] = useState({});

  // Gợi ý khung giờ trống cho form tạo suất (phim + phòng + ngày đã chọn)
  const [suggestedSlots, setSuggestedSlots] = useState([]);
  const [isLoadingSlots, setIsLoadingSlots] = useState(false);

  const [confirmDelete, setConfirmDelete] = useState(null); // showtimeId
  const [confirmCancel, setConfirmCancel] = useState(null); // showtime object
  const [cancelReason, setCancelReason] = useState('');
  const [refundSummary, setRefundSummary] = useState(null); // result từ cancelShowtimeAndRefund
  const [copySource, setCopySource] = useState(null); // showtime object
  const [copyDate, setCopyDate] = useState('');
  const [isCopying, setIsCopying] = useState(false);
  const [detailModal, setDetailModal] = useState(null); // showtime object
  const [roomSeatTypesById, setRoomSeatTypesById] = useState({});

  const findUiMovie = useCallback((movieId) => (
    (moviesList || []).find(m => getMovieOptionId(m) === String(movieId))
  ), [moviesList]);

  // Tải gợi ý khung giờ trống (debounced) khi đã chọn đủ phim + phòng + ngày ở form tạo suất
  const slotSuggestionDate = form.startTime ? String(form.startTime).split('T')[0] : '';
  useEffect(() => {
    if (mode !== 'create' || !form.movieId || !form.roomId || !slotSuggestionDate) {
      setSuggestedSlots([]);
      return undefined;
    }
    const token = getTokenRef.current?.();
    if (!token) return undefined;
    let cancelled = false;
    const timeoutId = window.setTimeout(() => {
      setIsLoadingSlots(true);
      adminService.getAvailableShowtimeSlots(token, {
        roomId: Number(form.roomId),
        movieId: Number(form.movieId),
        date: slotSuggestionDate
      })
        .then((slots) => { if (!cancelled) setSuggestedSlots(Array.isArray(slots) ? slots : []); })
        .catch(() => { if (!cancelled) setSuggestedSlots([]); })
        .finally(() => { if (!cancelled) setIsLoadingSlots(false); });
    }, 400);
    return () => { cancelled = true; window.clearTimeout(timeoutId); };
  }, [mode, form.movieId, form.roomId, slotSuggestionDate]);

  const findShowtimeMovie = useCallback((showtime) => {
    const movieId = showtime?.movieId ?? showtime?.movie?.id ?? showtime?.movie?.backendId;
    return findUiMovie(movieId) || showtime?.movie || null;
  }, [findUiMovie]);

  const allowsChildTicketsForShowtime = useCallback((showtime) => {
    const movie = findShowtimeMovie(showtime);
    const ageRating = movie?.ageRating ?? movie?.raw?.ageRating ?? showtime?.ageRating ?? showtime?.movieAgeRating ?? showtime?.movie?.ageRating;
    return getAgeRatingMinimum(ageRating) <= CHILD_TICKET_MAX_AGE;
  }, [findShowtimeMovie]);

  const getMovieReleaseWindow = useCallback((movie) => ({
    releaseDate: normalizeDateInput(movie?.releaseDate || movie?.raw?.releaseDate),
    endDate: normalizeDateInput(movie?.endDate || movie?.raw?.endDate),
  }), []);

  const getShowtimeWindowError = useCallback((movie, startTime) => {
    if (!movie) return null;
    const { releaseDate, endDate } = getMovieReleaseWindow(movie);
    if (!releaseDate || !endDate) return 'Ngày chiếu phim đang không nằm trong khoảng ngày phát hành hợp lệ.';
    const showDate = normalizeDateInput(startTime);
    if (!showDate) return null;
    if (showDate < releaseDate || showDate > endDate) {
      return `Suất chiếu phải nằm trong thời gian phát hành ${formatDateInput(releaseDate)} - ${formatDateInput(endDate)}.`;
    }
    return null;
  }, [getMovieReleaseWindow]);

  /* fetch rooms once */
  useEffect(() => {
    const token = getTokenRef.current?.();
    if (!token) return;
    adminService.getAdminRooms(token).then(data => {
      const list = Array.isArray(data) ? data : (data?.content || data?.items || []);
      setRooms(list);
    }).catch(() => { });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const roomId = getShowtimeRoomId(detailModal);
    if (!roomId || roomSeatTypesById[roomId]) return;

    const token = getTokenRef.current?.();
    if (!token) return;

    adminService.getAdminRoomSeats(token, roomId)
      .then(data => {
        const seats = Array.isArray(data) ? data : (data?.content || data?.items || []);
        const seatTypes = [...new Set(seats.map(seat => seat.seatType).filter(Boolean))];
        setRoomSeatTypesById(current => ({ ...current, [roomId]: seatTypes }));
      })
      .catch(() => {
        setRoomSeatTypesById(current => ({ ...current, [roomId]: [] }));
      });
  }, [detailModal, roomSeatTypesById]);

  /* fetch showtimes */
  const fetchShowtimes = useCallback(async (p = 0) => {
    const token = getTokenRef.current?.();
    if (!token) return;
    setLoading(true);
    try {
      const params = { page: p, size: 10, ...filters };
      Object.keys(params).forEach(k => { if (!params[k]) delete params[k]; });
      const data = await adminService.getAdminShowtimes(token, params);
      const items = data?.content || data?.items || (Array.isArray(data) ? data : []);
      const sortedItems = [...items].sort((a, b) => {
        const idA = Number(a.id ?? a.showtimeId ?? 0);
        const idB = Number(b.id ?? b.showtimeId ?? 0);
        return idB - idA;
      });
      setShowtimes(sortedItems);
      setTotalPages(data?.totalPages || 1);
      setPage(p);
    } catch (e) {
      showToast?.('Không thể tải danh sách suất chiếu: ' + e.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [filters, showToast]);


  useEffect(() => { fetchShowtimes(0); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /* ── create / edit submit ── */
  const handleFormSubmit = async (e) => {
    e.preventDefault();
    const token = getAdminToken();
    if (!token) return;
    setErrors({});
    const errs = {};
    const selectedMovie = findUiMovie(form.movieId);
    const allowChildTickets = allowsChildTicketsForMovie(selectedMovie);
    if (!form.movieId) errs.movieId = 'Chọn phim';
    if (!form.roomId) errs.roomId = 'Chọn phòng';
    if (!form.startTime || isNaN(new Date(form.startTime).getTime())) errs.startTime = 'Nhập thời gian hợp lệ';
    const formWindowError = selectedMovie && form.startTime ? getShowtimeWindowError(selectedMovie, form.startTime) : null;
    if (formWindowError) errs.startTime = formWindowError;
    if (!hasPriceInput(form.adultStandardPrice) && !hasPriceInput(form.basePrice)) errs.adultStandardPrice = 'Nhập giá người lớn ghế thường';
    if (allowChildTickets && !hasPriceInput(form.childStandardPrice)) errs.childStandardPrice = 'Nhập giá trẻ em ghế thường';
    if (!hasPriceInput(form.studentStandardPrice)) errs.studentStandardPrice = 'Nhập giá sinh viên ghế thường';
    const adultStandardError = validateStandardTicketPrice(form.adultStandardPrice || form.basePrice, 'Giá người lớn phải là 0 hoặc từ 10.000');
    const childStandardError = allowChildTickets ? validateStandardTicketPrice(form.childStandardPrice, 'Giá trẻ em phải là 0 hoặc từ 10.000') : null;
    const studentStandardError = validateStandardTicketPrice(form.studentStandardPrice, 'Giá sinh viên phải là 0 hoặc từ 10.000');
    if (adultStandardError) errs.adultStandardPrice = adultStandardError;
    if (childStandardError) errs.childStandardPrice = childStandardError;
    if (studentStandardError) errs.studentStandardPrice = studentStandardError;
    const formLateNightError = validateLateNightSurcharge(form.lateNightSurchargeAmount);
    if (formLateNightError) errs.lateNightSurchargeAmount = formLateNightError;
    if (Object.keys(errs).length) { setErrors(errs); return; }

    const payload = {
      movieId: Number(form.movieId),
      roomId: Number(form.roomId),
      startTime: toApiLocalDateTime(form.startTime),
      ...buildShowtimePayload(form, allowChildTickets),
      status: form.status || 'SCHEDULED',
    };

    setSaving(true);
    try {
      if (editingId) {
        if (editingStatus === 'OPEN') {
          showToast?.('Suất chiếu đang mở bán không thể chỉnh sửa.', 'error');
          return;
        }
        await adminService.updateAdminShowtime(token, editingId, payload);
        showToast?.('Cập nhật suất chiếu thành công', 'success');
      } else {
        await adminService.createAdminShowtime(token, payload);
        showToast?.('Tạo suất chiếu thành công', 'success');
      }
      setMode('list');
      setForm(EMPTY_FORM);
      setEditingId(null);
      setEditingStatus('');
      fetchShowtimes(0);
    } catch (e) {
      showToast?.('Lỗi: ' + e.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  /* ── bulk submit ── */
  const handleBulkSubmit = async (e) => {
    e.preventDefault();
    const token = getAdminToken();
    if (!token) return;
    const errs = {};
    const selectedMovie = findUiMovie(bulkForm.movieId);
    const allowChildTickets = allowsChildTicketsForMovie(selectedMovie);
    if (!bulkForm.movieId) errs.movieId = 'Chọn phim';
    if (!hasPriceInput(bulkForm.adultStandardPrice) && !hasPriceInput(bulkForm.basePrice)) errs.adultStandardPrice = 'Nhập giá người lớn ghế thường';
    if (allowChildTickets && !hasPriceInput(bulkForm.childStandardPrice)) errs.childStandardPrice = 'Nhập giá trẻ em ghế thường';
    if (!hasPriceInput(bulkForm.studentStandardPrice)) errs.studentStandardPrice = 'Nhập giá sinh viên ghế thường';
    const bulkAdultStandardError = validateStandardTicketPrice(bulkForm.adultStandardPrice || bulkForm.basePrice, 'Giá người lớn phải là 0 hoặc từ 10.000');
    const bulkChildStandardError = allowChildTickets ? validateStandardTicketPrice(bulkForm.childStandardPrice, 'Giá trẻ em phải là 0 hoặc từ 10.000') : null;
    const bulkStudentStandardError = validateStandardTicketPrice(bulkForm.studentStandardPrice, 'Giá sinh viên phải là 0 hoặc từ 10.000');
    if (bulkAdultStandardError) errs.adultStandardPrice = bulkAdultStandardError;
    if (bulkChildStandardError) errs.childStandardPrice = bulkChildStandardError;
    if (bulkStudentStandardError) errs.studentStandardPrice = bulkStudentStandardError;
    const bulkLateNightError = validateLateNightSurcharge(bulkForm.lateNightSurchargeAmount);
    if (bulkLateNightError) errs.lateNightSurchargeAmount = bulkLateNightError;
    const selectedSlotIndexes = bulkForm.slots
      .map((slot, index) => (slot.selected !== false ? index : null))
      .filter(index => index !== null);
    const selectedSlots = selectedSlotIndexes.map(index => bulkForm.slots[index]);
    if (!selectedSlots.length) errs.slot_selection = 'Chon it nhat mot khung gio';
    const seenRoomStartTimes = new Set();
    selectedSlots.forEach((s, i) => {
      const slotIndex = selectedSlotIndexes[i];
      if (!s.roomId) errs[`slot_room_${slotIndex}`] = 'Chọn phòng';
      if (!s.startTime || isNaN(new Date(s.startTime).getTime())) {
        errs[`slot_time_${slotIndex}`] = 'Nhập thời gian hợp lệ';
        return;
      }
      const slotWindowError = selectedMovie ? getShowtimeWindowError(selectedMovie, s.startTime) : null;
      if (slotWindowError) {
        errs[`slot_time_${slotIndex}`] = slotWindowError;
        return;
      }
      const normalizedStart = toApiLocalDateTime(s.startTime).slice(0, 16);
      const roomStartKey = `${s.roomId}|${normalizedStart}`;
      if (seenRoomStartTimes.has(roomStartKey)) {
        errs[`slot_time_${slotIndex}`] = 'Phòng này đã có slot khác cùng giờ trong danh sách đang chọn';
      }
      seenRoomStartTimes.add(roomStartKey);
    });
    if (bulkStepMinutes) {
      selectedSlots.forEach((slot, slotPosition) => {
        const slotIndex = selectedSlotIndexes[slotPosition];
        if (!slot.roomId || !slot.startTime || Number.isNaN(new Date(slot.startTime).getTime())) return;
        const slotStart = new Date(slot.startTime);
        const slotEnd = addMinutes(slot.startTime, bulkStepMinutes);
        if (!slotEnd) return;

        selectedSlots.forEach((otherSlot, otherPosition) => {
          if (otherPosition <= slotPosition) return;
          const otherIndex = selectedSlotIndexes[otherPosition];
          if (String(slot.roomId) !== String(otherSlot.roomId)) return;
          if (!otherSlot.startTime || Number.isNaN(new Date(otherSlot.startTime).getTime())) return;

          const otherStart = new Date(otherSlot.startTime);
          const otherEnd = addMinutes(otherSlot.startTime, bulkStepMinutes);
          if (!otherEnd) return;

          if (intervalsOverlap(slotStart, slotEnd, otherStart, otherEnd)) {
            const message = 'Slot này trùng thời gian với slot khác cùng phòng trong danh sách đang chọn';
            errs[`slot_time_${slotIndex}`] = message;
            errs[`slot_time_${otherIndex}`] = message;
          }
        });
      });
    }
    if (Object.keys(errs).length) { setErrors(errs); return; }

    const conflictErrs = {};
    const slotDates = [...new Set(selectedSlots
      .map(slot => normalizeDateInput(slot.startTime))
      .filter(Boolean))];
    try {
      const existingShowtimes = await fetchExistingShowtimesForDates(token, slotDates);

      selectedSlots.forEach((slot, slotPosition) => {
        const slotIndex = selectedSlotIndexes[slotPosition];
        if (slotConflictsWithExisting(slot, existingShowtimes)) {
          conflictErrs[`slot_time_${slotIndex}`] = 'Slot này trùng thời gian với suất chiếu đã tồn tại trong phòng này';
        }
      });
    } catch (error) {
      showToast?.('Không thể kiểm tra trùng lịch trước khi tạo: ' + error.message, 'error');
    }

    if (Object.keys(conflictErrs).length) {
      const firstConflictIndex = Number(Object.keys(conflictErrs)[0].replace('slot_time_', ''));
      setErrors(conflictErrs);
      setEditingBulkSlotIndex(Number.isFinite(firstConflictIndex) ? firstConflictIndex : 0);
      showToast?.(`Có ${Object.keys(conflictErrs).length} khung giờ bị trùng thời gian. Các ô lỗi đã được đánh viền đỏ.`, 'error');
      return;
    }

    setErrors({});

    const payload = {
      movieId: Number(bulkForm.movieId),
      ...buildShowtimePayload(bulkForm, allowChildTickets),
      defaultStatus: bulkForm.defaultStatus || 'SCHEDULED',
      slots: selectedSlots.map(s => ({
        roomId: Number(s.roomId),
        startTime: toApiLocalDateTime(s.startTime),
        status: s.status || null,
      })),
    };

    setSaving(true);
    try {
      const result = await adminService.createAdminShowtimesBulk(token, payload);
      const count = Array.isArray(result) ? result.length : '?';
      showToast?.(`Tạo thành công ${count} suất chiếu`, 'success');
      setMode('list');
      setBulkForm(EMPTY_BULK);
      setEditingStatus('');
      fetchShowtimes(0);
    } catch (e) {
      const errorMessage = e.message || '';
      const errorSlotIndex = getBulkSlotIndexFromError(errorMessage);
      if (errorSlotIndex !== null) {
        const actualSlotIndex = selectedSlotIndexes[errorSlotIndex] ?? errorSlotIndex;
        setErrors(prev => ({
          ...prev,
          [`slot_time_${actualSlotIndex}`]: errorMessage,
        }));
        setEditingBulkSlotIndex(actualSlotIndex);
      }
      showToast?.('Lỗi: ' + e.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  /* ── status change ── */
  const handleStatusChange = async (showtimeId, newStatus) => {
    const token = getAdminToken();
    if (!token) return;
    try {
      await adminService.updateAdminShowtimeStatus(token, showtimeId, newStatus);
      showToast?.('Cập nhật trạng thái thành công', 'success');
      fetchShowtimes(page);
    } catch (e) {
      showToast?.('Lỗi: ' + e.message, 'error');
    }
  };

  /* ── delete ── */
  const handleConfirmCancel = async () => {
    if (!confirmCancel?.id) return;
    const showtimeId = confirmCancel.id;
    const token = getAdminToken();
    if (!token) return;
    setSaving(true);
    try {
      const result = await adminService.cancelShowtimeAndRefund(token, showtimeId, cancelReason.trim());
      setRefundSummary(result);
      showToast?.(`Đã hủy suất chiếu. Hoàn tiền: ${result?.successCount ?? 0} thành công, ${result?.failedCount ?? 0} lỗi.`, 'success');
      setConfirmCancel(null);
      setCancelReason('');
      setDetailModal(null);
      fetchShowtimes(page);
    } catch (e) {
      showToast?.('Lỗi hủy suất chiếu và hoàn tiền: ' + e.message, 'error');
    } finally {
      setSaving(false);
    }
  };


  const handleDelete = async (showtimeId) => {
    const token = getAdminToken();
    if (!token) return;
    try {
      await adminService.deleteAdminShowtime(token, showtimeId);
      showToast?.('Đã xóa suất chiếu', 'success');
      setConfirmDelete(null);
      fetchShowtimes(page);
    } catch (e) {
      showToast?.('Lỗi: ' + e.message, 'error');
      setConfirmDelete(null);
    }
  };

  /* ── open edit form ── */
  const openCopy = (st) => {
    const sourceDate = st.startTime ? new Date(st.startTime) : new Date();
    const target = new Date(sourceDate);
    target.setDate(target.getDate() + 1);
    const defaultDate = toLocalDateInput(target);
    setCopySource(st);
    setCopyDate(isFutureDateInput(defaultDate) ? defaultDate : '');
    setErrors({});
  };

  const handleCopySubmit = async (event) => {
    event.preventDefault();
    const token = getAdminToken();
    if (!token || !copySource) return;
    if (!isFutureDateInput(copyDate)) {
      setErrors({ copyDate: 'Chọn một ngày trong tương lai.' });
      return;
    }

    const movieId = copySource.movieId ?? copySource.movie?.id;
    const roomId = copySource.roomId ?? copySource.room?.id;
    const nextStartTime = buildDateTimeOnDate(copySource.startTime, copyDate);
    const copyMovie = findUiMovie(movieId) || copySource.movie;
    const allowChildTickets = allowsChildTicketsForMovie(copyMovie);
    if (!movieId || !roomId || !nextStartTime) {
      showToast?.('Không đủ dữ liệu phim/phòng/giờ để copy suất chiếu.', 'error');
      return;
    }

    const payload = {
      movieId: Number(movieId),
      roomId: Number(roomId),
      startTime: toApiLocalDateTime(nextStartTime),
      basePrice: copySource.basePrice ?? copySource.adultStandardPrice ?? 0,
      vipPrice: copySource.vipPrice ?? copySource.adultVipPrice ?? null,
      couplePrice: copySource.couplePrice ?? copySource.adultCouplePrice ?? null,
      adultStandardPrice: copySource.adultStandardPrice ?? copySource.basePrice ?? 0,
      childStandardPrice: allowChildTickets ? copySource.childStandardPrice ?? null : null,
      studentStandardPrice: copySource.studentStandardPrice ?? copySource.basePrice ?? 0,
      adultVipPrice: copySource.adultVipPrice ?? copySource.vipPrice ?? null,
      childVipPrice: allowChildTickets ? copySource.childVipPrice ?? null : null,
      studentVipPrice: copySource.studentVipPrice ?? null,
      adultCouplePrice: copySource.adultCouplePrice ?? copySource.couplePrice ?? null,
      childCouplePrice: allowChildTickets ? copySource.childCouplePrice ?? null : null,
      studentCouplePrice: copySource.studentCouplePrice ?? null,
      weekendSurcharge: Boolean(copySource.weekendSurcharge),
      holidaySurcharge: Boolean(copySource.holidaySurcharge),
      lateNightSurchargeAmount: copySource.lateNightSurchargeAmount ?? 20000,
      status: 'SCHEDULED',
    };

    setIsCopying(true);
    try {
      await adminService.createAdminShowtime(token, payload);
      showToast?.('Đã copy suất chiếu sang ngày mới.', 'success');
      setCopySource(null);
      setCopyDate('');
      setErrors({});
      fetchShowtimes(page);
    } catch (e) {
      showToast?.('Lỗi: ' + e.message, 'error');
    } finally {
      setIsCopying(false);
    }
  };

  const openEdit = (st) => {
    if (st.status === 'OPEN') {
      showToast?.('Suất chiếu đang mở bán không thể chỉnh sửa.', 'error');
      return;
    }
    setEditingId(st.id);
    setEditingStatus(st.status || '');
    setForm({
      movieId: st.movieId ?? st.movie?.id ?? '',
      roomId: st.roomId ?? st.room?.id ?? '',
      startTime: toInputDatetime(st.startTime),
      basePrice: st.basePrice ?? '',
      vipPrice: st.vipPrice ?? '',
      couplePrice: st.couplePrice ?? '',
      adultStandardPrice: st.adultStandardPrice ?? st.basePrice ?? '',
      childStandardPrice: st.childStandardPrice ?? st.basePrice ?? '',
      studentStandardPrice: st.studentStandardPrice ?? st.basePrice ?? '',
      adultVipPrice: st.adultVipPrice ?? st.vipPrice ?? '',
      childVipPrice: st.childVipPrice ?? '',
      studentVipPrice: st.studentVipPrice ?? '',
      adultCouplePrice: st.adultCouplePrice ?? st.couplePrice ?? '',
      childCouplePrice: st.childCouplePrice ?? '',
      studentCouplePrice: st.studentCouplePrice ?? '',
      weekendSurcharge: Boolean(st.weekendSurcharge),
      holidaySurcharge: Boolean(st.holidaySurcharge),
      lateNightSurchargeAmount: formatPriceInput(st.lateNightSurchargeAmount ?? 20000),
      status: st.status ?? 'SCHEDULED',
    });
    setErrors({});
    setMode('edit');
  };

  const activeMovies = (moviesList || []).filter(m => m.status === 'ACTIVE' || m.status === 'NOW_SHOWING' || m.status === 'UPCOMING');
  const formMovie = findUiMovie(form.movieId);
  const formReleaseWindow = getMovieReleaseWindow(formMovie);
  const formAllowsChildTickets = allowsChildTicketsForMovie(formMovie);
  const bulkMovie = findUiMovie(bulkForm.movieId);
  const bulkReleaseWindow = getMovieReleaseWindow(bulkMovie);
  const bulkAllowsChildTickets = allowsChildTicketsForMovie(bulkMovie);
  const bulkMovieDuration = Number(bulkMovie?.durationMinutes ?? bulkMovie?.duration ?? 0);
  const bulkStepMinutes = bulkMovieDuration ? bulkMovieDuration + 15 : 0;
  const selectedBulkSlotsCount = bulkForm.slots.filter(s => s.selected !== false).length;
  const activeBulkSlotIndex = Math.min(editingBulkSlotIndex, Math.max(bulkForm.slots.length - 1, 0));
  const activeBulkSlot = bulkForm.slots[activeBulkSlotIndex] || EMPTY_BULK.slots[0];

  const getBulkSlotEnd = (startTime) => addMinutes(startTime, bulkMovieDuration);

  const getBulkSlotCapacity = (slot) => {
    const room = rooms.find(r => String(r.id) === String(slot.roomId));
    return getRoomCapacity(room);
  };

  const fetchExistingShowtimesForDates = async (token, dates) => {
    const existingByDate = await Promise.all(dates.map(async (date) => {
      const data = await adminService.getAdminShowtimes(token, { page: 0, size: 500, date });
      const items = data?.content || data?.items || (Array.isArray(data) ? data : []);
      return items.map(item => ({ ...item, __date: date }));
    }));
    return existingByDate.flat()
      .filter(item => String(item.status || '').toUpperCase() !== 'CANCELLED');
  };

  const slotConflictsWithExisting = (slot, existingShowtimes) => {
    const slotStart = new Date(slot.startTime);
    const slotEnd = addMinutes(slot.startTime, bulkStepMinutes || bulkMovieDuration || 0);
    if (!slot.roomId || Number.isNaN(slotStart.getTime()) || !slotEnd) return false;
    return existingShowtimes.some((showtime) => {
      if (String(getShowtimeRoomId(showtime)) !== String(slot.roomId)) return false;
      if (normalizeDateInput(showtime.startTime) !== normalizeDateInput(slot.startTime)) return false;

      const existingStart = new Date(showtime.startTime);
      const rawExistingEnd = showtime.endTime
        ? new Date(showtime.endTime)
        : addMinutes(showtime.startTime, bulkMovieDuration || 0);
      const existingEnd = rawExistingEnd instanceof Date
        ? addMinutes(rawExistingEnd, 15)
        : null;
      if (Number.isNaN(existingStart.getTime()) || !existingEnd) return false;
      return intervalsOverlap(slotStart, slotEnd, existingStart, existingEnd);
    });
  };

  // Cảnh báo trùng lịch SỚM: chạy debounce ngay khi slot đủ phòng + giờ,
  // không đợi bấm submit. Submit vẫn validate lại lần cuối như cũ.
  useEffect(() => {
    if (mode !== 'bulk' || !bulkForm.movieId) {
      setLiveConflicts({});
      return undefined;
    }
    const readySlots = bulkForm.slots
      .map((slot, index) => ({ slot, index }))
      .filter(({ slot }) => slot.selected !== false
        && slot.roomId
        && slot.startTime
        && !Number.isNaN(new Date(slot.startTime).getTime()));
    if (!readySlots.length) {
      setLiveConflicts({});
      return undefined;
    }
    const token = getTokenRef.current?.();
    let cancelled = false;
    const timeoutId = window.setTimeout(async () => {
      const next = {};
      // Trùng/chồng giờ giữa các slot trong form (không cần API)
      readySlots.forEach(({ slot, index }, position) => {
        readySlots.slice(position + 1).forEach(({ slot: other, index: otherIndex }) => {
          if (String(slot.roomId) !== String(other.roomId)) return;
          if (toApiLocalDateTime(slot.startTime).slice(0, 16) === toApiLocalDateTime(other.startTime).slice(0, 16)) {
            const message = 'Phòng này đã có slot khác cùng giờ trong danh sách đang chọn';
            next[index] = message;
            next[otherIndex] = message;
            return;
          }
          if (!bulkStepMinutes) return;
          const slotEnd = addMinutes(slot.startTime, bulkStepMinutes);
          const otherEnd = addMinutes(other.startTime, bulkStepMinutes);
          if (!slotEnd || !otherEnd) return;
          if (intervalsOverlap(new Date(slot.startTime), slotEnd, new Date(other.startTime), otherEnd)) {
            const message = 'Slot này trùng thời gian với slot khác cùng phòng trong danh sách đang chọn';
            next[index] = message;
            next[otherIndex] = message;
          }
        });
      });
      // Trùng với suất chiếu đã tồn tại (lỗi mạng thì bỏ qua — submit sẽ check lại)
      if (token) {
        try {
          const dates = [...new Set(readySlots
            .map(({ slot }) => normalizeDateInput(slot.startTime))
            .filter(Boolean))];
          const existingShowtimes = await fetchExistingShowtimesForDates(token, dates);
          if (cancelled) return;
          readySlots.forEach(({ slot, index }) => {
            if (!next[index] && slotConflictsWithExisting(slot, existingShowtimes)) {
              next[index] = 'Slot này trùng thời gian với suất chiếu đã tồn tại trong phòng này';
            }
          });
        } catch { /* im lặng khi đang nhập dở */ }
      }
      if (!cancelled) setLiveConflicts(next);
    }, 500);
    return () => { cancelled = true; window.clearTimeout(timeoutId); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode, bulkForm.movieId, bulkForm.slots, bulkStepMinutes]);

  const setAllBulkSlotsRoom = (roomId) => {
    const nextSlots = (bulkForm.slots.length ? bulkForm.slots : EMPTY_BULK.slots)
      .map(slot => ({ ...slot, roomId, selected: slot.selected !== false }));
    setBulkForm({ ...bulkForm, slots: nextSlots });
  };

  const updateBulkSlot = (index, patch) => {
    const nextSlots = [...bulkForm.slots];
    nextSlots[index] = { ...nextSlots[index], ...patch };
    setBulkForm({ ...bulkForm, slots: nextSlots });
    // Sửa slot thì bỏ lỗi cũ của slot đó — live check sẽ đánh giá lại ngay sau đó
    setErrors(prev => {
      if (!(`slot_time_${index}` in prev) && !(`slot_room_${index}` in prev)) return prev;
      const next = { ...prev };
      delete next[`slot_time_${index}`];
      delete next[`slot_room_${index}`];
      return next;
    });
  };

  const toggleBulkSlot = (index) => {
    const nextSlots = [...bulkForm.slots];
    nextSlots[index] = { ...nextSlots[index], selected: nextSlots[index].selected === false };
    setBulkForm({ ...bulkForm, slots: nextSlots });
  };

  const addManualBulkSlot = (event) => {
    event?.preventDefault?.();
    event?.stopPropagation?.();

    const sourceSlot = bulkForm.slots[activeBulkSlotIndex] || bulkForm.slots[0] || {};
    const firstSlot = {
      roomId: sourceSlot.roomId || bulkForm.slots[0]?.roomId || '',
      startTime: sourceSlot.startTime || bulkForm.slots[0]?.startTime || ''
    };
    const lastSlot = bulkForm.slots[bulkForm.slots.length - 1] || firstSlot;
    const nextStart = lastSlot.startTime && bulkStepMinutes ? toInputDatetime(addMinutes(lastSlot.startTime, bulkStepMinutes)) : '';
    const nextIndex = bulkForm.slots.length;
    setBulkForm({
      ...bulkForm,
      slots: [...bulkForm.slots, { roomId: firstSlot.roomId || '', startTime: nextStart, selected: true }]
    });
    setEditingBulkSlotIndex(nextIndex);
  };

  const removeBulkSlot = (index) => {
    if (bulkForm.slots.length === 1) return;
    const nextSlots = bulkForm.slots.filter((_, j) => j !== index);
    setBulkForm({ ...bulkForm, slots: nextSlots });
    setEditingBulkSlotIndex(prev => Math.max(0, Math.min(prev >= index ? prev - 1 : prev, nextSlots.length - 1)));
  };

  const generateBulkSlotsForDay = (event) => {
    event?.preventDefault?.();
    event?.stopPropagation?.();

    const sourceSlot = bulkForm.slots[activeBulkSlotIndex] || bulkForm.slots[0] || {};
    const firstSlot = {
      roomId: sourceSlot.roomId || bulkForm.slots[0]?.roomId || '',
      startTime: sourceSlot.startTime || bulkForm.slots[0]?.startTime || ''
    };
    if (!bulkForm.movieId || !firstSlot.roomId || !firstSlot.startTime) {
      setErrors(prev => ({
        ...prev,
        ...(!bulkForm.movieId ? { movieId: 'Chọn phim trước khi tự tạo khung giờ.' } : {}),
        ...(!firstSlot.roomId ? { [`slot_room_${activeBulkSlotIndex}`]: 'Chọn phòng cho slot đang sửa.' } : {}),
        ...(!firstSlot.startTime ? { [`slot_time_${activeBulkSlotIndex}`]: 'Nhập giờ bắt đầu cho slot đang sửa.' } : {})
      }));
      showToast?.('Chọn phim, phòng và giờ bắt đầu đầu tiên để tự tạo khung giờ.', 'error');
      return;
    }
    if (!bulkMovieDuration) {
      showToast?.('Phim chưa có thời lượng hợp lệ.', 'error');
      return;
    }
    const start = new Date(firstSlot.startTime);
    if (Number.isNaN(start.getTime())) {
      showToast?.('Giờ bắt đầu không hợp lệ.', 'error');
      return;
    }
    const windowError = getShowtimeWindowError(bulkMovie, firstSlot.startTime);
    if (windowError) {
      setErrors(prev => ({ ...prev, [`slot_time_${activeBulkSlotIndex}`]: windowError }));
      showToast?.(windowError, 'error');
      return;
    }
    const dayEnd = new Date(start);
    dayEnd.setHours(23, 59, 59, 999);
    const slots = [];
    const cursor = new Date(start);
    while (cursor <= dayEnd) {
      slots.push({ roomId: firstSlot.roomId, startTime: toInputDatetime(cursor), selected: true });
      cursor.setMinutes(cursor.getMinutes() + bulkStepMinutes);
    }
    setBulkForm({ ...bulkForm, slots });
    setEditingBulkSlotIndex(0);
    /* ════════════════════════════════════════════════ */
  };

  /* bulk slot controls */
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div>
          <h2 className="text-xs font-black uppercase tracking-[0.18em] text-neutral-200">Quản lý Suất Chiếu</h2>
          <p className="text-xs text-neutral-300 mt-0.5">Tạo, chỉnh sửa và giám sát lịch chiếu phim</p>
        </div>
        {mode === 'list' && (
          <div className="flex gap-2">
            <button onClick={() => ctx?.changeAdminSection?.('showtime-incidents')}
              className="flex items-center gap-1.5 px-3 py-2 bg-amber-500/10 border border-amber-500/30 text-amber-400 text-[10px] font-bold uppercase tracking-wider hover:bg-amber-500 hover:text-black transition cursor-pointer">
              <AlertCircle className="w-3 h-3" /> Báo cáo sự cố &amp; hoàn tiền
            </button>
            <button onClick={() => { setMode('bulk'); setBulkForm(EMPTY_BULK); setEditingBulkSlotIndex(0); setEditingId(null); setEditingStatus(''); setErrors({}); }}
              className="flex items-center gap-1.5 px-3 py-2 bg-amber-500 text-black text-[10px] font-black uppercase tracking-wider hover:bg-amber-400 transition cursor-pointer">
              <Zap className="w-3 h-3" /> Tạo suất chiếu
            </button>
          </div>
        )}
        {mode !== 'list' && (
          <button onClick={() => { setMode('list'); setEditingId(null); setEditingStatus(''); setErrors({}); }}
            className="flex items-center gap-1.5 px-3 py-2 border border-white/[0.10] text-neutral-200 text-[10px] font-bold uppercase tracking-wider hover:border-white/[0.20] transition">
            <X className="w-3 h-3" /> Hủy
          </button>
        )}
      </div>

      {/* ── EDIT FORM ── */}

      <AnimatePresence mode="wait">
        {mode === 'edit' && (
          <motion.div key="form-single" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
            className="border border-white/[0.08] bg-gradient-to-b from-[#0a0a0a] to-[#040404] p-6">
            <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white mb-4 pb-2 border-b border-white/[0.06]">
              {mode === 'edit' ? '✏️ Chỉnh sửa suất chiếu' : '➕ Tạo suất chiếu mới'}
            </h3>
            <form onSubmit={handleFormSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-sans">

              {/* Movie */}
              <div className="space-y-1.5">
                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Phim *</label>
                <select value={form.movieId} onChange={e => {
                  const nextMovieId = e.target.value;
                  const nextMovie = findUiMovie(nextMovieId);
                  const nextState = { ...form, movieId: nextMovieId };
                  if (nextState.startTime && getShowtimeWindowError(nextMovie, nextState.startTime)) {
                    nextState.startTime = '';
                  }
                  setForm(allowsChildTicketsForMovie(nextMovie) ? nextState : clearChildPrices(nextState));
                }}
                  className="w-full bg-black border border-white/[0.08] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400">
                  <option value="">-- Chọn phim --</option>
                  {activeMovies.map(m => (
                    <option key={m.id} value={m.backendId ?? m.id}>{m.title}</option>
                  ))}
                </select>
                {errors.movieId && <p className="text-rose-400 text-[9px]">{errors.movieId}</p>}
              </div>

              {/* Room */}
              <div className="space-y-1.5">
                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Phòng chiếu *</label>
                <select value={form.roomId} onChange={e => setForm({ ...form, roomId: e.target.value })}
                  className="w-full bg-black border border-white/[0.08] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400">
                  <option value="">-- Chọn phòng --</option>
                  {rooms.filter(r => r.status === 'ACTIVE').map(r => (
                    <option key={r.id} value={r.id}>{r.name} ({r.roomType})</option>
                  ))}
                </select>
                {errors.roomId && <p className="text-rose-400 text-[9px]">{errors.roomId}</p>}
              </div>

              {/* Start time */}
              <DateTimePicker
                label="Thời gian bắt đầu *"
                value={form.startTime}
                onChange={v => setForm({ ...form, startTime: v })}
                error={errors.startTime}
                minDate={formReleaseWindow.releaseDate}
                maxDate={formReleaseWindow.endDate}
                helpText={formMovie ? `Chỉ tạo trong thời gian phát hành: ${formatDateInput(formReleaseWindow.releaseDate)} - ${formatDateInput(formReleaseWindow.endDate)}` : 'Chọn phim để giới hạn ngày chiếu.'}
              />

              {/* Gợi ý khung giờ trống của phòng trong ngày đã chọn */}
              {form.movieId && form.roomId && slotSuggestionDate && (
                <div className="md:col-span-2 space-y-2 border border-emerald-500/20 bg-emerald-950/10 p-3">
                  <p className="text-[10px] uppercase tracking-[0.18em] text-emerald-400 font-black">
                    Khung giờ còn trống ngày {formatDateInput(slotSuggestionDate)} — bấm để điền
                  </p>
                  {isLoadingSlots ? (
                    <p className="text-[10px] text-neutral-300">Đang tính khung giờ trống…</p>
                  ) : suggestedSlots.length === 0 ? (
                    <p className="text-[10px] text-neutral-300">Không còn khung giờ trống phù hợp trong ngày này.</p>
                  ) : (
                    <div className="flex flex-wrap gap-1.5">
                      {suggestedSlots.map((slot) => {
                        const startLabel = String(slot.startTime).slice(11, 16);
                        const endLabel = String(slot.endTime).slice(11, 16);
                        const isSelected = String(form.startTime).slice(0, 16) === String(slot.startTime).slice(0, 16);
                        return (
                          <button
                            key={slot.startTime}
                            type="button"
                            onClick={() => setForm({ ...form, startTime: String(slot.startTime).slice(0, 16) })}
                            className={`border px-2.5 py-1.5 font-mono text-[10px] font-bold transition ${isSelected
                              ? 'border-emerald-400 bg-emerald-400 text-black'
                              : 'border-emerald-500/30 bg-black text-emerald-300 hover:border-emerald-400 hover:bg-emerald-500/15'
                              }`}
                            title={`Suất ${startLabel} – ${endLabel} (đã gồm thời gian dọn phòng)`}
                          >
                            {startLabel} – {endLabel}
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}

              {/* Status */}
              <div className="space-y-1.5">
                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Trạng thái ban đầu</label>
                <select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}
                  className="w-full bg-black border border-white/[0.08] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400">
                  <option value="SCHEDULED">SCHEDULED — lên lịch, chưa bán</option>
                  <option value="OPEN">OPEN — mở bán ngay</option>
                </select>
              </div>

              <PriceMatrix
                value={form}
                onChange={setForm}
                errors={errors}
                allowChildTickets={formAllowsChildTickets}
                ageRatingLabel={formMovie?.ageRating}
              />

              <div className="md:col-span-2">
                <button type="submit" disabled={saving}
                  className="w-full py-3.5 bg-white text-black font-black uppercase text-[10.5px] tracking-widest hover:bg-amber-400 transition disabled:opacity-50">
                  {saving ? 'Đang lưu...' : mode === 'edit' ? 'Cập nhật suất chiếu' : 'Tạo suất chiếu'}
                </button>
              </div>
            </form>
          </motion.div>
        )}

        {/* ── BULK FORM ── */}
        {mode === 'bulk' && (
          <motion.div key="form-bulk" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
            className="border border-amber-500/30 bg-gradient-to-b from-[#0d0900] to-[#040404] p-6">
            <div className="flex items-center gap-2 mb-4 pb-2 border-b border-white/[0.06]">
              <Zap className="w-3.5 h-3.5 text-amber-400" />
              <h3 className="text-xs font-black uppercase tracking-[0.18em] text-amber-300">Tạo hàng loạt suất chiếu</h3>

            </div>
            <form onSubmit={handleBulkSubmit} className="space-y-5 text-xs font-sans">

              {/* Shared fields */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Phim *</label>
                  <select value={bulkForm.movieId} onChange={e => {
                    const nextMovieId = e.target.value;
                    const nextMovie = findUiMovie(nextMovieId);
                    const nextState = {
                      ...bulkForm,
                      movieId: nextMovieId,
                      slots: bulkForm.slots.map(slot => (
                        slot.startTime && getShowtimeWindowError(nextMovie, slot.startTime)
                          ? { ...slot, startTime: '' }
                          : slot
                      ))
                    };
                    setBulkForm(allowsChildTicketsForMovie(nextMovie) ? nextState : clearChildPrices(nextState));
                  }}
                    className="w-full bg-black border border-white/[0.08] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400">
                    <option value="">-- Chọn phim --</option>
                    {activeMovies.map(m => (
                      <option key={m.id} value={m.backendId ?? m.id}>{m.title}</option>
                    ))}
                  </select>
                  {errors.movieId && <p className="text-rose-400 text-[9px]">{errors.movieId}</p>}
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Trạng thái mặc định</label>
                  <select value={bulkForm.defaultStatus} onChange={e => setBulkForm({ ...bulkForm, defaultStatus: e.target.value })}
                    className="w-full bg-black border border-white/[0.08] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400">
                    <option value="SCHEDULED">SCHEDULED — lên lịch, chưa bán</option>
                    <option value="OPEN">OPEN — mở bán ngay</option>
                  </select>
                </div>
                <PriceMatrix
                  value={bulkForm}
                  onChange={setBulkForm}
                  errors={errors}
                  allowChildTickets={bulkAllowsChildTickets}
                  ageRatingLabel={bulkMovie?.ageRating}
                />
              </div>

              <div className="space-y-4 border border-white/[0.06] bg-white/[0.01] p-4">
                <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
                  <div>
                    <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-300 font-black">
                      Khung giờ có thể chiếu ({selectedBulkSlotsCount}/{bulkForm.slots.length} đã chọn)
                    </label>
                    <p className="text-[10px] text-neutral-200 mt-1">
                      Chọn phim, phòng và giờ bắt đầu. Hệ thống cộng thời lượng phim + 15 phút dọn phòng để sinh các ca còn lại trong ngày.
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button type="button"
                      onClick={generateBulkSlotsForDay}
                      className="flex items-center gap-1.5 text-[9px] text-emerald-300 border border-emerald-500/30 px-3 py-2 hover:border-emerald-400/60 hover:bg-emerald-950/20 transition">
                      <Clock className="w-3 h-3" /> Tự tạo trong ngày
                    </button>
                    <button type="button"
                      onClick={addManualBulkSlot}
                      className="flex items-center gap-1.5 text-[9px] text-amber-300 border border-amber-500/30 px-3 py-2 hover:border-amber-400/60 hover:bg-amber-950/20 transition">
                      <Plus className="w-3 h-3" /> Thêm slot
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-start">
                  <div className="space-y-3">
                    <div className="space-y-2">
                      <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-300 font-black">Phòng chiếu *</label>
                      <select value={activeBulkSlot?.roomId || ''}
                        onChange={e => updateBulkSlot(activeBulkSlotIndex, { roomId: e.target.value, selected: true })}
                        className="w-full bg-black border border-white/[0.08] p-2.5 text-sm font-bold text-white focus:outline-none focus:border-amber-400 appearance-none">
                        <option value="">-- Chọn phòng --</option>
                        {rooms.filter(r => r.status === 'ACTIVE').map(r => (
                          <option key={r.id} value={r.id}>{r.name}</option>
                        ))}
                      </select>
                      {errors[`slot_room_${activeBulkSlotIndex}`] && <p className="text-rose-400 text-[8px]">{errors[`slot_room_${activeBulkSlotIndex}`]}</p>}
                      <button type="button"
                        onClick={() => setAllBulkSlotsRoom(activeBulkSlot?.roomId || '')}
                        disabled={!activeBulkSlot?.roomId}
                        className="text-[10px] uppercase tracking-wider text-amber-400 hover:text-amber-500 disabled:opacity-40">
                        Áp dụng phòng này cho tất cả slot
                      </button>
                    </div>
                    <div className="border border-amber-500/20 bg-amber-500/10 p-3 text-[9px] text-amber-100/80">
                      <p className="uppercase tracking-widest text-amber-300 font-black">Bước nhảy</p>
                      <p className="mt-1 font-mono text-white">{bulkMovieDuration || '--'} phút phim + 15 phút</p>
                      <p className="mt-2 text-amber-200/70">Suất bắt đầu từ 23:00 đến trước 05:00 được cộng +{formatPriceInput(bulkForm.lateNightSurchargeAmount || 20000)}đ/vé.</p>
                    </div>
                  </div>
                  <DateTimePicker
                    label="Ngày"
                    value={activeBulkSlot?.startTime || ''}
                    onChange={v => updateBulkSlot(activeBulkSlotIndex, { startTime: v, selected: true })}
                    error={errors[`slot_time_${activeBulkSlotIndex}`] || liveConflicts[activeBulkSlotIndex]}
                    minDate={bulkReleaseWindow.releaseDate}
                    maxDate={bulkReleaseWindow.endDate}
                    helpText={bulkMovie ? `Chỉ tạo trong thời gian phát hành: ${formatDateInput(bulkReleaseWindow.releaseDate)} - ${formatDateInput(bulkReleaseWindow.endDate)}` : 'Chọn phim để giới hạn ngày chiếu.'}
                  />
                </div>

                {errors.slot_selection && <p className="text-rose-400 text-[9px]">{errors.slot_selection}</p>}

                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-4">
                  {bulkForm.slots.map((slot, i) => {
                    const selected = slot.selected !== false;
                    const endTime = getBulkSlotEnd(slot.startTime);
                    const capacity = getBulkSlotCapacity(slot);
                    const night = isNightSlot(slot.startTime);
                    const slotError = errors[`slot_time_${i}`] || errors[`slot_room_${i}`] || liveConflicts[i];
                    return (
                      <div key={`${slot.startTime}-${i}`}
                        role="button"
                        tabIndex={0}
                        onClick={() => setEditingBulkSlotIndex(i)}
                        onKeyDown={(e) => {
                          if (e.key !== 'Enter' && e.key !== ' ') return;
                          e.preventDefault();
                          setEditingBulkSlotIndex(i);
                        }}
                        className={`relative min-h-[168px] overflow-hidden border text-left transition ${slotError ? 'border-rose-500 bg-rose-500/[0.04] ring-1 ring-rose-500/30' : activeBulkSlotIndex === i ? 'border-amber-400 bg-amber-500/[0.06] ring-1 ring-amber-400/30' : selected ? 'border-amber-500/60 bg-amber-500/[0.04]' : 'border-white/[0.08] bg-white/[0.03] opacity-55 hover:opacity-90'}`}>
                        <div className="px-5 py-4 pt-10">
                          {slot.startTime ? (
                            <>
                              <div className="flex items-end gap-1.5">
                                <div className="flex flex-col items-start">
                                  <span className="text-[9px] font-semibold text-neutral-200 uppercase tracking-wider mb-0.5">Giờ bắt đầu</span>
                                  <span className="text-2xl font-black tracking-normal text-white font-mono">{formatTimeOnly(slot.startTime)}</span>
                                </div>
                                <span className="text-2xl font-black text-neutral-200 mb-0.5">→</span>
                                <div className="flex flex-col items-start">
                                  <span className="text-[9px] font-semibold text-neutral-200 uppercase tracking-wider mb-0.5">Giờ kết thúc</span>
                                  <span className="text-2xl font-black tracking-normal text-white font-mono">{formatTimeOnly(endTime)}</span>
                                </div>
                              </div>
                              {bulkStepMinutes > 0 && (
                                <p className="mt-0.5 text-[9px] font-semibold text-neutral-200">
                                  Phòng trống {formatTimeOnly(addMinutes(slot.startTime, bulkStepMinutes))} (+15p dọn)
                                </p>
                              )}
                            </>
                          ) : (
                            <p className="text-xs font-black uppercase tracking-wider text-neutral-300 py-2">Chưa khởi tạo</p>
                          )}
                          <p className="mt-2 truncate text-[10px] font-bold uppercase tracking-wider text-neutral-200">
                            {rooms.find(r => String(r.id) === String(slot.roomId))?.name || 'Chưa chọn phòng'}
                          </p>
                        </div>
                        <div className={`border px-5 pt-0 pb-5 text-center text-sm font-bold ${night ? 'border-amber-500/30 bg-amber-500/[0.08] text-amber-300' : 'border-white/[0.10] bg-white/[0.04] text-neutral-300'}`}>
                          {capacity ? `Sức chứa: ${capacity} ghế` : slot.roomId ? 'Chưa có dữ liệu ghế' : 'Chưa chọn phòng'}
                        </div>
                        <div className="absolute right-2 top-2 flex gap-1">
                          {slotError && <span className="rounded bg-rose-600 px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wider text-white">Lỗi</span>}
                          {activeBulkSlotIndex === i && <span className="bg-amber-400 px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wider text-black">Sửa</span>}
                          {night && <span className="bg-amber-500/20 px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wider text-amber-300">Đêm +{formatPriceInput(bulkForm.lateNightSurchargeAmount || 20000)}đ</span>}
                          {selected && <span className="rounded bg-amber-400 px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wider text-black">Chọn</span>}
                        </div>
                        <span
                          role="button"
                          tabIndex={0}
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleBulkSlot(i);
                          }}
                          onKeyDown={(e) => {
                            if (e.key !== 'Enter' && e.key !== ' ') return;
                            e.preventDefault();
                            e.stopPropagation();
                            toggleBulkSlot(i);
                          }}
                          className={`absolute bottom-2 left-2 rounded px-2 py-1 text-[8px] font-black uppercase tracking-wider ${selected ? 'bg-white/[0.03] text-white hover:bg-rose-600' : 'bg-amber-400 text-black hover:bg-amber-300'}`}>
                          {selected ? 'Bỏ chọn' : 'Chọn'}
                        </span>
                        <span
                          role="button"
                          tabIndex={0}
                          onClick={(e) => {
                            e.stopPropagation();
                            if (bulkForm.slots.length === 1) return;
                            removeBulkSlot(i);
                          }}
                          onKeyDown={(e) => {
                            if (e.key !== 'Enter' && e.key !== ' ') return;
                            e.preventDefault();
                            e.stopPropagation();
                            if (bulkForm.slots.length === 1) return;
                            removeBulkSlot(i);
                          }}
                          className={`absolute bottom-2 right-2 p-1 text-neutral-200 hover:text-rose-500 ${bulkForm.slots.length === 1 ? 'pointer-events-none opacity-20' : ''}`}>
                          <Trash2 className="w-3.5 h-3.5" />
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Slots */}
              <div className="hidden">
                <div className="flex items-center justify-between">
                  <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-300 font-black">
                    Danh sách khung giờ ({bulkForm.slots.length} slot)
                  </label>
                  <div className="flex gap-2">
                    <button type="button"
                      onClick={generateBulkSlotsForDay}
                      className="flex items-center gap-1 text-[9px] text-emerald-400 hover:text-emerald-300 border border-emerald-500/30 px-2 py-1 hover:border-emerald-400/50 transition">
                      <Clock className="w-2.5 h-2.5" /> Tự tạo trong ngày
                    </button>
                    <button type="button"
                      onClick={addManualBulkSlot}
                      className="flex items-center gap-1 text-[9px] text-amber-400 hover:text-amber-300 border border-amber-500/30 px-2 py-1 hover:border-amber-400/50 transition">
                      <Plus className="w-2.5 h-2.5" /> Thêm slot
                    </button>
                  </div>
                </div>

                {bulkForm.slots.map((slot, i) => (
                  <div key={i} className="grid grid-cols-[1fr_1fr_auto] gap-4 items-start border border-white/[0.06] p-4 bg-black/30">
                    <div className="space-y-2 mt-4">
                      <label className="text-xs uppercase tracking-widest text-amber-500 font-black block text-center">Phòng chiếu {i + 1}</label>
                      <select value={slot.roomId}
                        onChange={e => {
                          const s = [...bulkForm.slots]; s[i] = { ...s[i], roomId: e.target.value };
                          setBulkForm({ ...bulkForm, slots: s });
                        }}
                        className="w-full bg-black border border-white/[0.08] p-2.5 text-sm text-center font-bold text-white focus:outline-none focus:border-amber-400 appearance-none">
                        <option value="">-- Chọn phòng --</option>
                        {rooms.filter(r => r.status === 'ACTIVE').map(r => (
                          <option key={r.id} value={r.id}>{r.name}</option>
                        ))}
                      </select>
                      {errors[`slot_room_${i}`] && <p className="text-rose-400 text-[8px]">{errors[`slot_room_${i}`]}</p>}
                    </div>
                    <div className="space-y-1">
                      <DateTimePicker
                        label="Giờ bắt đầu"
                        value={slot.startTime}
                        onChange={v => {
                          const s = [...bulkForm.slots]; s[i] = { ...s[i], startTime: v };
                          setBulkForm({ ...bulkForm, slots: s });
                        }}
                        error={errors[`slot_time_${i}`]}
                        minDate={bulkReleaseWindow.releaseDate}
                        maxDate={bulkReleaseWindow.endDate}
                      />
                    </div>
                    <button type="button" disabled={bulkForm.slots.length === 1}
                      onClick={() => {
                        const s = bulkForm.slots.filter((_, j) => j !== i);
                        setBulkForm({ ...bulkForm, slots: s });
                      }}
                      className="p-2 text-neutral-200 hover:text-rose-400 transition disabled:opacity-30">
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>

              <button type="submit" disabled={saving}
                className="w-full py-3.5 bg-amber-500 text-black font-black uppercase text-[10.5px] tracking-widest hover:bg-amber-400 transition disabled:opacity-50">
                {saving ? 'Đang tạo...' : `Tạo ${selectedBulkSlotsCount} suất chiếu cùng lúc`}
              </button>
            </form>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── FILTER BAR ── */}
      {mode === 'list' && (
        <div className="flex flex-wrap gap-2 items-end">
          <div className="space-y-1">
            <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-300 block">Phim</label>
            <select
              value={filters.movieId}
              onChange={e => {
                const nextFilters = { ...filters, movieId: e.target.value };
                setFilters(nextFilters);
                fetchShowtimes(0, nextFilters);
              }}
              className="bg-black border border-white/[0.08] text-xs text-neutral-200 px-2.5 py-2 focus:outline-none focus:border-amber-400 cursor-pointer"
            >
              <option value="">Tất cả phim</option>
              {(moviesList || []).map(m => <option key={m.id} value={m.backendId ?? m.id}>{m.title}</option>)}
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-300 block">Trạng thái</label>
            <select
              value={filters.status}
              onChange={e => {
                const nextFilters = { ...filters, status: e.target.value };
                setFilters(nextFilters);
                fetchShowtimes(0, nextFilters);
              }}
              className="bg-black border border-white/[0.08] text-xs text-neutral-200 px-2.5 py-2 focus:outline-none focus:border-amber-400 cursor-pointer"
            >
              <option value="">Tất cả</option>
              <option value="SCHEDULED">Đã lên lịch</option>
              <option value="OPEN">Đang mở bán</option>
              <option value="COMPLETED">Đã kết thúc</option>
              <option value="CANCELLED">Đã hủy</option>
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-300 block">Ngày</label>
            <div
              onClick={() => {
                try {
                  dateFilterRef.current?.showPicker?.();
                } catch {
                  dateFilterRef.current?.focus?.();
                }
              }}
              className="relative flex items-center bg-black border border-white/[0.08] px-2.5 py-2 cursor-pointer text-xs text-neutral-200 hover:border-amber-400 focus-within:border-amber-400 transition min-w-[130px]"
            >
              <Calendar className="w-3.5 h-3.5 text-amber-400 mr-2 shrink-0 pointer-events-none" />
              <span className="font-mono text-xs select-none">
                {filters.date ? filters.date.split('-').reverse().join('/') : 'dd/mm/yyyy'}
              </span>
              <input
                ref={dateFilterRef}
                type="date"
                value={filters.date}
                onKeyDown={(e) => e.preventDefault()}
                onChange={e => {
                  const nextFilters = { ...filters, date: e.target.value };
                  setFilters(nextFilters);
                  fetchShowtimes(0, nextFilters);
                }}
                className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
                aria-label="Chọn ngày chiếu"
              />
            </div>
          </div>

          <button onClick={() => { const emptyFilters = { movieId: '', roomId: '', status: '', date: '' }; setFilters(emptyFilters); fetchShowtimes(0, emptyFilters); }}
            className="flex items-center gap-1.5 px-3 py-2 border border-white/[0.08] text-neutral-300 text-[10px] font-bold uppercase hover:border-white/[0.20] hover:text-white transition cursor-pointer">
            <RefreshCw className="w-3 h-3" /> Reset
          </button>
        </div>
      )}

      {/* ── TABLE ── */}
      {mode === 'list' && (
        <div className="border border-white/[0.06] overflow-hidden">
          {loading ? (
            <div className="flex items-center justify-center py-16 text-neutral-200">
              <RefreshCw className="w-4 h-4 animate-spin mr-2" /> Đang tải...
            </div>
          ) : showtimes.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-neutral-200">
              <Film className="w-8 h-8 mb-2 opacity-30" />
              <p className="text-xs">Không có suất chiếu nào</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-xs font-sans">
                <thead>
                  <tr className="border-b border-white/[0.06] bg-white/[0.02]">
                    {['Phim', 'Phòng', 'Bắt đầu', 'Kết thúc', 'Giá vé', 'Trạng thái', 'Thao tác'].map(h => (
                      <th key={h} className="text-left px-3 py-3 text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black whitespace-nowrap">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {showtimes.map((st, idx) => {
                    const meta = STATUS_META[st.status] || STATUS_META.SCHEDULED;
                    const movieTitle = st.movieTitle ?? st.movie?.title ?? '—';
                    const roomName = st.roomName ?? st.room?.name ?? '—';
                    return (
                      <tr key={st.id ?? idx}
                        className="border-b border-white/[0.04] hover:bg-white/[0.01] transition-colors">
                        <td className="px-3 py-3 max-w-[160px]">
                          <span className="font-bold text-white truncate block" title={movieTitle}>{movieTitle}</span>
                        </td>
                        <td className="px-3 py-3 text-neutral-200 whitespace-nowrap">{roomName}</td>
                        <td className="px-3 py-3 font-mono text-neutral-200 whitespace-nowrap">{fmt(st.startTime)}</td>
                        <td className="px-3 py-3 font-mono text-neutral-300 whitespace-nowrap">{fmt(st.endTime)}</td>
                        <td className="px-3 py-3 text-neutral-200 whitespace-nowrap">
                          <div className="font-mono">{fmtPrice(st.adultStandardPrice ?? st.basePrice)}</div>
                          {Number(st.surchargeAmount || 0) > 0 && (
                            <div className="text-[9px] text-amber-400">+{fmtPrice(st.surchargeAmount)} phụ thu</div>
                          )}
                        </td>
                        <td className="px-3 py-3">
                          <span className={`text-[8.5px] px-1.5 py-0.5 font-bold border ${meta.bg} ${meta.color}`}>
                            {meta.label}
                          </span>
                        </td>
                        <td className="px-3 py-3">
                          <div className="flex items-center gap-1">
                            {/* Status actions */}
                            {st.status === 'SCHEDULED' && (
                              <button onClick={() => handleStatusChange(st.id, 'OPEN')} title="Mở bán"
                                className="p-1 text-emerald-500 hover:text-emerald-300 hover:bg-emerald-950/30 rounded transition">
                                <Play className="w-3 h-3" />
                              </button>
                            )}
                            {(st.status === 'SCHEDULED' || st.status === 'OPEN' || st.status === 'COMPLETED') && (
                              <button onClick={() => setConfirmCancel(st)} title="Hủy suất chiếu"
                                className="p-1 text-amber-500 hover:text-amber-300 hover:bg-amber-950/30 rounded transition">
                                <Ban className="w-3 h-3" />
                              </button>
                            )}
                            {/* Edit */}
                            {(st.status === 'SCHEDULED' || st.status === 'OPEN') && (
                              <button onClick={() => openEdit(st)} title="Chỉnh sửa"
                                className="p-1 text-blue-400 hover:text-blue-300 hover:bg-blue-950/30 rounded transition">
                                <Edit3 className="w-3 h-3" />
                              </button>
                            )}
                            {/* Detail */}
                            <button onClick={() => setDetailModal(st)} title="Chi tiết"
                              className="p-1 text-neutral-200 hover:text-neutral-300 hover:bg-white/[0.03] rounded transition">
                              <Eye className="w-3 h-3" />
                            </button>
                            {/* Delete */}
                            <button onClick={() => setConfirmDelete(st.id)} title="Xóa"
                              className="p-1 text-neutral-200 hover:text-rose-400 hover:bg-rose-950/30 rounded transition">
                              <Trash2 className="w-3 h-3" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-white/[0.06] bg-white/[0.01]">
              <span className="text-[9px] text-neutral-200 font-mono">Trang {page + 1} / {totalPages}</span>
              <div className="flex gap-1">
                <button disabled={page === 0} onClick={() => fetchShowtimes(page - 1)}
                  className="p-1 text-neutral-200 hover:text-white disabled:opacity-30 transition">
                  <ChevronLeft className="w-3.5 h-3.5" />
                </button>
                <button disabled={page >= totalPages - 1} onClick={() => fetchShowtimes(page + 1)}
                  className="p-1 text-neutral-200 hover:text-white disabled:opacity-30 transition">
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── CONFIRM DELETE MODAL ── */}
      <AnimatePresence>
        {confirmDelete && (
          <motion.div key="delete-confirm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-[250] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}
              className="bg-black border border-rose-500/30 p-6 max-w-sm w-full space-y-4">
              <div className="flex items-center gap-2 text-rose-400">
                <AlertCircle className="w-4 h-4" />
                <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">Xác nhận xóa suất chiếu</h3>
              </div>
              <p className="text-[11px] text-neutral-200">
                Suất chiếu sẽ bị xóa vĩnh viễn. Nếu còn booking đang hoạt động, thao tác sẽ bị từ chối (409).
                Hãy hủy suất chiếu trước để tự động xử lý booking.
              </p>
              <div className="flex gap-2">
                <button onClick={() => setConfirmDelete(null)}
                  className="flex-1 py-2.5 border border-white/[0.10] text-neutral-200 text-[10px] font-bold uppercase hover:border-white/[0.20] transition">
                  Hủy bỏ
                </button>
                <button onClick={() => handleDelete(confirmDelete)}
                  className="flex-1 py-2.5 bg-rose-600 text-white text-[10px] font-black uppercase hover:bg-rose-500 transition">
                  Xóa ngay
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Cancel confirmation modal */}
      <AnimatePresence>
        {confirmCancel && (
          <motion.div key="cancel-confirm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-[250] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}
              className="bg-black border border-amber-500/30 p-6 max-w-sm w-full space-y-4">
              <div className="flex items-center gap-2 text-amber-400">
                <AlertCircle className="w-4 h-4" />
                <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">Xác nhận hủy suất chiếu</h3>
              </div>
              <p className="text-[11px] text-neutral-200">
                Bạn có chắc muốn hủy suất chiếu
                <span className="mx-1 font-bold text-neutral-300">#{confirmCancel.id}</span>
                của phim <span className="font-bold text-white">{confirmCancel.movieTitle ?? confirmCancel.movie?.title ?? 'này'}</span>?
                Thao tác này sẽ chặn khách đặt vé cho suất chiếu đó.
              </p>
              <label className="block space-y-1.5">
                <span className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">LÝ DO HỦY.
                </span>
                <textarea
                  value={cancelReason}
                  onChange={e => setCancelReason(e.target.value)}
                  maxLength={500}
                  rows={3}
                  placeholder="VD: Cup dien, su co ky thuat phong chieu..."
                  className="w-full resize-none bg-black border border-white/[0.08] p-2.5 text-xs text-white focus:outline-none focus:border-amber-400"
                />
              </label>
              <div className="flex gap-2">
                <button onClick={() => { setConfirmCancel(null); setCancelReason(''); }}
                  className="flex-1 py-2.5 border border-white/[0.10] text-neutral-200 text-[10px] font-bold uppercase hover:border-white/[0.20] transition">
                  Quay lại
                </button>
                <button onClick={handleConfirmCancel} disabled={saving}
                  className="flex-1 py-2.5 bg-amber-500 text-black text-[10px] font-black uppercase hover:bg-amber-400 transition disabled:opacity-50">
                  Xác nhận hủy
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
      {/* Refund Summary Result Modal */}
      <AnimatePresence>
        {refundSummary && (
          <motion.div key="refund-summary-modal" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-[300] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}
              className="bg-zinc-950 border border-emerald-500/40 p-6 max-w-md w-full space-y-4 shadow-2xl">
              <div className="flex items-center gap-2 text-emerald-400">
                <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                <h3 className="text-sm font-black uppercase tracking-[0.18em] text-white">Đã Hoàn Tất Hủy Suất Chiếu &amp; Hoàn Tiền</h3>
              </div>
              <div className="border border-white/10 bg-black/60 p-4 text-xs space-y-2 text-neutral-300 font-sans">
                <div className="flex justify-between">
                  <span className="text-neutral-400">Mã suất chiếu:</span>
                  <strong className="text-amber-400 font-mono">#{refundSummary.showtimeId}</strong>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-400">Tổng đơn vé bị ảnh hưởng:</span>
                  <strong className="text-white font-mono">{refundSummary.totalBookings ?? 0} đơn</strong>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-400 font-bold">Hoàn tiền thành công:</span>
                  <strong className="text-emerald-400 font-mono font-bold">{refundSummary.successCount ?? 0} đơn</strong>
                </div>
                {refundSummary.failedCount > 0 && (
                  <div className="flex justify-between">
                    <span className="text-red-400 font-bold">Lỗi hoàn tiền:</span>
                    <strong className="text-red-400 font-mono font-bold">{refundSummary.failedCount} đơn</strong>
                  </div>
                )}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setRefundSummary(null)}
                  className="flex-1 py-2.5 border border-white/10 text-neutral-300 text-[10px] font-bold uppercase hover:bg-neutral-900 transition cursor-pointer"
                >
                  Đóng
                </button>
                <button
                  onClick={() => {
                    setRefundSummary(null);
                    ctx?.changeAdminSection?.('showtime-incidents');
                  }}
                  className="flex-1 py-2.5 bg-amber-500 text-black text-[10px] font-black uppercase hover:bg-amber-400 transition cursor-pointer"
                >
                  Xem Báo Cáo Chi Tiết
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>



      {/* Detail modal */}
      <AnimatePresence>
        {detailModal && (
          <motion.div key="detail-modal" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={() => setDetailModal(null)}
            className="fixed inset-0 z-[250] flex items-start justify-center overflow-y-auto bg-black/80 p-4 pt-20 pb-24 backdrop-blur-sm">
            <motion.div initial={{ scale: 0.95, y: 10 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 10 }}
              onClick={e => e.stopPropagation()}
              className="max-h-[calc(100vh-8rem)] w-full max-w-md space-y-4 overflow-y-auto border border-white/[0.08] bg-black p-6 shadow-2xl">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">Chi tiết suất chiếu #{detailModal.id}</h3>
                <button onClick={() => setDetailModal(null)} className="text-neutral-300 hover:text-white"><X className="w-4 h-4" /></button>
              </div>
              <div className="space-y-2 text-[11px]">
                {(() => {
                  const roomId = getShowtimeRoomId(detailModal);
                  const seatTypes = roomSeatTypesById[roomId] || [];
                  const showChildPrices = allowsChildTicketsForShowtime(detailModal);
                  const hasVipSeats = seatTypes.includes('VIP');
                  const hasCoupleSeats = seatTypes.includes('COUPLE');
                  const rows = [
                    ['Phim', detailModal.movieTitle ?? detailModal.movie?.title],
                    ['Phòng', detailModal.roomName ?? detailModal.room?.name],
                    ['Bắt đầu', fmt(detailModal.startTime)],
                    ['Kết thúc', fmt(detailModal.endTime)],
                  ];
                  const addPriceRow = (label, value) => {
                    const formatted = fmtDetailPrice(value);
                    if (formatted) rows.push([label, formatted]);
                  };

                  addPriceRow('Người lớn - thường', detailModal.adultStandardPrice ?? detailModal.basePrice);
                  if (showChildPrices) addPriceRow('Trẻ em - thường', detailModal.childStandardPrice);
                  addPriceRow('Sinh viên - thường', detailModal.studentStandardPrice);

                  if (hasVipSeats) {
                    addPriceRow('Người lớn - VIP', detailModal.adultVipPrice ?? detailModal.vipPrice);
                    if (showChildPrices) addPriceRow('Trẻ em - VIP', detailModal.childVipPrice);
                    addPriceRow('Sinh viên - VIP', detailModal.studentVipPrice);
                  }

                  if (hasCoupleSeats) {
                    addPriceRow('Người lớn - ghế đôi', detailModal.adultCouplePrice ?? detailModal.couplePrice);
                    if (showChildPrices) addPriceRow('Trẻ em - ghế đôi', detailModal.childCouplePrice);
                    addPriceRow('Sinh viên - ghế đôi', detailModal.studentCouplePrice);
                  }

                  addPriceRow('Phụ thu áp dụng', detailModal.surchargeAmount);
                  rows.push(['Trạng thái', STATUS_META[detailModal.status]?.label ?? detailModal.status]);
                  return rows.map(([label, val]) => (
                    <div key={label} className="flex justify-between border-b border-white/[0.06] pb-1.5">
                      <span className="text-neutral-300 font-bold">{label}</span>
                      <span className="text-neutral-300 font-mono">{val ?? '—'}</span>
                    </div>
                  ));
                })()}
              </div>
              {/* Quick status actions in detail */}
              {(detailModal.status === 'SCHEDULED' || detailModal.status === 'OPEN' || detailModal.status === 'COMPLETED') && (
                <div className="flex gap-2 pt-2">
                  {detailModal.status === 'SCHEDULED' && (
                    <button onClick={() => { handleStatusChange(detailModal.id, 'OPEN'); setDetailModal(null); }}
                      className="flex-1 py-2 bg-emerald-600 text-white text-[10px] font-black uppercase hover:bg-emerald-500 transition">
                      Mở bán ngay
                    </button>
                  )}
                  <button onClick={() => { setConfirmCancel(detailModal); setDetailModal(null); }}
                    className="flex-1 py-2 bg-rose-900 text-rose-300 text-[10px] font-black uppercase hover:bg-rose-800 transition">
                    Hủy suất chiếu
                  </button>
                </div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}


