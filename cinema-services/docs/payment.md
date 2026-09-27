Bạn đang tiếp tục phát triển project Cinema Management System hiện tại.

KHÔNG được redesign toàn bộ project từ đầu.
KHÔNG được rewrite những phần Booking / VNPay / Outbox / RabbitMQ đã chạy ổn nếu không thật sự cần thiết.

Trước tiên phải đọc code hiện tại, hiểu kiến trúc và xác nhận phần nào đã có, phần nào chưa có, sau đó mới triển khai phần còn thiếu.

=====================================================
I. TRẠNG THÁI HIỆN TẠI CỦA PROJECT
=====================================================

Project là hệ thống quản lý và đặt vé rạp chiếu phim đa chi nhánh theo microservice.

Các service chính:

1. Identity Service
2. Catalog Service
3. Booking Service
4. Payment Service
5. Recommendation Service

Hiện tại nhóm đã triển khai được các phần sau.

-----------------------------------------------------
1. AUTHORITATIVE PRICING QUOTE
-----------------------------------------------------

Booking Service gọi Catalog Service để lấy giá chính thức:

POST /api/v1/ticket-pricing/checkout-quote

Booking không tin giá gửi từ frontend.

Catalog chịu trách nhiệm tính:

- giá vé
- Ticket Type
- Seat Type
- Room Type
- VIP/Couple
- weekend
- holiday
- late night
- food item
- food combo

Mục tiêu:

Frontend không thể sửa giá bằng DevTools rồi gửi amount giả.

-----------------------------------------------------
2. IMMUTABLE BOOKING SNAPSHOT
-----------------------------------------------------

Booking lưu snapshot bất biến tại thời điểm checkout, bao gồm các thông tin như:

- movie name
- movie poster
- cinema name
- room name
- showtime
- seat label
- unit price
- food price
- tổng tiền

Nếu Catalog thay đổi sau này thì Booking/Ticket cũ vẫn giữ đúng dữ liệu lịch sử.

-----------------------------------------------------
3. BOOKING FLOW
-----------------------------------------------------

BookingController hiện đã có:

POST /api/v1/bookings/hold
- giữ ghế 3 phút
- có concurrency control

PUT /api/v1/bookings/{id}/items
- cập nhật ticket/food trong lúc giữ ghế

POST /api/v1/bookings/{id}/checkout
- xác nhận order
- booking chuyển sang PENDING_PAYMENT

DELETE /api/v1/bookings/{id}
- cancel hold
- release seat ngay

GET /api/v1/bookings
- booking history của Customer

GET /api/v1/bookings/{id}
- booking detail

GET /api/v1/bookings/code/{bookingCode}
- lookup bằng booking code

-----------------------------------------------------
4. PAYMENT SERVICE
-----------------------------------------------------

Payment Service là microservice riêng.

Có database riêng.

Payment Service gọi Booking Service bằng internal REST API:

GET /internal/v1/bookings/{id}

Có X-Internal-Service-Secret.

Payment Service phải xác minh:

- booking tồn tại
- booking thuộc đúng Customer
- booking amount hợp lệ

trước khi tạo Payment.

-----------------------------------------------------
5. VNPAY SANDBOX
-----------------------------------------------------

Hiện đã tích hợp VNPay Sandbox.

Endpoint:

POST /api/v1/payments/vnpay/create

Dùng:

- parameter sorting
- HMAC-SHA512
- VNPay signed URL

IPN:

GET/POST /api/v1/payments/vnpay/ipn

Hiện đã có:

- verify signature
- verify amount 100%
- idempotency
- xử lý transaction đã confirm trước đó

Ngoài ra có:

POST /api/v1/payments/mock

để test payment success nhanh trong local.

Có API:

GET /api/v1/payments/booking/{bookingId}

GET /api/v1/payments/{paymentId}

-----------------------------------------------------
6. TRANSACTIONAL OUTBOX + RABBITMQ
-----------------------------------------------------

Khi Payment thành công:

PaymentServiceImpl ghi:

Payment
+
PaymentSucceededEvent

vào cùng database transaction.

Event được ghi vào:

outbox_events

OutboxPublisherWorker:

- scan PENDING mỗi 2 giây
- publish lên RabbitMQ
- exchange:
  cinema.payment.events
