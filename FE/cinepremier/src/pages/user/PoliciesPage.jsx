import React from 'react';
import {
  AlertTriangle,
  BadgeCheck,
  Clock3,
  CreditCard,
  LockKeyhole,
  RefreshCw,
  ShieldCheck,
  TicketCheck,
  Users
} from 'lucide-react';

const policyHighlights = [
  {
    icon: ShieldCheck,
    title: 'Bảo vệ khách hàng',
    text: 'Mọi giao dịch đặt vé, lưu thẻ và thay đổi tài khoản đều được kiểm tra bằng lớp bảo mật nội bộ.'
  },
  {
    icon: TicketCheck,
    title: 'Vé điện tử minh bạch',
    text: 'Mã vé, ghế ngồi, suất chiếu và tổng thanh toán được lưu theo hồ sơ khách hàng để dễ đối soát.'
  },
  {
    icon: Users,
    title: 'Quy chế Thượng Khách',
    text: 'Thành viên VIP được hưởng ưu đãi theo hạng, đồng thời cần tuân thủ quy định độ tuổi và nội quy rạp.'
  }
];

const policySections = [
  {
    icon: BadgeCheck,
    title: '1. Điều kiện sử dụng dịch vụ',
    items: [
      'Khách hàng cần cung cấp thông tin đăng ký chính xác, bao gồm họ tên, email và số điện thoại liên hệ.',
      'Với phim phân loại T18, khách hàng xác nhận đã đủ 18 tuổi và có thể được yêu cầu xuất trình giấy tờ tùy thân tại rạp.',
      'CinePremier có quyền từ chối phục vụ nếu phát hiện hành vi gian lận điểm thưởng, mã vé hoặc ưu đãi thành viên.'
    ]
  },
  {
    icon: CreditCard,
    title: '2. Thanh toán và xuất vé',
    items: [
      'Vé chỉ được xác nhận sau khi giao dịch thanh toán hoàn tất hoặc được hệ thống ghi nhận thành công.',
      'Khách hàng cần kiểm tra kỹ rạp, phim, ngày chiếu, suất chiếu và ghế trước khi xác nhận đặt vé.',
      'Mã vé điện tử có thể dùng để check-in trực tiếp tại quầy hoặc máy quét của rạp.'
    ]
  },
  {
    icon: RefreshCw,
    title: '3. Đổi trả và hoàn tiền',
    items: [
      'Yêu cầu hoàn tiền chỉ được xử lý theo điều kiện từng chương trình, tối thiểu trước giờ chiếu theo quy định rạp.',
      'Vé khuyến mãi, vé combo hoặc vé đã check-in có thể không đủ điều kiện hoàn tiền.',
      'Khi suất chiếu bị hủy bởi rạp, khách hàng được hỗ trợ đổi suất tương đương hoặc hoàn tiền theo phương thức thanh toán ban đầu.'
    ]
  },
  {
    icon: LockKeyhole,
    title: '4. Bảo mật tài khoản',
    items: [
      'Khách hàng chịu trách nhiệm bảo vệ mật khẩu và mã OTP được gửi tới email hoặc số điện thoại cá nhân.',
      'Không chia sẻ tài khoản VIP, mã vé hoặc thông tin thẻ thanh toán cho bên thứ ba.',
      'CinePremier có thể tạm khóa tài khoản khi phát hiện đăng nhập bất thường hoặc giao dịch có rủi ro.'
    ]
  },
  {
    icon: AlertTriangle,
    title: '5. Nội quy tại rạp',
    items: [
      'Vui lòng đến trước giờ chiếu ít nhất 15 phút để kiểm tra vé và ổn định chỗ ngồi.',
      'Không quay phim, chụp ảnh màn hình hoặc gây ảnh hưởng tới trải nghiệm của khách hàng khác.',
      'Tuân thủ hướng dẫn của nhân viên rạp trong các tình huống an toàn, kỹ thuật hoặc thay đổi lịch chiếu.'
    ]
  }
];

const vipRules = [
  { label: 'Hạng Silver', value: 'Tích lũy 500 điểm, nhận ưu đãi bắp nước theo tháng.' },
  { label: 'Hạng Gold', value: 'Tích lũy 2.000 điểm, ưu tiên đặt ghế đẹp và nhận voucher sinh nhật.' },
  { label: 'Hạng Platinum', value: 'Tích lũy 5.000 điểm, hỗ trợ đổi vé linh hoạt và phòng chờ VIP theo rạp áp dụng.' }
];

