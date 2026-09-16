import React, { useEffect, useState } from "react";
import { motion } from "motion/react";
import {
  DollarSign, Ticket, ReceiptText, BarChart2, Users,
  TrendingUp, TrendingDown, BadgeDollarSign, Activity,
  ArrowUpRight, Cpu, Shield, RefreshCw, ShoppingBag,
  Coffee, Film, Layers, PieChart as PieIcon, Sparkles
} from "lucide-react";
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, LabelList, PieChart, Pie, Cell, Legend, AreaChart, Area
} from "recharts";
import { adminService } from "../../../services/adminService";

const CHART_COLORS = ["#f59e0b", "#10b981", "#06b6d4", "#a855f7", "#f43f5e", "#3b82f6", "#ec4899"];
const GRID_COLOR = "rgba(255,255,255,0.04)";
const TICK_STYLE = { fill: "#71717a", fontSize: 10, fontFamily: "Inter, sans-serif" };

const RANGE_PRESETS = [
  { days: 7, label: "7D" },
  { days: 30, label: "30D" },
  { days: 90, label: "90D" },
];

const SECTION_TABS = [
  { key: "ALL", label: "📊 Tất cả báo cáo" },
  { key: "TICKETS", label: "🎫 Vé & Doanh thu phim" },
  { key: "FNB", label: "🍿 Bắp nước & F&B" },
  { key: "LOYALTY", label: "💎 Điểm thành viên" },
  { key: "LOGS", label: "📋 Audit Logs" },
];