- routing key:
  payment.succeeded
- retry tối đa 5 lần
- success thì chuyển PUBLISHED

Booking Service có PaymentEventListener.

Consumer queue:

booking.payment.succeeded

Đã có idempotent consumer thông qua:

processed_events

Nếu eventId đã xử lý rồi thì bỏ qua.

Khi xử lý PaymentSucceeded:

- Booking → PAID
- set paidAt
- Seat Hold → BOOKED
- tạo Ticket
- tạo ticketCode
- tạo QR check-in

Booking QR format hiện tại:

CINEMA:{bookingCode}:{id}

Ticket QR:

TICKET:{ticketCode}

=====================================================
II. MỤC TIÊU PHASE TIẾP THEO
=====================================================

Không tập trung làm lại VNPay.

Phase tiếp theo cần hoàn thiện phần PAYMENT / REFUND / WALLET / WITHDRAWAL.

Ưu tiên theo thứ tự:

1. Payment transaction/retry/expiration hardening
2. Cinema Manager authorization
3. Showtime incident cancellation
4. Refund
5. Refund Batch
6. Wallet
7. Wallet Transaction
8. Refund → Wallet
9. Wallet Payment
10. Withdrawal
11. Mock Payout Provider
12. Withdrawal reversal
13. Payment reconciliation
14. RabbitMQ recovery / DLQ nếu cần
15. Ticket Check-in hardening

=====================================================
III. PAYMENT TRANSACTION
=====================================================

Trước tiên kiểm tra code hiện tại xem đã có entity PaymentTransaction chưa.

Nếu chưa có thì bổ sung.

Mục đích:

Một Payment có thể có nhiều lần transaction attempt.

Ví dụ:

Payment P001

Transaction T001 → FAILED
Transaction T002 → TIMEOUT
Transaction T003 → SUCCESS

Payment cuối cùng:

SUCCESS

Không được mất lịch sử các attempt cũ.

PaymentTransaction nên cân nhắc các field:

- id
- paymentId
- provider
- providerTransactionId
- providerReference
- amount
- currency
- status
- failureCode
- failureMessage
- expiresAt
- createdAt
- completedAt

Không nhất thiết phải thêm đúng 100% field nếu project đã có field tương đương.

=====================================================
IV. PAYMENT STATE MACHINE
=====================================================

Payment nên có lifecycle rõ ràng.

Tối thiểu:

PENDING
PROCESSING
SUCCESS
FAILED
EXPIRED
CANCELLED

Khi có refund thêm:

PARTIALLY_REFUNDED
REFUNDED

Không cho phép state transition sai.

Ví dụ hợp lệ:

PENDING → PROCESSING
PROCESSING → SUCCESS
PROCESSING → FAILED
PENDING → EXPIRED
SUCCESS → REFUNDED

Ví dụ không hợp lệ:

REFUNDED → SUCCESS

=====================================================
V. PAYMENT EXPIRATION
=====================================================

VNPay payment không được valid vô hạn.

Cần có:

expiresAt

Flow:

Create VNPay payment
→ PENDING
→ hết thời gian
→ EXPIRED

Kiểm tra edge case:

- callback đến ngay sát thời điểm expire
- callback đến sau khi Payment đã expire
- booking/seat hold đã hết hạn nhưng payment callback lại SUCCESS

Phải có business rule rõ cho case này.

=====================================================
VI. PAYMENT RETRY
=====================================================

Phải chốt một chiến lược retry duy nhất.

Ưu tiên:

Booking
→ Payment
→ nhiều PaymentTransaction

Ví dụ:

Booking B001
→ Payment P001
   ├── Transaction 1 FAILED
   ├── Transaction 2 TIMEOUT
   └── Transaction 3 SUCCESS

Nếu code hiện tại đang dùng:

Booking
→ nhiều Payment

thì phân tích trước khi thay đổi.

Không được tự chuyển model nếu sẽ phá code hiện tại.

=====================================================
VII. REFUND BUSINESS RULE
=====================================================

Scope hiện tại:

Refund chủ yếu xảy ra khi Cinema gặp sự cố vận hành.

Ví dụ:

POWER_OUTAGE
SHOWTIME_CANCELLED
TECHNICAL_ISSUE
OTHER_OPERATIONAL_INCIDENT

