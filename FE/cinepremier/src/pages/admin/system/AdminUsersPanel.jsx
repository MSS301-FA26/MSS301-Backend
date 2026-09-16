import React, { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { getStoredAuth } from '../../../services/authService';
import { adminService } from '../../../services/adminService';
import {
  BadgeCheck,
  Clock,
  Eye,
  EyeOff,
  Mail,
  Phone,
  Plus,
  RefreshCw,
  Search,
  ShieldAlert,
  UserCheck,
  UserPlus,
  Users,
  UserX,
  X
} from 'lucide-react';
import { PASSWORD_VALIDATION_MESSAGE, isStrongPassword } from '../../../utils/validation';

const USER_STATUS_OPTIONS = ['ACTIVE', 'DISABLED', 'PENDING_VERIFICATION'];
const STAFF_PROFILE_STATUS_OPTIONS = ['ACTIVE', 'INACTIVE', 'SUSPENDED'];
const EMPTY_STAFF_FORM = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  birthYear: ''
};
const EMPTY_PROFILE_FORM = {
  employeeCode: '',
  position: '',
  status: 'ACTIVE'
};

const formatDateTime = (value) => {
  if (!value) return 'Chưa có dữ liệu';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('vi-VN');
};

const getStatusMeta = (status = '') => {
  const normalized = String(status).toUpperCase();
  if (normalized === 'ACTIVE') {
    return {
      label: 'Đang hoạt động',
      icon: UserCheck,
      className: 'border-emerald-500/30 bg-emerald-950/20 text-emerald-300'
    };
  }
  if (normalized === 'DISABLED') {
    return {
      label: 'Đã vô hiệu',
      icon: UserX,
      className: 'border-rose-500/35 bg-rose-950/20 text-rose-300'
    };
  }
  return {
    label: 'Chờ xác minh',
    icon: ShieldAlert,
    className: 'border-amber-500/35 bg-amber-950/20 text-amber-300'
  };
};

