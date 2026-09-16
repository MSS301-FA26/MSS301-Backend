import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import {
  BarChart3,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  DollarSign,
  PackageCheck,
  ReceiptText,
  RefreshCw,
  ShoppingBag,
  Trophy,
  TrendingUp,
} from 'lucide-react';
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { adminService } from '../../../services/adminService';

const DAY_IN_MS = 24 * 60 * 60 * 1000;

const toDateInput = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const toMonthInput = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;

const formatVnd = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`;
const formatCompactVnd = (value) => new Intl.NumberFormat('vi-VN', {
  notation: 'compact',
  maximumFractionDigits: 1,
}).format(Number(value || 0));
const formatDate = (value) => new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`));

const SOURCE_META = {
  WITH_TICKET: {
    label: 'Mua cùng vé',
    description: 'Món được thanh toán trong booking vé',
  },
  ADD_ON: {
    label: 'Đặt thêm cho vé',
    description: 'Đơn F&B riêng liên kết với booking đã có',
  },
  STANDALONE: {
    label: 'Mua riêng',
    description: 'Đơn bắp nước không yêu cầu vé xem phim',
  },
};

function SalesTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  const row = payload[0]?.payload;
  return (
    <div className="min-w-44 border border-white/15 bg-[#090909] p-3 shadow-2xl">
      <p className="text-[9px] font-black uppercase tracking-[0.16em] text-neutral-300">{label}</p>
      <div className="mt-2 space-y-1.5 text-xs">
        <p className="flex justify-between gap-5 text-neutral-300"><span>Số đơn</span><strong className="font-mono text-white">{row.orderCount}</strong></p>
        <p className="flex justify-between gap-5 text-neutral-300"><span>Món/combo</span><strong className="font-mono text-amber-300">{row.quantity}</strong></p>
        <p className="flex justify-between gap-5 text-neutral-300"><span>Doanh thu</span><strong className="font-mono text-emerald-300">{formatVnd(row.revenue)}</strong></p>
      </div>
    </div>
  );
}

function MetricCard({ icon: Icon, label, value, helper, tone = 'text-white', valueClassName = 'text-xl' }) {
  return (
    <article className="min-w-0 border border-white/10 bg-[#080808] p-3.5">
      <div className="flex items-center justify-between gap-3">
        <p className="text-[9px] font-black uppercase tracking-[0.18em] text-neutral-300">{label}</p>
        <Icon className="h-4 w-4 text-amber-400" aria-hidden="true" />
      </div>
      <p className={`mt-2 truncate font-mono font-black leading-tight ${tone} ${valueClassName}`} title={String(value)}>{value}</p>
      <p className="mt-1 text-[10px] leading-relaxed text-neutral-300">{helper}</p>
    </article>
  );
}

function LoadingState() {
  return (
    <div className="space-y-5" aria-label="Đang tải báo cáo F&B" aria-busy="true">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {[0, 1, 2, 3, 4].map((item) => <div key={item} className="h-28 animate-pulse border border-white/10 bg-white/[0.035]" />)}
      </div>
      <div className="h-80 animate-pulse border border-white/10 bg-white/[0.035]" />
    </div>
  );
}