Manager không được tự nhập số tiền refund tùy ý.

Refund amount phải được hệ thống tính từ:

successful payment amount
-
previous completed refund
=
remaining refundable amount

Tổng refund không bao giờ được vượt số tiền đã thanh toán thành công.

=====================================================
VIII. CINEMA MANAGER
=====================================================

Hệ thống multi-branch có Cinema Manager.

Manager chỉ được thao tác với Cinema được assign.

Manager có thể:

- cancel Showtime của Cinema mình
- khai báo/confirm operational incident
- approve refund của Cinema mình
- xem refund của Cinema mình

Manager KHÔNG được:

- refund Cinema khác
- sửa Payment amount
- mark Payment SUCCESS
- sửa Wallet balance trực tiếp
- thao tác booking của Cinema khác

Authorization phải kiểm tra server-side.

Không được tin cinemaId do frontend gửi lên.

=====================================================
IX. SHOWTIME INCIDENT FLOW
=====================================================

Ví dụ Cinema mất điện:

Cinema A
→ Manager A
→ cancel Showtime S001
→ reason = POWER_OUTAGE

System phải:

1. xác định Showtime thuộc Cinema nào
2. kiểm tra Manager có quyền với Cinema đó
3. tìm các Booking đã PAID
4. tính refund amount
5. tạo Refund hoặc RefundBatch
6. approve theo flow business
7. credit Wallet Customer

=====================================================
X. REFUND ENTITY
=====================================================

Refund là entity tài chính riêng.

Nên có lifecycle:

REQUESTED
APPROVED
PROCESSING
COMPLETED
FAILED
REJECTED

Cân nhắc các field:

- id
- paymentId
- bookingId
- amount
- reason
- status
- approvedBy
- requestedAt
- approvedAt
- completedAt
- failureReason

Không nhất thiết thêm tất cả nếu đã có equivalent field.

=====================================================
XI. REFUND BATCH
=====================================================

Rất nên cân nhắc thêm RefundBatch.

Use case:

Một Showtime bị cancel
→ 200 booking bị ảnh hưởng

Không nên để Manager refund từng booking.

Ví dụ:

RefundBatch RB001

Cinema A
Showtime S001
Reason POWER_OUTAGE

Refunds:
- B001 → 200k
- B002 → 150k
- B003 → 300k

Manager approve batch.

Payment Service xử lý từng refund.

Nếu thấy RefundBatch quá phức tạp với code hiện tại thì phải giải thích trước khi bỏ.

=====================================================
XII. WALLET
=====================================================

Mỗi Customer có một Wallet.

Wallet scope hiện tại:

- nhận tiền Refund
- dùng để thanh toán Booking
- Customer có thể Withdrawal
- không peer-to-peer
- không manual top-up
- không manager sửa balance

Không triển khai full e-wallet.

Không triển khai nạp tiền chủ động.

=====================================================
XIII. WALLET TRANSACTION
=====================================================

Không được chỉ có:

wallet.balance += amount
wallet.balance -= amount

Phải có ledger:

WalletTransaction

Các type:

REFUND_CREDIT
PAYMENT_DEBIT
WITHDRAWAL_DEBIT
WITHDRAWAL_REVERSAL

Có thể có thêm type khác nếu project thật sự cần.

Cân nhắc field:

- id
- walletId
- type
- amount
- balanceBefore
- balanceAfter
- referenceType
- referenceId
- description
- status
- createdAt

Ví dụ:

Balance before:
200k

Wallet payment:
-120k

Balance after:
80k

=====================================================
XIV. WALLET CONCURRENCY
=====================================================

Phải chống double spending.

Ví dụ:

Wallet = 100k

Request A:
Pay 80k

Request B:
Pay 80k

Hai request chạy cùng lúc.

Không được để cả hai SUCCESS.

Phải dùng:

- pessimistic lock
hoặc
- optimistic lock
hoặc
- atomic update
hoặc
- cơ chế transactional phù hợp với project

Wallet balance không bao giờ được âm.

=====================================================
XV. REFUND TO WALLET
=====================================================

Flow:

Cinema incident
→ Manager cancel Showtime
→ Refund approved
→ Payment Service
→ WalletTransaction REFUND_CREDIT
→ Wallet balance tăng
→ Refund COMPLETED

