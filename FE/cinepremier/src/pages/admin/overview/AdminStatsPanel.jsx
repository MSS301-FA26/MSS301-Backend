import React, { useEffect, useMemo, useState } from 'react';
import { Armchair, BarChart2, DoorOpen, Gift, RefreshCw } from 'lucide-react';
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip
} from 'recharts';
import { adminService } from '../../../services/adminService';

const toDateInput = (date) => date.toISOString().split('T')[0];
const formatVnd = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`;

/**
 * Panel thống kê vận hành — cung cấp số liệu để ra quyết định giá và lịch chiếu:
 * tỷ lệ lấp đầy suất chiếu, no-show, giờ cao điểm, đặt-bỏ (abandoned), doanh thu bắp nước,
 * heatmap ghế theo phòng, và danh sách tài khoản loyalty sắp hết hạn điểm.
 *
 * @param {{ getAdminToken: () => string|null, showToast: (msg: string) => void }} ctx
 */
export default function AdminStatsPanel({ ctx }) {
  const { getAdminToken, showToast } = ctx;
  const [fromDate, setFromDate] = useState(() => toDateInput(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)));
  const [toDate, setToDate] = useState(() => toDateInput(new Date()));
  const [isLoading, setIsLoading] = useState(false);
  const [showtimeFill, setShowtimeFill] = useState([]);
  const [noShow, setNoShow] = useState(null);
  const [peakHours, setPeakHours] = useState([]);
  const [abandoned, setAbandoned] = useState(null);
  const [concessions, setConcessions] = useState(null);
  const [expiredUsers, setExpiredUsers] = useState([]);
  const [grantingUserId, setGrantingUserId] = useState(null);
  const [topSeats, setTopSeats] = useState([]);
  const [seatRoomId, setSeatRoomId] = useState('');
  const [rooms, setRooms] = useState([]);
  const [fillDayKey, setFillDayKey] = useState('');

  const loadTopSeats = async (roomIdValue = seatRoomId) => {
    const token = getAdminToken();
    if (!token) return;
    const params = { from: fromDate, to: toDate };
    if (roomIdValue) params.roomId = roomIdValue;
    const data = await adminService.getTopSeats(token, params).catch(() => []);
    setTopSeats(Array.isArray(data) ? data : []);
  };

  const loadAll = async () => {
    const token = getAdminToken();
    if (!token) return;
    setIsLoading(true);
    const range = { from: fromDate, to: toDate };
    try {
      const [fill, noShows, peaks, abandonedRate, concessionSales, expired] = await Promise.all([
        adminService.getShowtimeFill(token, { days: 7 }).catch(() => []),
        adminService.getNoShowReport(token, range).catch(() => null),
        adminService.getPeakHours(token, range).catch(() => []),
        adminService.getAbandonedRate(token, range).catch(() => null),
        adminService.getConcessionSales(token, range).catch(() => null),
        adminService.getExpiredUsers(token, range).catch(() => []),
        loadTopSeats()
      ]);
      setShowtimeFill(Array.isArray(fill) ? fill : []);
      setNoShow(noShows);
      setPeakHours(Array.isArray(peaks) ? peaks : []);
      setAbandoned(abandonedRate);
      setConcessions(concessionSales);
      setExpiredUsers(Array.isArray(expired) ? expired : []);
    } catch (err) {
      showToast(err?.message || 'Không thể tải thống kê.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadAll(); }, []);

  // Danh sách phòng cho bộ lọc heatmap ghế
  useEffect(() => {
    const token = getAdminToken();
    if (!token) return;
    adminService.getAdminRooms(token)
      .then((data) => {
        const list = Array.isArray(data) ? data : data?.content || data?.items || [];
        setRooms(list);
      })
      .catch(() => setRooms([]));
  }, []);

  const handleSeatRoomChange = (value) => {
    setSeatRoomId(value);
    loadTopSeats(value);
  };

  // Nhóm suất chiếu theo ngày (theo giờ ĐỊA PHƯƠNG — toISOString là UTC sẽ lệch ngày ở VN)
  const localDayKey = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  const fillByDay = useMemo(() => {
    const groups = new Map();
    for (const st of showtimeFill) {
      const start = new Date(st.startTime);
      const dayKey = localDayKey(start);
      if (!groups.has(dayKey)) groups.set(dayKey, []);
      groups.get(dayKey).push(st);
    }
    const todayKey = localDayKey(new Date());
    return [...groups.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([dayKey, items]) => {
        const day = new Date(`${dayKey}T00:00:00`);
        const dayLabel = day.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit' });
        const shortDate = day.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
        const shortWeekday = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][day.getDay()];
        return {
          dayKey,
          dayLabel: dayKey === todayKey ? `Hôm nay · ${shortDate}` : dayLabel,
          chipLabel: dayKey === todayKey ? `Hôm nay ${shortDate}` : `${shortWeekday} ${shortDate}`,
          items: [...items].sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
        };
      });
  }, [showtimeFill]);

  // Phân trang theo ngày: mặc định chọn ngày đầu tiên có suất; tự sửa khi key không còn
  useEffect(() => {
    if (fillByDay.length === 0) return;
    if (!fillByDay.some((g) => g.dayKey === fillDayKey)) setFillDayKey(fillByDay[0].dayKey);
  }, [fillByDay, fillDayKey]);

  const activeFillDay = fillByDay.find((g) => g.dayKey === fillDayKey) || fillByDay[0] || null;

  const peakChartData = useMemo(() => {
    const byHour = new Map(peakHours.map((p) => [p.hour, p.ticketsSold]));
    return Array.from({ length: 18 }, (_, i) => {
      const hour = i + 6; // rạp hoạt động 6h-23h
      return { hour: `${hour}h`, 'Vé bán': byHour.get(hour) || 0 };
    });
  }, [peakHours]);

  // Gộp theo vị trí ghế (A5 cộng dồn các phòng khi xem "Tất cả phòng");
  // seatType hiển thị = loại chiếm nhiều lượt mua nhất tại vị trí đó
  const seatHeat = useMemo(() => {
    const byPos = new Map();
    for (const s of topSeats) {
      const key = `${s.rowLabel}${s.seatNumber}`;
      const cur = byPos.get(key) || { rowLabel: s.rowLabel, seatNumber: s.seatNumber, label: key, count: 0, types: {} };
      cur.count += s.timesPurchased;
      cur.types[s.seatType] = (cur.types[s.seatType] || 0) + s.timesPurchased;
      byPos.set(key, cur);
    }
    const positions = Array.from(byPos.values()).map((p) => ({
      ...p,
      seatType: Object.entries(p.types).sort((a, b) => b[1] - a[1])[0]?.[0] || 'STANDARD'
    }));

    // Chọn 1 phòng cụ thể → vẽ TOÀN BỘ sơ đồ phòng (cả ghế chưa từng bán);
    // "Tất cả phòng" → chỉ vẽ union các vị trí có dữ liệu
    const selectedRoom = rooms.find((r) => String(r.id) === String(seatRoomId));
    let rowLabels;
    let maxNumber;
    if (selectedRoom?.rowCount && selectedRoom?.columnCount) {
      rowLabels = Array.from({ length: selectedRoom.rowCount }, (_, i) => String.fromCharCode(65 + i));
      maxNumber = selectedRoom.columnCount;
    } else {
      rowLabels = [...new Set(positions.map((p) => p.rowLabel))].sort();
      maxNumber = positions.length ? Math.max(...positions.map((p) => p.seatNumber)) : 0;
    }
    const maxCount = positions.length ? Math.max(...positions.map((p) => p.count)) : 1;
    const grid = new Map(positions.map((p) => [p.label, p]));
    const top = [...positions].sort((a, b) => b.count - a.count).slice(0, 10);
    const totalTickets = positions.reduce((sum, p) => sum + p.count, 0);
    return { rowLabels, maxNumber, maxCount, grid, top, totalTickets };
  }, [topSeats, rooms, seatRoomId]);

  const handleGrantPoints = async (user) => {
    const token = getAdminToken();
    if (!token || grantingUserId) return;
    setGrantingUserId(user.userId);
    try {
      await adminService.grantLoyaltyPoints(token, { userId: user.userId, points: 100, reason: 'Tặng điểm thiện chí — booking hết hạn chưa thanh toán' });
      showToast(`Đã tặng 100 điểm cho ${user.email}.`);
    } catch (err) {
      showToast(err?.message || 'Không thể tặng điểm (kiểm tra API loyalty).');
    } finally {
      setGrantingUserId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-[9px] font-sans font-black uppercase tracking-[0.25em] text-neutral-300">Phân tích & Giám sát</p>
          <h2 className="mt-1 flex items-center gap-2 text-sm font-sans font-black uppercase tracking-wide text-neutral-200">
            <BarChart2 className="h-5 w-5 text-amber-500" /> Thống Kê Mua Bán
          </h2>
          <p className="mt-1 text-xs text-neutral-500">Ghế trống theo suất, khách không đến, giờ cao điểm và tỷ lệ bỏ đơn — dữ liệu để quyết định tăng/giảm giá.</p>
        </div>
        <div className="flex flex-wrap items-end gap-2">
          <label className="text-[9px] font-black uppercase tracking-widest text-neutral-500">
            Từ ngày
            <input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)}
              className="mt-1 block border border-white/10 bg-black px-3 py-2 text-xs text-white outline-none focus:border-amber-500/60" />
          </label>
          <label className="text-[9px] font-black uppercase tracking-widest text-neutral-500">
            Đến ngày
            <input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)}
              className="mt-1 block border border-white/10 bg-black px-3 py-2 text-xs text-white outline-none focus:border-amber-500/60" />
          </label>
          <button
            onClick={loadAll}
            disabled={isLoading}
            className="flex items-center gap-2 border border-white/10 bg-black px-4 py-2.5 text-[10px] font-sans font-black uppercase tracking-widest text-neutral-300 transition hover:border-amber-500/50 hover:text-white disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} /> Cập nhật
          </button>
        </div>
      </div>

      {/* Stat tiles */}
      <div className="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <div className="border border-white/[0.12] bg-white/[0.04] p-4">
          <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Khách mua mà không đến</p>
          <p className="mt-1 text-xl font-black font-mono text-rose-400">{noShow ? `${noShow.noShowBookings}` : '—'}</p>
          <p className="text-[10px] text-neutral-500">
            {noShow ? `${noShow.noShowRatePercent.toFixed(1)}% trên ${noShow.finishedPaidBookings} đơn đã chiếu xong` : 'Chưa có dữ liệu'}
          </p>
        </div>
        <div className="border border-white/[0.12] bg-white/[0.04] p-4">
          <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Đặt mà không thanh toán</p>
          <p className="mt-1 text-xl font-black font-mono text-amber-400">{abandoned ? abandoned.expiredBookings : '—'}</p>
          <p className="text-[10px] text-neutral-500">
            {abandoned ? `${abandoned.abandonedRatePercent.toFixed(1)}% so với ${abandoned.paidBookings} đơn đã trả tiền` : 'Chưa có dữ liệu'}
          </p>
        </div>
        <div className="border border-white/[0.12] bg-white/[0.04] p-4">
          <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Bắp nước bán ra</p>
          <p className="mt-1 text-xl font-black font-mono text-emerald-400">{concessions ? concessions.totalItemsSold : '—'}</p>
          <p className="text-[10px] text-neutral-500">{concessions ? `Doanh thu ${formatVnd(concessions.totalRevenue)}` : 'Chưa có dữ liệu'}</p>
        </div>
        <div className="border border-white/[0.12] bg-white/[0.04] p-4">
          <p className="text-[9px] font-black uppercase tracking-widest text-neutral-500">Giờ cao điểm</p>
          <p className="mt-1 text-xl font-black font-mono text-sky-400">
            {peakHours.length > 0 ? `${peakHours.reduce((best, p) => (p.ticketsSold > best.ticketsSold ? p : best), peakHours[0]).hour}h` : '—'}
          </p>
          <p className="text-[10px] text-neutral-500">Khung giờ bán nhiều vé nhất — cân nhắc tăng giá</p>
        </div>
      </div>

      {/* Histogram vé theo giờ */}
      <div className="border border-white/[0.12] bg-white/[0.04] p-5">
        <p className="mb-4 text-[10px] font-black uppercase tracking-widest text-neutral-400">Vé bán theo giờ chiếu trong ngày (giờ cao điểm)</p>
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={peakChartData} margin={{ top: 5, right: 10, bottom: 5, left: -20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
              <XAxis dataKey="hour" tick={{ fill: '#737373', fontSize: 10 }} />
              <YAxis tick={{ fill: '#737373', fontSize: 10 }} allowDecimals={false} />
              <Tooltip contentStyle={{ background: '#0a0a0a', border: '1px solid rgba(255,255,255,0.1)', fontSize: 11 }} />
              <Bar dataKey="Vé bán" fill="#f59e0b" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Heatmap ghế được mua nhiều nhất */}
      <div className="border border-white/[0.12] bg-white/[0.04] p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <p className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-neutral-400">
            <Armchair className="h-3.5 w-3.5 text-amber-500" /> Ghế được mua nhiều nhất
          </p>
          <select
            value={seatRoomId}
            onChange={(e) => handleSeatRoomChange(e.target.value)}
            className="border border-white/10 bg-black px-3 py-2 text-xs text-white outline-none focus:border-amber-500/60"
          >
            <option value="">Tất cả phòng (gộp theo vị trí)</option>
            {rooms.map((room) => (
              <option key={room.id} value={room.id}>{room.name}</option>
            ))}
          </select>
        </div>
        {seatHeat.top.length === 0 ? (
          <p className="py-8 text-center text-xs text-neutral-600">Chưa có vé nào được bán trong khoảng thời gian này.</p>
        ) : (
          <div className="grid gap-6 lg:grid-cols-[1fr_260px]">
            {/* Sơ đồ rạp: màn hình chiếu phát sáng + luồng đèn + ghế tô nhiệt theo lượt mua */}
            <div className="min-w-0 overflow-x-auto rounded border border-white/10 bg-[#040a0c] p-5">
              {/* Màn hình chiếu */}
              <div className="relative mx-auto max-w-xl">
                <div
                  className="mx-auto h-2 w-[85%] rounded-[100%] bg-cyan-300"
                  style={{ boxShadow: '0 0 24px 6px rgba(103,232,249,0.55), 0 0 70px 18px rgba(34,211,238,0.25)' }}
                />
                <p className="mt-3 text-center text-[10px] font-black uppercase tracking-[0.45em] text-cyan-100/90">
                  Màn hình chiếu
                </p>
                {/* Luồng đèn chiếu xuống */}
                <div
                  className="pointer-events-none mx-auto -mt-1 h-16 w-[55%]"
                  style={{
                    clipPath: 'polygon(28% 0, 72% 0, 100% 100%, 0 100%)',
                    background: 'linear-gradient(to bottom, rgba(103,232,249,0.14), rgba(103,232,249,0))'
                  }}
                />
              </div>

              {/* Lưới ghế */}
              <div className="mx-auto max-w-xl space-y-2">
                {seatHeat.rowLabels.map((rowLabel) => (
                  <div key={rowLabel} className="flex items-center gap-3">
                    <span className="w-4 shrink-0 text-center text-[11px] font-black text-neutral-400">{rowLabel}</span>
                    <div className="grid flex-1 gap-2" style={{ gridTemplateColumns: `repeat(${seatHeat.maxNumber}, minmax(0, 1fr))` }}>
                      {Array.from({ length: seatHeat.maxNumber }, (_, i) => {
                        const seat = seatHeat.grid.get(`${rowLabel}${i + 1}`);
                        const heat = seat ? seat.count / seatHeat.maxCount : 0;
                        const borderClass = seat?.seatType === 'VIP'
                          ? 'border-purple-400/70'
                          : seat?.seatType === 'COUPLE'
                            ? 'border-rose-400/60'
                            : 'border-cyan-500/35';
                        return (
                          <div
                            key={i}
                            title={seat ? `${seat.label} · ${seat.count} lần mua · ${seat.seatType}` : `${rowLabel}${i + 1} · chưa được mua`}
                            className={`flex aspect-square min-w-7 items-center justify-center rounded-sm border font-mono text-[10px] font-bold transition ${borderClass} ${
                              heat > 0.55 ? 'text-black' : seat ? 'text-amber-200' : 'text-cyan-300/60'
                            }`}
                            style={{
                              backgroundColor: seat ? `rgba(245, 158, 11, ${0.14 + 0.8 * heat})` : 'rgba(8, 27, 32, 0.6)',
                              boxShadow: heat > 0.75 ? '0 0 10px rgba(245,158,11,0.55)' : undefined
                            }}
                          >
                            {i + 1}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>

              {/* Legend + cửa ra vào */}
              <div className="mx-auto mt-6 flex max-w-xl items-end justify-between gap-3">
                <div className="flex items-center gap-2 text-[9px] uppercase tracking-widest text-neutral-500">
                  Ít
                  {[0.18, 0.38, 0.58, 0.78, 0.94].map((a) => (
                    <span key={a} className="h-3 w-5 rounded-sm" style={{ backgroundColor: `rgba(245, 158, 11, ${a})` }} />
                  ))}
                  Nhiều
                </div>
                <div className="flex items-center gap-2 rounded border border-emerald-500/50 bg-emerald-950/20 px-4 py-2.5 text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300">
                  <DoorOpen className="h-4 w-4" /> Cửa ra vào
                </div>
              </div>
            </div>

            {/* Top 10 ghế */}
            <div>
              <p className="mb-2 text-[9px] font-black uppercase tracking-widest text-neutral-500">
                Top ghế · {seatHeat.totalTickets} vé trong kỳ
              </p>
              <div className="space-y-1.5">
                {seatHeat.top.map((seat, index) => (
                  <div key={seat.label} className="flex items-center gap-2 text-xs">
                    <span className="w-4 shrink-0 font-mono text-[10px] text-neutral-600">{index + 1}</span>
                    <span className="w-8 shrink-0 font-black text-white">{seat.label}</span>
                    <span className={`shrink-0 border px-1 py-0.5 text-[8px] font-black uppercase ${
                      seat.seatType === 'VIP'
                        ? 'border-amber-500/50 text-amber-400'
                        : seat.seatType === 'COUPLE'
                          ? 'border-rose-500/40 text-rose-400'
                          : 'border-white/15 text-neutral-500'
                    }`}>
                      {seat.seatType}
                    </span>
                    <div className="h-1.5 min-w-0 flex-1 bg-white/5">
                      <div className="h-full bg-amber-500/80" style={{ width: `${(seat.count / seatHeat.maxCount) * 100}%` }} />
                    </div>
                    <span className="shrink-0 font-mono text-amber-400">{seat.count}</span>
                  </div>
                ))}
              </div>
              <p className="mt-3 text-[10px] leading-relaxed text-neutral-600">
                Ghế mua nhiều → vị trí khách ưa chuộng, cân nhắc nâng hạng VIP hoặc điều chỉnh giá theo vị trí.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Độ lấp đầy từng suất chiếu sắp tới — progress row theo ngày */}
      <div className="border border-white/[0.12] bg-white/[0.04] p-5">
        <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
          <p className="text-[10px] font-black uppercase tracking-widest text-neutral-400">Độ lấp đầy suất chiếu (7 ngày tới)</p>
          <p className="text-[10px] text-neutral-600">
            {activeFillDay ? `${activeFillDay.items.length} suất trong ngày · ` : ''}tổng {showtimeFill.length} suất/7 ngày
          </p>
        </div>

        {/* Phân trang theo ngày — mỗi lần chỉ xem 1 ngày */}
        {fillByDay.length > 0 && (
          <div className="mb-4 flex flex-wrap gap-2">
            {fillByDay.map((group) => (
              <button
                key={group.dayKey}
                onClick={() => setFillDayKey(group.dayKey)}
                className={`border px-3 py-1.5 text-[9px] font-black uppercase tracking-widest transition ${
                  activeFillDay?.dayKey === group.dayKey
                    ? 'border-amber-500/60 bg-amber-500/15 text-amber-300'
                    : 'border-white/10 bg-black text-neutral-400 hover:border-white/30 hover:text-white'
                }`}
              >
                {group.chipLabel} <span className="ml-1 font-mono normal-case text-neutral-500">({group.items.length})</span>
              </button>
            ))}
          </div>
        )}

        {!activeFillDay ? (
          <p className="py-8 text-center text-xs text-neutral-600">Không có suất chiếu nào trong 7 ngày tới.</p>
        ) : (
          <div className="max-h-[560px] space-y-4 overflow-y-auto pr-1 [scrollbar-width:thin] [scrollbar-color:rgba(245,158,11,0.25)_transparent]">
            {[activeFillDay].map((group) => (
              <div key={group.dayKey}>
                <p className="mb-2 text-[9px] font-black uppercase tracking-[0.2em] text-neutral-400">{group.dayLabel}</p>
                <div className="space-y-1.5">
                  {group.items.map((st) => {
                    const capacity = Number(st.capacity) || 0;
                    const sold = Number(st.sold) || 0;
                    const pct = capacity > 0 ? Math.round((sold / capacity) * 100) : 0;
                    const time = new Date(st.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
                    return (
                      <div key={st.showtimeId} className="flex items-center gap-3">
                        <span className="w-12 shrink-0 bg-white/5 py-1 text-center font-mono text-[10px] text-neutral-300">{time}</span>
                        <div className="w-40 min-w-0 shrink-0 sm:w-52" title={`${st.movieTitle} · ${st.roomName}`}>
                          <p className="truncate text-xs text-neutral-200">{st.movieTitle}</p>
                          <p className="truncate text-[9px] uppercase tracking-wider text-neutral-600">{st.roomName}</p>
                        </div>
                        <div className="h-2 min-w-0 flex-1 bg-white/5">
                          <div
                            className="h-full bg-amber-500"
                            style={{ width: `${pct}%`, minWidth: sold > 0 ? 3 : 0 }}
                          />
                        </div>
                        {pct >= 80 && (
                          <span className="shrink-0 border border-amber-500/50 px-1.5 py-0.5 text-[8px] font-black uppercase tracking-widest text-amber-400">Gần đầy</span>
                        )}
                        <span className={`w-14 shrink-0 text-right font-mono text-[11px] ${sold > 0 ? 'text-white' : 'text-neutral-600'}`}>
                          {sold}/{capacity}
                        </span>
                        <span className={`w-10 shrink-0 text-right font-mono text-[11px] ${sold > 0 ? 'text-amber-400' : 'text-neutral-600'}`}>
                          {pct}%
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* Top bắp nước */}
        <div className="border border-white/[0.12] bg-white/[0.04] p-5">
          <p className="mb-3 text-[10px] font-black uppercase tracking-widest text-neutral-400">Bắp nước bán chạy</p>
          {!concessions || concessions.lines.length === 0 ? (
            <p className="py-6 text-center text-xs text-neutral-600">Chưa bán được món nào trong khoảng thời gian này.</p>
          ) : (
            <div className="space-y-2">
              {concessions.lines.slice(0, 8).map((line) => (
                <div key={line.name} className="flex items-center justify-between gap-3 text-xs">
                  <span className="min-w-0 truncate text-neutral-300">{line.name}</span>
                  <span className="shrink-0 font-mono text-amber-400">×{line.quantity}</span>
                  <span className="shrink-0 font-mono text-white">{formatVnd(line.revenue)}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* User đặt mà không thanh toán → tặng điểm */}
        <div className="border border-white/[0.12] bg-white/[0.04] p-5">
          <p className="mb-3 flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-neutral-400">
            <Gift className="h-3.5 w-3.5 text-amber-500" /> Khách đặt vé nhưng hết tiền / không thanh toán
          </p>
          {expiredUsers.length === 0 ? (
            <p className="py-6 text-center text-xs text-neutral-600">Không có booking hết hạn nào trong khoảng thời gian này.</p>
          ) : (
            <div className="space-y-2">
              {expiredUsers.slice(0, 8).map((user) => (
                <div key={user.userId} className="flex items-center justify-between gap-3 text-xs">
                  <div className="min-w-0">
                    <p className="truncate text-neutral-300">{user.email}</p>
                    <p className="text-[10px] text-neutral-600">{user.expiredBookings} đơn bỏ ngang · {formatVnd(user.lostAmount)}</p>
                  </div>
                  <button
                    onClick={() => handleGrantPoints(user)}
                    disabled={grantingUserId === user.userId}
                    className="shrink-0 border border-amber-500/40 bg-amber-500/10 px-2.5 py-1.5 text-[9px] font-black uppercase tracking-widest text-amber-300 transition hover:bg-amber-500 hover:text-black disabled:opacity-50"
                  >
                    {grantingUserId === user.userId ? 'Đang tặng…' : 'Tặng 100 điểm'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