const fmtVND = (v) => `${Number(v || 0).toLocaleString("vi-VN")}đ`;
const fmtNumber = (v) => Number(v || 0).toLocaleString("vi-VN");
const fmtCompact = (v) => {
  const n = Number(v || 0);
  if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toLocaleString("vi-VN", { maximumFractionDigits: 1 })}B`;
  if (n >= 1_000_000) return `${(n / 1_000_000).toLocaleString("vi-VN", { maximumFractionDigits: 1 })}M`;
  if (n >= 1_000) return `${(n / 1_000).toLocaleString("vi-VN", { maximumFractionDigits: 0 })}K`;
  return String(n);
};

const isoDaysAgo = (days) => {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

const ChartTooltip = ({ active, payload, label, formatter }) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: "rgba(9,9,11,0.96)",
      border: "1px solid rgba(255,255,255,0.08)",
      borderRadius: 0,
      padding: "10px 14px",
      fontFamily: "Inter, sans-serif",
      boxShadow: "0 20px 40px rgba(0,0,0,0.8)",
      backdropFilter: "blur(12px)",
      zIndex: 50,
    }}>
      <p style={{ color: "#f59e0b", fontWeight: 700, fontSize: 11, marginBottom: 4 }}>
        {label ?? payload[0]?.name}
      </p>
      {payload.map((entry, idx) => (
        <p key={entry.dataKey || entry.name || idx} style={{ color: "#fff", fontWeight: 600, fontSize: 11, margin: 0, display: "flex", gap: 6, alignItems: "center" }}>
          <span style={{ width: 6, height: 6, borderRadius: "50%", backgroundColor: entry.color || "#f59e0b", display: "inline-block" }} />
          {formatter ? formatter(entry) : `${entry.name}: ${entry.value}`}
        </p>
      ))}
    </div>
  );
};

const EmptyChart = ({ message = "Chưa có dữ liệu trong khoảng này" }) => (
  <div style={{
    height: 180, display: "flex", flexDirection: "column",
    alignItems: "center", justifyContent: "center", gap: 10,
    border: "1px dashed rgba(255,255,255,0.06)",
    borderRadius: 2
  }}>
    <Activity size={22} color="rgba(255,255,255,0.12)" />
    <span style={{ color: "rgba(255,255,255,0.25)", fontSize: 10, fontFamily: "Inter, sans-serif", letterSpacing: "0.12em", textTransform: "uppercase" }}>
      {message}
    </span>
  </div>
);

const KpiCard = ({ label, value, icon: Icon, accent = "#f59e0b", sub, delay = 0 }) => (
  <motion.div
    initial={{ opacity: 0, y: 8 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.3, delay }}
    style={{
      border: "1px solid rgba(255,255,255,0.06)",
      background: "linear-gradient(145deg, rgba(14,14,17,0.9) 0%, rgba(7,7,10,0.95) 100%)",
      padding: "16px 18px",
      display: "flex",
      flexDirection: "column",
      gap: 10,
      position: "relative",
      overflow: "hidden",
      boxShadow: "0 8px 24px rgba(0,0,0,0.4)"
    }}
  >
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
      <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: "0.18em", textTransform: "uppercase", color: "rgba(255,255,255,0.4)", fontFamily: "monospace" }}>
        {label}
      </span>
      <div style={{ width: 28, height: 28, display: "flex", alignItems: "center", justifyContent: "center", border: `1px solid ${accent}35`, background: `${accent}0f` }}>
        <Icon size={14} color={accent} />
      </div>
    </div>
    <div>
      <span style={{ display: "block", fontSize: 22, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif", letterSpacing: "-0.02em", lineHeight: 1 }}>
        {value}
      </span>
      {sub && (
        <span style={{ fontSize: 9, color: "rgba(255,255,255,0.3)", fontFamily: "Inter, sans-serif", marginTop: 6, display: "block" }}>
          {sub}
        </span>
      )}
    </div>
  </motion.div>
);

const SectionHeader = ({ eyebrow, title, badge }) => (
  <div style={{ marginBottom: 16, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
    <div>
      <span style={{ fontSize: 8.5, fontWeight: 800, letterSpacing: "0.2em", textTransform: "uppercase", color: "rgba(255,255,255,0.35)", fontFamily: "monospace" }}>
        {eyebrow}
      </span>
      <h3 style={{ margin: "4px 0 0", fontSize: 14, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif" }}>
        {title}
      </h3>
    </div>
    {badge && (
      <span style={{ fontSize: 9, fontWeight: 700, padding: "3px 8px", background: "rgba(245,158,11,0.1)", border: "1px solid rgba(245,158,11,0.3)", color: "#f59e0b", fontFamily: "monospace" }}>
        {badge}
      </span>
    )}
  </div>
);

const ChartCard = ({ children, style = {} }) => (
  <div style={{
    border: "1px solid rgba(255,255,255,0.06)",
    background: "rgba(7,7,10,0.6)",
    padding: "20px 22px 18px",
    boxShadow: "0 10px 30px rgba(0,0,0,0.35)",
    ...style,
  }}>
    {children}
  </div>
);

export default function AdminOverviewPanel({ ctx }) {
  const { activeTab, getAdminToken, playPulseSound } = ctx;

  const [rangeDays, setRangeDays] = useState(30);
  const [activeSection, setActiveSection] = useState("ALL");
  const [isLoadingReports, setIsLoadingReports] = useState(false);
  const [revenueReport, setRevenueReport] = useState(null);
  const [loyaltyReport, setLoyaltyReport] = useState(null);
  const [topMovies, setTopMovies] = useState([]);
  const [dailyOccupancy, setDailyOccupancy] = useState([]);
  const [concessionReport, setConcessionReport] = useState(null);
  const [occupancyGroupBy, setOccupancyGroupBy] = useState("day");
  const [recentAuditLogs, setRecentAuditLogs] = useState([]);
  const [lastRefresh, setLastRefresh] = useState(new Date());

  useEffect(() => {
    if (activeTab !== "overview") return undefined;
    const token = getAdminToken?.(false);
    if (!token) return undefined;
    let cancelled = false;
    const params = { from: isoDaysAgo(rangeDays), to: isoDaysAgo(0) };
    setIsLoadingReports(true);
    Promise.all([
      adminService.getRevenueReport(token, params).catch(() => null),
      adminService.getLoyaltyReport(token, { from: `${params.from}T00:00:00`, to: `${params.to}T23:59:59` }).catch(() => null),
      adminService.getTopMovies(token, { ...params, limit: 10 }).catch(() => []),
      adminService.getAuditLogs(token, { page: 0, size: 8 }).catch(() => null),
      adminService.getDailyOccupancy(token, params).catch(() => []),
      adminService.getConcessionSales(token, params).catch(() => null),
    ])
      .then(([revenue, loyalty, movies, audit, daily, concessions]) => {
        if (cancelled) return;
        setRevenueReport(revenue || null);
        setLoyaltyReport(loyalty || null);
        setTopMovies(Array.isArray(movies) ? movies : []);
        setDailyOccupancy(Array.isArray(daily) ? daily : []);
        setRecentAuditLogs((audit?.items || []).map((log) => ({
          id: log.id,
          time: log.createdAt ? new Date(log.createdAt).toLocaleTimeString("vi-VN") : "--:--:--",
          action: log.action,
          target: `${log.targetType}${log.targetId ? ` #${log.targetId}` : ""}${log.detail ? ` — ${log.detail}` : ""}`,
          user: log.actorEmail || "hệ thống",
        })));
        setConcessionReport(concessions || null);
        setLastRefresh(new Date());
      })
      .finally(() => { if (!cancelled) setIsLoadingReports(false); });
    return () => { cancelled = true; };
  }, [activeTab, rangeDays, getAdminToken]);

  if (activeTab !== "overview") return null;

  const effectiveRevenue = revenueReport || { totalRevenue: 0, totalTransactions: 0, totalTicketsSold: 0 };
  const effectiveLoyalty = loyaltyReport || { newMembers: 0, totalIssuedPoints: 0, totalBurnedPoints: 0, pointFlowRatio: 0 };
  const effectiveMovies = topMovies;
  const effectiveAuditLogs = recentAuditLogs;

  const revenueByMovie = effectiveMovies
    .map((m) => ({ name: m.movieTitle, revenue: Number(m.revenue || 0), tickets: Number(m.ticketsSold || 0) }))
    .sort((a, b) => b.revenue - a.revenue);

  // Grouped Occupancy data
  const GROUP_LABELS = { day: "ngày", week: "tuần", month: "tháng" };
  const occupancyChartData = (() => {
    if (occupancyGroupBy === "day") {
      return dailyOccupancy.map((d) => ({
        label: d.date ? d.date.slice(5).split("-").reverse().join("/") : "",
        rate: Math.round((d.occupancyRate || 0) * 10) / 10,
        sold: Number(d.ticketsSold || 0),
        capacity: Number(d.totalCapacity || 0),
        shows: Number(d.totalShowtimes || 0),
      }));
    }
    const buckets = new Map();
    dailyOccupancy.forEach((d) => {
      if (!d.date) return;
      const date = new Date(`${d.date}T00:00:00`);
      let key; let label;
      if (occupancyGroupBy === "week") {
        const monday = new Date(date);
        monday.setDate(date.getDate() - ((date.getDay() + 6) % 7));
        key = monday.toISOString().slice(0, 10);
        label = `Tuần ${String(monday.getDate()).padStart(2, "0")}/${String(monday.getMonth() + 1).padStart(2, "0")}`;
      } else {
        key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
        label = `Th${date.getMonth() + 1}/${date.getFullYear()}`;
      }
      const bucket = buckets.get(key) || { label, sold: 0, capacity: 0, shows: 0 };
      bucket.sold += Number(d.ticketsSold || 0);
      bucket.capacity += Number(d.totalCapacity || 0);
      bucket.shows += Number(d.totalShowtimes || 0);
      buckets.set(key, bucket);
    });
    return [...buckets.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, b]) => ({
        ...b,
        rate: b.capacity > 0 ? Math.round((b.sold / b.capacity) * 1000) / 10 : 0,
      }));
  })();

  // Ticket share donut
  const ticketShareSource = [...effectiveMovies].sort((a, b) => b.ticketsSold - a.ticketsSold);
  const ticketShare = ticketShareSource.slice(0, 5).map((m) => ({ name: m.movieTitle, value: Number(m.ticketsSold || 0) }));
  const restTickets = ticketShareSource.slice(5).reduce((s, m) => s + Number(m.ticketsSold || 0), 0);
  if (restTickets > 0) ticketShare.push({ name: "Phim khác", value: restTickets });
  const totalTicketsInShare = ticketShare.reduce((s, t) => s + t.value, 0);

  // Provider breakdown
  const PROVIDER_LABELS = { CASH: "Tiền mặt (Quầy)", VNPAY: "VNPay", MOMO: "MoMo", MOCK: "Demo Checkout" };
  const providerBreakdown = (effectiveRevenue.byProvider || [])
    .filter((p) => Number(p.revenue) > 0)
    .map((p) => ({ ...p, label: PROVIDER_LABELS[p.provider] || p.provider }));

  // F&B Concessions Data
  const fnbTotalItems = Number(concessionReport?.totalItemsSold || 0);
  const fnbTotalRevenue = Number(concessionReport?.totalRevenue || 0);
  const fnbTotalOrders = Number(concessionReport?.totalOrders || 0);
  const fnbAvgOrderValue = Number(concessionReport?.averageOrderValue || 0);

  const concessionItemsData = (concessionReport?.lines || [])
    .map((item) => ({
      name: item.name,
      quantity: Number(item.quantity || 0),
      revenue: Number(item.revenue || 0),
    }))
    .sort((a, b) => b.quantity - a.quantity)
    .slice(0, 8);

  const concessionDailyData = (concessionReport?.daily || []).map((d) => ({
    label: d.date ? String(d.date).slice(5).split("-").reverse().join("/") : "",
    quantity: Number(d.quantity || 0),
    revenue: Number(d.revenue || 0),
    orders: Number(d.orderCount || 0),
  }));

  const SOURCE_LABELS = {
    BOOKING_LINKED: "Đi kèm khi đặt vé",
    STANDALONE: "Đặt tự do / Tại quầy",
  };
  const concessionSourcesData = (concessionReport?.sources || [])
    .filter((s) => Number(s.quantity || 0) > 0)
    .map((s) => ({
      name: SOURCE_LABELS[s.source] || s.source,
      value: Number(s.quantity || 0),
      revenue: Number(s.revenue || 0),
      orders: Number(s.orderCount || 0),
    }));

  const totalFnBSourceQuantity = concessionSourcesData.reduce((acc, curr) => acc + curr.value, 0);

  // Top overall KPI Strip
  const primaryKpis = [
    { label: "TỔNG DOANH THU PHIM & VÉ", value: fmtVND(effectiveRevenue.totalRevenue), icon: DollarSign, accent: "#f59e0b", sub: `${rangeDays} ngày gần nhất` },
    { label: "SỐ LƯỢNG VÉ BÁN RA",        value: `${fmtNumber(effectiveRevenue.totalTicketsSold)} vé`, icon: Ticket, accent: "#06b6d4", sub: `Trung bình ~${Math.round(effectiveRevenue.totalTicketsSold / Math.max(1, rangeDays))} vé/ngày` },
    { label: "BẮP NƯỚC BÁN RA",           value: `${fmtNumber(fnbTotalItems)} món`, icon: ShoppingBag, accent: "#10b981", sub: `Doanh thu F&B: ${fmtVND(fnbTotalRevenue)}` },
    { label: "DOANH THU BẮP NƯỚC (F&B)",   value: fmtVND(fnbTotalRevenue), icon: BadgeDollarSign, accent: "#a855f7", sub: `${fmtNumber(fnbTotalOrders)} đơn hàng F&B` },
    { label: "GIAO DỊCH THÀNH CÔNG",    value: `${fmtNumber(effectiveRevenue.totalTransactions)} GD`, icon: ReceiptText, accent: "#f43f5e", sub: `${rangeDays} ngày gần nhất` },
  ];

  const loyaltyHealthLabel = effectiveLoyalty.totalBurnedPoints > 0
    ? `${Number(effectiveLoyalty.pointFlowRatio || 0).toFixed(2)}x`
    : effectiveLoyalty.totalIssuedPoints > 0 ? "Chưa có điểm tiêu" : "0x";

  const loyaltyKpis = [
    { label: "THÀNH VIÊN MỚI",  value: fmtNumber(effectiveLoyalty.newMembers),        icon: Users,          accent: "#06b6d4" },
    { label: "ĐIỂM PHÁT RA",    value: fmtNumber(effectiveLoyalty.totalIssuedPoints), icon: TrendingUp,     accent: "#10b981" },
    { label: "ĐIỂM THU VỀ",     value: fmtNumber(effectiveLoyalty.totalBurnedPoints), icon: TrendingDown,   accent: "#f59e0b" },
    { label: "ISSUED / BURNED", value: loyaltyHealthLabel,                             icon: BadgeDollarSign, accent: "#a855f7" },
  ];

  const fnbKpis = [
    { label: "SỐ LƯỢNG BẮP NƯỚC BÁN RA", value: `${fmtNumber(fnbTotalItems)} món`, icon: ShoppingBag, accent: "#10b981", sub: "Tổng món & combo tiêu thụ" },
    { label: "DOANH THU BẮP NƯỚC",         value: fmtVND(fnbTotalRevenue), icon: DollarSign, accent: "#f59e0b", sub: "Tổng doanh thu F&B" },
    { label: "SỐ ĐƠN HÀNG F&B",            value: `${fmtNumber(fnbTotalOrders)} đơn`, icon: ReceiptText, accent: "#06b6d4", sub: "Lượt mua bắp nước" },
    { label: "GIÁ TRỊ ĐƠN F&B TRUNG BÌNH",  value: fmtVND(fnbAvgOrderValue), icon: Sparkles, accent: "#a855f7", sub: "Giá trị trung bình / đơn" },
  ];

  const logColor = (action) => {
    const a = (action || "").toUpperCase();
    if (a.includes("ERROR") || a.includes("FAIL") || a.includes("DELETE")) return "#f43f5e";
    if (a.includes("WARN") || a.includes("UPDATE") || a.includes("EDIT")) return "#f59e0b";
    if (a.includes("CREATE") || a.includes("ADD") || a.includes("SUCCESS")) return "#10b981";
    return "#06b6d4";
  };

  const showTicketsSec = activeSection === "ALL" || activeSection === "TICKETS";
  const showFnbSec     = activeSection === "ALL" || activeSection === "FNB";
  const showLoyaltySec = activeSection === "ALL" || activeSection === "LOYALTY";
  const showLogsSec    = activeSection === "ALL" || activeSection === "LOGS";

  return (
    <motion.div
      key="panel-overview"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -12 }}
      transition={{ duration: 0.3 }}
      style={{ display: "flex", flexDirection: "column", gap: 28 }}
    >
      {/* ── Header ── */}
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", flexWrap: "wrap", gap: 16 }}>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
            <div style={{ width: 34, height: 34, border: "1px solid rgba(245,158,11,0.3)", background: "rgba(245,158,11,0.08)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <BarChart2 size={16} color="#f59e0b" />
            </div>
            <div>
              <p style={{ margin: 0, fontSize: 8.5, fontWeight: 800, color: "rgba(255,255,255,0.35)", letterSpacing: "0.2em", textTransform: "uppercase", fontFamily: "monospace" }}>
                Analytics & Performance Report
              </p>
              <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif", letterSpacing: "-0.01em", lineHeight: 1.2 }}>
                Tổng quan hệ thống & Báo cáo số liệu
              </h1>
            </div>
          </div>
          <p style={{ margin: 0, fontSize: 10, color: "rgba(255,255,255,0.3)", fontFamily: "Inter, sans-serif", display: "flex", alignItems: "center", gap: 6 }}>
            Cập nhật lần cuối: {lastRefresh.toLocaleTimeString("vi-VN")}
            {isLoadingReports && (
              <span style={{ display: "inline-flex", alignItems: "center", gap: 4, color: "#f59e0b", marginLeft: 6 }}>
                <motion.span animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: "linear" }} style={{ display: "inline-block" }}>
                  <RefreshCw size={9} />
                </motion.span>
                Đang làm mới dữ liệu…
              </span>
            )}
          </p>
        </div>

        {/* Range presets */}
        <div style={{ display: "flex", alignItems: "center", border: "1px solid rgba(255,255,255,0.08)", background: "rgba(0,0,0,0.4)" }}>
          {RANGE_PRESETS.map((preset) => {
            const isActive = rangeDays === preset.days;
            return (
              <button
                key={preset.days}
                type="button"
                onClick={() => { playPulseSound?.(500, "sine", 0.03); setRangeDays(preset.days); }}
                style={{
                  padding: "8px 20px", fontSize: 10, fontWeight: 800,
                  fontFamily: "Inter, sans-serif", border: "none",
                  borderRight: "1px solid rgba(255,255,255,0.06)",
                  cursor: "pointer", transition: "all 0.15s",
                  background: isActive ? "rgba(245,158,11,0.15)" : "transparent",
                  color: isActive ? "#f59e0b" : "rgba(255,255,255,0.4)",
                }}
              >
                {preset.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Category Section Filter Tabs ── */}
      <div style={{ display: "flex", alignItems: "center", gap: 8, overflowX: "auto", paddingBottom: 4 }}>
        {SECTION_TABS.map((tab) => {
          const isActive = activeSection === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => { playPulseSound?.(540, "sine", 0.03); setActiveSection(tab.key); }}
              style={{
                padding: "7px 14px",
                fontSize: 11,
                fontWeight: 700,
                fontFamily: "Inter, sans-serif",
                border: isActive ? "1px solid rgba(245,158,11,0.5)" : "1px solid rgba(255,255,255,0.06)",
                background: isActive ? "rgba(245,158,11,0.12)" : "rgba(14,14,17,0.6)",
                color: isActive ? "#f59e0b" : "rgba(255,255,255,0.5)",
                cursor: "pointer",
                transition: "all 0.15s",
                whiteSpace: "nowrap"
              }}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* ── KPI Strip (Top 5 Chỉ số quan trọng nhất) ── */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: 16 }}>
        {primaryKpis.map((kpi, i) => <KpiCard key={kpi.label} {...kpi} delay={i * 0.06} />)}
      </div>

      {/* ── Phân rã doanh thu theo phương thức thanh toán ── */}
      {providerBreakdown.length > 0 && (
        <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: 10, border: "1px solid rgba(245,158,11,0.25)", background: "rgba(245,158,11,0.04)", padding: "12px 16px" }}>
          <span style={{ fontSize: 9, fontWeight: 800, color: "#a3a3a3", fontFamily: "Inter, sans-serif", letterSpacing: "0.15em", textTransform: "uppercase" }}>
            Kênh thanh toán
          </span>
          {providerBreakdown.map((p) => (
            <span key={p.provider} style={{ fontSize: 11, fontFamily: "Inter, sans-serif", color: "#e5e5e5", border: "1px solid rgba(255,255,255,0.08)", background: "rgba(0,0,0,0.5)", padding: "5px 12px" }}>
              <b style={{ color: p.provider === "CASH" ? "#34d399" : "#fbbf24" }}>{p.label}</b>
              {": "}{fmtVND(p.revenue)}
              <span style={{ color: "#737373" }}> ({fmtNumber(p.transactions)} GD)</span>
            </span>
          ))}
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────────
          SECTION 1: BÁO CÁO VÉ BÁN RA & DOANH THU PHIM
      ───────────────────────────────────────────────────────────────── */}
      {showTicketsSec && (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          <div style={{ borderBottom: "1px solid rgba(255,255,255,0.08)", pb: 8 }}>
            <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: "0.2em", textTransform: "uppercase", color: "#06b6d4", fontFamily: "monospace" }}>
              BOX OFFICE & TICKET SALES REPORT
            </span>
            <h2 style={{ margin: "2px 0 0", fontSize: 16, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif" }}>
              1. Báo cáo Số lượng vé bán ra & Doanh thu Phim
            </h2>
          </div>

          {/* Occupancy & Daily Ticket Trend Area Chart */}
          <ChartCard>
            <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
              <SectionHeader
                eyebrow="TẢI SUẤT CHIẾU & VÉ THEO NGHỆ AN"
                title={`Xu hướng Vé bán ra & Tỷ lệ lấp đầy theo ${GROUP_LABELS[occupancyGroupBy]} (${rangeDays} ngày gần nhất)`}
                badge={`Tổng vé: ${fmtNumber(effectiveRevenue.totalTicketsSold)}`}
              />
              <div style={{ display: "flex", alignItems: "center", border: "1px solid rgba(255,255,255,0.08)" }}>
                {[
                  { key: "day", label: "Theo ngày" },
                  { key: "week", label: "Theo tuần" },
                  { key: "month", label: "Theo tháng" },
                ].map((option) => {
                  const isActive = occupancyGroupBy === option.key;
                  return (
                    <button
                      key={option.key}
                      type="button"
                      onClick={() => { playPulseSound?.(510, "sine", 0.03); setOccupancyGroupBy(option.key); }}
                      style={{
                        padding: "6px 14px", fontSize: 9.5, fontWeight: 700,
                        fontFamily: "Inter, sans-serif", border: "none",
                        borderRight: "1px solid rgba(255,255,255,0.06)",
                        cursor: "pointer", transition: "all 0.15s",
                        background: isActive ? "rgba(6,182,212,0.15)" : "transparent",
                        color: isActive ? "#06b6d4" : "rgba(255,255,255,0.4)",
                      }}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {occupancyChartData.length === 0 ? <EmptyChart message="Chưa có dữ liệu vé bán ra trong khoảng thời gian này" /> : (
              <ResponsiveContainer width="100%" height={260}>
                <AreaChart data={occupancyChartData} margin={{ top: 12, right: 12, bottom: 4, left: 4 }}>
                  <defs>
                    <linearGradient id="areaGradTicketsSold" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#06b6d4" stopOpacity={0.5} />
                      <stop offset="100%" stopColor="#06b6d4" stopOpacity={0.02} />
                    </linearGradient>
                    <linearGradient id="areaGradOccRate" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#f59e0b" stopOpacity={0.4} />
                      <stop offset="100%" stopColor="#f59e0b" stopOpacity={0.01} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid vertical={false} stroke={GRID_COLOR} />
                  <XAxis dataKey="label" tick={TICK_STYLE} axisLine={false} tickLine={false} interval="preserveStartEnd" minTickGap={20} />
                  <YAxis yAxisId="left" tickFormatter={(v) => fmtNumber(v)} tick={TICK_STYLE} axisLine={false} tickLine={false} width={40} />
                  <YAxis yAxisId="right" orientation="right" tickFormatter={(v) => `${v}%`} tick={{ ...TICK_STYLE, fill: "#f59e0b" }} axisLine={false} tickLine={false} width={40} />
                  <Tooltip
                    cursor={{ stroke: "rgba(255,255,255,0.15)" }}
                    content={<ChartTooltip formatter={(e) => (
                      e.dataKey === "sold"
                        ? `Vé bán ra: ${fmtNumber(e.value)} vé (${e.payload.shows} suất)`
                        : `Lấp đầy: ${e.value}% (${fmtNumber(e.payload.sold)}/${fmtNumber(e.payload.capacity)} ghế)`
                    )} />}
                  />
                  <Area yAxisId="left" type="monotone" dataKey="sold" name="Số lượng vé" stroke="#06b6d4" strokeWidth={2.5} fill="url(#areaGradTicketsSold)" />
                  <Area yAxisId="right" type="monotone" dataKey="rate" name="Tỷ lệ lấp đầy" stroke="#f59e0b" strokeWidth={2} strokeDasharray="3 3" fill="url(#areaGradOccRate)" />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </ChartCard>

          {/* Row 2: Top movies bar & Ticket share donut */}
          <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 20 }}>
            {/* Top movies bar */}
            <ChartCard>
              <SectionHeader eyebrow="TOP PHIM BAN RA" title="Top Phim theo Doanh thu & Lượng vé" />
              {revenueByMovie.length === 0 ? <EmptyChart message="Chưa có dữ liệu doanh thu phim" /> : (
                <ResponsiveContainer width="100%" height={Math.max(220, revenueByMovie.length * 44)}>
                  <BarChart data={revenueByMovie} layout="vertical" margin={{ top: 4, right: 80, bottom: 4, left: 4 }}>
                    <defs>
                      <linearGradient id="barGradRev" x1="0" y1="0" x2="1" y2="0">
                        <stop offset="0%" stopColor="#f59e0b" stopOpacity={0.9} />
                        <stop offset="100%" stopColor="#d97706" stopOpacity={0.65} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid horizontal={false} stroke={GRID_COLOR} />
                    <XAxis type="number" tickFormatter={fmtCompact} tick={TICK_STYLE} axisLine={false} tickLine={false} />
                    <YAxis type="category" dataKey="name" width={140} tick={{ ...TICK_STYLE, fontSize: 10, fill: "#a1a1aa" }} axisLine={false} tickLine={false} />
                    <Tooltip cursor={{ fill: "rgba(255,255,255,0.03)" }} content={<ChartTooltip formatter={(e) => `${fmtVND(e.payload.revenue)} · ${fmtNumber(e.payload.tickets)} vé đã bán`} />} />
                    <Bar dataKey="revenue" name="Doanh thu" fill="url(#barGradRev)" barSize={16} radius={[0, 2, 2, 0]}>
                      <LabelList dataKey="tickets" position="right" formatter={(v) => `${fmtNumber(v)} vé`} style={{ fill: "rgba(255,255,255,0.5)", fontSize: 10, fontFamily: "Inter, sans-serif", fontWeight: 600 }} />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </ChartCard>

            {/* Ticket share donut */}
            <ChartCard>
              <SectionHeader eyebrow="THỊ PHẦN SẢN LƯỢNG" title="Cơ cấu Vé bán ra theo Phim" />
              {ticketShare.length === 0 ? <EmptyChart message="Chưa có dữ liệu thị phần vé" /> : (
                <div style={{ position: "relative" }}>
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <defs>
                        {CHART_COLORS.map((c, i) => (
                          <radialGradient key={i} id={`pieG${i}`} cx="50%" cy="50%" r="50%">
                            <stop offset="0%" stopColor={c} stopOpacity={1} />
                            <stop offset="100%" stopColor={c} stopOpacity={0.65} />
                          </radialGradient>
                        ))}
                      </defs>
                      <Pie data={ticketShare} dataKey="value" nameKey="name" innerRadius={60} outerRadius={88} paddingAngle={ticketShare.length > 1 ? 3 : 0} stroke="none">
                        {ticketShare.map((entry, index) => (
                          <Cell key={entry.name} fill={`url(#pieG${index % CHART_COLORS.length})`} />
                        ))}
                      </Pie>
                      <Tooltip content={<ChartTooltip formatter={(e) => `${fmtNumber(e.value)} vé · ${totalTicketsInShare ? Math.round((e.value / totalTicketsInShare) * 100) : 0}% thị phần`} />} />
                      <Legend verticalAlign="bottom" iconType="circle" iconSize={7} formatter={(value) => <span style={{ fontSize: 9.5, fontFamily: "Inter, sans-serif", color: "rgba(255,255,255,0.5)" }}>{value}</span>} />
                    </PieChart>
                  </ResponsiveContainer>
                  <div style={{ position: "absolute", left: "50%", top: "42%", transform: "translate(-50%, -50%)", textAlign: "center", pointerEvents: "none" }}>
                    <span style={{ display: "block", fontSize: 8, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.1em", fontFamily: "Inter, sans-serif", fontWeight: 800 }}>TỔNG VÉ</span>
                    <span style={{ display: "block", fontSize: 18, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif" }}>{fmtNumber(totalTicketsInShare)}</span>
                  </div>
                </div>
              )}
            </ChartCard>
          </div>
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────────
          SECTION 2: BÁO CÁO BẮP NƯỚC & CONCESSIONS (F&B SALES REPORT)
      ───────────────────────────────────────────────────────────────── */}
      {showFnbSec && (
        <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
          <div style={{ borderBottom: "1px solid rgba(255,255,255,0.08)", pb: 8 }}>
            <span style={{ fontSize: 9, fontWeight: 800, letterSpacing: "0.2em", textTransform: "uppercase", color: "#10b981", fontFamily: "monospace" }}>
              CONCESSIONS & F&B PERFORMANCE REPORT
            </span>
            <h2 style={{ margin: "2px 0 0", fontSize: 16, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif" }}>
              2. Báo cáo Số lượng Bắp nước bán ra & Doanh thu F&B
            </h2>
          </div>

          {/* F&B KPI summary cards */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 14 }}>
            {fnbKpis.map((kpi, i) => <KpiCard key={kpi.label} {...kpi} delay={i * 0.05} />)}
          </div>

          {/* F&B Charts Grid */}
          <div style={{ display: "grid", gridTemplateColumns: "1.5fr 1fr", gap: 20 }}>
            {/* Top Concessions items sold chart */}
            <ChartCard>
              <SectionHeader
                eyebrow="TOP F&B TIÊU THỤ"
                title="Top Bắp nước & Combo bán chạy nhất"
                badge={`Tổng: ${fmtNumber(fnbTotalItems)} món`}
              />
              {concessionItemsData.length === 0 ? <EmptyChart message="Chưa có dữ liệu món bắp nước bán ra" /> : (
                <ResponsiveContainer width="100%" height={Math.max(220, concessionItemsData.length * 40)}>
                  <BarChart data={concessionItemsData} layout="vertical" margin={{ top: 4, right: 80, bottom: 4, left: 4 }}>
                    <defs>
                      <linearGradient id="barGradFnB" x1="0" y1="0" x2="1" y2="0">
                        <stop offset="0%" stopColor="#10b981" stopOpacity={0.9} />
                        <stop offset="100%" stopColor="#059669" stopOpacity={0.65} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid horizontal={false} stroke={GRID_COLOR} />
                    <XAxis type="number" tickFormatter={(v) => fmtNumber(v)} tick={TICK_STYLE} axisLine={false} tickLine={false} />
                    <YAxis type="category" dataKey="name" width={150} tick={{ ...TICK_STYLE, fontSize: 10, fill: "#a1a1aa" }} axisLine={false} tickLine={false} />
                    <Tooltip cursor={{ fill: "rgba(255,255,255,0.03)" }} content={<ChartTooltip formatter={(e) => `${fmtNumber(e.payload.quantity)} món bán ra · Doanh thu ${fmtVND(e.payload.revenue)}`} />} />
                    <Bar dataKey="quantity" name="Số lượng bán" fill="url(#barGradFnB)" barSize={16} radius={[0, 2, 2, 0]}>
                      <LabelList dataKey="quantity" position="right" formatter={(v) => `${fmtNumber(v)} món`} style={{ fill: "rgba(255,255,255,0.5)", fontSize: 10, fontFamily: "Inter, sans-serif", fontWeight: 600 }} />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </ChartCard>

            {/* F&B Sales Source channel chart */}
            <ChartCard>
              <SectionHeader eyebrow="KÊNH PHÁT HÀNH F&B" title="Cơ cấu Kênh đặt Bắp nước" />
              {concessionSourcesData.length === 0 ? <EmptyChart message="Chưa có dữ liệu kênh đặt bắp nước" /> : (
                <div style={{ position: "relative" }}>
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <defs>
                        <radialGradient id="pieFnb0" cx="50%" cy="50%" r="50%">
                          <stop offset="0%" stopColor="#10b981" stopOpacity={1} />
                          <stop offset="100%" stopColor="#059669" stopOpacity={0.65} />
                        </radialGradient>
                        <radialGradient id="pieFnb1" cx="50%" cy="50%" r="50%">
                          <stop offset="0%" stopColor="#a855f7" stopOpacity={1} />
                          <stop offset="100%" stopColor="#7e22ce" stopOpacity={0.65} />
                        </radialGradient>
                      </defs>
                      <Pie data={concessionSourcesData} dataKey="value" nameKey="name" innerRadius={58} outerRadius={86} paddingAngle={3} stroke="none">
                        {concessionSourcesData.map((entry, index) => (
                          <Cell key={entry.name} fill={index === 0 ? "url(#pieFnb0)" : "url(#pieFnb1)"} />
                        ))}
                      </Pie>
                      <Tooltip content={<ChartTooltip formatter={(e) => `${fmtNumber(e.value)} món (${fmtVND(e.payload.revenue)}) · ${fmtNumber(e.payload.orders)} đơn`} />} />
                      <Legend verticalAlign="bottom" iconType="circle" iconSize={7} formatter={(value) => <span style={{ fontSize: 9.5, fontFamily: "Inter, sans-serif", color: "rgba(255,255,255,0.5)" }}>{value}</span>} />
                    </PieChart>
                  </ResponsiveContainer>
                  <div style={{ position: "absolute", left: "50%", top: "42%", transform: "translate(-50%, -50%)", textAlign: "center", pointerEvents: "none" }}>
                    <span style={{ display: "block", fontSize: 8, color: "rgba(255,255,255,0.35)", textTransform: "uppercase", letterSpacing: "0.1em", fontFamily: "Inter, sans-serif", fontWeight: 800 }}>TỔNG MÓN</span>
                    <span style={{ display: "block", fontSize: 18, fontWeight: 800, color: "#fff", fontFamily: "Inter, sans-serif" }}>{fmtNumber(totalFnBSourceQuantity || fnbTotalItems)}</span>
                  </div>
                </div>
              )}
            </ChartCard>
          </div>

          {/* Daily F&B sales trend area chart */}
          {concessionDailyData.length > 0 && (
            <ChartCard>
              <SectionHeader
                eyebrow="DIỄN BIẾN THEO THỜI GIAN"
                title={`Xu hướng tiêu thụ Bắp nước & Doanh thu F&B (${rangeDays} ngày gần nhất)`}
                badge={`Doanh thu F&B: ${fmtVND(fnbTotalRevenue)}`}
              />
              <ResponsiveContainer width="100%" height={230}>
                <AreaChart data={concessionDailyData} margin={{ top: 12, right: 12, bottom: 4, left: 4 }}>
                  <defs>
                    <linearGradient id="areaGradFnBDaily" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#10b981" stopOpacity={0.45} />
                      <stop offset="100%" stopColor="#10b981" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid vertical={false} stroke={GRID_COLOR} />
                  <XAxis dataKey="label" tick={TICK_STYLE} axisLine={false} tickLine={false} interval="preserveStartEnd" minTickGap={20} />
                  <YAxis yAxisId="left" tickFormatter={(v) => fmtNumber(v)} tick={TICK_STYLE} axisLine={false} tickLine={false} width={40} />
                  <YAxis yAxisId="right" orientation="right" tickFormatter={fmtCompact} tick={{ ...TICK_STYLE, fill: "#10b981" }} axisLine={false} tickLine={false} width={40} />
                  <Tooltip
                    cursor={{ stroke: "rgba(255,255,255,0.15)" }}
                    content={<ChartTooltip formatter={(e) => (
                      e.dataKey === "quantity"
                        ? `Số món bán: ${fmtNumber(e.value)} món (${e.payload.orders} đơn F&B)`
                        : `Doanh thu F&B: ${fmtVND(e.value)}`
                    )} />}
                  />
                  <Area yAxisId="left" type="monotone" dataKey="quantity" name="Số món bán" stroke="#06b6d4" strokeWidth={2} fill="rgba(6,182,212,0.1)" />
                  <Area yAxisId="right" type="monotone" dataKey="revenue" name="Doanh thu F&B" stroke="#10b981" strokeWidth={2.5} fill="url(#areaGradFnBDaily)" />
                </AreaChart>
              </ResponsiveContainer>
            </ChartCard>
          )}
        </div>
      )}

      {/* ─────────────────────────────────────────────────────────────────
          SECTION 3: TỔNG QUAN ĐIỂM THÀNH VIÊN (LOYALTY HEALTH)
      ───────────────────────────────────────────────────────────────── */}
      {showLoyaltySec && (
        <ChartCard>
          <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 4 }}>
            <SectionHeader eyebrow="LOYALTY HEALTH & REWARDS" title="3. Báo cáo Tổng quan Điểm thưởng Thành viên" />
            <div style={{ display: "flex", alignItems: "center", gap: 5, border: "1px solid rgba(16,185,129,0.25)", background: "rgba(16,185,129,0.07)", padding: "4px 12px" }}>
              <Shield size={10} color="#10b981" />
              <span style={{ fontSize: 8.5, fontWeight: 800, color: "#10b981", fontFamily: "monospace", letterSpacing: "0.15em", textTransform: "uppercase" }}>Active Program</span>
            </div>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))", gap: 14 }}>
            {loyaltyKpis.map((kpi, i) => <KpiCard key={kpi.label} {...kpi} sub={`${rangeDays} ngày gần nhất`} delay={i * 0.05} />)}
          </div>
        </ChartCard>
      )}

      {/* ─────────────────────────────────────────────────────────────────
          SECTION 4: SYSTEM AUDIT LOGS
      ───────────────────────────────────────────────────────────────── */}
      {showLogsSec && (
        <div style={{ border: "1px solid rgba(255,255,255,0.06)", background: "rgba(7,7,10,0.8)", overflow: "hidden" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 18px", background: "rgba(255,255,255,0.02)", borderBottom: "1px solid rgba(255,255,255,0.05)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <Cpu size={12} color="rgba(255,255,255,0.4)" />
              <span style={{ fontSize: 8.5, fontWeight: 800, color: "rgba(255,255,255,0.4)", fontFamily: "monospace", letterSpacing: "0.2em", textTransform: "uppercase" }}>
                System Audit Log & Live Traces
              </span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <motion.div animate={{ opacity: [1, 0.3, 1] }} transition={{ duration: 1.4, repeat: Infinity }} style={{ width: 6, height: 6, borderRadius: "50%", background: "#10b981" }} />
              <span style={{ fontSize: 8.5, color: "#10b981", fontFamily: "monospace", fontWeight: 800, letterSpacing: "0.15em" }}>LIVE MONITOR</span>
            </div>
          </div>
          <div style={{ padding: "12px 18px", maxHeight: 240, overflowY: "auto" }} id="terminal-audit-box">
            {effectiveAuditLogs.length === 0 && (
              <p style={{ margin: 0, padding: "12px 0", fontSize: 10, color: "rgba(255,255,255,0.25)", fontFamily: "monospace" }}>
                Chưa có hoạt động quản trị nào được ghi nhận.
              </p>
            )}
            {effectiveAuditLogs.map((log) => (
              <div key={log.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 16, padding: "6px 0", borderBottom: "1px solid rgba(255,255,255,0.03)", fontFamily: "monospace" }}>
                <div style={{ display: "flex", gap: 8, fontSize: 10, flexWrap: "wrap" }}>
                  <span style={{ color: "rgba(255,255,255,0.25)", flexShrink: 0 }}>[{log.time}]</span>
                  <span style={{ color: logColor(log.action), fontWeight: 700, flexShrink: 0 }}>{log.action}:</span>
                  <span style={{ color: "rgba(255,255,255,0.7)" }}>{log.target}</span>
                </div>
                <span style={{ fontSize: 8, color: "rgba(255,255,255,0.25)", flexShrink: 0, border: "1px solid rgba(255,255,255,0.06)", padding: "2px 7px", fontFamily: "monospace", fontWeight: 600, letterSpacing: "0.06em", textTransform: "uppercase" }}>
                  {log.user}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </motion.div>
  );
}