export default function PoliciesPage() {
  return (
    <div className="bg-black text-white">
      <section className="border-b border-white/10 px-4 py-14 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-6xl">
          <div className="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-3xl space-y-5">
              <span className="inline-flex items-center gap-2 border border-amber-400/30 bg-amber-400/10 px-3 py-1.5 text-[10px] font-black uppercase tracking-[0.24em] text-amber-300">
                <ShieldCheck className="h-3.5 w-3.5" />
                CinePremier Policy Center
              </span>
              <div className="space-y-3">
                <h1 className="font-serif text-4xl italic tracking-wide text-white sm:text-5xl">
                  Chính sách rạp CinePremier
                </h1>
                <p className="max-w-2xl text-sm leading-7 text-neutral-300 sm:text-base">
                  Quy định sử dụng dịch vụ, đặt vé, thanh toán, bảo mật tài khoản và quyền lợi Thượng Khách VIP được mô phỏng bằng dữ liệu mẫu để khách hàng đọc nhanh trước khi đăng ký.
                </p>
              </div>
            </div>

            <div className="border border-white/10 bg-neutral-950 px-5 py-4 text-sm text-neutral-300">
              <div className="mb-2 flex items-center gap-2 text-amber-300">
                <Clock3 className="h-4 w-4" />
                <span className="text-[10px] font-black uppercase tracking-[0.2em]">Cập nhật mock</span>
              </div>
              <p className="font-mono text-xs text-white">27/05/2026 - Phiên bản 1.0</p>
            </div>
          </div>
        </div>
      </section>

      <section className="border-b border-white/10 px-4 py-8 sm:px-6 lg:px-8">
        <div className="mx-auto grid max-w-6xl gap-4 md:grid-cols-3">
          {policyHighlights.map((item) => {
            const Icon = item.icon;
            return (
              <article key={item.title} className="border border-white/10 bg-[#080808] p-5">
                <Icon className="mb-4 h-5 w-5 text-amber-300" />
                <h2 className="mb-2 text-sm font-black uppercase tracking-[0.18em] text-white">{item.title}</h2>
                <p className="text-sm leading-6 text-neutral-300">{item.text}</p>
              </article>
            );
          })}
        </div>
      </section>

      <section className="px-4 py-12 sm:px-6 lg:px-8">
        <div className="mx-auto grid max-w-6xl gap-8 lg:grid-cols-[1fr_340px]">
          <div className="space-y-5">
            {policySections.map((section) => {
              const Icon = section.icon;
              return (
                <section key={section.title} className="border border-white/10 bg-[#070707] p-5 sm:p-6">
                  <div className="mb-4 flex items-center gap-3 border-b border-white/10 pb-4">
                    <Icon className="h-5 w-5 text-amber-300" />
                    <h2 className="text-base font-black uppercase tracking-[0.16em] text-white">{section.title}</h2>
                  </div>
                  <ul className="space-y-3 text-sm leading-7 text-neutral-300">
                    {section.items.map((item) => (
                      <li key={item} className="flex gap-3">
                        <span className="mt-2 h-1.5 w-1.5 shrink-0 bg-amber-300" />
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </section>
              );
            })}
          </div>

          <aside className="space-y-5">
            <section className="border border-amber-400/25 bg-amber-400/10 p-5">
              <h2 className="mb-4 text-sm font-black uppercase tracking-[0.2em] text-amber-200">
                Quyền lợi Thượng Khách VIP
              </h2>
              <div className="space-y-4">
                {vipRules.map((rule) => (
                  <div key={rule.label} className="border-b border-amber-300/15 pb-4 last:border-b-0 last:pb-0">
                    <p className="mb-1 text-xs font-black uppercase tracking-[0.16em] text-white">{rule.label}</p>
                    <p className="text-sm leading-6 text-neutral-200">{rule.value}</p>
                  </div>
                ))}
              </div>
            </section>

            <section className="border border-white/10 bg-neutral-950 p-5">
              <h2 className="mb-3 text-sm font-black uppercase tracking-[0.2em] text-white">Liên hệ hỗ trợ</h2>
              <p className="text-sm leading-7 text-neutral-300">
                Email: support@cinepremier.vn
                <br />
                Hotline: 1900 2026
                <br />
                Thời gian: 08:00 - 22:00 mỗi ngày
              </p>
            </section>
          </aside>
        </div>
      </section>
    </div>
  );
}
