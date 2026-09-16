import React from 'react';
import { motion } from 'motion/react';
import {
  Plus, Trash2, Edit3, Search, Check, ChevronLeft, ChevronRight
} from 'lucide-react';

export default function AdminGenresPanel({ ctx }) {
  const {
    activeTab,
    genres,
    genrePagination,
    genreSearch,
    setGenreSearch,
    genreForm,
    setGenreForm,
    genreErrors,
    setGenreErrors,
    editingGenreId,
    isGenreLoading,
    isGenreSaving,
    fetchGenres,
    resetGenreForm,
    handleGenreSubmit,
    handleEditGenre,
    handleDeleteGenre,
  } = ctx;

  const { page, totalPages, totalItems } = genrePagination || { page: 0, totalPages: 1, totalItems: 0 };

  // Client-side search filter (trên page hiện tại)
  const filteredGenres = genreSearch.trim()
    ? genres.filter((g) => {
      const q = genreSearch.trim().toLowerCase();
      return g.name?.toLowerCase().includes(q) || g.description?.toLowerCase().includes(q);
    })
    : genres;

  const goToPage = (nextPage) => {
    if (nextPage < 0 || nextPage >= totalPages) return;
    fetchGenres(nextPage);
  };

  return (
    <>
      {activeTab === 'genres' && (
        <motion.div
          key="panel-genres"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          transition={{ duration: 0.2 }}
          className="space-y-6"
        >
          <div className="border border-white/[0.05] bg-gradient-to-r from-[#090909] to-[#050505] p-5">
            <div>
              <span className="text-[9px] font-mono tracking-[0.24em] text-neutral-300 uppercase font-black block">ADMIN GENRE</span>
              <h2 className="text-xs font-black uppercase tracking-[0.18em] text-neutral-200 mt-1">Quản trị thể loại phim</h2>
            </div>
          </div>

          <div className="grid grid-cols-1 xl:grid-cols-[300px_minmax(0,1fr)] gap-5 items-start">
            {/* ── Form tạo / sửa ── */}
            <div className="border border-white/[0.05] bg-[#070707] p-4 space-y-4">
              <div className="flex items-center justify-between border-b border-white/[0.03] pb-3">
                <div>
                  <span className="text-[8px] font-mono uppercase tracking-[0.2em] text-neutral-300 font-black">
                    {editingGenreId ? 'Chỉnh sửa bản ghi' : 'Tạo bản ghi mới'}
                  </span>
                  <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white mt-1">
                    {editingGenreId ? 'Cập nhật thể loại' : 'Thêm thể loại'}
                  </h3>
                </div>
                {editingGenreId && (
                  <button
                    type="button"
                    onClick={resetGenreForm}
                    className="border border-amber-400/50 bg-amber-400/10 px-3 py-1.5 text-[10px] uppercase tracking-widest text-amber-200 hover:bg-amber-400 hover:text-black font-black transition"
                  >
                    Hủy sửa
                  </button>
                )}
              </div>

              <form onSubmit={handleGenreSubmit} className="space-y-4 text-xs font-sans">
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Tên thể loại</label>
                    <span className={`text-[9px] font-mono ${genreForm.name.length > 100 ? 'text-rose-400' : 'text-neutral-200'}`}>
                      {genreForm.name.length}/100
                    </span>
                  </div>
                  <textarea
                    value={genreForm.name}
                    maxLength={100}
                    rows={2}
                    wrap="soft"
                    onChange={(e) => {
                      const nextName = e.target.value.replace(/[\r\n]+/g, ' ');
                      setGenreForm((prev) => ({ ...prev, name: nextName.slice(0, 100) }));
                      if (genreErrors.name) setGenreErrors((prev) => ({ ...prev, name: undefined }));
                    }}
                    placeholder="Ví dụ: Hành động, Tâm lý, Sci-Fi..."
                    className={`w-full min-h-[64px] resize-none break-words bg-black border p-2.5 text-sm text-white focus:outline-none rounded-none font-bold leading-relaxed caret-amber-300 ${genreErrors.name ? 'border-rose-500 focus:border-rose-400' : 'border-white/[0.06] focus:border-amber-400'}`}
                  />
                  {genreErrors.name && (
                    <p className="text-[10px] text-rose-400 font-bold">{genreErrors.name}</p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-[10px] uppercase tracking-[0.18em] text-neutral-200 font-black">Mô tả</label>
                    <span className={`text-[9px] font-mono ${genreForm.description.length < 50 || genreForm.description.length > 1000 ? 'text-rose-400' : 'text-neutral-200'}`}>
                      {genreForm.description.length}/1000
                    </span>
                  </div>
                  <textarea
                    value={genreForm.description}
                    maxLength={1000}
                    rows={5}
                    onChange={(e) => {
                      setGenreForm((prev) => ({ ...prev, description: e.target.value }));
                      if (genreErrors.description) setGenreErrors((prev) => ({ ...prev, description: undefined }));
                    }}
                    placeholder="Mô tả ngắn về gu phim, nhịp kể, nhóm khán giả phù hợp..."
                    className={`w-full resize-none bg-black border p-2.5 text-sm text-white focus:outline-none rounded-none leading-relaxed ${genreErrors.description ? 'border-rose-500 focus:border-rose-400' : 'border-white/[0.06] focus:border-amber-400'}`}
                  />
                  {genreErrors.description && (
                    <p className="text-[10px] text-rose-400 font-bold">{genreErrors.description}</p>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={isGenreSaving}
                  className="w-full py-3.5 bg-amber-500 hover:bg-amber-400 text-black font-sans font-black text-xs uppercase tracking-widest transition disabled:opacity-60 flex items-center justify-center gap-2"
                >
                  {isGenreSaving ? (
                    <span className="h-4 w-4 border-2 border-black border-t-transparent animate-spin rounded-full inline-block" />
                  ) : editingGenreId ? (
                    <>Cập nhật thể loại <Check className="h-4 w-4" /></>
                  ) : (
                    <>Tạo thể loại <Plus className="h-4 w-4" /></>
                  )}
                </button>
              </form>
            </div>

            {/* ── Danh sách ── */}
            <div className="min-w-0 border border-white/[0.05] bg-neutral-950 overflow-hidden">
              <div className="p-3 border-b border-white/[0.05] flex flex-col md:flex-row md:items-center gap-3 justify-between">
                <div>
                  <span className="text-[8px] font-mono text-white uppercase tracking-[0.2em] font-black">Danh sách loại phim</span>
                  <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white mt-1">
                    {totalItems} thể loại đã đồng bộ
                  </h3>
                </div>
                <div className="relative w-full md:w-72">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-neutral-200" />
                  <input
                    type="text"
                    value={genreSearch}
                    onChange={(e) => setGenreSearch(e.target.value)}
                    placeholder="Tìm thể loại..."
                    className="w-full bg-black border border-white/[0.06] py-2.5 pl-9 pr-3 text-xs text-white focus:outline-none focus:border-amber-400"
                  />
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full table-fixed text-left border-collapse">
                  <thead className="bg-black">
                    <tr className="border-b border-white/[0.05] text-[9px] uppercase tracking-widest text-neutral-300 font-sans">
                      <th className="py-2.5 px-3 w-[54px]">ID</th>
                      <th className="py-2.5 px-3 w-[170px]">Tên thể loại</th>
                      <th className="py-2.5 px-3">Mô tả</th>
                      <th className="py-2.5 px-3 text-right w-[86px]">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/[0.03]">
                    {isGenreLoading ? (
                      <tr>
                        <td colSpan={4} className="py-10 text-center text-neutral-300 font-mono uppercase tracking-wider">
                          Đang tải danh sách thể loại...
                        </td>
                      </tr>
                    ) : filteredGenres.length > 0 ? (
                      filteredGenres.map((genre) => (
                        <tr key={genre.id} className="hover:bg-white/[0.03] transition">
                          <td className="py-2.5 px-3 text-[10px] font-mono text-neutral-300">#{genre.id}</td>
                          <td className="py-2.5 px-3">
                            <div className="font-black text-white text-xs uppercase tracking-wide truncate">{genre.name}</div>
                            <div className="text-[9px] text-neutral-200 font-mono mt-0.5">
                              {genre.updatedAt ? `Cập nhật: ${new Date(genre.updatedAt).toLocaleDateString('vi-VN')}` : 'Chưa có mốc cập nhật'}
                            </div>
                          </td>
                          <td className="py-2.5 px-3 text-xs text-neutral-300 leading-relaxed">
                            <div className="line-clamp-2 break-words">{genre.description || <span className="text-neutral-200 italic">Chưa có mô tả</span>}</div>
                          </td>
                          <td className="py-2.5 px-3">
                            <div className="flex justify-end gap-2">
                              <button
                                type="button"
                                onClick={() => handleEditGenre(genre)}
                                className="p-1.5 text-amber-300 border border-amber-500/20 bg-amber-500/5 hover:bg-amber-400 hover:text-black transition"
                                title="Chỉnh sửa thể loại"
                              >
                                <Edit3 className="h-3.5 w-3.5" />
                              </button>
                              <button
                                type="button"
                                onClick={() => handleDeleteGenre(genre)}
                                className="p-1.5 text-rose-400 border border-rose-500/20 bg-rose-500/5 hover:bg-rose-500 hover:text-black transition"
                                title="Xóa thể loại"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={4} className="py-10 text-center text-neutral-300 font-mono uppercase tracking-wider">
                          Không có thể loại phù hợp truy vấn
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              {/* ── Pagination Bar ── */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between border-t border-white/[0.05] bg-[#060606] px-4 py-3">
                  <span className="text-[10px] font-mono text-neutral-300">
                    Trang <span className="text-amber-400 font-black">{page + 1}</span> / {totalPages}
                    <span className="ml-2 text-neutral-200">({totalItems} thể loại)</span>
                  </span>

                  <div className="flex items-center gap-1">
                    {/* Prev */}
                    <button
                      type="button"
                      disabled={page === 0}
                      onClick={() => goToPage(page - 1)}
                      className="flex items-center gap-1 border border-white/[0.06] bg-black px-2.5 py-1.5 text-[10px] font-black uppercase text-neutral-200 hover:border-amber-500/50 hover:text-amber-300 disabled:opacity-30 disabled:cursor-not-allowed transition"
                    >
                      <ChevronLeft className="h-3 w-3" /> Trước
                    </button>

                    {/* Page numbers */}
                    {Array.from({ length: totalPages }, (_, i) => i).map((i) => {
                      const isNear = Math.abs(i - page) <= 1 || i === 0 || i === totalPages - 1;
                      if (!isNear && Math.abs(i - page) === 2) {
                        return <span key={`ellipsis-${i}`} className="px-1 text-neutral-300 text-xs select-none">…</span>;
                      }
                      if (!isNear) return null;
                      return (
                        <button
                          key={i}
                          type="button"
                          onClick={() => goToPage(i)}
                          className={`min-w-[30px] h-[30px] border text-[10px] font-black transition ${i === page
                            ? 'border-amber-500 bg-amber-500 text-black'
                            : 'border-white/[0.06] bg-black text-neutral-200 hover:border-amber-500/50 hover:text-amber-300'
                            }`}
                        >
                          {i + 1}
                        </button>
                      );
                    })}

                    {/* Next */}
                    <button
                      type="button"
                      disabled={page >= totalPages - 1}
                      onClick={() => goToPage(page + 1)}
                      className="flex items-center gap-1 border border-white/[0.06] bg-black px-2.5 py-1.5 text-[10px] font-black uppercase text-neutral-200 hover:border-amber-500/50 hover:text-amber-300 disabled:opacity-30 disabled:cursor-not-allowed transition"
                    >
                      Tiếp <ChevronRight className="h-3 w-3" />
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </motion.div>
      )}
    </>
  );
}