Manager không chuyển tiền bằng tay.

Manager chỉ authorize business action.

=====================================================
XVI. WALLET PAYMENT
=====================================================

Thêm payment method:

WALLET

Flow:

Customer checkout
→ chọn WALLET
→ Payment Service
→ check balance
→ atomic debit
→ WalletTransaction PAYMENT_DEBIT
→ Payment SUCCESS
→ PaymentSucceededEvent
→ Outbox
→ RabbitMQ
→ Booking PAID
→ Ticket generated

QUAN TRỌNG:

Không tạo một flow confirm Booking riêng cho Wallet.

VNPay Payment và Wallet Payment phải hội tụ về cùng flow:

Payment SUCCESS
→ PaymentSucceededEvent
→ Booking PAID
→ Ticket Issued

Nếu Wallet không đủ tiền:

return business error:

INSUFFICIENT_WALLET_BALANCE

Không tự động split:

Wallet + VNPay

trừ khi business yêu cầu sau.

=====================================================
XVII. WITHDRAWAL
=====================================================

Customer có thể request rút tiền Wallet về bank account.

Với project demo:

KHÔNG cần payout ngân hàng thật.

Dùng:

MockPayoutProvider

Withdrawal entity nên có status:

REQUESTED
PROCESSING
COMPLETED
FAILED
CANCELLED

Flow:

Customer
→ request Withdrawal
→ validate Wallet balance
→ debit/reserve Wallet
→ WalletTransaction WITHDRAWAL_DEBIT
→ Withdrawal PROCESSING
→ MockPayoutProvider
→ SUCCESS
→ Withdrawal COMPLETED

=====================================================
XVIII. WITHDRAWAL FAILURE / REVERSAL
=====================================================

Nếu payout FAILED sau khi Wallet đã trừ:

Withdrawal FAILED
→ WalletTransaction WITHDRAWAL_REVERSAL
→ cộng lại Wallet
→ balance restored

Không được tồn tại trạng thái:

Wallet bị trừ
nhưng
Customer không nhận tiền
và
Wallet không được hoàn lại

=====================================================
XIX. MOCK PAYOUT PROVIDER
=====================================================

Tạo abstraction kiểu:

PayoutProvider

implementation demo:

MockPayoutProvider

Có thể hỗ trợ:

SUCCESS
FAILED

để test:

- withdrawal success
- withdrawal failure
- reversal

Không tích hợp payout ngân hàng thật trong phase này.

=====================================================
XX. PAYMENT / WALLET HISTORY
=====================================================

Backend phải tách rõ:

Payment History
Wallet History
Withdrawal History
Refund History

Không gộp tất cả thành một table/entity.

Payment History:

Booking B001
VNPay
200k
SUCCESS

Booking B002
Wallet
120k
SUCCESS

Wallet History:

+200k REFUND_CREDIT
-120k PAYMENT_DEBIT
-80k WITHDRAWAL_DEBIT
+80k WITHDRAWAL_REVERSAL

Frontend có thể gom thành Financial History sau này.

=====================================================
XXI. PAYMENT DETAIL
=====================================================

Kiểm tra API hiện tại.

Payment detail nên đủ để audit.

Ví dụ:

Payment ID
Booking ID
Booking Code

Ticket subtotal
Food subtotal
Discount
Final amount

Payment method
Payment status

Provider transaction reference
Provider transaction number

Created at
Paid at

Nếu có retry:

transaction history

Không lấy lại dữ liệu lịch sử từ Catalog mutable.

Dùng snapshot/reference an toàn.

=====================================================
XXII. WALLET DETAIL
=====================================================

Wallet detail nên hỗ trợ:

Current balance

Wallet history:
- type
- amount
- balanceBefore
- balanceAfter
- reference
- createdAt
- status

Có pagination.

=====================================================
XXIII. WITHDRAWAL HISTORY
=====================================================

Customer xem được:

- withdrawal amount
- status
- destination reference
- requestedAt
- completedAt
- failureReason

=====================================================
XXIV. IDEMPOTENCY
=====================================================

Hiện VNPay IPN đã có idempotency.

Giữ nguyên và kiểm tra thêm idempotency cho:

- Refund
- Refund Batch
- Wallet Payment
- Withdrawal
- Withdrawal callback/mock result

Không được:

- duplicate refund credit
- duplicate wallet debit
- duplicate withdrawal debit
- duplicate ticket issuance

=====================================================
XXV. TRANSACTION BOUNDARIES
=====================================================

Các operation sau phải nằm trong DB transaction phù hợp:

Wallet Payment:

Wallet debit
+
WalletTransaction
+
Payment status update

Refund:

Refund complete
+
Wallet credit
+
WalletTransaction

Withdrawal failure:

Withdrawal FAILED
+
WalletTransaction WITHDRAWAL_REVERSAL
+
Wallet balance restore

Không để partial update.

=====================================================
XXVI. EDGE CASE QUAN TRỌNG
=====================================================

Phải test case:

Seat Hold = 3 phút

Ví dụ:

12:00
Customer hold A5

12:02:59
Customer thanh toán VNPay

12:03:00
Hold expired

12:03:02
VNPay IPN SUCCESS

Phải quyết định rõ:

Booking có được confirm không?

Seat có còn thuộc Booking đó không?

Có risk seat bị người khác lấy không?

Ưu tiên xem xét:

Khi Booking chuyển PENDING_PAYMENT,
seat hold nên được giữ đến payment session expiry
hoặc có một payment reservation policy riêng.

Không được để Payment SUCCESS nhưng Seat đã được giao cho booking khác.

=====================================================
XXVII. OUTBOX RECOVERY
=====================================================

Hiện Outbox retry tối đa 5 lần.

Kiểm tra trường hợp:

Payment SUCCESS
→ Booking Service down
→ publish fail 5 lần

Sau đó event xử lý thế nào?

Cần có một trong các hướng:

- manual retry
- replay
- failed status
- admin recovery
- scheduler retry later

Không được silent drop.

=====================================================
XXVIII. RABBITMQ DLQ
=====================================================

Kiểm tra current RabbitMQ config.

Nếu chưa có, cân nhắc:

booking.payment.succeeded
→ retry
→ DLQ

Ví dụ:

booking.payment.succeeded.dlq

Không bắt buộc thêm nếu architecture hiện tại đã có cơ chế recovery tốt hơn.

Phải giải thích quyết định.

=====================================================
XXIX. PAYMENT RECONCILIATION
=====================================================

Bổ sung khả năng hoặc extension point để đối soát:

Internal PaymentTransaction
vs
VNPay transaction state

Ví dụ mismatch:

Internal = PENDING
VNPay = SUCCESS

hoặc

Internal = SUCCESS
VNPay = FAILED

Ít nhất phải:

- search được providerTransactionId
- có service method/extension point reconciliation
- không silently overwrite mismatch

Không cần full bank reconciliation system nếu vượt scope.

=====================================================
XXX. QR CHECK-IN SECURITY
=====================================================

Hiện QR:

CINEMA:{bookingCode}:{id}

TICKET:{ticketCode}

Không coi QR content là proof đủ để cho Customer vào rạp.

Check-in backend phải validate:

- Ticket tồn tại
- Ticket status hợp lệ
- Booking PAID
- đúng Showtime
- đúng Cinema
- chưa check-in
- không refunded/cancelled
- Staff thuộc đúng Cinema

QR chỉ là identifier/token để lookup.

=====================================================
XXXI. TICKET COMBO
=====================================================

Final business decision:

Ticket Combo không sử dụng nữa.

Nếu backend vẫn còn:

TicketCombo
comboId
comboPrice
TicketComboController
TicketComboService
TicketComboRepository
Ticket combo DTO
warnings

thì:

1. phân tích dependency
2. xóa hoặc deprecate an toàn
3. không làm hỏng checkout quote

Không xóa mù.

=====================================================
XXXII. KHÔNG TRIỂN KHAI TOP-UP
=====================================================

Không tạo:

TopUp
Deposit
WalletTopUp

Wallet inflow hiện tại chỉ từ Refund.

=====================================================
XXXIII. EXPECTED TARGET FLOW
=====================================================

FLOW A — VNPAY

Booking
→ VNPay Payment
→ PaymentTransaction
→ VNPay IPN
→ Payment SUCCESS
→ Outbox
→ RabbitMQ
→ Booking PAID
→ Ticket generated