export default function AdminUsersPanel({ ctx }) {
  const {
    activeTab,
    adminUsers,
    selectedAdminUser,
    userSearch,
    setUserSearch,
    isUsersLoading,
    isUserDetailLoading,
    isUserStatusSaving,
    isStaffCreating,
    fetchAdminUsers,
    handleSelectAdminUser,
    handleCreateStaff,
    handleUpdateAdminUserStatus,
    currentUser
  } = ctx;
  const [isStaffFormOpen, setIsStaffFormOpen] = useState(false);
  const [showStaffPassword, setShowStaffPassword] = useState(false);
  const [staffForm, setStaffForm] = useState(EMPTY_STAFF_FORM);
  const [staffFormErrors, setStaffFormErrors] = useState({});
  const [staffProfiles, setStaffProfiles] = useState([]);
  const [profileForm, setProfileForm] = useState(EMPTY_PROFILE_FORM);
  const [profileError, setProfileError] = useState('');
  const [isProfileLoading, setIsProfileLoading] = useState(false);
  const [isProfileSaving, setIsProfileSaving] = useState(false);

  const selectedStaffProfile = staffProfiles.find((profile) => (
    String(profile.userId) === String(selectedAdminUser?.id)
  ));
  const isSelectedStaff = (selectedAdminUser?.roles || []).some((role) => (
    String(role).toUpperCase().replace('ROLE_', '') === 'STAFF'
  ));

  useEffect(() => {
    if (activeTab !== 'users') return undefined;
    let cancelled = false;
    const loadProfiles = async () => {
      const { accessToken } = getStoredAuth();
      if (!accessToken) return;
      setIsProfileLoading(true);
      try {
        const profiles = await adminService.getAdminStaffProfiles(accessToken);
        if (!cancelled) setStaffProfiles(Array.isArray(profiles) ? profiles : []);
      } catch (error) {
        if (!cancelled) setProfileError(error.message || 'Khong the tai staff profile.');
      } finally {
        if (!cancelled) setIsProfileLoading(false);
      }
    };
    loadProfiles();
    return () => {
      cancelled = true;
    };
  }, [activeTab]);

  useEffect(() => {
    if (selectedStaffProfile) {
      setProfileForm({
        employeeCode: selectedStaffProfile.employeeCode || '',
        position: selectedStaffProfile.position || '',
        status: selectedStaffProfile.status || 'ACTIVE'
      });
      setProfileError('');
      return;
    }
    setProfileForm(EMPTY_PROFILE_FORM);
    setProfileError('');
  }, [selectedStaffProfile?.id, selectedAdminUser?.id]);

  if (activeTab !== 'users') return null;

  const updateStaffForm = (field, value) => {
    setStaffForm((prev) => ({ ...prev, [field]: value }));
    setStaffFormErrors((prev) => ({ ...prev, [field]: '' }));
  };

  const updateProfileForm = (field, value) => {
    setProfileForm((prev) => ({ ...prev, [field]: value }));
    setProfileError('');
  };

  const saveStaffProfile = async (event) => {
    event.preventDefault();
    if (!selectedAdminUser?.id || !isSelectedStaff) return;

    const employeeCode = profileForm.employeeCode.trim();
    const position = profileForm.position.trim();
    if (!employeeCode || !position) {
      setProfileError('Nhap ma nhan vien va vi tri truoc khi luu.');
      return;
    }

    const { accessToken } = getStoredAuth();
    if (!accessToken) {
      setProfileError('Phien dang nhap admin khong hop le.');
      return;
    }

    setIsProfileSaving(true);
    try {
      const payload = {
        userId: Number(selectedAdminUser.id),
        employeeCode,
        position,
        status: profileForm.status || 'ACTIVE'
      };
      const savedProfile = selectedStaffProfile
        ? await adminService.updateAdminStaffProfile(accessToken, selectedStaffProfile.id, {
          employeeCode,
          position,
          status: payload.status
        })
        : await adminService.createAdminStaffProfile(accessToken, payload);
      setStaffProfiles((prev) => [
        savedProfile,
        ...prev.filter((profile) => String(profile.id) !== String(savedProfile.id))
      ]);
      setProfileError('');
    } catch (error) {
      setProfileError(error.message || 'Khong the luu staff profile.');
    } finally {
      setIsProfileSaving(false);
    }
  };

  const submitStaffForm = async (event) => {
    event.preventDefault();
    const errors = {};
    const email = staffForm.email.trim();
    const fullName = staffForm.fullName.trim();
    const phone = staffForm.phone.trim();
    const birthYear = staffForm.birthYear ? Number(staffForm.birthYear) : null;

    if (!email) errors.email = 'Email là bắt buộc.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Email không hợp lệ.';
    if (!fullName) errors.fullName = 'Họ tên là bắt buộc.';
    if (!isStrongPassword(staffForm.password)) errors.password = PASSWORD_VALIDATION_MESSAGE;
    if (phone && !/^\+?[0-9]{10,15}$/.test(phone)) errors.phone = 'Số điện thoại gồm 10-15 chữ số.';
    if (birthYear && (birthYear < 1900 || birthYear > 2100)) errors.birthYear = 'Năm sinh không hợp lệ.';

    if (Object.keys(errors).length > 0) {
      setStaffFormErrors(errors);
      return;
    }

    try {
      const createdStaff = await handleCreateStaff({
        email,
        password: staffForm.password,
        fullName,
        phone: phone || null,
        birthYear
      });
      if (!createdStaff) return;
      setStaffForm(EMPTY_STAFF_FORM);
      setStaffFormErrors({});
      setShowStaffPassword(false);
      setIsStaffFormOpen(false);
    } catch {
      // The shared admin handler displays the backend validation/conflict message.
    }
  };

  const query = userSearch.trim().toLowerCase();
  const filteredUsers = adminUsers.filter((user) => {
    if (!query) return true;
    return (
      user.email?.toLowerCase().includes(query) ||
      user.fullName?.toLowerCase().includes(query) ||
      user.phone?.toLowerCase().includes(query) ||
      String(user.id || '').includes(query)
    );
  });

  const selectedStatus = getStatusMeta(selectedAdminUser?.status);
  const SelectedStatusIcon = selectedStatus.icon;
  const isViewingCurrentAdmin =
    String(currentUser?.id || '') === String(selectedAdminUser?.id || '') ||
    (currentUser?.email && selectedAdminUser?.email && currentUser.email === selectedAdminUser.email);

  return (
    <motion.div
      key="panel-users"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.2 }}
      className="space-y-6"
    >
      <div className="flex flex-col gap-4 border border-white/[0.05] bg-[#070707] p-4 md:flex-row md:items-center md:justify-between">
        <div>
          <span className="text-[8px] font-mono font-black uppercase tracking-widest text-neutral-300">
            ADMIN USER CONTROL
          </span>
          <h2 className="text-xs font-black uppercase tracking-[0.18em] text-neutral-200">
            Quản lý người dùng hệ thống
          </h2>
        </div>

        <button
          type="button"
          onClick={() => setIsStaffFormOpen((prev) => !prev)}
          className="flex items-center justify-center gap-2 border border-amber-500/60 bg-amber-500/10 px-4 py-2 text-[10px] font-mono font-black uppercase tracking-widest text-amber-300 transition hover:bg-amber-500 hover:text-black"
        >
          {isStaffFormOpen ? <X className="h-3.5 w-3.5" /> : <UserPlus className="h-3.5 w-3.5" />}
          {isStaffFormOpen ? 'Đóng biểu mẫu' : 'Cấp tài khoản STAFF'}
        </button>

        <button
          type="button"
          onClick={fetchAdminUsers}
          disabled={isUsersLoading}
          className="flex items-center justify-center gap-2 border border-white/[0.06] bg-black px-4 py-2 text-[10px] font-mono uppercase tracking-widest text-neutral-300 transition hover:border-amber-400 hover:text-amber-300 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isUsersLoading ? 'animate-spin' : ''}`} />
          Làm mới dữ liệu
        </button>
      </div>

      {isStaffFormOpen && (
        <motion.form
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          onSubmit={submitStaffForm}
          className="border border-amber-500/30 bg-[#080704] p-5"
        >
          <div className="mb-4 flex items-start justify-between gap-4 border-b border-amber-500/15 pb-4">
            <div>
              <div className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-amber-300">
                <Plus className="h-4 w-4" />
                Cấp tài khoản STAFF
              </div>
              <p className="mt-1 text-[10px] text-neutral-300">
                Tài khoản được kích hoạt ngay và có quyền truy cập màn hình nghiệp vụ nhân viên.
              </p>
            </div>
            <span className="border border-amber-500/30 bg-black px-2 py-1 text-[8px] font-black uppercase tracking-widest text-amber-300">
              AUTH-12
            </span>
          </div>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-5">
            {[
              { field: 'fullName', label: 'Họ và tên *', placeholder: 'Nguyễn Văn A', type: 'text' },
              { field: 'email', label: 'Email đăng nhập *', placeholder: 'staff@cinepremier.vn', type: 'email' },
              { field: 'phone', label: 'Số điện thoại', placeholder: '0901234567', type: 'tel' },
              { field: 'birthYear', label: 'Năm sinh', placeholder: '2000', type: 'number' }
            ].map(({ field, label, placeholder, type }) => (
              <label key={field} className="space-y-1.5">
                <span className="text-[9px] font-black uppercase tracking-widest text-neutral-200">{label}</span>
                <input
                  type={type}
                  value={staffForm[field]}
                  onChange={(event) => updateStaffForm(field, event.target.value)}
                  placeholder={placeholder}
                  min={field === 'birthYear' ? 1900 : undefined}
                  max={field === 'birthYear' ? 2100 : undefined}
                  className={`w-full border bg-black px-3 py-2.5 text-xs text-white outline-none transition placeholder:text-neutral-200 focus:border-amber-400 ${staffFormErrors[field] ? 'border-rose-500' : 'border-white/[0.06]'}`}
                />
                {staffFormErrors[field] && <span className="block text-[9px] text-rose-300">{staffFormErrors[field]}</span>}
              </label>
            ))}

            <label className="space-y-1.5">
              <span className="text-[9px] font-black uppercase tracking-widest text-neutral-200">Mật khẩu cấp ban đầu *</span>
              <div className="relative">
                <input
                  type={showStaffPassword ? 'text' : 'password'}
                  value={staffForm.password}
                  onChange={(event) => updateStaffForm('password', event.target.value)}
                  placeholder="Tối thiểu 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt"
                  className={`w-full border bg-black px-3 py-2.5 pr-10 text-xs text-white outline-none transition placeholder:text-neutral-200 focus:border-amber-400 ${staffFormErrors.password ? 'border-rose-500' : 'border-white/[0.06]'}`}
                />
                <button
                  type="button"
                  onClick={() => setShowStaffPassword((prev) => !prev)}
                  className="absolute right-2.5 top-2.5 text-neutral-300 transition hover:text-white"
                  aria-label={showStaffPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showStaffPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {staffFormErrors.password && <span className="block text-[9px] text-rose-300">{staffFormErrors.password}</span>}
            </label>
          </div>

          <div className="mt-5 flex justify-end">
            <button
              type="submit"
              disabled={isStaffCreating}
              className="flex items-center gap-2 border border-amber-400 bg-amber-500 px-5 py-2.5 text-[10px] font-black uppercase tracking-widest text-black transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isStaffCreating ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <UserPlus className="h-3.5 w-3.5" />}
              {isStaffCreating ? 'Đang cấp tài khoản...' : 'Tạo tài khoản STAFF'}
            </button>
          </div>
        </motion.form>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <div className="xl:col-span-7 border border-white/[0.05] bg-neutral-950 overflow-hidden">
          <div className="flex flex-col gap-3 border-b border-white/[0.05] bg-black p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-white">
              <Users className="h-4 w-4 text-amber-400" />
              Danh sách tài khoản
            </div>
            <div className="relative w-full sm:max-w-xs">
              <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-neutral-200" />
              <input
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
                placeholder="Tìm email, tên, SĐT..."
                className="w-full border border-white/[0.06] bg-[#050505] py-2 pl-9 pr-3 text-xs text-white outline-none transition placeholder:text-neutral-200 focus:border-amber-400"
              />
            </div>
          </div>

          <div className="divide-y divide-white/[0.03]">
            <div className="hidden grid-cols-[42px_minmax(0,1.6fr)_minmax(86px,0.75fr)_minmax(74px,0.65fr)_minmax(118px,0.85fr)_48px] gap-2 bg-[#050505] px-3 py-3 text-[8px] uppercase tracking-widest text-neutral-300 lg:grid">
              <span>ID</span>
              <span>Người dùng</span>
              <span>Liên hệ</span>
              <span>Vai trò</span>
              <span>Trạng thái</span>
              <span className="text-right">Xem</span>
            </div>

            {isUsersLoading ? (
              <div className="px-4 py-10 text-center font-mono text-[10px] uppercase tracking-widest text-neutral-300">
                Đang tải danh sách người dùng...
              </div>
            ) : filteredUsers.length > 0 ? (
              filteredUsers.map((user) => {
                const statusMeta = getStatusMeta(user.status);
                const StatusIcon = statusMeta.icon;
                const isSelected = String(selectedAdminUser?.id) === String(user.id);

                return (
                  <div
                    key={user.id}
                    className={`grid grid-cols-1 gap-3 px-3 py-3 text-xs transition lg:grid-cols-[42px_minmax(0,1.6fr)_minmax(86px,0.75fr)_minmax(74px,0.65fr)_minmax(118px,0.85fr)_48px] lg:items-center ${isSelected ? 'bg-amber-500/5' : 'hover:bg-white/[0.02]'}`}
                  >
                    <div className="font-mono text-neutral-300">#{user.id}</div>

                    <div className="min-w-0">
                      <div className="truncate font-black text-white">
                        {user.fullName || 'Chưa cập nhật tên'}
                      </div>
                      <div className="truncate text-[10px] text-neutral-300">{user.email}</div>
                    </div>

                    <div className="min-w-0 text-neutral-200">
                      <span className="lg:hidden text-[8px] uppercase tracking-widest text-neutral-200">
                        Liên hệ:{' '}
                      </span>
                      <span className="break-words">{user.phone || 'Chưa có SĐT'}</span>
                    </div>

                    <div className="min-w-0">
                      <div className="flex flex-wrap gap-1">
                        {(user.roles || []).map((role) => (
                          <span
                            key={role}
                            className="border border-white/[0.06] bg-black px-1.5 py-0.5 text-[8px] font-bold uppercase text-neutral-300"
                          >
                            {role}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div className="min-w-0">
                      <span className={`inline-flex max-w-full items-center gap-1.5 border px-2 py-1 text-[8px] font-black uppercase ${statusMeta.className}`}>
                        <StatusIcon className="h-3 w-3 shrink-0" />
                        <span className="truncate">{statusMeta.label}</span>
                      </span>
                    </div>

                    <div className="lg:text-right">
                      <button
                        type="button"
                        onClick={() => handleSelectAdminUser(user.id)}
                        className="border border-white/[0.06] px-2.5 py-1.5 text-[9px] font-bold uppercase text-neutral-300 transition hover:border-white hover:text-white"
                      >
                        Xem
                      </button>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="px-4 py-10 text-center font-mono text-[10px] uppercase tracking-widest text-neutral-300">
                Không tìm thấy người dùng phù hợp
              </div>
            )}
          </div>
        </div>

        <div className="xl:col-span-5 border border-white/[0.05] bg-[#070707] p-5">
          {selectedAdminUser ? (
            <div className="space-y-5">
              <div className="flex items-start justify-between gap-3 border-b border-white/[0.05] pb-4">
                <div className="min-w-0">
                  <span className="text-[9px] font-mono uppercase tracking-widest text-neutral-300">
                    Hồ sơ người dùng
                  </span>
                  <h3 className="mt-1 break-words text-sm font-black text-white">
                    {selectedAdminUser.fullName || selectedAdminUser.email}
                  </h3>
                  <p className="text-[10px] font-mono text-neutral-300">ID #{selectedAdminUser.id}</p>
                </div>
                <span className={`inline-flex shrink-0 items-center gap-1.5 border px-2.5 py-1.5 text-[9px] font-black uppercase ${selectedStatus.className}`}>
                  <SelectedStatusIcon className="h-3.5 w-3.5" />
                  {selectedStatus.label}
                </span>
              </div>

              {isUserDetailLoading ? (
                <div className="py-12 text-center font-mono text-[10px] uppercase tracking-widest text-neutral-300">
                  Đang tải chi tiết...
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <div className="border border-white/[0.05] bg-black p-3">
                      <div className="flex items-center gap-2 text-[9px] uppercase text-neutral-300">
                        <Mail className="h-3.5 w-3.5" /> Email
                      </div>
                      <div className="mt-1 break-all text-xs font-bold text-white">{selectedAdminUser.email}</div>
                    </div>
                    <div className="border border-white/[0.05] bg-black p-3">
                      <div className="flex items-center gap-2 text-[9px] uppercase text-neutral-300">
                        <Phone className="h-3.5 w-3.5" /> Số điện thoại
                      </div>
                      <div className="mt-1 break-words text-xs font-bold text-white">
                        {selectedAdminUser.phone || 'Chưa cập nhật'}
                      </div>
                    </div>
                    <div className="border border-white/[0.05] bg-black p-3">
                      <div className="flex items-center gap-2 text-[9px] uppercase text-neutral-300">
                        <BadgeCheck className="h-3.5 w-3.5" /> Xác minh
                      </div>
                      <div className="mt-1 text-xs font-bold leading-relaxed text-white">
                        Email: {selectedAdminUser.emailVerified ? 'Đã xác minh' : 'Chưa xác minh'} / SĐT:{' '}
                        {selectedAdminUser.phoneVerified ? 'Đã xác minh' : 'Chưa xác minh'}
                      </div>
                    </div>
                    <div className="border border-white/[0.05] bg-black p-3">
                      <div className="flex items-center gap-2 text-[9px] uppercase text-neutral-300">
                        <Clock className="h-3.5 w-3.5" /> Cập nhật
                      </div>
                      <div className="mt-1 text-xs font-bold text-white">
                        {formatDateTime(selectedAdminUser.updatedAt)}
                      </div>
                    </div>
                  </div>

                  {isSelectedStaff && (
                    <form onSubmit={saveStaffProfile} className="border border-amber-500/20 bg-black p-4 space-y-3">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <div className="text-[10px] font-black uppercase tracking-widest text-amber-300">
                            Staff profile
                          </div>
                          <p className="mt-1 text-[10px] text-neutral-300">
                            Ma nhan vien, vi tri, trang thai va rap duy nhat cua STAFF.
                          </p>
                        </div>
                        {isProfileLoading && <RefreshCw className="h-4 w-4 animate-spin text-amber-300" />}
                      </div>

                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <label className="space-y-1.5">
                          <span className="text-[9px] font-black uppercase tracking-widest text-neutral-200">Ma nhan vien *</span>
                          <input
                            value={profileForm.employeeCode}
                            onChange={(event) => updateProfileForm('employeeCode', event.target.value)}
                            placeholder="EMP001"
                            className="w-full border border-white/[0.06] bg-neutral-950 px-3 py-2.5 text-xs text-white outline-none transition placeholder:text-neutral-200 focus:border-amber-400"
                          />
                        </label>
                        <label className="space-y-1.5">
                          <span className="text-[9px] font-black uppercase tracking-widest text-neutral-200">Vi tri *</span>
                          <input
                            value={profileForm.position}
                            onChange={(event) => updateProfileForm('position', event.target.value)}
                            placeholder="Gate Staff"
                            className="w-full border border-white/[0.06] bg-neutral-950 px-3 py-2.5 text-xs text-white outline-none transition placeholder:text-neutral-200 focus:border-amber-400"
                          />
                        </label>
                      </div>

                      <div className="grid grid-cols-3 gap-2">
                        {STAFF_PROFILE_STATUS_OPTIONS.map((status) => (
                          <button
                            key={status}
                            type="button"
                            onClick={() => updateProfileForm('status', status)}
                            className={`border px-2 py-2 text-[8px] font-black uppercase tracking-wider transition ${
                              profileForm.status === status
                                ? 'border-amber-400 bg-amber-500 text-black'
                                : 'border-white/[0.06] bg-neutral-950 text-neutral-300 hover:border-amber-400 hover:text-amber-300'
                            }`}
                          >
                            {status}
                          </button>
                        ))}
                      </div>

                      {selectedStaffProfile?.cinemaName && (
                        <div className="border border-white/[0.05] bg-neutral-950 px-3 py-2 text-[10px] text-neutral-200">
                          Rap: <span className="font-bold text-neutral-200">{selectedStaffProfile.cinemaName}</span>
                        </div>
                      )}
                      {profileError && <div className="text-[10px] font-bold text-rose-300">{profileError}</div>}
                      <button
                        type="submit"
                        disabled={isProfileSaving}
                        className="flex items-center justify-center gap-2 border border-amber-400 bg-amber-500 px-4 py-2.5 text-[10px] font-black uppercase tracking-widest text-black transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {isProfileSaving ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <BadgeCheck className="h-3.5 w-3.5" />}
                        {selectedStaffProfile ? 'Cap nhat profile' : 'Tao profile'}
                      </button>
                    </form>
                  )}

                  <div className="border border-white/[0.05] bg-black p-4 space-y-3">
                    <div className="text-[10px] font-black uppercase tracking-widest text-neutral-200">
                      Đổi trạng thái tài khoản
                    </div>
                    {isViewingCurrentAdmin && (
                      <div className="border border-amber-500/20 bg-amber-500/10 px-3 py-2 text-[10px] font-bold leading-relaxed text-amber-200">
                        Không thể đổi trạng thái của chính tài khoản admin đang đăng nhập.
                      </div>
                    )}
                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                      {USER_STATUS_OPTIONS.map((status) => (
                        <button
                          key={status}
                          type="button"
                          disabled={isViewingCurrentAdmin || isUserStatusSaving || selectedAdminUser.status === status}
                          onClick={() => handleUpdateAdminUserStatus(selectedAdminUser.id, status)}
                          className={`min-w-0 border px-2 py-2 text-[8px] font-black uppercase tracking-wider transition disabled:cursor-not-allowed disabled:opacity-45 ${
                            selectedAdminUser.status === status
                              ? 'border-amber-400 bg-amber-500 text-black'
                              : 'border-white/[0.06] bg-neutral-950 text-neutral-300 hover:border-amber-400 hover:text-amber-300'
                          }`}
                        >
                          <span className="block truncate">{status}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="text-[10px] leading-relaxed text-neutral-300">
                    Tạo lúc:{' '}
                    <span className="font-mono text-neutral-300">
                      {formatDateTime(selectedAdminUser.createdAt)}
                    </span>
                  </div>
                </>
              )}
            </div>
          ) : (
            <div className="py-20 text-center text-neutral-300">
              <Users className="mx-auto h-8 w-8 text-neutral-300" />
              <p className="mt-3 text-xs font-mono uppercase tracking-widest">Chưa chọn người dùng</p>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
}


