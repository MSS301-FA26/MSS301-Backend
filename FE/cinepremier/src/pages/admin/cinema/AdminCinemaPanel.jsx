import React, { useEffect, useState } from 'react';
import {
  Building2, CheckCircle2, Globe2, MapPin,
  Phone, Power, RefreshCw, Save, ShieldCheck
} from 'lucide-react';
import { adminService } from '../../../services/adminService';

const emptyForm = {
  name: '',
  address: '',
  city: '',
  phone: '',
  status: 'ACTIVE'
};

export default function AdminCinemaPanel({ ctx }) {
  const { getAdminToken, showToast, addAuditLog, onCinemaChanged = () => { } } = ctx;
  const [cinema, setCinema] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const syncForm = (data) => {
    setCinema(data);
    setForm({
      name: data?.name || '',
      address: data?.address || '',
      city: data?.city || '',
      phone: data?.phone || '',
      status: data?.status || 'ACTIVE'
    });
  };

  const loadCinema = async () => {
    const token = getAdminToken();
    if (!token) return;
    setIsLoading(true);
    try {
      syncForm(await adminService.getAdminCinema(token));
    } catch (error) {
      if (error.status === 404) {
        syncForm(null);
        showToast('Rạp chưa được cấu hình trong dữ liệu hệ thống.');
      } else showToast(error.message || 'Không thể tải thông tin rạp.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadCinema();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const token = getAdminToken();
    if (!token) return;
    if (!cinema) {
      showToast('Rạp chưa được cấu hình. Không thể cập nhật thông tin.');
      return;
    }

    setIsSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        address: form.address.trim(),
        city: form.city.trim(),
        phone: form.phone.trim(),
        status: form.status
      };
      const saved = await adminService.updateAdminCinema(token, payload);
      syncForm(saved);
      addAuditLog('Cập nhật rạp chiếu', saved.name);
      showToast('Đã cập nhật thông tin rạp.');
      await onCinemaChanged();
    } catch (error) {
      showToast(error.message || 'Không thể lưu thông tin rạp.');
    } finally {
      setIsSaving(false);
    }
  };

  const changeStatus = async (status) => {
    const token = getAdminToken();
    if (!token) return;
    setIsSaving(true);
    try {
      const saved = await adminService.updateAdminCinemaStatus(token, status);
      syncForm(saved);
      addAuditLog('Đổi trạng thái rạp', `${saved.name}: ${saved.status}`);
      showToast(`Đã chuyển rạp sang trạng thái ${saved.status === 'ACTIVE' ? 'đang hoạt động' : 'tạm ẩn'}.`);
      await onCinemaChanged();
    } catch (error) {
      showToast(error.message || 'Không thể đổi trạng thái rạp.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex min-h-72 items-center justify-center border border-white/[0.06] bg-neutral-950">
        <RefreshCw className="mr-3 h-4 w-4 animate-spin text-amber-400" />
        <span className="text-[10px] font-black uppercase tracking-[0.22em] text-neutral-200">Đang tải thông tin rạp</span>
      </div>
    );
  }

  const isActive = cinema?.status === 'ACTIVE';

  return (
    <div className="space-y-5">
      <section className="relative overflow-hidden border border-amber-500/20 bg-gradient-to-br from-amber-500/10 via-[#090909] to-black p-5 sm:p-7">
        <div className="absolute -right-16 -top-24 h-64 w-64 rounded-full bg-amber-500/10 blur-3xl" />
        <div className="relative flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
          <div className="max-w-2xl">
            <div className="flex items-center gap-2 text-[9px] font-black uppercase tracking-[0.26em] text-amber-400">
              <Building2 className="h-4 w-4" />
              Quản lý rạp chiếu
            </div>
            <h2 className="mt-3 text-xl font-black uppercase tracking-tight text-neutral-200 sm:text-2xl">
              {cinema?.name || 'Thiết lập rạp chiếu'}
            </h2>
            <p className="mt-2 max-w-xl text-xs leading-relaxed text-neutral-200">
              Chỉnh sửa thông tin rạp mà khách hàng nhìn thấy tại đầu và cuối trang.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge active={isActive} exists={Boolean(cinema)} />
            <button
              type="button"
              onClick={loadCinema}
              className="flex items-center gap-2 border border-white/10 bg-black/40 px-4 py-2.5 text-[9px] font-black uppercase tracking-[0.16em] text-neutral-300 transition hover:border-amber-400/50 hover:text-amber-300"
            >
              <RefreshCw className="h-3.5 w-3.5" /> Làm mới
            </button>
          </div>
        </div>
      </section>

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.45fr)_minmax(300px,0.75fr)]">
        <form onSubmit={handleSubmit} className="border border-white/[0.06] bg-[#070707] p-5 sm:p-6">
          <div className="mb-6 flex items-start justify-between gap-4 border-b border-white/10 pb-4">
            <div>
              <div className="text-[9px] font-black uppercase tracking-[0.22em] text-amber-400">Thông tin vận hành</div>
              <h3 className="mt-1 text-xs font-black uppercase tracking-[0.18em] text-white">Cập nhật rạp chiếu</h3>
            </div>
            <Building2 className="h-6 w-6 text-neutral-300" />
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Field icon={Building2} label="Tên rạp" value={form.name} onChange={(value) => setForm({ ...form, name: value })} placeholder="CinePremier Central" required />
            <Field icon={Globe2} label="Thành phố" value={form.city} onChange={(value) => setForm({ ...form, city: value })} placeholder="TP. Hồ Chí Minh" />
            <div className="md:col-span-2">
              <Field icon={MapPin} label="Địa chỉ hiển thị cho khách hàng" value={form.address} onChange={(value) => setForm({ ...form, address: value })} placeholder="Nhập địa chỉ đầy đủ của rạp" required />
            </div>
            <Field icon={Phone} label="Số điện thoại" value={form.phone} onChange={(value) => setForm({ ...form, phone: value })} placeholder="0900 000 000" required />
            <label className="block space-y-2">
              <span className="text-[9px] font-black uppercase tracking-[0.18em] text-neutral-300">Trạng thái khi lưu</span>
              <select
                value={form.status}
                onChange={(event) => setForm({ ...form, status: event.target.value })}
                className="h-12 w-full border border-white/[0.06] bg-black px-4 text-xs font-bold text-white outline-none transition focus:border-amber-400"
              >
                <option value="ACTIVE">Đang hoạt động - Hiển thị cho khách</option>
                <option value="INACTIVE">Tạm ẩn - Khách hàng không nhìn thấy</option>
              </select>
            </label>
          </div>

          <button
            disabled={isSaving || !cinema}
            className="mt-6 flex w-full items-center justify-center gap-2 bg-amber-500 px-5 py-3.5 text-[10px] font-black uppercase tracking-[0.2em] text-black transition hover:bg-amber-300 disabled:opacity-50"
          >
            <Save className="h-4 w-4" /> Lưu thay đổi
          </button>
        </form>

        <aside className="space-y-4">
          <div className="border border-white/[0.06] bg-[#070707] p-5">
            <div className="flex items-center gap-2 text-[9px] font-black uppercase tracking-[0.2em] text-neutral-300">
              <ShieldCheck className="h-4 w-4 text-amber-400" /> Khách hàng đang thấy
            </div>
            <div className="mt-5 space-y-4">
              <InfoRow icon={Building2} label="Tên rạp" value={cinema?.name || 'Chưa cấu hình'} />
              <InfoRow icon={MapPin} label="Địa chỉ" value={[cinema?.address, cinema?.city].filter(Boolean).join(', ') || 'Chưa có địa chỉ'} />
              <InfoRow icon={Phone} label="Liên hệ" value={cinema?.phone || 'Chưa cập nhật'} />
            </div>
          </div>

          {cinema && (
            <div className="border border-white/[0.06] bg-[#070707] p-5">
              <div className="text-[9px] font-black uppercase tracking-[0.2em] text-neutral-300">Thao tác trạng thái</div>
              <button
                type="button"
                disabled={isSaving}
                onClick={() => changeStatus(isActive ? 'INACTIVE' : 'ACTIVE')}
                className="mt-4 flex w-full items-center justify-between border border-cyan-500/25 bg-cyan-500/[0.06] px-4 py-3 text-left transition hover:border-cyan-400 disabled:opacity-40"
              >
                <span>
                  <span className="block text-[9px] font-black uppercase tracking-wider text-cyan-300">{isActive ? 'Tạm ẩn rạp' : 'Hiển thị lại rạp'}</span>
                  <span className="mt-1 block text-[8px] uppercase text-neutral-300">{isActive ? 'Khách hàng sẽ tạm thời không thấy rạp' : 'Cho phép khách hàng nhìn thấy rạp'}</span>
                </span>
                <Power className="h-4 w-4 text-cyan-300" />
              </button>
            </div>
          )}
        </aside>
      </section>
    </div>
  );
}

