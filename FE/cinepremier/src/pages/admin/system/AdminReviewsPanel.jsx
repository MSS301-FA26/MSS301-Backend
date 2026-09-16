import React, { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import { MessageSquare, RefreshCw, Star, EyeOff, Ban } from 'lucide-react';
import { adminService } from '../../../services/adminService';

const formatDateTime = (value) => {
  if (!value) return 'Chưa có dữ liệu';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('vi-VN');
};

const statusMeta = {
  VISIBLE: 'border-emerald-500/30 bg-emerald-950/20 text-emerald-300',
  HIDDEN: 'border-amber-500/30 bg-amber-950/20 text-amber-300',
  DELETED: 'border-rose-500/30 bg-rose-950/20 text-rose-300'
};

const statusLabel = {
  VISIBLE: 'Đang hiển thị',
  HIDDEN: 'Đã ẩn',
  DELETED: 'Đã ẩn'
};

export default function AdminReviewsPanel({ ctx }) {
  const { activeTab, getAdminToken, showToast, moviesList = [] } = ctx;
  const [reviews, setReviews] = useState({ items: [], page: 0, totalPages: 1, totalItems: 0 });
  const [movieId, setMovieId] = useState('');
  const [status, setStatus] = useState('VISIBLE');
  const [loading, setLoading] = useState(false);
  const [subTab, setSubTab] = useState('all'); // 'all' | 'hidden_report'

  const movieOptions = useMemo(() => (
    [...moviesList].sort((a, b) => String(a.title || '').localeCompare(String(b.title || ''), 'vi'))
  ), [moviesList]);

  const loadReviews = async (page = 0) => {
    if (activeTab !== 'reviews') return;
    const token = getAdminToken?.();
    if (!token) return;
    setLoading(true);
    try {
      const data = await adminService.getAdminReviews(token, {
        movieId: movieId || undefined,
        status: status || undefined,
        page,
        size: 10
      });
      setReviews({
        items: Array.isArray(data?.items) ? data.items : [],
        page: Number(data?.page || 0),
        totalPages: Math.max(1, Number(data?.totalPages || 1)),
        totalItems: Number(data?.totalItems || 0)
      });
    } catch (error) {
      showToast?.(error.message || 'Không thể tải danh sách đánh giá.', 4500, null, 'sad');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReviews(0);
  }, [activeTab, movieId, status]);

  if (activeTab !== 'reviews') return null;

  const requestDisableReview = (review) => {
    showToast?.(
      `Ẩn đánh giá #${review.id} của ${review.userFullName || review.userEmail || 'khách hàng'}?`,
      9000,
      {
        label: 'Ẩn',
        onClick: async () => {
          const token = getAdminToken?.();
          if (!token) return;
          try {
            await adminService.deleteAdminReview(token, review.id);
            showToast?.('Đã ẩn đánh giá.');
            await loadReviews(reviews.page);
          } catch (error) {
            showToast?.(error.message || 'Không thể ẩn đánh giá.', 4500, null, 'sad');
          }
        }
      },
      'sad'
    );
  };

  return (
    <motion.div
      key="panel-reviews"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.2 }}
      className="space-y-5"
    >
      {/* Sub-tab navigation */}
      <div className="flex border-b border-white/10 gap-2">
        <button
          type="button"
          onClick={() => {
            setSubTab('all');
            setStatus('VISIBLE');
          }}
          className={`px-4 py-2.5 text-xs font-black uppercase tracking-wider transition border-b-2 ${
            subTab === 'all'
              ? 'border-amber-400 text-amber-300 bg-amber-500/10'
              : 'border-transparent text-neutral-400 hover:text-white'
          }`}
        >
          Tất cả đánh giá {subTab === 'all' && `(${reviews.totalItems})`}
        </button>
        <button
          type="button"
          onClick={() => {
            setSubTab('hidden_report');
            setStatus('DELETED');
          }}
          className={`px-4 py-2.5 text-xs font-black uppercase tracking-wider transition border-b-2 flex items-center gap-1.5 ${
            subTab === 'hidden_report'
              ? 'border-rose-500 text-rose-300 bg-rose-950/20'
              : 'border-transparent text-neutral-400 hover:text-white'
          }`}
        >
          <EyeOff className="h-3.5 w-3.5" />
          Báo cáo bình luận bị ẩn {subTab === 'hidden_report' && `(${reviews.totalItems})`}
        </button>
      </div>

      <div className="flex flex-col gap-3 border border-white/[0.05] bg-black p-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-white">
            <MessageSquare className="h-4 w-4 text-amber-400" />
            {subTab === 'hidden_report' ? 'Báo cáo bình luận bị ẩn' : 'Quản lý đánh giá'}
          </h3>
          <p className="mt-1 text-[10px] text-neutral-300">
            {subTab === 'hidden_report'
              ? 'Danh sách chi tiết các đánh giá đã bị Admin ẩn do vi phạm quy định.'
              : 'Xem đánh giá người dùng theo phim, suất chiếu, rạp và phòng chiếu.'}
          </p>
        </div>
        <button
          type="button"
          onClick={() => loadReviews(reviews.page)}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 border border-amber-500/35 bg-amber-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-wider text-amber-300 transition hover:bg-amber-500 hover:text-black disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </div>

      {subTab === 'hidden_report' && (
        <div className="border border-rose-500/30 bg-rose-950/20 p-4 space-y-2">
          <div className="flex items-center gap-2 text-rose-300 font-bold uppercase tracking-wider text-xs">
            <EyeOff className="h-4 w-4 text-rose-400 shrink-0" />
            <span>BÁO CÁO THỐNG KÊ BÌNH LUẬN BỊ ẨN BỞI QUẢN TRỊ VIÊN</span>
          </div>
          <p className="text-[11px] text-neutral-300 leading-relaxed font-sans">
            Tất cả bình luận dưới đây đã bị ẩn hoàn toàn khỏi giao diện xem phim công khai.
          </p>
          <div className="flex flex-wrap gap-4 text-[10px] text-neutral-400 font-mono pt-1">
            <span>• Tổng số bình luận bị ẩn: <strong className="text-white">{reviews.totalItems}</strong></span>
            <span>• Hiển thị phía khách hàng khác: <strong className="text-rose-400">Ẩn hoàn toàn (Không thấy)</strong></span>
            <span>• Hiển thị phía tác giả: <strong className="text-amber-300">Nhãn 'Bình luận đã bị ẩn bởi Quản trị viên'</strong></span>
          </div>
        </div>
      )}

      <div className="grid gap-3 border border-white/[0.05] bg-neutral-950 p-4 md:grid-cols-[minmax(0,1fr)_180px]">
        <label className="space-y-1.5">
          <span className="block text-[10px] font-black uppercase tracking-[0.14em] text-neutral-300">Phim</span>
          <select
            value={movieId}
            onChange={(event) => setMovieId(event.target.value)}
            className="w-full border border-white/[0.06] bg-black px-3 py-2.5 text-xs text-white outline-none transition focus:border-amber-500/70"
          >
            <option value="">Tất cả phim</option>
            {movieOptions.map((movie) => (
              <option key={movie.backendId || movie.id} value={movie.backendId || movie.id}>{movie.title}</option>
            ))}
          </select>
        </label>
        <label className="space-y-1.5">
          <span className="block text-[10px] font-black uppercase tracking-[0.14em] text-neutral-300">Trạng thái</span>
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
            className="w-full border border-white/[0.06] bg-black px-3 py-2.5 text-xs text-white outline-none transition focus:border-amber-500/70"
          >
            <option value="">Tất cả</option>
            <option value="VISIBLE">Đang hiển thị</option>
            <option value="DELETED">Đã ẩn</option>
          </select>
        </label>
      </div>

      <div className="overflow-hidden border border-white/[0.05] bg-neutral-950">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1040px] border-collapse text-left">
            <thead>
              <tr className="border-b border-white/[0.05] bg-black text-[9.5px] font-black uppercase tracking-wider text-neutral-300">
                <th className="px-4 py-3">Khách hàng</th>
                <th className="px-4 py-3">Phim / Booking</th>
                <th className="px-4 py-3">Suất chiếu</th>
                <th className="px-4 py-3">Rạp / Phòng</th>
                <th className="px-4 py-3">Đánh giá</th>
                <th className="px-4 py-3">Thời gian</th>
                <th className="px-4 py-3 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.03] text-xs">
              {reviews.items.map((review) => (
                <tr key={review.id} className="hover:bg-white/[0.02]">
                  <td className="px-4 py-3">
                    <div className="font-bold text-white">{review.userFullName || 'Khách hàng'}</div>
                    <div className="mt-1 break-all text-[10px] text-neutral-300">{review.userEmail}</div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="font-bold text-amber-200">{review.movieTitle || 'Chưa rõ phim'}</div>
                    <div className="mt-1 whitespace-nowrap font-mono text-[10px] text-neutral-300">{review.bookingCode || (review.bookingId ? `#${review.bookingId}` : 'Không gắn booking')}</div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="font-mono text-[11px] text-neutral-300">{formatDateTime(review.showtimeStart)}</div>
                    <div className="mt-1 font-mono text-[10px] text-neutral-300">{review.showtimeId ? `#${review.showtimeId}` : 'Chưa rõ suất'}</div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="text-neutral-300">{review.cinemaName || 'Chưa rõ rạp'}</div>
                    <div className="mt-1 text-[10px] text-neutral-300">{review.roomName || 'Chưa rõ phòng'}</div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="mb-1 flex items-center gap-1 text-amber-300">
                      {Array.from({ length: 5 }).map((_, index) => (
                        <Star key={index} className={`h-3.5 w-3.5 ${index < Number(review.rating || 0) ? 'fill-amber-300' : 'text-neutral-300'}`} />
                      ))}
                      <span className="ml-1 font-mono text-[10px] text-neutral-300">{review.rating}/5</span>
                    </div>
                    <p className="max-w-md whitespace-pre-wrap break-words text-neutral-300">{review.comment || 'Không có nội dung'}</p>
                    <span className={`mt-2 inline-flex border px-2 py-1 text-[9px] font-black uppercase tracking-wider ${statusMeta[review.status] || 'border-white/[0.08] text-neutral-200'}`}>
                      {statusLabel[review.status] || review.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono text-[11px] text-neutral-200">{formatDateTime(review.createdAt)}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      type="button"
                      onClick={() => requestDisableReview(review)}
                      disabled={review.status === 'DELETED' || review.status === 'HIDDEN'}
                      className="inline-flex items-center justify-center gap-1.5 border border-rose-500/35 bg-rose-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-wider text-rose-300 transition hover:bg-rose-500 hover:text-black disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      <EyeOff className="h-3.5 w-3.5" />
                      Ẩn
                    </button>
                  </td>
                </tr>
              ))}
              {!reviews.items.length && (
                <tr>
                  <td colSpan="7" className="px-4 py-10 text-center text-xs text-neutral-300">Chưa có đánh giá phù hợp.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="flex items-center justify-between border-t border-white/[0.05] px-4 py-3 text-[10px] text-neutral-300">
          <span>{Number(reviews.totalItems || 0).toLocaleString('vi-VN')} đánh giá</span>
          <div className="flex items-center gap-2">
            <button type="button" disabled={reviews.page <= 0 || loading} onClick={() => loadReviews(reviews.page - 1)} className="border border-white/[0.06] px-3 py-1.5 font-bold uppercase text-neutral-300 disabled:opacity-40">Trước</button>
            <span className="font-mono">{reviews.page + 1}/{reviews.totalPages}</span>
            <button type="button" disabled={reviews.page + 1 >= reviews.totalPages || loading} onClick={() => loadReviews(reviews.page + 1)} className="border border-white/[0.06] px-3 py-1.5 font-bold uppercase text-neutral-300 disabled:opacity-40">Sau</button>
          </div>
        </div>
      </div>
    </motion.div>
  );
}


