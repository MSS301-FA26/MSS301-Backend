import React, { useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  AlertCircle, ArrowDownCircle, ArrowUpCircle, BanknoteIcon,
  Building2, CheckCircle2, Clock, CreditCard, RefreshCw,
  Wallet, XCircle, User,
} from 'lucide-react';
import { staffService } from '../../services/staffService';

/* ─── Helpers ─── */
const fmtVND = (v) => `${Number(v || 0).toLocaleString('vi-VN')}đ`;
const fmtDate = (d) =>
  d
    ? new Date(d).toLocaleString('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      })
    : '—';

/* ─── Status config ─── */
const STATUS_CFG = {
  PENDING:  { label: 'Chờ xử lý',  color: '#f59e0b', bg: 'rgba(245,158,11,0.12)',  icon: Clock },
  PAID:     { label: 'Đã chuyển',  color: '#10b981', bg: 'rgba(16,185,129,0.12)',   icon: CheckCircle2 },
  REJECTED: { label: 'Từ chối',    color: '#f43f5e', bg: 'rgba(244,63,94,0.12)',    icon: XCircle },
};

function StatusBadge({ status }) {
  const cfg = STATUS_CFG[status] || STATUS_CFG.PENDING;
  const Icon = cfg.icon;
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      padding: '3px 9px', borderRadius: 6, fontSize: 10, fontWeight: 700,
      color: cfg.color, background: cfg.bg, border: `1px solid ${cfg.color}30`,
      fontFamily: 'Inter, sans-serif', textTransform: 'uppercase', letterSpacing: '0.06em',
      position: 'relative',
    }}>
      {status === 'PENDING' && (
        <span style={{
          width: 6, height: 6, borderRadius: '50%', background: '#ef4444',
          boxShadow: '0 0 6px #ef4444', animation: 'ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite',
        }} />
      )}
      <Icon size={10} /> {cfg.label}
    </span>
  );
}

function KpiCard({ label, value, icon: Icon, accent, sub, hasPendingBadge }) {
  return (
    <div style={{
      background: 'linear-gradient(135deg, rgba(255,255,255,0.04), rgba(255,255,255,0.01))',
      border: '1px solid rgba(255,255,255,0.07)', borderRadius: 12, padding: '18px 20px',
      display: 'flex', flexDirection: 'column', gap: 10, position: 'relative', overflow: 'hidden',
    }}>
      {hasPendingBadge && (
        <span style={{
          position: 'absolute', top: 8, right: 8, width: 9, height: 9,
          borderRadius: '50%', background: '#ef4444', border: '1.5px solid #000',
          boxShadow: '0 0 8px #ef4444'
        }} />
      )}
      <div style={{
        position: 'absolute', top: -28, right: -28, width: 80, height: 80, borderRadius: '50%',
        background: `radial-gradient(circle, ${accent}20 0%, transparent 70%)`, pointerEvents: 'none',
      }} />
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ fontSize: 9, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'rgba(255,255,255,0.3)', fontFamily: 'Inter, sans-serif' }}>{label}</span>
        <div style={{ width: 30, height: 30, borderRadius: 8, background: `${accent}18`, display: 'flex', alignItems: 'center', justifyContent: 'center', border: `1px solid ${accent}25` }}>
          <Icon size={13} color={accent} />
        </div>
      </div>
      <div>
        <span style={{ display: 'block', fontSize: 20, fontWeight: 800, color: '#fff', fontFamily: 'Inter, sans-serif', letterSpacing: '-0.02em' }}>{value}</span>
        {sub && <span style={{ fontSize: 9, color: 'rgba(255,255,255,0.2)', fontFamily: 'Inter, sans-serif', marginTop: 3, display: 'block' }}>{sub}</span>}
      </div>
    </div>
  );
}

function InfoRow({ label, value, accent }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      padding: '9px 13px', background: accent ? `${accent}08` : 'rgba(255,255,255,0.025)',
      borderRadius: 8, border: `1px solid ${accent ? `${accent}18` : 'rgba(255,255,255,0.05)'}`,
    }}>
      <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.38)', fontFamily: 'Inter, sans-serif' }}>{label}</span>
      <span style={{ fontSize: 12, fontWeight: 700, color: accent || '#fff', fontFamily: 'Inter, sans-serif' }}>{value}</span>
    </div>
  );
}

