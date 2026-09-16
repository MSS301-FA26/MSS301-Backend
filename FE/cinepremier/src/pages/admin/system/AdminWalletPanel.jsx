import React, { useEffect, useState, useCallback } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  Wallet, ArrowDownCircle, ArrowUpCircle, Clock, CheckCircle2,
  XCircle, RefreshCw, Search, Eye, ChevronDown, DollarSign,
  TrendingUp, TrendingDown, AlertCircle, CreditCard
} from "lucide-react";
import { adminService } from "../../../services/adminService";

const fmtVND = (v) => `${Number(v || 0).toLocaleString("vi-VN")}đ`;
const fmtDate = (d) => d ? new Date(d).toLocaleString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" }) : "—";

const STATUS_CONFIG = {
  PENDING:  { label: "Chờ xử lý",   color: "#f59e0b", bg: "rgba(245,158,11,0.12)", icon: Clock },
  PAID:     { label: "Đã chuyển",    color: "#10b981", bg: "rgba(16,185,129,0.12)",  icon: CheckCircle2 },
  REJECTED: { label: "Từ chối",      color: "#f43f5e", bg: "rgba(244,63,94,0.12)",   icon: XCircle },
};

const StatusBadge = ({ status }) => {
  const cfg = STATUS_CONFIG[status] || STATUS_CONFIG.PENDING;
  const Icon = cfg.icon;
  return (
    <span style={{
      display: "inline-flex", alignItems: "center", gap: 5,
      padding: "4px 10px", borderRadius: 6, fontSize: 10, fontWeight: 700,
      color: cfg.color, background: cfg.bg, border: `1px solid ${cfg.color}30`,
      fontFamily: "Inter, sans-serif", textTransform: "uppercase", letterSpacing: "0.06em",
    }}>
      <Icon size={11} /> {cfg.label}
    </span>
  );
};