FLOW B — REFUND

Cinema incident
→ Manager cancel Showtime
→ Refund / RefundBatch
→ Manager approve
→ WalletTransaction REFUND_CREDIT
→ Wallet updated


FLOW C — WALLET PAYMENT

Booking
→ WALLET
→ atomic debit
→ WalletTransaction PAYMENT_DEBIT
→ Payment SUCCESS
→ Outbox
→ RabbitMQ
→ Booking PAID
→ Ticket generated


FLOW D — WITHDRAWAL

Wallet
→ Withdrawal
→ WalletTransaction WITHDRAWAL_DEBIT
→ MockPayoutProvider
→ SUCCESS
→ Withdrawal COMPLETED


FLOW E — WITHDRAWAL FAILURE

Wallet
→ Withdrawal
→ debit
→ MockPayoutProvider FAILED
→ WalletTransaction WITHDRAWAL_REVERSAL
→ balance restored

=====================================================
XXXIV. YÊU CẦU LÀM VIỆC
=====================================================

PHASE 1 — ANALYZE FIRST

Trước khi sửa code, phải trả về:

1. Payment architecture hiện tại
2. Payment entities hiện tại
3. PaymentTransaction đã có hay chưa
4. Payment state hiện tại
5. VNPay implementation hiện tại
6. Booking integration hiện tại
7. Outbox implementation hiện tại
8. RabbitMQ implementation hiện tại
9. Những phần đã đáp ứng requirement
10. Những phần còn thiếu
11. File nào cần sửa
12. File nào không nên sửa
13. Database migration cần gì
14. Risk có thể làm vỡ flow hiện tại

KHÔNG CODE NGAY.

-----------------------------------------------------

PHASE 2 — IMPLEMENTATION PLAN

Sau analysis, đưa plan rõ theo:

Domain
Repository
Service
Controller
DTO
Mapper
Database
Security
Concurrency
RabbitMQ
Events
Tests

Nói rõ:

Reuse cái gì
Add cái gì
Modify cái gì
Remove cái gì

-----------------------------------------------------

PHASE 3 — IMPLEMENT

Sau khi đã hiểu code:

Implement lần lượt:

1. PaymentTransaction hardening
2. Payment states / expiration
3. Manager authorization
4. Refund
5. RefundBatch nếu phù hợp
6. Wallet
7. WalletTransaction
8. Refund → Wallet
9. Wallet Payment
10. Withdrawal
11. MockPayoutProvider
12. Withdrawal reversal
13. History/detail API
14. Reconciliation extension
15. DLQ/recovery nếu cần
16. Ticket Combo cleanup

Giữ nguyên:

- coding style
- package style
- exception format
- response wrapper
- mapper convention
- security convention
- migration convention

-----------------------------------------------------

PHASE 4 — TEST

Ít nhất test:

1. VNPay payment success
2. VNPay payment failed
3. duplicate VNPay IPN
4. wrong amount callback
5. payment expiration
6. payment retry
7. wallet payment success
8. insufficient wallet balance
9. concurrent wallet payment
10. refund success
11. duplicate refund
12. refund vượt paid amount
13. refund vào wallet
14. refund batch nhiều booking
15. manager refund Cinema khác
16. withdrawal success
17. withdrawal failure
18. withdrawal reversal
19. duplicate withdrawal request
20. outbox retry
21. event consumer idempotency
22. payment success khi seat hold hết hạn
23. historical snapshot không đổi

-----------------------------------------------------

PHASE 5 — BUILD & REPORT

Sau implementation:

Run build/test.

Cuối cùng báo cáo:

1. Những gì đã có sẵn
2. Những gì đã sửa
3. Entity mới
4. Enum mới
5. API mới
6. Migration mới
7. Payment flow
8. Refund flow
9. Wallet flow
10. Withdrawal flow
11. Concurrency handling
12. Idempotency handling
13. Event handling
14. Ticket Combo đã xử lý thế nào
15. Build result
16. Test result
17. Limitations còn lại
18. Next recommended task

Không được nói “done” nếu build/test chưa verify.
Không được bỏ qua test fail.
Không được tự invent flow không tồn tại trong code.