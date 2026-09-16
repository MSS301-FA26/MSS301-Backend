import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Plus, Trash2, Edit3, ShieldAlert, FileText, Database,
  Calendar, Users, DollarSign, Activity, AlertCircle, CheckCircle2,
  Search, Sliders, ChevronDown, Check, RefreshCw, Layers, ShoppingBag,
  BarChart2, Clock, MapPin, Film, Play, Eye, EyeOff, Sparkles, TrendingUp, Info, Globe, Tags, ImagePlus, ChevronLeft, ChevronRight
} from 'lucide-react';
import { adminService } from '../../../services/adminService';

const FOOD_PAGE_SIZE = 10;

const FOOD_STATUS_META = {
  ACTIVE: { label: 'Mở bán', className: 'text-emerald-300' },
  LOW_STOCK: { label: 'Sắp hết', className: 'text-amber-300' },
  OUT_OF_STOCK: { label: 'Hết', className: 'text-rose-300' },
  INACTIVE: { label: 'Hết', className: 'text-rose-300' },
};

const getFoodStatusMeta = (status) => FOOD_STATUS_META[status] || FOOD_STATUS_META.OUT_OF_STOCK;

const formatVnd = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`;

const toLocalDateValue = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export default function AdminFoodsPanel({ ctx }) {
  const {
    activeTab,
    setActiveTab,
    activeChartPoint,
    setActiveChartPoint,
    searchQuery,
    setSearchQuery,
    filmFilter,
    setFilmFilter,
    adminMoviePagination,
    setAdminMoviePagination,
    editingMovie,
    setEditingMovie,
    showMovieForm,
    setShowMovieForm,
    formData,
    setFormData,
    newShowtime,
    setNewShowtime,
    isAddingShowtime,
    setIsAddingShowtime,
    showtimeSuccessMessage,
    setShowtimeSuccessMessage,
    genres,
    setGenres,
    genreSearch,
    setGenreSearch,
    genreForm,
    setGenreForm,
    genreErrors,
    setGenreErrors,
    editingGenreId,
    setEditingGenreId,
    isGenreLoading,
    setIsGenreLoading,
    isGenreSaving,
    setIsGenreSaving,
    foodItems,
    setFoodItems,
    foodCombos,
    setFoodCombos,
    foodSearch,
    setFoodSearch,
    foodKind,
    setFoodKind,
    editingFood,
    setEditingFood,
    foodForm,
    setFoodForm,
    foodErrors,
    setFoodErrors,
    isFoodLoading,
    setIsFoodLoading,
    isFoodSaving,
    setIsFoodSaving,
    isFoodImageUploading,
    handleFoodImageUpload,
    visibleFoods,
    HALL_OPTIONS,
    TIME_OPTIONS,
    playPulseSound,
    auditLogs,
    setAuditLogs,
    addAuditLog,
    resetFoodForm,
    validateFoodForm,
    fetchFoods,
    handleFoodSubmit,
    handleEditFood,
    handleToggleFoodStatus,
    getAdminToken,
    changeAdminSection,
    validateGenreForm,
    fetchGenres,
    resetGenreForm,
    handleGenreSubmit,
    handleEditGenre,
    performDeleteGenre,
    handleDeleteGenre,
    totalBookingsCount,
    calculatedRevenue,
    averageFillRate,
    handleCreateMovieSubmit,
    handleDeleteMovie,
    handleAddShowtimeSubmit,
    handleRefundTicket,
    filteredMovies,
    filteredGenres,
    moviesList,
    setMoviesList,
    bookedTickets,
    setBookedTickets,
    onSelectMovie,
    showToast,
    initialSection,
    onSectionChange,
    onFoodCatalogChanged,
    isAdmin
  } = ctx;
  const [foodPage, setFoodPage] = React.useState(1);
  const [salesSummary, setSalesSummary] = React.useState(null);
  const [isSalesSummaryLoading, setIsSalesSummaryLoading] = React.useState(true);
  const [salesSummaryError, setSalesSummaryError] = React.useState('');
  const totalFoodPages = Math.max(1, Math.ceil(visibleFoods.length / FOOD_PAGE_SIZE));
  const safeFoodPage = Math.min(foodPage, totalFoodPages);
  const foodStartIndex = (safeFoodPage - 1) * FOOD_PAGE_SIZE;
  const paginatedFoods = visibleFoods.slice(foodStartIndex, foodStartIndex + FOOD_PAGE_SIZE);
  const foodDisplayStart = visibleFoods.length === 0 ? 0 : foodStartIndex + 1;
  const foodDisplayEnd = Math.min(foodStartIndex + FOOD_PAGE_SIZE, visibleFoods.length);

  React.useEffect(() => {
    setFoodPage(1);
  }, [foodSearch]);

  React.useEffect(() => {
    setFoodPage((page) => Math.min(page, totalFoodPages));
  }, [totalFoodPages]);

  const loadSalesSummary = React.useCallback(async () => {
    setIsSalesSummaryLoading(true);
    setSalesSummaryError('');

    try {
      const today = new Date();
      const from = new Date(today.getFullYear(), today.getMonth(), 1);
      const response = await adminService.getConcessionSales(getAdminToken(), {
        from: toLocalDateValue(from),
        to: toLocalDateValue(today),
      });
      setSalesSummary(response || null);
    } catch (error) {
      setSalesSummary(null);
      setSalesSummaryError(error?.message || 'Không thể tải hiệu suất bán hàng.');
    } finally {
      setIsSalesSummaryLoading(false);
    }
  }, [getAdminToken]);

  React.useEffect(() => {
    loadSalesSummary();
  }, [loadSalesSummary]);

  const topSellingItem = salesSummary?.lines?.[0] || null;
  const currentPeriodLabel = new Intl.DateTimeFormat('vi-VN', {
    month: 'long',
    year: 'numeric',
  }).format(new Date());

  return (
    <>
      {/* TAB 2C: FOODS */}
      {activeTab === 'foods' && (
        <motion.div
          key="panel-foods"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          transition={{ duration: 0.2 }}
          className="space-y-6"
        >
          <div className="border border-white/[0.05] bg-gradient-to-r from-[#090909] to-[#050505] p-5">
            <span className="text-[9px] font-mono tracking-[0.24em] text-neutral-300 uppercase font-black block">ADMIN FOOD</span>
            <h2 className="text-xs font-black uppercase tracking-[0.18em] text-neutral-200 mt-1">Quản lý bắp nước & combo</h2>
          </div>

          {/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · component: operational KPI strip · genre: technical · theme: existing CinePremier */}
          <section className="border border-white/10 bg-[#070707]" aria-labelledby="food-sales-summary-title">
            <div className="flex flex-col gap-4 border-b border-white/10 px-5 py-4 md:flex-row md:items-center md:justify-between">
              <div className="min-w-0">
                <div className="flex items-center gap-2 text-amber-400">
                  <BarChart2 className="h-4 w-4" aria-hidden="true" />
                  <h3 id="food-sales-summary-title" className="text-[11px] font-black uppercase tracking-[0.2em]">
                    Hiệu suất F&amp;B · {currentPeriodLabel}
                  </h3>
                </div>
                <p className="mt-1 text-[10px] text-neutral-500">
                  Chỉ tính món và combo thuộc đơn đã thanh toán hoặc đã giao cho khách.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={loadSalesSummary}
                  disabled={isSalesSummaryLoading}
                  className="inline-flex min-h-11 items-center gap-2 border border-white/10 px-3 text-[9px] font-black uppercase tracking-[0.16em] text-neutral-300 transition hover:border-amber-400/50 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-400 disabled:cursor-wait disabled:opacity-50"
                  aria-label="Làm mới số liệu F&B"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${isSalesSummaryLoading ? 'animate-spin' : ''}`} aria-hidden="true" />
                  Làm mới
                </button>
                <button
                  type="button"
                  onClick={() => changeAdminSection('fnb-report')}
                  className="inline-flex min-h-11 items-center gap-2 bg-amber-500 px-4 text-[9px] font-black uppercase tracking-[0.16em] text-black transition hover:bg-amber-400 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                >
                  Xem báo cáo chi tiết
                  <TrendingUp className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </div>
            </div>

            {salesSummaryError ? (
              <div className="flex min-h-24 items-center justify-between gap-4 px-5 py-4" role="alert">
                <div>
                  <p className="text-xs font-bold text-rose-300">Chưa tải được số liệu bán hàng</p>
                  <p className="mt-1 text-[10px] text-neutral-500">{salesSummaryError}</p>
                </div>
                <button
                  type="button"
                  onClick={loadSalesSummary}
                  className="min-h-11 border border-rose-400/40 px-4 text-[9px] font-black uppercase tracking-widest text-rose-200 transition hover:bg-rose-400 hover:text-black focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-rose-300"
                >
                  Thử lại
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 divide-y divide-white/10 sm:grid-cols-3 sm:divide-x sm:divide-y-0">
                <div className="min-w-0 px-5 py-4">
                  <p className="text-[9px] font-black uppercase tracking-[0.18em] text-neutral-500">Món/combo đã bán</p>
                  <p className="mt-2 font-mono text-2xl font-black text-white">
                    {isSalesSummaryLoading ? '—' : Number(salesSummary?.totalItemsSold || 0).toLocaleString('vi-VN')}
                  </p>
                  <p className="mt-1 text-[9px] text-neutral-600">Mỗi combo được tính là một đơn vị bán</p>
                </div>

                <div className="min-w-0 px-5 py-4">
                  <p className="text-[9px] font-black uppercase tracking-[0.18em] text-neutral-500">Doanh thu F&amp;B</p>
                  <p className="mt-2 truncate font-mono text-2xl font-black text-emerald-400">
                    {isSalesSummaryLoading ? '—' : formatVnd(salesSummary?.totalRevenue)}
                  </p>
                  <p className="mt-1 text-[9px] text-neutral-600">Theo thời điểm ghi nhận thanh toán</p>
                </div>

                <div className="min-w-0 px-5 py-4">
                  <p className="text-[9px] font-black uppercase tracking-[0.18em] text-neutral-500">Bán chạy nhất</p>
                  <p className="mt-2 truncate text-sm font-black uppercase tracking-wide text-amber-300" title={topSellingItem?.name || ''}>
                    {isSalesSummaryLoading ? 'Đang tải…' : topSellingItem?.name || 'Chưa có dữ liệu'}
                  </p>
                  <p className="mt-1 text-[9px] text-neutral-600">
                    {topSellingItem ? `${Number(topSellingItem.quantity).toLocaleString('vi-VN')} lượt bán · ${formatVnd(topSellingItem.revenue)}` : 'Chưa phát sinh giao dịch trong kỳ'}
                  </p>
                </div>
              </div>
            )}
          </section>

          <div className="grid grid-cols-1 2xl:grid-cols-12 gap-6">
            <form onSubmit={handleFoodSubmit} className="2xl:col-span-4 border border-white/[0.05] bg-[#070707] p-5 space-y-4">
              <div className="flex items-center justify-between border-b border-white/[0.03] pb-3">
                <div>
                  <span className="text-[8px] font-mono uppercase tracking-[0.2em] text-neutral-300 font-black">
                    {editingFood ? 'Chỉnh sửa món' : 'Tạo món mới'}
                  </span>
                  <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white mt-1">
                    {foodKind === 'combo' ? 'Combo bắp nước' : 'Món lẻ'}
                  </h3>
                </div>
                {editingFood && (
                  <button
                    type="button"
                    onClick={resetFoodForm}
                    className="border border-amber-400/50 bg-amber-400/10 px-3 py-1.5 text-[10px] uppercase tracking-widest text-amber-200 hover:bg-amber-400 hover:text-black font-black transition"
                  >
                    Hủy sửa
                  </button>
                )}
              </div>

              <div className="grid grid-cols-2 gap-2">
                {['item', 'combo'].map((kind) => (
                  <button
                    key={kind}
                    type="button"
                    onClick={() => {
                      setFoodKind(kind);
                      setEditingFood(null);
                    }}
                    className={`py-2 text-[10px] uppercase tracking-widest font-black border transition ${foodKind === kind ? 'border-amber-400 bg-amber-400 text-black' : 'border-white/[0.06] text-neutral-200 hover:text-white'}`}
                  >
                    {kind === 'combo' ? 'Combo' : 'Món lẻ'}
                  </button>
                ))}
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Tên món</label>
                <input
                  value={foodForm.name}
                  onChange={(e) => {
                    setFoodForm((prev) => ({ ...prev, name: e.target.value }));
                    if (foodErrors.name) setFoodErrors((prev) => ({ ...prev, name: undefined }));
                  }}
                  placeholder="Combo Couple, Coca Size L..."
                  className={`w-full bg-black border p-3 text-sm text-white focus:outline-none rounded-none font-bold ${foodErrors.name ? 'border-rose-500' : 'border-white/[0.06] focus:border-amber-400'}`}
                />
                {foodErrors.name && <p className="text-[10px] text-rose-400 font-bold">{foodErrors.name}</p>}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Giá bán</label>
                  <input
                    type="number"
                    min="1"
                    value={foodForm.price}
                    onChange={(e) => {
                      setFoodForm((prev) => ({ ...prev, price: e.target.value }));
                      if (foodErrors.price) setFoodErrors((prev) => ({ ...prev, price: undefined }));
                    }}
                    className={`w-full bg-black border p-3 text-sm text-white focus:outline-none rounded-none font-bold ${foodErrors.price ? 'border-rose-500' : 'border-white/[0.06] focus:border-amber-400'}`}
                  />
                  {foodErrors.price && <p className="text-[10px] text-rose-400 font-bold">{foodErrors.price}</p>}
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Trạng thái</label>
                  <select
                    value={foodForm.status}
                    onChange={(e) => setFoodForm((prev) => ({ ...prev, status: e.target.value }))}
                    className="w-full bg-black border border-white/[0.06] p-3 text-sm text-white focus:outline-none rounded-none font-bold focus:border-amber-400"
                  >
                    <option value="ACTIVE">Mở bán</option>
                    <option value="LOW_STOCK">Sắp hết</option>
                    <option value="OUT_OF_STOCK">Hết</option>
                  </select>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Hình ảnh món</label>
                {foodForm.imageUrl && (
                  <div className="space-y-1">
                    <div className="overflow-hidden border border-white/[0.05] bg-black">
                      <img
                        src={foodForm.imageUrl}
                        alt={foodForm.name || 'Ảnh món'}
                        className="h-32 w-full object-cover"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                    <p className="text-[9px] text-neutral-200 truncate" title={foodForm.imageUrl}>{foodForm.imageUrl}</p>
                  </div>
                )}
                <label
                  className={`w-full py-3 flex items-center justify-center gap-2 border text-[10px] font-black uppercase tracking-widest transition cursor-pointer ${isFoodImageUploading ? 'pointer-events-none border-white/[0.06] text-neutral-300 opacity-70' : 'border-amber-500/60 bg-amber-500/10 text-amber-300 hover:bg-amber-400 hover:text-black'}`}
                  title="Upload ảnh từ máy lên Cloudinary"
                >
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    onChange={handleFoodImageUpload}
                    disabled={isFoodImageUploading}
                    className="hidden"
                  />
                  {isFoodImageUploading ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <ImagePlus className="h-3.5 w-3.5" />}
                  {isFoodImageUploading ? 'Đang tải ảnh...' : foodForm.imageUrl ? 'Chọn ảnh khác' : 'Chọn ảnh từ máy'}
                </label>
                <p className="text-[10px] text-neutral-300 font-medium">JPG, PNG, WEBP hoặc GIF, tối đa 5 MB.</p>
                {foodErrors.imageUrl && <p className="text-[10px] text-rose-400 font-bold">{foodErrors.imageUrl}</p>}
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Mô tả</label>
                <textarea
                  value={foodForm.description}
                  maxLength={500}
                  rows={5}
                  onChange={(e) => {
                    setFoodForm((prev) => ({ ...prev, description: e.target.value }));
                    if (foodErrors.description) setFoodErrors((prev) => ({ ...prev, description: undefined }));
                  }}
                  className={`w-full resize-none bg-black border p-3 text-sm text-white focus:outline-none rounded-none leading-relaxed ${foodErrors.description ? 'border-rose-500' : 'border-white/[0.06] focus:border-amber-400'}`}
                />
                {foodErrors.description && <p className="text-[10px] text-rose-400 font-bold">{foodErrors.description}</p>}
              </div>

              <button
                type="submit"
                disabled={isFoodSaving}
                className="w-full py-3.5 bg-amber-500 hover:bg-amber-400 text-black font-sans font-black text-xs uppercase tracking-widest transition disabled:opacity-60 flex items-center justify-center gap-2"
              >
                {isFoodSaving ? 'Đang lưu...' : editingFood ? 'Cập nhật món' : 'Tạo món'} <Check className="h-4 w-4" />
              </button>
            </form>

            <div className="2xl:col-span-8 border border-white/[0.05] bg-neutral-950 overflow-hidden">
              <div className="p-4 border-b border-white/[0.05] flex flex-col md:flex-row md:items-center gap-3 justify-between">
                <div>
                  <span className="text-[10px] font-mono text-neutral-300 uppercase tracking-[0.2em] font-black">Danh sách món</span>
                  <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white mt-1">
                    {foodItems.length + foodCombos.length} món đang quản lý
                  </h3>
                </div>
                <div className="relative w-full md:w-80">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-neutral-200" />
                  <input
                    value={foodSearch}
                    onChange={(e) => setFoodSearch(e.target.value)}
                    placeholder="Tìm món..."
                    className="w-full bg-black border border-white/[0.06] py-2.5 pl-9 pr-3 text-xs text-white focus:outline-none focus:border-amber-400"
                  />
                </div>
              </div>

              <div className="overflow-hidden">
                <table className="w-full table-fixed text-left border-collapse">
                  <thead>
                    <tr className="bg-black/60 border-b border-white/[0.05] text-[9px] uppercase tracking-widest text-neutral-300 font-sans">
                      <th className="py-3 px-4 w-[46%]">Món</th>
                      <th className="py-3 px-3 w-[13%]">Loại</th>
                      <th className="py-3 px-3 w-[14%]">Giá</th>
                      <th className="py-3 px-3 w-[14%]">Trạng thái</th>
                      <th className="py-3 px-3 w-[13%] text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/[0.03]">
                    {isFoodLoading ? (
                      <tr>
                        <td colSpan={5} className="py-10 text-center text-neutral-300 font-mono uppercase tracking-wider">
                          Đang tải danh sách bắp nước...
                        </td>
                      </tr>
                    ) : visibleFoods.length > 0 ? (
                      paginatedFoods
                        .map((item) => (
                          <tr key={`${item.kind}-${item.id}`} className="hover:bg-white/[0.03] transition">
                            <td className="py-3.5 px-4 min-w-0">
                              <div className="flex items-center gap-3">
                                <img
                                  src={item.imageUrl || 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?q=80&w=300&auto=format&fit=crop'}
                                  alt={item.name}
                                  className="h-12 w-12 object-cover border border-white/[0.06] bg-black"
                                  referrerPolicy="no-referrer"
                                />
                                <div className="min-w-0">
                                  <div className="font-black text-white text-sm uppercase tracking-wide truncate">{item.name}</div>
                                  <div className="text-[10px] text-neutral-300 truncate">{item.description || 'Chưa có mô tả'}</div>
                                </div>
                              </div>
                            </td>
                            <td className="py-3.5 px-3 text-xs font-black uppercase text-amber-300 truncate">{item.kind === 'combo' ? 'Combo' : 'Món lẻ'}</td>
                            <td className="py-3.5 px-3 text-xs font-mono text-white truncate">{Number(item.price).toLocaleString()}đ</td>
                            <td className="py-3.5 px-3">
                              <span className={`text-[10px] font-black uppercase ${getFoodStatusMeta(item.status).className}`}>
                                {getFoodStatusMeta(item.status).label}
                              </span>
                            </td>
                            <td className="py-3.5 px-3">
                              <div className="flex justify-end gap-2">
                                <button
                                  type="button"
                                  onClick={() => handleEditFood(item, item.kind)}
                                  className="p-2 text-amber-300 border border-amber-500/20 bg-amber-500/5 hover:bg-amber-400 hover:text-black transition"
                                  title="Chỉnh sửa món"
                                >
                                  <Edit3 className="h-3.5 w-3.5" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleToggleFoodStatus(item, item.kind)}
                                  className="p-2 text-rose-400 border border-rose-500/20 bg-rose-500/5 hover:bg-rose-500 hover:text-black transition"
                                  title={item.status === 'OUT_OF_STOCK' ? 'Mở bán cho khách hàng' : 'Đánh dấu hết'}
                                >
                                  {item.status === 'OUT_OF_STOCK' ? <Eye className="h-3.5 w-3.5" /> : <EyeOff className="h-3.5 w-3.5" />}
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))
                    ) : (
                      <tr>
                        <td colSpan={5} className="py-10 text-center text-neutral-300 font-mono uppercase tracking-wider">
                          Không có món bắp nước phù hợp.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
              <div className="flex flex-col gap-3 border-t border-white/[0.05] bg-black/80 p-3 text-[10px] font-black uppercase tracking-[0.16em] text-neutral-300 sm:flex-row sm:items-center sm:justify-between">
                <span>
                  Hiển thị {foodDisplayStart}-{foodDisplayEnd}/{visibleFoods.length} món - Trang {safeFoodPage}/{totalFoodPages}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={safeFoodPage <= 1}
                    onClick={() => setFoodPage((page) => Math.max(1, page - 1))}
                    className="inline-flex items-center gap-1 border border-white/[0.06] px-3 py-2 text-white transition hover:border-white disabled:opacity-30"
                  >
                    <ChevronLeft className="h-3.5 w-3.5" /> Trước
                  </button>
                  <button
                    type="button"
                    disabled={safeFoodPage >= totalFoodPages}
                    onClick={() => setFoodPage((page) => Math.min(totalFoodPages, page + 1))}
                    className="inline-flex items-center gap-1 border border-white/[0.06] px-3 py-2 text-white transition hover:border-white disabled:opacity-30"
                  >
                    Sau <ChevronRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      )}
    </>
  );
}


