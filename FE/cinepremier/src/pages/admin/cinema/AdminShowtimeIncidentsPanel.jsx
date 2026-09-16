import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  AlertTriangle, RefreshCw, Search, Calendar, User, Ticket,
  DollarSign, FileText, CheckCircle2, ChevronDown, ChevronUp,
  Printer, ShieldAlert, Clock
} from 'lucide-react';
import { getStoredAuth } from '../../../services/authService';
import { adminService } from '../../../services/adminService';

const fmtVND = (v) => `${Number(v || 0).toLocaleString('vi-VN')}đ`;

const fmtDateTime = (d) => {
  if (!d) return '—';
  const date = new Date(d);
  if (Number.isNaN(date.getTime())) return String(d);
  return date.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

export default function AdminShowtimeIncidentsPanel({ ctx }) {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [expandedShowtimeId, setExpandedShowtimeId] = useState(null);

  const getAdminToken = () => {
    const { accessToken } = getStoredAuth();
    return accessToken;
  };

  const fetchReport = async () => {
    const token = getAdminToken();
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      const params = {};
      if (fromDate) params.from = fromDate;
      if (toDate) params.to = toDate;
      const res = await adminService.getShowtimeIncidentsReport(token, params);
      const data = res?.data || res || {};
      setReport(data);
      if (data?.incidents?.length > 0 && expandedShowtimeId === null) {
        setExpandedShowtimeId(data.incidents[0].showtimeId);
      }
    } catch (e) {
      setError(e.message || 'Không thể tải báo cáo sự cố hủy suất chiếu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchReport(); }, [fromDate, toDate]);

  const filteredIncidents = useMemo(() => {
    if (!report?.incidents) return [];
    if (!searchQuery.trim()) return report.incidents;
    const q = searchQuery.toLowerCase().trim();
    return report.incidents.filter((incident) => {
      const matchMovie = (incident.movieTitle || '').toLowerCase().includes(q);
      const matchReason = (incident.cancellationReason || '').toLowerCase().includes(q);
      const matchRoom = (incident.roomName || '').toLowerCase().includes(q);
      const matchUsers = incident.refundedUsers?.some((u) =>
        (u.userName || '').toLowerCase().includes(q) ||
        (u.userEmail || '').toLowerCase().includes(q) ||
        (u.bookingCode || '').toLowerCase().includes(q) ||
        (u.userPhone || '').toLowerCase().includes(q)
      );
      return matchMovie || matchReason || matchRoom || matchUsers;
    });
  }, [report, searchQuery]);

  const toggleExpand = (id) => setExpandedShowtimeId(expandedShowtimeId === id ? null : id);

  return (
    <div className="space-y-5">

      {/* HEADER */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[9px] font-sans font-black uppercase tracking-[0.25em] text-neutral-300">Báo cáo sự cố & hoàn tiền</p>
          <h2 className="mt-1 text-sm font-sans font-black uppercase tracking-wide text-neutral-200 flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-amber-500" /> Thống kê suất chiếu hủy &amp; tài khoản hoàn tiền
          </h2>
          <p className="mt-1 text-xs text-neutral-300">Theo dõi các suất chiếu tạm dừng do sự cố và lịch sử hoàn tiền về CineWallet.</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={fetchReport}
            disabled={loading}
            className="flex items-center gap-2 border border-white/10 bg-black px-4 py-2.5 text-[10px] font-sans font-black uppercase tracking-widest text-neutral-300 transition hover:border-amber-500/50 hover:text-white disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} /> Làm mới
          </button>
          <button
            onClick={() => window.print()}
            className="flex items-center gap-2 border border-amber-500/50 bg-amber-500/10 px-4 py-2.5 text-[10px] font-sans font-black uppercase tracking-widest text-amber-300 transition hover:bg-amber-500 hover:text-black"
          >
            <Printer className="h-3.5 w-3.5" /> Xuất báo cáo
          </button>
        </div>
      </div>

      {/* KPI METRICS */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {[
          { label: 'Suất chiếu hủy', value: report?.totalCancelledShowtimes ?? 0, unit: 'suất', icon: AlertTriangle },
          { label: 'Đơn/vé đã hoàn', value: report?.totalRefundedBookings ?? 0, unit: 'đơn', icon: Ticket },
          { label: 'Tổng tiền hoàn trả', value: fmtVND(report?.totalRefundedAmount), unit: '', icon: DollarSign },
          { label: 'Tài khoản ảnh hưởng', value: report?.totalRefundedUsers ?? 0, unit: 'khách', icon: User },
        ].map(({ label, value, unit, icon: Icon }) => (
          <div key={label} className="bg-gradient-to-b from-[#0d0d0d] to-[#050505] border border-white/[0.04] p-4 space-y-3">
            <div className="flex justify-between items-start">
              <span className="text-[8.5px] tracking-[0.15em] font-extrabold text-neutral-300 uppercase font-mono">{label}</span>
              <Icon className="h-4 w-4 shrink-0 text-amber-500" />
            </div>
            <div>
              <h2 className="text-xl font-mono font-black text-white leading-none">
                {value}{unit && <span className="text-[11px] font-bold text-neutral-300 ml-1">{unit}</span>}
              </h2>
            </div>
          </div>
        ))}
      </div>

      {/* FILTER CONTROLS */}
      <div className="border border-white/[0.05] bg-[#090909] p-4 flex flex-wrap items-center gap-4">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-neutral-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo tên phim, lý do, họ tên khách, email, mã vé..."
            className="w-full border border-white/[0.06] bg-black pl-9 pr-4 py-2 text-xs text-white placeholder-neutral-500 focus:border-amber-400 focus:outline-none"
          />
        </div>
        <div className="flex items-center gap-3 text-xs font-sans">
          <div className="flex items-center gap-2">
            <span className="text-neutral-300 text-[10px] font-black uppercase tracking-widest">Từ ngày:</span>
            <input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)}
              className="border border-white/[0.06] bg-black px-2.5 py-1.5 text-xs text-white focus:border-amber-400 focus:outline-none" />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-neutral-300 text-[10px] font-black uppercase tracking-widest">Đến ngày:</span>
            <input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)}
              className="border border-white/[0.06] bg-black px-2.5 py-1.5 text-xs text-white focus:border-amber-400 focus:outline-none" />
          </div>
        </div>
      </div>

      {/* ERROR */}
      {error && (
        <div className="border border-rose-500/30 bg-rose-950/20 p-4 text-xs text-rose-400 flex items-center gap-3">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* INCIDENT LIST */}
      <div className="space-y-3">
        {loading ? (
          <div className="border border-white/[0.05] bg-[#080808] p-12 text-center text-xs text-neutral-300 space-y-3">
            <RefreshCw className="h-5 w-5 animate-spin mx-auto text-amber-500" />
            <p className="uppercase tracking-widest font-mono">Đang tải dữ liệu sự cố &amp; hoàn tiền...</p>
          </div>
        ) : filteredIncidents.length === 0 ? (
          <div className="border border-dashed border-white/10 bg-[#070707] p-12 text-center space-y-3">
            <CheckCircle2 className="h-7 w-7 text-emerald-500/50 mx-auto" />
            <p className="text-xs font-sans font-black uppercase tracking-widest text-neutral-300">Không có sự cố nào được ghi nhận</p>
            <p className="text-xs text-neutral-400 max-w-md mx-auto">
              Không tìm thấy suất chiếu bị hủy trong khoảng thời gian được chọn.
            </p>
          </div>
        ) : (
          filteredIncidents.map((incident, idx) => {
            const isExpanded = expandedShowtimeId === incident.showtimeId;
            return (
              <div key={incident.showtimeId || idx} className="border border-white/[0.07] bg-[#0A0A0A] overflow-hidden transition hover:border-white/[0.14]">
                {/* SHOWTIME HEADER ROW */}
                <div
                  onClick={() => toggleExpand(incident.showtimeId)}
                  className="p-4 flex flex-wrap items-center justify-between gap-4 cursor-pointer hover:bg-white/[0.02] transition"
                >
                  <div className="space-y-1.5 min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="border border-amber-500/40 bg-amber-500/10 text-amber-400 text-[9px] font-mono font-black px-2 py-0.5 uppercase tracking-wider">
                        SUẤT #{incident.showtimeId}
                      </span>
                      <span className="border border-rose-500/40 bg-rose-950/30 text-rose-400 text-[9px] font-mono font-black px-2 py-0.5 uppercase tracking-wider flex items-center gap-1">
                        <AlertTriangle className="h-2.5 w-2.5" /> Đã hủy do sự cố
                      </span>
                      <span className="text-[10px] text-neutral-400 font-mono">
                        | {incident.cinemaName || 'Rạp'} — {incident.roomName || 'Phòng chiếu'}
                      </span>
                    </div>

                    <h2 className="text-sm font-sans font-black uppercase tracking-wide text-neutral-200">
                      {incident.movieTitle || 'Phim chưa xác định'}
                    </h2>

                    <div className="flex flex-wrap items-center gap-4 text-[11px] text-neutral-400 font-mono">
                      <span className="flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        Giờ chiếu: <strong className="text-neutral-200">{fmtDateTime(incident.showtimeStart)}</strong>
                      </span>
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        Hủy lúc: <strong className="text-neutral-200">{fmtDateTime(incident.cancelledAt)}</strong>
                      </span>
                    </div>

                    <div className="text-[10px] text-amber-300/80 font-sans border border-amber-500/15 bg-amber-950/10 px-3 py-1 inline-block">
                      Lý do: {incident.cancellationReason || 'Sự cố vận hành rạp'}
                    </div>
                  </div>

                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <div className="text-[9px] font-mono uppercase text-neutral-400 tracking-wider">Tổng hoàn trả</div>
                      <div className="text-base font-mono font-black text-emerald-400">{fmtVND(incident.totalRefundedAmount)}</div>
                      <div className="text-[10px] text-neutral-400 font-mono">
                        {incident.refundedBookingsCount} đơn ({incident.refundedUsers?.length ?? 0} vé)
                      </div>
                    </div>
                    <button className="p-2 border border-white/10 text-neutral-400 hover:text-white hover:border-white/30 transition">
                      {isExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                    </button>
                  </div>
                </div>

                {/* EXPANDED TABLE */}
                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.2 }}
                      className="border-t border-white/[0.05]"
                    >
                      <div className="px-4 py-3 flex items-center justify-between bg-[#060606]">
                        <span className="text-[9px] font-mono uppercase font-black tracking-widest text-neutral-300 flex items-center gap-2">
                          <User className="h-3 w-3" /> Danh sách khách hàng được hoàn tiền ({incident.refundedUsers?.length ?? 0})
                        </span>
                        <span className="text-[9px] text-neutral-400 font-mono">Tự động hoàn vào CineWallet</span>
                      </div>

                      {(!incident.refundedUsers || incident.refundedUsers.length === 0) ? (
                        <div className="p-6 text-center text-xs text-neutral-400 italic">
                          Chưa có đơn hàng nào bị ảnh hưởng trong suất chiếu này.
                        </div>
                      ) : (
                        <div className="overflow-x-auto">
                          <table className="min-w-full divide-y divide-white/[0.05] text-left text-xs font-sans">
                            <thead className="bg-[#0B0B0B] text-[9px] uppercase tracking-[0.15em] text-neutral-300 font-bold">
                              <tr>
                                <th className="px-4 py-3">#</th>
                                <th className="px-4 py-3">Mã vé / Booking</th>
                                <th className="px-4 py-3">Tài khoản</th>
                                <th className="px-4 py-3">Liên hệ</th>
                                <th className="px-4 py-3">Ghế</th>
                                <th className="px-4 py-3">Số tiền hoàn</th>
                                <th className="px-4 py-3">Phương thức</th>
                                <th className="px-4 py-3">Thời gian hoàn</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-white/[0.03] text-neutral-300">
                              {incident.refundedUsers.map((u, uIdx) => (
                                <tr key={u.bookingId || uIdx} className="transition hover:bg-white/[0.02]">
                                  <td className="px-4 py-3 font-mono text-neutral-400">{uIdx + 1}</td>
                                  <td className="px-4 py-3">
                                    <span className="font-mono font-black text-amber-400 border border-amber-500/30 bg-amber-950/20 px-2 py-0.5 text-[10px]">
                                      {u.bookingCode || `BK-${u.bookingId}`}
                                    </span>
                                  </td>
                                  <td className="px-4 py-3">
                                    <p className="font-black text-white">{u.userName || 'Khách hàng'}</p>
                                    <p className="text-[10px] font-mono text-neutral-400">ID: USER-{u.userId}</p>
                                  </td>
                                  <td className="px-4 py-3 font-mono text-[11px]">
                                    <div>{u.userEmail || '—'}</div>
                                    <div className="text-neutral-400">{u.userPhone || '—'}</div>
                                  </td>
                                  <td className="px-4 py-3 font-mono text-[11px] text-amber-300">{u.seatLabels || '—'}</td>
                                  <td className="px-4 py-3 font-mono font-black text-emerald-400">{fmtVND(u.amount)}</td>
                                  <td className="px-4 py-3">
                                    <span className="border border-emerald-500/30 bg-emerald-950/20 text-emerald-300 text-[9px] font-mono font-black px-2 py-0.5 uppercase tracking-wider">
                                      {u.refundMethod === 'CINEWALLET' ? 'VÍ CINEWALLET' : (u.refundMethod || 'CINEWALLET')}
                                    </span>
                                  </td>
                                  <td className="px-4 py-3 font-mono text-[11px] text-neutral-400">{fmtDateTime(u.refundedAt)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            );
          })
        )}
      </div>

    </div>
  );
}
