import React, { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, RefreshCw, ShieldAlert } from 'lucide-react';
import { adminService } from '../../../services/adminService';

const ACTION_META = {
  CREATE: { label: 'Tạo mới', className: 'border-emerald-500/30 bg-emerald-950/30 text-emerald-300' },
  UPDATE: { label: 'Cập nhật', className: 'border-sky-500/30 bg-sky-950/30 text-sky-300' },
  DELETE: { label: 'Xóa/Hủy', className: 'border-rose-500/30 bg-rose-950/30 text-rose-300' },
  CHECK_IN: { label: 'Check-in', className: 'border-amber-500/30 bg-amber-950/30 text-amber-300' },
  REFUND: { label: 'Hoàn tiền', className: 'border-purple-500/30 bg-purple-950/30 text-purple-300' },
  APPROVE: { label: 'Duyệt', className: 'border-emerald-500/30 bg-emerald-950/30 text-emerald-300' },
  REJECT: { label: 'Từ chối', className: 'border-rose-500/30 bg-rose-950/30 text-rose-300' },
};

// Tab lọc theo loại đối tượng — value là TIỀN TỐ targetType gửi lên BE
const TARGET_TABS = [
  { value: '', label: 'Tất cả' },
  { value: 'MOVIE', label: 'Phim' },
  { value: 'SHOWTIME', label: 'Suất chiếu' },
  { value: 'FOOD', label: 'Bắp nước' },
  { value: 'BOOKING', label: 'Vé / Booking' },
  { value: 'GENRE', label: 'Thể loại' },
  { value: 'ACTOR', label: 'Diễn viên' },
  { value: 'ROOM', label: 'Phòng chiếu' },
  { value: 'CINEMA', label: 'Rạp' },
  { value: 'TICKET_PRICING', label: 'Giá vé' },
  { value: 'USER', label: 'Người dùng' },
  { value: 'STAFF_PROFILE', label: 'Nhân viên' },
  { value: 'LOYALTY', label: 'Điểm thưởng' },
  { value: 'REVIEW', label: 'Đánh giá' },
];

const getActionMeta = (action) => ACTION_META[action] || { label: action, className: 'border-white/10 bg-neutral-900 text-neutral-300' };

/**
 * Panel hiển thị audit log toàn hệ thống — ai làm gì, với đối tượng nào, lúc mấy giờ.
 * Hỗ trợ lọc theo loại đối tượng (targetType prefix) và phân trang 20 bản ghi/trang.
 *
 * @param {{ getAdminToken: () => string|null, showToast: (msg: string) => void }} ctx
 */
export default function AdminAuditPanel({ ctx }) {
  const { getAdminToken, showToast } = ctx;
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [targetFilter, setTargetFilter] = useState('');

  const loadLogs = async (nextPage = 0, target = targetFilter) => {
    const token = getAdminToken();
    if (!token) return;
    setIsLoading(true);
    try {
      const data = await adminService.getAuditLogs(token, { page: nextPage, size: 20, targetType: target || undefined });
      setLogs(data.items || []);
      setPage(data.page || 0);
      setTotalPages(data.totalPages || 1);
      setTotalItems(data.totalItems || 0);
    } catch (err) {
      showToast(err?.message || 'Không thể tải audit log.');
      setLogs([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadLogs(0); }, []);

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[9px] font-sans font-black uppercase tracking-[0.25em] text-neutral-300">Giám sát hệ thống</p>
          <h2 className="mt-1 text-sm font-sans font-black uppercase tracking-wide text-neutral-200 flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-amber-500" /> Audit Log
          </h2>
          <p className="mt-1 text-xs text-neutral-300">Ai đã làm gì trên hệ thống — ghi thật từ backend cho mọi thao tác quản trị.</p>
        </div>
        <button
          onClick={() => loadLogs(page)}
          disabled={isLoading}
          className="flex items-center gap-2 border border-white/10 bg-black px-4 py-2.5 text-[10px] font-sans font-black uppercase tracking-widest text-neutral-300 transition hover:border-amber-500/50 hover:text-white disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} /> Làm mới
        </button>
      </div>

      {/* Tab lọc theo loại đối tượng */}
      <div className="flex flex-wrap gap-2">
        {TARGET_TABS.map((tab) => (
          <button
            key={tab.value || 'all'}
            onClick={() => { setTargetFilter(tab.value); loadLogs(0, tab.value); }}
            className={`border px-3 py-1.5 text-[9px] font-sans font-black uppercase tracking-widest transition ${
              targetFilter === tab.value
                ? 'border-amber-500/60 bg-amber-500/15 text-amber-300'
                : 'border-white/10 bg-black text-neutral-200 hover:border-white/30 hover:text-white'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="overflow-x-auto border border-white/10 bg-[#050505]">
        <table className="min-w-full divide-y divide-white/10 text-left text-xs font-sans">
          <thead className="bg-[#0B0B0B] text-[9px] uppercase tracking-[0.15em] text-neutral-300 font-bold">
            <tr>
              <th className="px-5 py-3.5">Thời gian</th>
              <th className="px-5 py-3.5">Người thực hiện</th>
              <th className="px-5 py-3.5">Hành động</th>
              <th className="px-5 py-3.5">Đối tượng</th>
              <th className="px-5 py-3.5">Chi tiết</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5 text-neutral-300">
            {isLoading ? (
              <tr><td colSpan={5} className="px-5 py-12 text-center text-neutral-300">Đang tải audit log…</td></tr>
            ) : logs.length === 0 ? (
              <tr><td colSpan={5} className="px-5 py-12 text-center text-neutral-300">Chưa có hoạt động nào được ghi nhận.</td></tr>
            ) : logs.map((log) => {
              const meta = getActionMeta(log.action);
              return (
                <tr key={log.id} className="transition hover:bg-white/5">
                  <td className="whitespace-nowrap px-5 py-3.5 font-mono text-[11px] text-neutral-200">
                    {log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : '—'}
                  </td>
                  <td className="px-5 py-3.5">
                    <p className="font-bold text-white">{log.actorName || '—'}</p>
                    <p className="text-[10px] text-neutral-300">{log.actorEmail || 'hệ thống'}</p>
                  </td>
                  <td className="px-5 py-3.5">
                    <span className={`inline-block border px-2 py-1 text-[9px] font-black uppercase tracking-wider ${meta.className}`}>
                      {meta.label}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-5 py-3.5 font-mono text-[11px]">
                    {log.targetType}{log.targetId ? ` #${log.targetId}` : ''}
                  </td>
                  <td className="max-w-md truncate px-5 py-3.5 text-neutral-200" title={log.detail}>{log.detail || '—'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-[10px] font-sans font-black uppercase tracking-widest text-neutral-300">
        <span>{totalItems} bản ghi · Trang {page + 1}/{totalPages}</span>
        <div className="flex gap-2">
          <button
            disabled={page <= 0 || isLoading}
            onClick={() => loadLogs(page - 1)}
            className="flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-amber-500/50 disabled:opacity-30"
          >
            <ChevronLeft className="h-3.5 w-3.5" /> Trước
          </button>
          <button
            disabled={page >= totalPages - 1 || isLoading}
            onClick={() => loadLogs(page + 1)}
            className="flex items-center gap-1 border border-white/10 px-3 py-2 text-white transition hover:border-amber-500/50 disabled:opacity-30"
          >
            Sau <ChevronRight className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}