function StatusBadge({ active, exists }) {
  if (!exists) {
    return <span className="border border-white/[0.08] bg-black/50 px-3 py-2 text-[8px] font-black uppercase tracking-[0.18em] text-neutral-200">Chưa cấu hình</span>;
  }
  return (
    <span className={`flex items-center gap-2 border px-3 py-2 text-[8px] font-black uppercase tracking-[0.18em] ${active ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' : 'border-rose-500/30 bg-rose-500/10 text-rose-300'}`}>
      <CheckCircle2 className="h-3.5 w-3.5" /> {active ? 'Đang hoạt động' : 'Ngừng hoạt động'}
    </span>
  );
}

function Field({ icon: Icon, label, value, onChange, placeholder, required = false }) {
  return (
    <label className="block space-y-2">
      <span className="text-[9px] font-black uppercase tracking-[0.18em] text-neutral-300">{label}</span>
      <span className="relative block">
        <Icon className="absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-neutral-200" />
        <input
          required={required}
          value={value}
          placeholder={placeholder}
          onChange={(event) => onChange(event.target.value)}
          className="h-12 w-full border border-white/[0.06] bg-black pl-11 pr-4 text-xs text-white outline-none transition placeholder:text-neutral-200 focus:border-amber-400"
        />
      </span>
    </label>
  );
}

function InfoRow({ icon: Icon, label, value }) {
  return (
    <div className="flex items-start gap-3 border-b border-white/[0.06] pb-4 last:border-0 last:pb-0">
      <span className="flex h-8 w-8 shrink-0 items-center justify-center border border-white/10 bg-black text-amber-400">
        <Icon className="h-3.5 w-3.5" />
      </span>
      <span className="min-w-0">
        <span className="block text-[8px] font-black uppercase tracking-widest text-neutral-200">{label}</span>
        <span className="mt-1 block text-[11px] font-bold leading-relaxed text-neutral-200">{value}</span>
      </span>
    </div>
  );
}