const KpiCard = ({ label, value, icon: Icon, accent, sub }) => (
  <div style={{
    background: "linear-gradient(135deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%)",
    border: "1px solid rgba(255,255,255,0.07)", borderRadius: 12, padding: "20px 22px",
    display: "flex", flexDirection: "column", gap: 10, position: "relative", overflow: "hidden",
  }}>
    <div style={{
      position: "absolute", top: -30, right: -30, width: 90, height: 90, borderRadius: "50%",
      background: `radial-gradient(circle, ${accent}25 0%, transparent 70%)`, pointerEvents: "none",
    }} />
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
      <span style={{ fontSize: 10, fontWeight: 700, letterSpacing: "0.08em", textTransform: "uppercase", color: "rgba(255,255,255,0.35)", fontFamily: "Inter, sans-serif" }}>
        {label}
      </span>
      <div style={{ width: 32, height: 32, borderRadius: 8, background: `${accent}18`, display: "flex", alignItems: "center", justifyContent: "center", border: `1px solid ${accent}30` }}>
        <Icon size={14} color={accent} />
      </div>
    </div>
    <div>
      <span style={{ display: "block", fontSize: 22, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif", letterSpacing: "-0.02em" }}>{value}</span>
      {sub && <span style={{ fontSize: 10, color: "rgba(255,255,255,0.25)", fontFamily: "Inter, sans-serif", marginTop: 4, display: "block" }}>{sub}</span>}
    </div>
  </div>
);

export default function AdminWalletPanel({ ctx }) {
  const { activeTab, getAdminToken, showToast, playPulseSound } = ctx;

  const [dashboard, setDashboard] = useState(null);
  const [withdrawals, setWithdrawals] = useState([]);
  const [wdPage, setWdPage] = useState({ page: 0, totalPages: 1 });
  const [statusFilter, setStatusFilter] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [processing, setProcessing] = useState(null);
  const [selectedWd, setSelectedWd] = useState(null);
  const [processNote, setProcessNote] = useState("");
  const [processMethod] = useState("BANK_TRANSFER");
  const [txPage, setTxPage] = useState(0);

  const fetchDashboard = useCallback(async () => {
    const token = getAdminToken?.(false);
    if (!token) return;
    try {
      const data = await adminService.getWalletDashboard(token);
      setDashboard(data);
    } catch (e) { console.error("Dashboard fetch failed", e); }
  }, [getAdminToken]);

  const fetchWithdrawals = useCallback(async (page = 0) => {
    const token = getAdminToken?.(false);
    if (!token) return;
    setIsLoading(true);
    try {
      const params = { page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const data = await adminService.getAdminWithdrawals(token, params);
      const items = data?.content || data?.items || (Array.isArray(data) ? data : []);
      setWithdrawals(items);
      setWdPage({ page: data?.page ?? data?.number ?? page, totalPages: data?.totalPages ?? 1 });
    } catch (e) { console.error("Withdrawals fetch failed", e); }
    finally { setIsLoading(false); }
  }, [getAdminToken, statusFilter]);

  useEffect(() => {
    if (activeTab !== "cinewallet") return;
    fetchDashboard();
    fetchWithdrawals(0);
  }, [activeTab, fetchDashboard, fetchWithdrawals]);

  const handleApprove = async (wd) => {
    const token = getAdminToken?.();
    if (!token) return;
    setProcessing(wd.id);
    try {
      await adminService.approveWithdrawal(token, wd.id, { method: processMethod, note: processNote });
      showToast?.("Đã duyệt yêu cầu rút tiền #" + wd.id);
      playPulseSound?.(600, "sine", 0.06);
      setSelectedWd(null); setProcessNote("");
      fetchWithdrawals(wdPage.page);
      fetchDashboard();
    } catch (e) { showToast?.("Lỗi: " + (e.message || "Không thể duyệt")); }
    finally { setProcessing(null); }
  };

  const handleReject = async (wd) => {
    const token = getAdminToken?.();
    if (!token) return;
    setProcessing(wd.id);
    try {
      await adminService.rejectWithdrawal(token, wd.id, { method: null, note: processNote || "Từ chối bởi admin" });
      showToast?.("Đã từ chối yêu cầu rút tiền #" + wd.id);
      playPulseSound?.(300, "sine", 0.06);
      setSelectedWd(null); setProcessNote("");
      fetchWithdrawals(wdPage.page);
      fetchDashboard();
    } catch (e) { showToast?.("Lỗi: " + (e.message || "Không thể từ chối")); }
    finally { setProcessing(null); }
  };

  if (activeTab !== "cinewallet") return null;

  const kpis = dashboard ? [
    { label: "Tổng số dư ví", value: fmtVND(dashboard.totalWalletBalance), icon: Wallet, accent: "#f59e0b", sub: `${dashboard.totalWallets || 0} ví hoạt động` },
    { label: "Tổng hoàn vào ví", value: fmtVND(dashboard.totalRefundedToWallet), icon: ArrowDownCircle, accent: "#10b981", sub: "Từ hủy suất chiếu" },
    { label: "Tổng đã rút", value: fmtVND(dashboard.totalWithdrawnAmount), icon: ArrowUpCircle, accent: "#06b6d4", sub: "Staff đã chuyển khoản" },
    { label: "Đang chờ rút", value: `${dashboard.pendingWithdrawalsCount} yêu cầu`, icon: Clock, accent: "#a855f7", sub: fmtVND(dashboard.pendingWithdrawalsAmount) },
  ] : [];

  return (
    <motion.div
      key="panel-cinewallet"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -12 }}
      transition={{ duration: 0.3 }}
      style={{ display: "flex", flexDirection: "column", gap: 24 }}
    >
      {/* Header */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{ width: 36, height: 36, borderRadius: 10, background: "linear-gradient(135deg, #f59e0b, #d97706)", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: "0 0 20px #f59e0b44" }}>
            <Wallet size={18} color="#fff" />
          </div>
          <div>
            <p style={{ margin: 0, fontSize: 10, fontWeight: 700, color: "#f59e0b", letterSpacing: "0.14em", textTransform: "uppercase", fontFamily: "Inter, sans-serif" }}>CineWallet Management</p>
            <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif", letterSpacing: "-0.02em" }}>Quản lý ví & rút tiền</h1>
          </div>
        </div>
        <button
          onClick={() => { fetchDashboard(); fetchWithdrawals(wdPage.page); playPulseSound?.(500, "sine", 0.03); }}
          style={{
            display: "flex", alignItems: "center", gap: 6, padding: "8px 16px", borderRadius: 8, border: "1px solid rgba(255,255,255,0.1)",
            background: "rgba(255,255,255,0.04)", color: "rgba(255,255,255,0.6)", fontSize: 11, fontWeight: 700, cursor: "pointer",
            fontFamily: "Inter, sans-serif", transition: "all 0.2s",
          }}
        >
          <RefreshCw size={12} /> Làm mới
        </button>
      </div>

      {/* KPI Cards */}
      {dashboard && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 16 }}>
          {kpis.map((kpi) => <KpiCard key={kpi.label} {...kpi} />)}
        </div>
      )}

      {/* Withdrawal Requests Table */}
      <div style={{
        background: "linear-gradient(160deg, rgba(255,255,255,0.025) 0%, rgba(255,255,255,0.008) 100%)",
        border: "1px solid rgba(255,255,255,0.07)", borderRadius: 14, overflow: "hidden",
      }}>
        {/* Table header */}
        <div style={{
          display: "flex", alignItems: "center", justifyContent: "space-between", padding: "16px 20px",
          borderBottom: "1px solid rgba(255,255,255,0.06)",
        }}>
          <div>
            <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: "0.15em", textTransform: "uppercase", color: "#a855f7", fontFamily: "Inter, sans-serif" }}>
              Withdrawal Requests
            </span>
            <h3 style={{ margin: "4px 0 0", fontSize: 14, fontWeight: 700, color: "rgba(255,255,255,0.85)", fontFamily: "Inter, sans-serif" }}>
              Yêu cầu rút tiền
            </h3>
          </div>
          <div style={{ display: "flex", gap: 4, background: "rgba(255,255,255,0.04)", borderRadius: 8, padding: 3, border: "1px solid rgba(255,255,255,0.06)" }}>
            {[
              { val: "", label: "Tất cả" },
              { val: "PENDING", label: "Chờ xử lý" },
              { val: "PAID", label: "Đã chuyển" },
              { val: "REJECTED", label: "Từ chối" },
            ].map((opt) => (
              <button
                key={opt.val}
                onClick={() => { setStatusFilter(opt.val); }}
                style={{
                  padding: "6px 14px", fontSize: 10, fontWeight: 700, fontFamily: "Inter, sans-serif",
                  borderRadius: 6, border: "none", cursor: "pointer", transition: "all 0.2s",
                  background: statusFilter === opt.val ? "linear-gradient(135deg, #a855f7, #7c3aed)" : "transparent",
                  color: statusFilter === opt.val ? "#fff" : "rgba(255,255,255,0.35)",
                  boxShadow: statusFilter === opt.val ? "0 2px 8px rgba(168,85,247,0.3)" : "none",
                }}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Table body */}
        <div style={{ padding: "0 20px 16px" }}>
          {isLoading ? (
            <div style={{ padding: 40, textAlign: "center", color: "rgba(255,255,255,0.3)", fontSize: 12, fontFamily: "Inter, sans-serif" }}>
              <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: "linear" }} style={{ display: "inline-block", marginBottom: 8 }}>
                <RefreshCw size={16} />
              </motion.div>
              <br />Đang tải...
            </div>
          ) : withdrawals.length === 0 ? (
            <div style={{ padding: 40, textAlign: "center", color: "rgba(255,255,255,0.2)", fontSize: 11, fontFamily: "Inter, sans-serif" }}>
              <AlertCircle size={20} style={{ marginBottom: 8, opacity: 0.3 }} /><br />
              Chưa có yêu cầu rút tiền nào
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 12 }}>
              {withdrawals.map((wd) => (
                <motion.div
                  key={wd.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  style={{
                    display: "grid", gridTemplateColumns: "1fr 140px 200px 120px 100px",
                    alignItems: "center", gap: 12, padding: "14px 16px",
                    background: "rgba(255,255,255,0.02)", border: "1px solid rgba(255,255,255,0.05)",
                    borderRadius: 10, transition: "border-color 0.2s",
                    cursor: "pointer",
                  }}
                  whileHover={{ borderColor: "rgba(168,85,247,0.3)" }}
                  onClick={() => setSelectedWd(selectedWd?.id === wd.id ? null : wd)}
                >
                  {/* User info */}
                  <div>
                    <span style={{ fontSize: 12, fontWeight: 700, color: "#fff", fontFamily: "Inter, sans-serif", display: "block" }}>
                      {wd.userName || wd.userEmail || "—"}
                    </span>
                    <span style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", fontFamily: "Inter, sans-serif" }}>
                      {wd.userEmail}
                    </span>
                  </div>
                  {/* Amount */}
                  <span style={{ fontSize: 13, fontWeight: 800, color: "#f59e0b", fontFamily: "Inter, sans-serif" }}>
                    {fmtVND(wd.amount)}
                  </span>
                  {/* Bank info */}
                  <div>
                    <span style={{ fontSize: 11, fontWeight: 600, color: "rgba(255,255,255,0.7)", fontFamily: "Inter, sans-serif", display: "block" }}>
                      {wd.bankName}
                    </span>
                    <span style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", fontFamily: "Inter, sans-serif" }}>
                      {wd.accountNumber} · {wd.accountHolder}
                    </span>
                  </div>
                  {/* Date */}
                  <span style={{ fontSize: 10, color: "rgba(255,255,255,0.35)", fontFamily: "Inter, sans-serif" }}>
                    {fmtDate(wd.createdAt)}
                  </span>
                  {/* Status */}
                  <StatusBadge status={wd.status} />
                </motion.div>
              ))}
            </div>
          )}

          {/* Pagination */}
          {wdPage.totalPages > 1 && (
            <div style={{ display: "flex", justifyContent: "center", gap: 6, marginTop: 16 }}>
              {Array.from({ length: wdPage.totalPages }, (_, i) => (
                <button
                  key={i}
                  onClick={() => fetchWithdrawals(i)}
                  style={{
                    width: 30, height: 30, borderRadius: 6, border: "1px solid rgba(255,255,255,0.1)",
                    background: i === wdPage.page ? "#a855f7" : "rgba(255,255,255,0.04)",
                    color: i === wdPage.page ? "#fff" : "rgba(255,255,255,0.4)",
                    fontSize: 11, fontWeight: 700, cursor: "pointer",
                  }}
                >
                  {i + 1}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Process Modal */}
      <AnimatePresence>
        {selectedWd && selectedWd.status === "PENDING" && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: "fixed", inset: 0, zIndex: 9999, display: "flex", alignItems: "center", justifyContent: "center",
              background: "rgba(0,0,0,0.7)", backdropFilter: "blur(8px)",
            }}
            onClick={(e) => { if (e.target === e.currentTarget) setSelectedWd(null); }}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              style={{
                width: 480, background: "linear-gradient(160deg, #141414, #0a0a0a)",
                border: "1px solid rgba(255,255,255,0.1)", borderRadius: 16, padding: 28,
                boxShadow: "0 40px 80px rgba(0,0,0,0.8)",
              }}
            >
              <h3 style={{ margin: "0 0 20px", fontSize: 16, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif" }}>
                Xử lý yêu cầu rút tiền #{selectedWd.id}
              </h3>

              <div style={{ display: "flex", flexDirection: "column", gap: 12, marginBottom: 20 }}>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "10px 14px", background: "rgba(255,255,255,0.03)", borderRadius: 8, border: "1px solid rgba(255,255,255,0.06)" }}>
                  <span style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", fontFamily: "Inter, sans-serif" }}>Khách hàng</span>
                  <span style={{ fontSize: 11, fontWeight: 700, color: "#fff", fontFamily: "Inter, sans-serif" }}>{selectedWd.userName}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "10px 14px", background: "rgba(245,158,11,0.06)", borderRadius: 8, border: "1px solid rgba(245,158,11,0.15)" }}>
                  <span style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", fontFamily: "Inter, sans-serif" }}>Số tiền</span>
                  <span style={{ fontSize: 14, fontWeight: 800, color: "#f59e0b", fontFamily: "Inter, sans-serif" }}>{fmtVND(selectedWd.amount)}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "10px 14px", background: "rgba(255,255,255,0.03)", borderRadius: 8, border: "1px solid rgba(255,255,255,0.06)" }}>
                  <span style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", fontFamily: "Inter, sans-serif" }}>Ngân hàng</span>
                  <span style={{ fontSize: 11, fontWeight: 700, color: "#fff", fontFamily: "Inter, sans-serif" }}>{selectedWd.bankName} · {selectedWd.accountNumber}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", padding: "10px 14px", background: "rgba(255,255,255,0.03)", borderRadius: 8, border: "1px solid rgba(255,255,255,0.06)" }}>
                  <span style={{ fontSize: 11, color: "rgba(255,255,255,0.4)", fontFamily: "Inter, sans-serif" }}>Chủ TK</span>
                  <span style={{ fontSize: 11, fontWeight: 700, color: "#fff", fontFamily: "Inter, sans-serif" }}>{selectedWd.accountHolder}</span>
                </div>
              </div>

              {/* Process form */}
              <div style={{ display: "flex", flexDirection: "column", gap: 10, marginBottom: 20 }}>
                <div style={{ padding: "10px 14px", background: "rgba(6,182,212,0.06)", borderRadius: 8, border: "1px solid rgba(6,182,212,0.2)", display: "flex", alignItems: "center", gap: 8 }}>
                  <CreditCard size={13} color="#06b6d4" />
                  <span style={{ fontSize: 11, fontWeight: 700, color: "#06b6d4", fontFamily: "Inter, sans-serif" }}>Chuyển khoản ngân hàng</span>
                </div>

                <label style={{ fontSize: 10, fontWeight: 700, color: "rgba(255,255,255,0.4)", textTransform: "uppercase", letterSpacing: "0.1em", fontFamily: "Inter, sans-serif", marginTop: 4 }}>
                  Ghi chú / Mã giao dịch
                </label>
                <textarea
                  value={processNote}
                  onChange={(e) => setProcessNote(e.target.value)}
                  placeholder="Mã giao dịch ngân hàng, ghi chú xử lý..."
                  rows={3}
                  style={{
                    padding: "10px 12px", borderRadius: 8, border: "1px solid rgba(255,255,255,0.1)",
                    background: "rgba(255,255,255,0.04)", color: "#fff", fontSize: 12,
                    fontFamily: "Inter, sans-serif", outline: "none", resize: "none",
                  }}
                />
              </div>

              <div style={{ display: "flex", gap: 10 }}>
                <button
                  onClick={() => handleApprove(selectedWd)}
                  disabled={processing === selectedWd.id}
                  style={{
                    flex: 1, padding: "12px 0", borderRadius: 8, border: "none", cursor: "pointer",
                    background: "linear-gradient(135deg, #10b981, #059669)", color: "#fff",
                    fontSize: 12, fontWeight: 800, fontFamily: "Inter, sans-serif",
                    opacity: processing === selectedWd.id ? 0.5 : 1, transition: "opacity 0.2s",
                    display: "flex", alignItems: "center", justifyContent: "center", gap: 6,
                  }}
                >
                  <CheckCircle2 size={14} /> Duyệt & Đã chuyển
                </button>
                <button
                  onClick={() => handleReject(selectedWd)}
                  disabled={processing === selectedWd.id}
                  style={{
                    flex: 1, padding: "12px 0", borderRadius: 8, border: "1px solid rgba(244,63,94,0.3)",
                    background: "rgba(244,63,94,0.1)", color: "#f43f5e", cursor: "pointer",
                    fontSize: 12, fontWeight: 800, fontFamily: "Inter, sans-serif",
                    opacity: processing === selectedWd.id ? 0.5 : 1, transition: "opacity 0.2s",
                    display: "flex", alignItems: "center", justifyContent: "center", gap: 6,
                  }}
                >
                  <XCircle size={14} /> Từ chối
                </button>
              </div>

              <button
                onClick={() => setSelectedWd(null)}
                style={{
                  width: "100%", marginTop: 10, padding: "10px 0", borderRadius: 8,
                  border: "1px solid rgba(255,255,255,0.08)", background: "transparent",
                  color: "rgba(255,255,255,0.4)", fontSize: 11, fontWeight: 700,
                  fontFamily: "Inter, sans-serif", cursor: "pointer",
                }}
              >
                Đóng
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Recent Activity */}
      {(() => {
        const recentTxs = (dashboard?.recentTransactions || []).filter((tx) => tx.type !== "WITHDRAWAL_PAID" && Math.abs(Number(tx.amount || 0)) > 0);
        if (recentTxs.length === 0) return null;
        const txPageSize = 10;
        const txTotalPages = Math.ceil(recentTxs.length / txPageSize) || 1;
        const currentTxs = recentTxs.slice(txPage * txPageSize, (txPage + 1) * txPageSize);

        return (
          <div style={{
            background: "linear-gradient(160deg, rgba(255,255,255,0.025) 0%, rgba(255,255,255,0.008) 100%)",
            border: "1px solid rgba(255,255,255,0.07)", borderRadius: 14, padding: 20,
          }}>
            <div style={{ marginBottom: 14, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: "0.15em", textTransform: "uppercase", color: "#10b981", fontFamily: "Inter, sans-serif" }}>
                  Recent Activity
                </span>
                <h3 style={{ margin: "4px 0 0", fontSize: 14, fontWeight: 700, color: "rgba(255,255,255,0.85)", fontFamily: "Inter, sans-serif" }}>
                  Giao dịch ví gần đây
                </h3>
              </div>
              <span style={{ fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "Inter, sans-serif" }}>
                Trang {txPage + 1}/{txTotalPages} ({recentTxs.length} giao dịch)
              </span>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
              {currentTxs.map((tx, i) => (
                <div
                  key={tx.id || i}
                  style={{
                    display: "flex", alignItems: "center", justifyContent: "space-between",
                    padding: "10px 14px", background: "rgba(255,255,255,0.02)",
                    borderRadius: 8, border: "1px solid rgba(255,255,255,0.04)",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{
                      width: 28, height: 28, borderRadius: 7, display: "flex", alignItems: "center", justifyContent: "center",
                      background: tx.type === "REFUND_CREDIT" ? "rgba(16,185,129,0.12)" : "rgba(245,158,11,0.12)",
                      border: `1px solid ${tx.type === "REFUND_CREDIT" ? "rgba(16,185,129,0.25)" : "rgba(245,158,11,0.25)"}`,
                    }}>
                      {tx.type === "REFUND_CREDIT"
                        ? <ArrowDownCircle size={13} color="#10b981" />
                        : <ArrowUpCircle size={13} color="#f59e0b" />
                      }
                    </div>
                    <div>
                      <span style={{ fontSize: 11, fontWeight: 700, color: "#fff", fontFamily: "Inter, sans-serif", display: "block" }}>
                        {tx.type === "REFUND_CREDIT" ? "Hoàn tiền" : tx.type === "WITHDRAWAL_HOLD" ? "Yêu cầu rút" : tx.type === "WITHDRAWAL_PAID" ? "Đã chuyển khoản" : tx.type === "WITHDRAWAL_REJECTED" ? "Hoàn lại (từ chối)" : tx.type}
                      </span>
                      <span style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", fontFamily: "Inter, sans-serif" }}>
                        {tx.referenceCode} · {fmtDate(tx.createdAt)}
                      </span>
                    </div>
                  </div>
                  <span style={{
                    fontSize: 12, fontWeight: 800, fontFamily: "Inter, sans-serif",
                    color: (tx.amount > 0 || tx.type === "REFUND_CREDIT") ? "#10b981" : "#f43f5e",
                  }}>
                    {tx.amount > 0 ? "+" : ""}{fmtVND(Math.abs(tx.amount))}
                  </span>
                </div>
              ))}
            </div>

            {txTotalPages > 1 && (
              <div style={{ display: "flex", justifyContent: "center", alignItems: "center", gap: 6, marginTop: 16 }}>
                <button
                  disabled={txPage === 0}
                  onClick={() => setTxPage((p) => Math.max(0, p - 1))}
                  style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid rgba(255,255,255,0.1)", background: "rgba(255,255,255,0.04)", color: txPage === 0 ? "rgba(255,255,255,0.2)" : "#fff", fontSize: 11, fontWeight: 700, cursor: txPage === 0 ? "not-allowed" : "pointer" }}
                >
                  Trước
                </button>
                {Array.from({ length: txTotalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => setTxPage(i)}
                    style={{
                      width: 30, height: 30, borderRadius: 6, border: "1px solid rgba(255,255,255,0.1)",
                      background: i === txPage ? "#10b981" : "rgba(255,255,255,0.04)",
                      color: i === txPage ? "#fff" : "rgba(255,255,255,0.4)",
                      fontSize: 11, fontWeight: 700, cursor: "pointer",
                    }}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  disabled={txPage >= txTotalPages - 1}
                  onClick={() => setTxPage((p) => Math.min(txTotalPages - 1, p + 1))}
                  style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid rgba(255,255,255,0.1)", background: "rgba(255,255,255,0.04)", color: txPage >= txTotalPages - 1 ? "rgba(255,255,255,0.2)" : "#fff", fontSize: 11, fontWeight: 700, cursor: txPage >= txTotalPages - 1 ? "not-allowed" : "pointer" }}
                >
                  Sau
                </button>
              </div>
            )}
          </div>
        );
      })()}
    </motion.div>
  );
}