export default function StaffWalletPanel({ token, showToast, onPendingCountChange }) {
  const [dashboard, setDashboard]       = useState(null);
  const [withdrawals, setWithdrawals]   = useState([]);
  const [wdPage, setWdPage]             = useState({ page: 0, totalPages: 1 });
  const [statusFilter, setStatusFilter] = useState('');
  const [isLoading, setIsLoading]       = useState(false);
  const [selectedWd, setSelectedWd]     = useState(null);
  const [processing, setProcessing]     = useState(null);
  const [processNote, setProcessNote]   = useState('');
  const [rejectNote, setRejectNote]     = useState('');
  const [showRejectConfirm, setShowRejectConfirm] = useState(false);
  const [txPage, setTxPage]             = useState(0);

  const fetchDashboard = useCallback(async () => {
    if (!token) return;
    try {
      const data = await staffService.getWalletDashboard(token);
      setDashboard(data);
      if (onPendingCountChange && data && typeof data.pendingWithdrawalsCount === 'number') {
        onPendingCountChange(data.pendingWithdrawalsCount);
      }
    } catch (e) { console.error('Dashboard fetch failed', e); }
  }, [token, onPendingCountChange]);

  const fetchWithdrawals = useCallback(async (page = 0) => {
    if (!token) return;
    setIsLoading(true);
    try {
      const params = { page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const data = await staffService.getWithdrawals(token, params);
      const items = data?.content || data?.items || (Array.isArray(data) ? data : []);
      setWithdrawals(items);
      setWdPage({ page: data?.page ?? data?.number ?? page, totalPages: data?.totalPages ?? 1 });
    } catch (e) { console.error('Withdrawals fetch failed', e); }
    finally { setIsLoading(false); }
  }, [token, statusFilter]);

  useEffect(() => {
    fetchDashboard();
    fetchWithdrawals(0);
  }, [fetchDashboard, fetchWithdrawals]);

  const handleApprove = async () => {
    if (!selectedWd || !token) return;
    setProcessing(selectedWd.id);
    try {
      await staffService.approveWithdrawal(token, selectedWd.id, {
        method: 'BANK_TRANSFER',
        note: processNote || null,
      });
      showToast?.('Đã duyệt & xác nhận chuyển khoản #' + selectedWd.id);
      setSelectedWd(null); setProcessNote(''); setShowRejectConfirm(false);
      fetchWithdrawals(wdPage.page); fetchDashboard();
    } catch (e) { showToast?.('Lỗi: ' + (e.message || 'Không thể duyệt')); }
    finally { setProcessing(null); }
  };

  const handleReject = async () => {
    if (!selectedWd || !token) return;
    setProcessing(selectedWd.id);
    try {
      await staffService.rejectWithdrawal(token, selectedWd.id, {
        method: null,
        note: rejectNote || 'Từ chối bởi staff',
      });
      showToast?.('Đã từ chối yêu cầu rút tiền #' + selectedWd.id);
      setSelectedWd(null); setRejectNote(''); setShowRejectConfirm(false);
      fetchWithdrawals(wdPage.page); fetchDashboard();
    } catch (e) { showToast?.('Lỗi: ' + (e.message || 'Không thể từ chối')); }
    finally { setProcessing(null); }
  };

  const kpis = dashboard ? [
    { label: 'Tổng đã rút', value: fmtVND(dashboard.totalWithdrawnAmount), icon: ArrowUpCircle, accent: '#06b6d4', sub: 'Đã chuyển khoản' },
    { label: 'Hoàn vào ví', value: fmtVND(dashboard.totalRefundedToWallet), icon: ArrowDownCircle, accent: '#10b981', sub: 'Từ hủy suất chiếu' },
    { label: 'Chờ xử lý', value: `${dashboard.pendingWithdrawalsCount || 0} yêu cầu`, icon: Clock, accent: '#a855f7', sub: fmtVND(dashboard.pendingWithdrawalsAmount), hasPendingBadge: (dashboard.pendingWithdrawalsCount || 0) > 0 },
  ] : [];

  const closeModal = () => { setSelectedWd(null); setShowRejectConfirm(false); setProcessNote(''); setRejectNote(''); };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22, padding: '4px 0' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 38, height: 38, borderRadius: 10, background: 'linear-gradient(135deg, #f59e0b, #d97706)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 0 20px #f59e0b40' }}>
            <Wallet size={18} color="#fff" />
          </div>
          <div>
            <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: '0.15em', textTransform: 'uppercase', color: '#f59e0b', fontFamily: 'Inter, sans-serif' }}>CineWallet — Staff</span>
            <h2 style={{ margin: 0, fontSize: 18, fontWeight: 800, color: '#fff', fontFamily: 'Inter, sans-serif' }}>Quản lý ví &amp; rút tiền</h2>
          </div>
        </div>
        <button
          onClick={() => { fetchDashboard(); fetchWithdrawals(wdPage.page); }}
          style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontSize: 11, fontWeight: 600, cursor: 'pointer', fontFamily: 'Inter, sans-serif' }}
        >
          <RefreshCw size={12} /> Làm mới
        </button>
      </div>

      {/* KPIs */}
      {dashboard && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 14 }}>
          {kpis.map((k) => <KpiCard key={k.label} {...k} />)}
        </div>
      )}

      {/* Withdrawal Table */}
      <div style={{ background: 'linear-gradient(160deg, rgba(255,255,255,0.025), rgba(255,255,255,0.008))', border: '1px solid rgba(255,255,255,0.07)', borderRadius: 14, overflow: 'hidden' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 20px', borderBottom: '1px solid rgba(255,255,255,0.06)', flexWrap: 'wrap', gap: 10 }}>
          <div>
            <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: '0.15em', textTransform: 'uppercase', color: '#a855f7', fontFamily: 'Inter, sans-serif' }}>Withdrawal Requests</span>
            <h3 style={{ margin: '3px 0 0', fontSize: 14, fontWeight: 700, color: 'rgba(255,255,255,0.85)', fontFamily: 'Inter, sans-serif' }}>Yêu cầu rút tiền</h3>
          </div>
          <div style={{ display: 'flex', gap: 4, background: 'rgba(255,255,255,0.04)', borderRadius: 8, padding: 3, border: '1px solid rgba(255,255,255,0.06)' }}>
            {[{ val: '', label: 'Tất cả' }, { val: 'PENDING', label: 'Chờ xử lý' }, { val: 'PAID', label: 'Đã chuyển' }, { val: 'REJECTED', label: 'Từ chối' }].map((opt) => (
              <button key={opt.val} onClick={() => setStatusFilter(opt.val)}
                style={{
                  position: 'relative',
                  padding: '5px 12px', fontSize: 10, fontWeight: 700, fontFamily: 'Inter, sans-serif', borderRadius: 6, border: 'none', cursor: 'pointer', transition: 'all 0.2s',
                  background: statusFilter === opt.val ? 'linear-gradient(135deg, #a855f7, #7c3aed)' : 'transparent',
                  color: statusFilter === opt.val ? '#fff' : 'rgba(255,255,255,0.35)',
                  boxShadow: statusFilter === opt.val ? '0 2px 8px rgba(168,85,247,0.3)' : 'none'
                }}>
                {opt.label}
                {opt.val === 'PENDING' && (dashboard?.pendingWithdrawalsCount || 0) > 0 && (
                  <span style={{
                    position: 'absolute', top: -2, right: -2, width: 7, height: 7,
                    borderRadius: '50%', background: '#ef4444', border: '1px solid #000',
                    boxShadow: '0 0 6px #ef4444'
                  }} />
                )}
              </button>
            ))}
          </div>
        </div>

        <div style={{ padding: '0 20px 16px' }}>
          {isLoading ? (
            <div style={{ padding: 40, textAlign: 'center', color: 'rgba(255,255,255,0.3)', fontSize: 12, fontFamily: 'Inter, sans-serif' }}>
              <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: 'linear' }} style={{ display: 'inline-block', marginBottom: 8 }}><RefreshCw size={16} /></motion.div>
              <br />Đang tải...
            </div>
          ) : withdrawals.length === 0 ? (
            <div style={{ padding: 40, textAlign: 'center', color: 'rgba(255,255,255,0.2)', fontSize: 11, fontFamily: 'Inter, sans-serif' }}>
              <AlertCircle size={20} style={{ marginBottom: 8, opacity: 0.3 }} /><br />Chưa có yêu cầu rút tiền
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 12 }}>
              {withdrawals.map((wd) => (
                <motion.div key={wd.id} initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }}
                  style={{ display: 'grid', gridTemplateColumns: 'minmax(100px,1fr) 120px minmax(140px,1fr) 95px 100px', alignItems: 'center', gap: 10, padding: '13px 15px', background: selectedWd?.id === wd.id ? 'rgba(168,85,247,0.06)' : 'rgba(255,255,255,0.02)', border: `1px solid ${selectedWd?.id === wd.id ? 'rgba(168,85,247,0.3)' : 'rgba(255,255,255,0.05)'}`, borderRadius: 10, cursor: wd.status === 'PENDING' ? 'pointer' : 'default', transition: 'all 0.2s' }}
                  whileHover={wd.status === 'PENDING' ? { borderColor: 'rgba(168,85,247,0.22)' } : {}}
                  onClick={() => { if (wd.status !== 'PENDING') return; setSelectedWd(selectedWd?.id === wd.id ? null : wd); setProcessNote(''); setRejectNote(''); setShowRejectConfirm(false); }}
                >
                  <div style={{ minWidth: 0 }}>
                    <span style={{ fontSize: 12, fontWeight: 700, color: '#fff', fontFamily: 'Inter, sans-serif', display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{wd.userName || wd.userEmail || '—'}</span>
                    <span style={{ fontSize: 9, color: 'rgba(255,255,255,0.28)', fontFamily: 'Inter, sans-serif', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>{wd.userEmail}</span>
                  </div>
                  <span style={{ fontSize: 13, fontWeight: 800, color: '#f59e0b', fontFamily: 'Inter, sans-serif' }}>{fmtVND(wd.amount)}</span>
                  <div>
                    <span style={{ fontSize: 11, fontWeight: 600, color: 'rgba(255,255,255,0.7)', display: 'block', fontFamily: 'Inter, sans-serif' }}>{wd.bankName}</span>
                    <span style={{ fontSize: 9, color: 'rgba(255,255,255,0.28)', fontFamily: 'Inter, sans-serif' }}>{wd.accountNumber} · {wd.accountHolder}</span>
                  </div>
                  <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.3)', fontFamily: 'Inter, sans-serif' }}>{fmtDate(wd.createdAt)}</span>
                  <StatusBadge status={wd.status} />
                </motion.div>
              ))}
            </div>
          )}

          {wdPage.totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 16 }}>
              {Array.from({ length: wdPage.totalPages }, (_, i) => (
                <button key={i} onClick={() => fetchWithdrawals(i)}
                  style={{ width: 30, height: 30, borderRadius: 6, border: '1px solid rgba(255,255,255,0.1)', background: i === wdPage.page ? '#a855f7' : 'rgba(255,255,255,0.04)', color: i === wdPage.page ? '#fff' : 'rgba(255,255,255,0.4)', fontSize: 11, fontWeight: 700, cursor: 'pointer' }}>
                  {i + 1}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Process Modal */}
      <AnimatePresence>
        {selectedWd && selectedWd.status === 'PENDING' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0,0,0,0.78)', backdropFilter: 'blur(10px)' }}
            onClick={(e) => { if (e.target === e.currentTarget) closeModal(); }}
          >
            <motion.div initial={{ scale: 0.88, opacity: 0, y: 20 }} animate={{ scale: 1, opacity: 1, y: 0 }} exit={{ scale: 0.88, opacity: 0, y: 20 }} transition={{ type: 'spring', damping: 22, stiffness: 320 }}
              style={{ width: 480, maxWidth: 'calc(100vw - 32px)', background: 'linear-gradient(160deg, #141414, #0a0a0a)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 18, padding: 28, boxShadow: '0 40px 80px rgba(0,0,0,0.88)' }}
            >
              {/* Header */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 20 }}>
                <div style={{ width: 36, height: 36, borderRadius: 9, background: 'linear-gradient(135deg, #f59e0b, #d97706)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <BanknoteIcon size={16} color="#fff" />
                </div>
                <div>
                  <p style={{ margin: 0, fontSize: 9, fontWeight: 700, color: '#f59e0b', letterSpacing: '0.12em', textTransform: 'uppercase', fontFamily: 'Inter, sans-serif' }}>Xử lý yêu cầu</p>
                  <h3 style={{ margin: 0, fontSize: 15, fontWeight: 800, color: '#fff', fontFamily: 'Inter, sans-serif' }}>Rút tiền #{selectedWd.id}</h3>
                </div>
              </div>

              {/* Info */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 7, marginBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
                  <User size={10} color="rgba(255,255,255,0.3)" />
                  <span style={{ fontSize: 9, fontWeight: 700, color: 'rgba(255,255,255,0.3)', textTransform: 'uppercase', letterSpacing: '0.1em', fontFamily: 'Inter, sans-serif' }}>Khách hàng</span>
                </div>
                <InfoRow label="Họ tên" value={selectedWd.userName || '—'} />
                <InfoRow label="Số tiền" value={fmtVND(selectedWd.amount)} accent="#f59e0b" />
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 6, marginBottom: 2 }}>
                  <Building2 size={10} color="rgba(255,255,255,0.3)" />
                  <span style={{ fontSize: 9, fontWeight: 700, color: 'rgba(255,255,255,0.3)', textTransform: 'uppercase', letterSpacing: '0.1em', fontFamily: 'Inter, sans-serif' }}>Ngân hàng</span>
                </div>
                <InfoRow label="Ngân hàng" value={selectedWd.bankName || '—'} />
                <InfoRow label="Số tài khoản" value={selectedWd.accountNumber || '—'} />
                <InfoRow label="Chủ tài khoản" value={selectedWd.accountHolder || '—'} />
              </div>

              {/* Method indicator */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '9px 13px', background: 'rgba(6,182,212,0.07)', borderRadius: 8, border: '1px solid rgba(6,182,212,0.2)', marginBottom: 14 }}>
                <CreditCard size={13} color="#06b6d4" />
                <div>
                  <span style={{ fontSize: 10, fontWeight: 700, color: '#06b6d4', fontFamily: 'Inter, sans-serif', display: 'block' }}>Chuyển khoản ngân hàng</span>
                  <span style={{ fontSize: 9, color: 'rgba(255,255,255,0.28)', fontFamily: 'Inter, sans-serif' }}>Phương thức duy nhất</span>
                </div>
              </div>

              {!showRejectConfirm ? (
                <>
                  <div style={{ marginBottom: 14 }}>
                    <label style={{ fontSize: 9, fontWeight: 700, color: 'rgba(255,255,255,0.35)', textTransform: 'uppercase', letterSpacing: '0.1em', fontFamily: 'Inter, sans-serif', display: 'block', marginBottom: 7 }}>
                      Ghi chú / Mã giao dịch
                    </label>
                    <textarea value={processNote} onChange={(e) => setProcessNote(e.target.value)}
                      placeholder="Nhập mã giao dịch ngân hàng hoặc ghi chú..." rows={2}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid rgba(255,255,255,0.09)', boxSizing: 'border-box', background: 'rgba(255,255,255,0.03)', color: '#fff', fontSize: 12, fontFamily: 'Inter, sans-serif', outline: 'none', resize: 'none' }}
                    />
                  </div>
                  <div style={{ display: 'flex', gap: 10, marginBottom: 10 }}>
                    <button onClick={handleApprove} disabled={processing === selectedWd.id}
                      style={{ flex: 1, padding: '12px 0', borderRadius: 9, border: 'none', cursor: 'pointer', background: 'linear-gradient(135deg, #10b981, #059669)', color: '#fff', fontSize: 12, fontWeight: 800, fontFamily: 'Inter, sans-serif', opacity: processing === selectedWd.id ? 0.5 : 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7, boxShadow: '0 4px 16px rgba(16,185,129,0.25)' }}>
                      {processing === selectedWd.id ? 'Đang xử lý...' : <><CheckCircle2 size={13} /> Duyệt &amp; Đã chuyển khoản</>}
                    </button>
                    <button onClick={() => setShowRejectConfirm(true)} disabled={processing === selectedWd.id}
                      style={{ flex: 1, padding: '12px 0', borderRadius: 9, border: '1px solid rgba(244,63,94,0.28)', background: 'rgba(244,63,94,0.08)', color: '#f43f5e', cursor: 'pointer', fontSize: 12, fontWeight: 800, fontFamily: 'Inter, sans-serif', opacity: processing === selectedWd.id ? 0.5 : 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7 }}>
                      <XCircle size={13} /> Từ chối
                    </button>
                  </div>
                </>
              ) : (
                <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} style={{ marginBottom: 10 }}>
                  <div style={{ padding: '10px 13px', background: 'rgba(244,63,94,0.07)', borderRadius: 8, border: '1px solid rgba(244,63,94,0.2)', marginBottom: 12 }}>
                    <span style={{ fontSize: 11, color: '#f43f5e', fontWeight: 700, fontFamily: 'Inter, sans-serif' }}>⚠ Số tiền sẽ được hoàn lại vào ví khách hàng</span>
                  </div>
                  <label style={{ fontSize: 9, fontWeight: 700, color: 'rgba(255,255,255,0.35)', textTransform: 'uppercase', letterSpacing: '0.1em', fontFamily: 'Inter, sans-serif', display: 'block', marginBottom: 7 }}>Lý do từ chối</label>
                  <textarea value={rejectNote} onChange={(e) => setRejectNote(e.target.value)} placeholder="Nhập lý do từ chối..." rows={2}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 8, boxSizing: 'border-box', border: '1px solid rgba(244,63,94,0.2)', background: 'rgba(244,63,94,0.04)', color: '#fff', fontSize: 12, fontFamily: 'Inter, sans-serif', outline: 'none', resize: 'none', marginBottom: 12 }}
                  />
                  <div style={{ display: 'flex', gap: 10 }}>
                    <button onClick={() => setShowRejectConfirm(false)}
                      style={{ flex: 1, padding: '11px 0', borderRadius: 8, border: '1px solid rgba(255,255,255,0.09)', background: 'transparent', color: 'rgba(255,255,255,0.45)', cursor: 'pointer', fontSize: 11, fontWeight: 700, fontFamily: 'Inter, sans-serif' }}>
                      Quay lại
                    </button>
                    <button onClick={handleReject} disabled={processing === selectedWd.id}
                      style={{ flex: 1, padding: '11px 0', borderRadius: 8, border: 'none', background: 'linear-gradient(135deg, #f43f5e, #e11d48)', color: '#fff', cursor: 'pointer', fontSize: 11, fontWeight: 800, fontFamily: 'Inter, sans-serif', opacity: processing === selectedWd.id ? 0.5 : 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                      {processing === selectedWd.id ? 'Đang xử lý...' : <><XCircle size={12} /> Xác nhận từ chối</>}
                    </button>
                  </div>
                </motion.div>
              )}

              <button onClick={closeModal}
                style={{ width: '100%', padding: '9px 0', borderRadius: 8, border: '1px solid rgba(255,255,255,0.07)', background: 'transparent', color: 'rgba(255,255,255,0.3)', fontSize: 11, fontWeight: 600, fontFamily: 'Inter, sans-serif', cursor: 'pointer', marginTop: 8 }}>
                Đóng
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Recent transactions */}
      {(() => {
        const recentTxs = (dashboard?.recentTransactions || []).filter((tx) => tx.type !== 'WITHDRAWAL_PAID' && Math.abs(Number(tx.amount || 0)) > 0);
        if (recentTxs.length === 0) return null;
        const txPageSize = 10;
        const txTotalPages = Math.ceil(recentTxs.length / txPageSize) || 1;
        const currentTxs = recentTxs.slice(txPage * txPageSize, (txPage + 1) * txPageSize);

        return (
          <div style={{ background: 'linear-gradient(160deg, rgba(255,255,255,0.025), rgba(255,255,255,0.008))', border: '1px solid rgba(255,255,255,0.07)', borderRadius: 14, padding: 20 }}>
            <div style={{ marginBottom: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: '0.15em', textTransform: 'uppercase', color: '#10b981', fontFamily: 'Inter, sans-serif' }}>Recent Activity</span>
                <h3 style={{ margin: '3px 0 0', fontSize: 13, fontWeight: 700, color: 'rgba(255,255,255,0.85)', fontFamily: 'Inter, sans-serif' }}>Giao dịch ví gần đây</h3>
              </div>
              <span style={{ fontSize: 10, color: 'rgba(255,255,255,0.3)', fontFamily: 'Inter, sans-serif' }}>
                Trang {txPage + 1}/{txTotalPages} ({recentTxs.length} giao dịch)
              </span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {currentTxs.map((tx, i) => (
                <div key={tx.id || i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '9px 13px', background: 'rgba(255,255,255,0.02)', borderRadius: 8, border: '1px solid rgba(255,255,255,0.04)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ width: 26, height: 26, borderRadius: 7, display: 'flex', alignItems: 'center', justifyContent: 'center', background: tx.type === 'REFUND_CREDIT' ? 'rgba(16,185,129,0.12)' : 'rgba(245,158,11,0.12)', border: `1px solid ${tx.type === 'REFUND_CREDIT' ? 'rgba(16,185,129,0.22)' : 'rgba(245,158,11,0.22)'}` }}>
                      {tx.type === 'REFUND_CREDIT' ? <ArrowDownCircle size={12} color="#10b981" /> : <ArrowUpCircle size={12} color="#f59e0b" />}
                    </div>
                    <div>
                      <span style={{ fontSize: 11, fontWeight: 700, color: '#fff', fontFamily: 'Inter, sans-serif', display: 'block' }}>
                        {tx.type === 'REFUND_CREDIT' ? 'Hoàn tiền' : tx.type === 'WITHDRAWAL_HOLD' ? 'Yêu cầu rút' : tx.type === 'WITHDRAWAL_REJECTED' ? 'Hoàn lại (từ chối)' : tx.type}
                      </span>
                      <span style={{ fontSize: 9, color: 'rgba(255,255,255,0.25)', fontFamily: 'Inter, sans-serif' }}>{tx.referenceCode} · {fmtDate(tx.createdAt)}</span>
                    </div>
                  </div>
                  <span style={{ fontSize: 12, fontWeight: 800, fontFamily: 'Inter, sans-serif', color: (tx.amount > 0 || tx.type === 'REFUND_CREDIT') ? '#10b981' : '#f43f5e' }}>
                    {tx.amount > 0 ? '+' : ''}{fmtVND(Math.abs(tx.amount))}
                  </span>
                </div>
              ))}
            </div>

            {txTotalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 6, marginTop: 16 }}>
                <button
                  disabled={txPage === 0}
                  onClick={() => setTxPage((p) => Math.max(0, p - 1))}
                  style={{ padding: '4px 10px', borderRadius: 6, border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.04)', color: txPage === 0 ? 'rgba(255,255,255,0.2)' : '#fff', fontSize: 11, fontWeight: 700, cursor: txPage === 0 ? 'not-allowed' : 'pointer' }}
                >
                  Trước
                </button>
                {Array.from({ length: txTotalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => setTxPage(i)}
                    style={{ width: 30, height: 30, borderRadius: 6, border: '1px solid rgba(255,255,255,0.1)', background: i === txPage ? '#10b981' : 'rgba(255,255,255,0.04)', color: i === txPage ? '#fff' : 'rgba(255,255,255,0.4)', fontSize: 11, fontWeight: 700, cursor: 'pointer' }}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  disabled={txPage >= txTotalPages - 1}
                  onClick={() => setTxPage((p) => Math.min(txTotalPages - 1, p + 1))}
                  style={{ padding: '4px 10px', borderRadius: 6, border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(255,255,255,0.04)', color: txPage >= txTotalPages - 1 ? 'rgba(255,255,255,0.2)' : '#fff', fontSize: 11, fontWeight: 700, cursor: txPage >= txTotalPages - 1 ? 'not-allowed' : 'pointer' }}
                >
                  Sau
                </button>
              </div>
            )}
          </div>
        );
      })()}
    </div>
  );
}