export default function AdminFnbReportPanel({ ctx }) {
  const { activeTab, getAdminToken, changeAdminSection } = ctx;
  const today = useMemo(() => new Date(), []);
  const [fromDate, setFromDate] = useState(() => toDateInput(new Date(today.getFullYear(), today.getMonth(), 1)));
  const [toDate, setToDate] = useState(() => toDateInput(today));
  const [selectedMonth, setSelectedMonth] = useState(() => toMonthInput(today));
  const [report, setReport] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [dailyPage, setDailyPage] = useState(1);

  const DAILY_PAGE_SIZE = 10;

  const loadReport = useCallback(async (nextFrom, nextTo) => {
    const requestFrom = nextFrom || fromDate;
    const requestTo = nextTo || toDate;
    if (requestFrom > requestTo) {
      setError('Ngày bắt đầu không được sau ngày kết thúc.');
      return;
    }

    const rangeDays = Math.round((new Date(`${requestTo}T00:00:00`) - new Date(`${requestFrom}T00:00:00`)) / DAY_IN_MS);
    if (rangeDays > 366) {
      setError('Khoảng báo cáo tối đa là 366 ngày.');
      return;
    }

    const token = getAdminToken();
    if (!token) {
      setError('Phiên quản trị đã hết hạn. Vui lòng đăng nhập lại.');
      return;
    }

    setIsLoading(true);
    setError('');
    setDailyPage(1);
    try {
      const data = await adminService.getConcessionSales(token, { from: requestFrom, to: requestTo });
      setReport(data || null);
    } catch (requestError) {
      setReport(null);
      setError(requestError?.message || 'Không thể tải báo cáo F&B.');
    } finally {
      setIsLoading(false);
    }
  }, [fromDate, getAdminToken, toDate]);

  useEffect(() => {
    if (activeTab === 'fnb-report') loadReport();
    // Load once when the report section is mounted; filters are applied explicitly.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  const applyMonth = (monthValue = selectedMonth) => {
    if (!monthValue) return;
    const [year, month] = monthValue.split('-').map(Number);
    const monthStart = new Date(year, month - 1, 1);
    const monthEnd = new Date(year, month, 0);
    const currentMonth = toMonthInput(new Date());
    const effectiveEnd = monthValue === currentMonth ? new Date() : monthEnd;
    const nextFrom = toDateInput(monthStart);
    const nextTo = toDateInput(effectiveEnd);
    setFromDate(nextFrom);
    setToDate(nextTo);
    loadReport(nextFrom, nextTo);
  };

  const applyPreset = (preset) => {
    const now = new Date();
    if (preset === 'month') {
      const currentMonth = toMonthInput(now);
      setSelectedMonth(currentMonth);
      applyMonth(currentMonth);
      return;
    }
    let start = now;
    if (preset === '7d') start = new Date(now.getTime() - 6 * DAY_IN_MS);
    if (preset === '30d') start = new Date(now.getTime() - 29 * DAY_IN_MS);
    const nextFrom = toDateInput(start);
    const nextTo = toDateInput(now);
    setSelectedMonth('');
    setFromDate(nextFrom);
    setToDate(nextTo);
    loadReport(nextFrom, nextTo);
  };

  const dailyList = useMemo(() => report?.daily || [], [report]);
  const totalDailyItems = dailyList.length;
  const totalDailyPages = Math.ceil(totalDailyItems / DAILY_PAGE_SIZE) || 1;
  const safeDailyPage = Math.min(Math.max(1, dailyPage), totalDailyPages);

  const paginatedDaily = useMemo(() => {
    const start = (safeDailyPage - 1) * DAILY_PAGE_SIZE;
    return dailyList.slice(start, start + DAILY_PAGE_SIZE);
  }, [dailyList, safeDailyPage]);

  const chartData = useMemo(() => dailyList.map((line) => ({
    ...line,
    label: formatDate(line.date).slice(0, 5),
  })), [dailyList]);

  const totalRevenue = Number(report?.totalRevenue || 0);
  const products = report?.lines || [];
  const sources = report?.sources || [];
  const bestProduct = products[0] || null;
  const hasSales = Number(report?.totalOrders || 0) > 0;

  if (activeTab !== 'fnb-report') return null;

  return (
    <motion.section
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2 }}
      className="space-y-5"
      aria-labelledby="fnb-report-title"
    >
      <header className="border border-white/10 bg-gradient-to-r from-[#090909] to-[#050505] p-5 sm:p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="font-mono text-[9px] font-black uppercase tracking-[0.24em] text-neutral-300">F&amp;B Intelligence</p>
            <h2 id="fnb-report-title" className="mt-1 text-sm font-sans font-black uppercase tracking-wide text-neutral-200">Báo cáo bán bắp nước</h2>
            <p className="mt-1 max-w-2xl text-xs leading-relaxed text-neutral-300">
              Theo dõi số đơn, sản lượng và doanh thu theo ngày. Chỉ ghi nhận giao dịch đã thanh toán hoặc đã giao món.
            </p>
          </div>
          <button
            type="button"
            onClick={() => changeAdminSection('foods')}
            className="inline-flex min-h-11 items-center justify-center gap-2 border border-white/15 px-4 text-[9px] font-black uppercase tracking-[0.16em] text-neutral-300 transition hover:border-amber-400/50 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-400"
          >
            <ShoppingBag className="h-3.5 w-3.5" aria-hidden="true" /> Quản lý danh mục
          </button>
        </div>
      </header>

      <form
        onSubmit={(event) => { event.preventDefault(); loadReport(); }}
        className="border border-white/10 bg-[#070707] p-3.5"
        aria-label="Bộ lọc thời gian báo cáo F&B"
      >
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(280px,1.15fr)_minmax(210px,1fr)_minmax(210px,1fr)]">
            <div className="space-y-1.5">
              <label htmlFor="fnb-report-month" className="text-[9px] font-black uppercase tracking-[0.16em] text-neutral-500">Xem theo tháng</label>
              <span className="flex h-11 items-stretch border border-amber-400/30 bg-amber-400/[0.035] focus-within:border-amber-400/70">
                <input
                  id="fnb-report-month"
                  type="month"
                  value={selectedMonth}
                  max={toMonthInput(new Date())}
                  onChange={(event) => setSelectedMonth(event.target.value)}
                  className="h-full min-w-0 flex-1 bg-transparent px-3 py-0 text-sm font-bold text-white outline-none [color-scheme:dark]"
                />
                <button
                  type="button"
                  disabled={!selectedMonth || isLoading}
                  onClick={() => applyMonth()}
                  className="h-full border-l border-amber-400/20 px-3 py-0 text-[9px] font-black uppercase tracking-wider text-amber-300 transition hover:bg-amber-400 hover:text-black focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-white disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Xem
                </button>
              </span>
            </div>
            <label className="space-y-1.5">
              <span className="text-[9px] font-black uppercase tracking-[0.16em] text-neutral-500">Từ ngày</span>
              <span className="flex h-11 items-center gap-2 border border-white/10 bg-black px-3 focus-within:border-amber-400/60">
                <CalendarDays className="h-4 w-4 text-amber-400" aria-hidden="true" />
                <input type="date" value={fromDate} max={toDate} onChange={(event) => { setFromDate(event.target.value); setSelectedMonth(''); }} className="h-full min-w-0 flex-1 bg-transparent py-0 text-sm font-bold text-white outline-none [color-scheme:dark]" />
              </span>
            </label>
            <label className="space-y-1.5">
              <span className="text-[9px] font-black uppercase tracking-[0.16em] text-neutral-500">Đến ngày</span>
              <span className="flex h-11 items-center gap-2 border border-white/10 bg-black px-3 focus-within:border-amber-400/60">
                <CalendarDays className="h-4 w-4 text-amber-400" aria-hidden="true" />
                <input type="date" value={toDate} min={fromDate} max={toDateInput(new Date())} onChange={(event) => { setToDate(event.target.value); setSelectedMonth(''); }} className="h-full min-w-0 flex-1 bg-transparent py-0 text-sm font-bold text-white outline-none [color-scheme:dark]" />
              </span>
            </label>
          </div>

          <div className="flex flex-wrap items-center gap-2 border-t border-white/[0.06] pt-4">
            {[
              ['today', 'Hôm nay'],
              ['7d', '7 ngày'],
              ['30d', '30 ngày'],
              ['month', 'Tháng này'],
            ].map(([value, label]) => (
              <button key={value} type="button" onClick={() => applyPreset(value)} className="h-11 border border-white/10 px-3 py-0 text-[9px] font-black uppercase tracking-wider text-neutral-400 transition hover:border-white/30 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-400">
                {label}
              </button>
            ))}
            <button type="submit" disabled={isLoading} className="inline-flex h-11 items-center gap-2 bg-amber-500 px-4 py-0 text-[9px] font-black uppercase tracking-[0.16em] text-black transition hover:bg-amber-400 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white disabled:cursor-wait disabled:opacity-60">
              <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} aria-hidden="true" /> Áp dụng
            </button>
          </div>
        </div>
      </form>

      {error && (
        <div className="flex flex-col gap-3 border border-rose-500/30 bg-rose-500/[0.06] p-4 sm:flex-row sm:items-center sm:justify-between" role="alert">
          <div>
            <p className="text-xs font-black uppercase tracking-wider text-rose-300">Không tải được báo cáo</p>
            <p className="mt-1 text-xs text-neutral-400">{error}</p>
          </div>
          <button type="button" onClick={() => loadReport()} className="min-h-11 border border-rose-400/40 px-4 text-[9px] font-black uppercase tracking-widest text-rose-200 transition hover:bg-rose-400 hover:text-black">Thử lại</button>
        </div>
      )}

      {isLoading ? <LoadingState /> : report && (
        <>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
            <MetricCard icon={ReceiptText} label="Đơn đã thanh toán" value={Number(report.totalOrders || 0).toLocaleString('vi-VN')} helper="Mỗi booking hoặc food order được tính một đơn" />
            <MetricCard icon={PackageCheck} label="Món/combo đã bán" value={Number(report.totalItemsSold || 0).toLocaleString('vi-VN')} helper="Mỗi combo được tính là một đơn vị bán" tone="text-amber-300" />
            <MetricCard icon={DollarSign} label="Doanh thu F&B" value={formatVnd(report.totalRevenue)} helper="Theo thời điểm thanh toán thành công" tone="text-emerald-300" />
            <MetricCard icon={TrendingUp} label="Giá trị đơn trung bình" value={formatVnd(report.averageOrderValue)} helper="Trung bình trên mỗi đơn đã thanh toán" />
            <MetricCard
              icon={Trophy}
              label="Món/combo bán chạy"
              value={bestProduct?.name || '—'}
              helper={bestProduct
                ? `${Number(bestProduct.quantity).toLocaleString('vi-VN')} lượt bán · ${formatVnd(bestProduct.revenue)}`
                : 'Chưa phát sinh giao dịch trong kỳ'}
              tone="text-amber-300"
              valueClassName="font-sans text-lg uppercase tracking-wide"
            />
          </div>

          {!hasSales ? (
            <div className="flex min-h-64 flex-col items-center justify-center border border-dashed border-white/15 bg-[#070707] px-6 text-center">
              <BarChart3 className="h-8 w-8 text-neutral-700" aria-hidden="true" />
              <h3 className="mt-4 text-sm font-black uppercase tracking-wider text-white">Chưa có giao dịch F&amp;B</h3>
              <p className="mt-2 max-w-md text-xs leading-relaxed text-neutral-500">Không có đơn đã thanh toán hoặc đã giao món trong khoảng thời gian đã chọn. Hãy thử mở rộng khoảng ngày.</p>
            </div>
          ) : (
            <>
              <section className="border border-white/10 bg-[#070707] p-4 sm:p-5" aria-labelledby="daily-sales-chart-title">
                <div className="flex flex-col gap-2 border-b border-white/10 pb-4 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <p className="text-[9px] font-black uppercase tracking-[0.2em] text-amber-400">Xu hướng theo ngày</p>
                    <h3 id="daily-sales-chart-title" className="mt-1 text-sm font-black uppercase tracking-[0.14em] text-white">Sản lượng và doanh thu</h3>
                  </div>
                  <div className="flex items-center gap-4 text-[9px] font-bold uppercase tracking-wider text-neutral-500">
                    <span className="flex items-center gap-1.5"><i className="h-2.5 w-2.5 bg-amber-500" /> Món/combo</span>
                    <span className="flex items-center gap-1.5"><i className="h-0.5 w-4 bg-emerald-400" /> Doanh thu</span>
                  </div>
                </div>
                <div className="mt-4 h-72 w-full sm:h-80">
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={chartData} margin={{ top: 12, right: 8, left: 0, bottom: 0 }}>
                      <CartesianGrid stroke="rgba(255,255,255,0.06)" vertical={false} />
                      <XAxis dataKey="label" tick={{ fill: '#737373', fontSize: 10 }} tickLine={false} axisLine={{ stroke: 'rgba(255,255,255,0.1)' }} minTickGap={24} />
                      <YAxis yAxisId="quantity" allowDecimals={false} tick={{ fill: '#737373', fontSize: 10 }} tickLine={false} axisLine={false} width={34} />
                      <YAxis yAxisId="revenue" orientation="right" tickFormatter={formatCompactVnd} tick={{ fill: '#737373', fontSize: 10 }} tickLine={false} axisLine={false} width={48} />
                      <Tooltip content={<SalesTooltip />} />
                      <Bar yAxisId="quantity" dataKey="quantity" fill="#f59e0b" maxBarSize={28} radius={[2, 2, 0, 0]} />
                      <Line yAxisId="revenue" type="monotone" dataKey="revenue" stroke="#34d399" strokeWidth={2} dot={chartData.length <= 31 ? { r: 2.5, fill: '#070707', strokeWidth: 2 } : false} activeDot={{ r: 4 }} />
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              </section>

              <div className="grid grid-cols-1 gap-5 2xl:grid-cols-[minmax(0,1.45fr)_minmax(320px,0.75fr)]">
                <section className="min-w-0 border border-white/10 bg-[#070707]" aria-labelledby="daily-sales-table-title">
                  <div className="border-b border-white/10 p-4">
                    <h3 id="daily-sales-table-title" className="text-[10px] font-black uppercase tracking-[0.18em] text-white">Chi tiết theo ngày</h3>
                    <p className="mt-1 text-[10px] text-neutral-500">Đối soát số đơn, sản lượng và giá trị trung bình từng ngày.</p>
                  </div>
                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[680px] border-collapse text-left">
                      <thead className="bg-black/60 text-[9px] font-black uppercase tracking-[0.16em] text-neutral-500">
                        <tr><th className="px-4 py-3">Ngày</th><th className="px-4 py-3 text-right">Số đơn</th><th className="px-4 py-3 text-right">Món/combo</th><th className="px-4 py-3 text-right">Doanh thu</th><th className="px-4 py-3 text-right">TB/đơn</th></tr>
                      </thead>
                      <tbody className="divide-y divide-white/[0.06]">
                        {paginatedDaily.map((line) => (
                          <tr key={line.date} className="text-xs transition hover:bg-white/[0.025]">
                            <td className="px-4 py-3 font-bold text-white">{formatDate(line.date)}</td>
                            <td className="px-4 py-3 text-right font-mono text-neutral-300">{line.orderCount}</td>
                            <td className="px-4 py-3 text-right font-mono text-amber-300">{line.quantity}</td>
                            <td className="px-4 py-3 text-right font-mono font-bold text-emerald-300">{formatVnd(line.revenue)}</td>
                            <td className="px-4 py-3 text-right font-mono text-neutral-400">{formatVnd(line.orderCount ? Number(line.revenue) / line.orderCount : 0)}</td>
                          </tr>
                        ))}
                        {paginatedDaily.length === 0 && (
                          <tr>
                            <td colSpan={5} className="px-4 py-6 text-center text-xs text-neutral-500">
                              Không có dữ liệu trong khoảng thời gian đã chọn.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>

                  {totalDailyItems > 0 && (
                    <div className="flex flex-col gap-3 border-t border-white/10 bg-black/60 p-3.5 text-[10px] font-black uppercase tracking-[0.16em] text-neutral-500 sm:flex-row sm:items-center sm:justify-between">
                      <span>
                        Hiển thị {Math.min((safeDailyPage - 1) * DAILY_PAGE_SIZE + 1, totalDailyItems)}-{Math.min(safeDailyPage * DAILY_PAGE_SIZE, totalDailyItems)} / {totalDailyItems} ngày · Trang {safeDailyPage}/{totalDailyPages}
                      </span>
                      {totalDailyPages > 1 && (
                        <div className="flex items-center gap-1.5">
                          <button
                            type="button"
                            disabled={safeDailyPage <= 1}
                            onClick={() => setDailyPage((p) => Math.max(1, p - 1))}
                            className="flex items-center gap-1 border border-neutral-800 bg-black px-2.5 py-1.5 text-white transition hover:border-amber-400/50 hover:text-amber-300 disabled:cursor-not-allowed disabled:opacity-30"
                          >
                            <ChevronLeft className="h-3.5 w-3.5" /> Trước
                          </button>
                          {Array.from({ length: totalDailyPages }, (_, i) => i + 1).map((p) => {
                            const isNear = Math.abs(p - safeDailyPage) <= 1 || p === 1 || p === totalDailyPages;
                            if (!isNear && Math.abs(p - safeDailyPage) === 2) {
                              return <span key={`ellipsis-${p}`} className="px-1 text-neutral-600 select-none">…</span>;
                            }
                            if (!isNear) return null;
                            return (
                              <button
                                key={p}
                                type="button"
                                onClick={() => setDailyPage(p)}
                                className={`h-7 min-w-7 border px-2 py-1 text-[10px] font-black transition ${
                                  p === safeDailyPage
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
                            disabled={safeDailyPage >= totalDailyPages}
                            onClick={() => setDailyPage((p) => Math.min(totalDailyPages, p + 1))}
                            className="flex items-center gap-1 border border-neutral-800 bg-black px-2.5 py-1.5 text-white transition hover:border-amber-400/50 hover:text-amber-300 disabled:cursor-not-allowed disabled:opacity-30"
                          >
                            Sau <ChevronRight className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </section>

                <section className="border border-white/10 bg-[#070707]" aria-labelledby="sales-source-title">
                  <div className="border-b border-white/10 p-4">
                    <h3 id="sales-source-title" className="text-[10px] font-black uppercase tracking-[0.18em] text-white">Nguồn đơn hàng</h3>
                    <p className="mt-1 text-[10px] text-neutral-500">Biết khách mua món ở giai đoạn nào.</p>
                  </div>
                  <div className="divide-y divide-white/[0.06]">
                    {sources.map((source) => {
                      const meta = SOURCE_META[source.source] || { label: source.source, description: '' };
                      const share = totalRevenue > 0 ? (Number(source.revenue) / totalRevenue) * 100 : 0;
                      return (
                        <article key={source.source} className="p-4">
                          <div className="flex items-start justify-between gap-3">
                            <div className="min-w-0"><p className="text-xs font-black uppercase tracking-wide text-white">{meta.label}</p><p className="mt-1 text-[10px] leading-relaxed text-neutral-500">{meta.description}</p></div>
                            <span className="shrink-0 font-mono text-sm font-black text-amber-300">{share.toFixed(1)}%</span>
                          </div>
                          <div className="mt-3 h-1.5 overflow-hidden bg-white/[0.06]"><div className="h-full bg-amber-500" style={{ width: `${share}%` }} /></div>
                          <div className="mt-3 grid grid-cols-3 gap-2 text-[9px] text-neutral-500">
                            <span><strong className="block font-mono text-xs text-white">{source.orderCount}</strong> đơn</span>
                            <span><strong className="block font-mono text-xs text-white">{source.quantity}</strong> món/combo</span>
                            <span className="text-right"><strong className="block font-mono text-xs text-emerald-300">{formatVnd(source.revenue)}</strong> doanh thu</span>
                          </div>
                        </article>
                      );
                    })}
                  </div>
                </section>
              </div>

              <section className="border border-white/10 bg-[#070707]" aria-labelledby="product-performance-title">
                <div className="border-b border-white/10 p-4">
                  <p className="text-[9px] font-black uppercase tracking-[0.2em] text-amber-400">Product mix</p>
                  <h3 id="product-performance-title" className="mt-1 text-sm font-black uppercase tracking-[0.14em] text-white">Hiệu suất theo món &amp; combo</h3>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[680px] border-collapse text-left">
                    <thead className="bg-black/60 text-[9px] font-black uppercase tracking-[0.16em] text-neutral-500">
                      <tr><th className="px-4 py-3">Xếp hạng</th><th className="px-4 py-3">Món/combo</th><th className="px-4 py-3 text-right">Lượt bán</th><th className="px-4 py-3 text-right">Doanh thu</th><th className="px-4 py-3 text-right">Tỷ trọng</th></tr>
                    </thead>
                    <tbody className="divide-y divide-white/[0.06]">
                      {products.map((line, index) => {
                        const share = totalRevenue > 0 ? (Number(line.revenue) / totalRevenue) * 100 : 0;
                        return (
                          <tr key={line.name} className="text-xs transition hover:bg-white/[0.025]">
                            <td className="px-4 py-3 font-mono text-neutral-600">#{String(index + 1).padStart(2, '0')}</td>
                            <td className="px-4 py-3 font-black uppercase tracking-wide text-white">{line.name}</td>
                            <td className="px-4 py-3 text-right font-mono text-amber-300">{Number(line.quantity).toLocaleString('vi-VN')}</td>
                            <td className="px-4 py-3 text-right font-mono font-bold text-emerald-300">{formatVnd(line.revenue)}</td>
                            <td className="px-4 py-3 text-right font-mono text-neutral-300">{share.toFixed(1)}%</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </section>
            </>
          )}
        </>
      )}
    </motion.section>
  );
}
