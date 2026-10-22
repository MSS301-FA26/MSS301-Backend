# Báo Cáo Toàn Diện Tính Năng `ai-service` (CinemaAI Intelligence Engine)

> **Dịch vụ**: `MSS301-Backend/cinema-services/ai-service`  
> **Nhánh Git**: `feat/ai-service`  
> **Ngôn ngữ & Nền tảng**: Python 3.12, FastAPI, PostgreSQL 16 + pgvector, RabbitMQ 3.13 (pika), Sentence-Transformers (`all-MiniLM-L6-v2`), OpenAI API (`gpt-4o-mini`), Pytest.  
> **Phiên bản Schema Database**: 4 Migrations versioned (`V1` $\to$ `V4`).  

---

## 1. Tổng Quan Kiến Trúc & Sứ Mệnh

`ai-service` là microservice phụ trách toàn bộ năng lực Trí tuệ Nhân tạo và Dữ liệu Thông minh cho hệ thống rạp chiếu phim **CinePremier**, bao gồm 4 trụ cột nghiệp vụ cốt lõi:
1. **Tìm kiếm phim thích ứng đa tầng (Adaptive Multi-Tier Semantic Search)**: Tự động điều tiết tài nguyên tính toán giữa tìm kiếm nhanh và re-ranking sâu dựa trên entropy độ bất định của câu hỏi.
2. **Gợi ý phim cá nhân hóa nâng cao (Adaptive Hybrid Recommendation Engine)**: Kết hợp lọc cộng tác (Pearson Shrinkage CF), hồ sơ kép nội dung (Dual-Profile Rocchio Algorithm), lọc cứng phim không thích, suy giảm hàm mũ thời gian, cân bằng đa dạng thể loại và phân tích rạp chiếu.
3. **Trợ lý ảo đàm thoại thông minh (PopBot Conversational TAG Agent)**: Kiến trúc Single-Hop Docstring-Driven Function Calling, phân giải ngữ cảnh đa lượt và cam kết 100% không bịa đặt phim (Grounded Synthesis).
4. **Quản trị Prompt động (Dynamic Prompt Management CRUD)**: Cho phép Product Owner / Admin điều chỉnh prompt hệ thống, model và tham số suy luận theo thời gian thực qua REST API với cơ chế Hot-Reload Cache và Zero Downtime.

```
                              ┌────────────────────────┐
                              │      API Gateway       │
                              └───────────┬────────────┘
                                          │ (X-Gateway-Secret, X-Correlation-Id)
                                          ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 ai-service (FastAPI)                                   │
│                                                                                        │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────┐ ┌─────────────┐ │
│ │   Adaptive Search    │ │Hybrid Recommendation │ │   PopBot Agent   │ │ Prompt CRUD │ │
│ │ - Soft Utility Router│ │ - Dual Rocchio Profile│ │ - Docstring TAG  │ │ - Versioning│ │
│ │ - R0/R1/R2 Re-rankers│ │ - Pearson Shrinkage  │ │ - Ellipsis Solver│ │ - Hot Reload │ │
│ │ - HNSW pgvector      │ │ - Diversity Ceiling  │ │ - Grounded LLM   │ │ - Dry-run   │ │
│ │                      │ │ - Circuit Breaker    │ │                  │ │   Test      │ │
│ └──────────────────────┘ └──────────────────────┘ └──────────────────┘ └─────────────┘ │
│                                            │                                           │
│                 ┌──────────────────────────┴──────────────────────────┐                │
│                 ▼                                                     ▼                │
│ ┌───────────────────────────────┐                   ┌────────────────────────────────┐ │
│ │    PostgreSQL 16 + pgvector   │                   │       RabbitMQ Broker          │ │
│ │ - movie_embeddings (384-dim)  │                   │ - cinema.catalog.events        │ │
│ │ - user_interactions          │                   │ - cinema.payment.events        │ │
│ │ - movie_reviews (aspects)     │                   │ - ai-service.events.dlq        │ │
│ │ - recommendation_sets / items │                   └────────────────────────────────┘ │
│ │ - prompt_templates            │                                                      │
│ │ - chat_sessions / metrics     │                                                      │
│ └───────────────────────────────┘                                                      │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Chi Tiết Các Tính Năng Hiện Có

### 2.1. Quản Trị Prompt Động (Dynamic Prompt Management)
- **Mục đích**: Thay vì hardcode prompt trong mã nguồn, toàn bộ chỉ dẫn và tham số LLM được lưu trữ trong PostgreSQL và cập nhật trực tiếp qua API.
- **Bảng dữ liệu**: `prompt_templates` (Flyway `V4__prompt_templates.sql`).
- **Các tính năng nổi bật**:
  1. **Hot-Reload In-Memory Cache**: Ứng dụng đọc prompt qua `TTLMemoryCache` (độ trễ < 0.1ms). Khi có request `PUT` hoặc `DELETE`, cache tự động bị vô hiệu hoá (`cache.delete`), prompt mới có hiệu lực ngay trong lượt gọi tiếp theo mà **không cần restart server**.
  2. **An toàn Fallback (Zero-Downtime Guarantee)**: Nếu database gặp sự cố, hệ thống tự động rơi về `DEFAULT_FALLBACK_PROMPTS` có sẵn trong code, ngăn chặn lỗi 500.
  3. **Dry-Run Test Render (`POST .../test-render`)**: Cho phép truyền mock variables để kiểm tra kết quả ráp biến `{variable}` hoặc gọi thử LLM trước khi lưu vào production.
- **Danh mục 4 Prompt cốt lõi đang hoạt động**:
  - `QUERY_REWRITE`: Viết lại câu hỏi tỉnh lược trong hội thoại.
  - `CHATBOT_GROUNDED_REPLY`: Sinh phản hồi trợ lý ảo dựa trên danh mục thực tế.
  - `SENTIMENT_ASPECT_ANALYSIS`: Trích xuất khía cạnh (Plot, Acting, Visuals, Audio) và đánh giá mâu thuẫn.
  - `RECOMMENDATION_EXPLAINER`: Sinh câu giải thích lý do gợi ý phim cá nhân hóa.

---

### 2.2. Gợi Ý Phim Cá Nhân Hoá Nâng Cao (Adaptive Hybrid Recommendation Engine)
- **Kiến trúc Hybrid đa tầng**:
  1. **Hồ sơ sở thích kép với suy giảm thời gian (Dual-Profile Rocchio Algorithm)**:
     - Xây dựng 2 vector trọng tâm đại diện cho sở thích tích cực $\vec{P}_u$ và sở thích tiêu cực $\vec{N}_u$:
       $$\vec{P}_u = \sum_{i \in I_u^+} w_{ui} \cdot e^{-\Delta t_i / \lambda} \cdot \vec{v}_i, \quad \vec{N}_u = \sum_{j \in I_u^-} w_{uj} \cdot e^{-\Delta t_j / \lambda} \cdot \vec{v}_j$$
     - Điểm tương đồng nội dung được tính bằng:
       $$S_{\text{content}}(u, m) = \cos(\vec{P}_u, \vec{v}_m) - 0.35 \cdot \cos(\vec{N}_u, \vec{v}_m)$$
  2. **Loại trừ tuyệt đối phim bị ghét (Strict Dislike Exclusion & Catalog Starvation)**:
     - Bất kỳ phim nào người dùng đã dislike hoặc chấm $\le 1.0$ sao đều bị **loại bỏ 100%** khỏi mọi tập ứng viên.
     - Kể cả khi toàn bộ danh mục bị dislike hoặc kích hoạt Circuit Breaker Fallback, hệ thống bảo đảm **không bao giờ rò rỉ phim bị ghét**.
  3. **Lọc cộng tác Pearson Shrinkage (User-User Collaborative Filtering)**:
     - Tính tương quan Pearson có điều chuẩn độ trùng lặp (Overlap Shrinkage Regularization, $\lambda = 5.0$) trên tập phim chung:
       $$s'_{uv} = s_{uv} \cdot \frac{|I_{uv}|}{|I_{uv}| + 5.0}$$
     - Gated Nu $\ge 5$: Chỉ áp dụng khi người dùng có tối thiểu 5 tương tác đánh giá để chống nhiễu cold-start.
  4. **Phân tích khía cạnh đánh giá & Giảm trọng số châm biếm (Sarcasm & Inconsistency Dampening)**:
     - Tự động bóc tách khía cạnh: `plot`, `acting`, `visuals`, `audio`.
     - Nếu phát hiện đánh giá mâu thuẫn (ví dụ: chấm 5 sao nhưng bình luận chê bai, châm biếm), hệ thống hạ thấp `confidence_score` xuống $\le 0.35$ để tránh làm sai lệch hồ sơ người dùng.
  5. **Giới hạn trần đa dạng thể loại (Genre Diversity Ceiling & MMR)**:
     - Khống chế tỷ lệ tối đa không quá 40% cho bất kỳ thể loại nào trong danh sách gợi ý, ngăn chặn hiện tượng "bong bóng lọc" (filter bubble).
  6. **Ràng buộc suất chiếu theo chi nhánh rạp (`branch_id`)**:
     - Lọc cứng và ưu tiên các phim đang có suất chiếu hoạt động tại chi nhánh người dùng chọn.
  7. **Cầu dao bảo vệ quá tải (Circuit Breaker FSM)**:
     - Tự động ngắt sang trạng thái `OPEN` nếu xảy ra 5 lỗi kết nối database liên tiếp.
     - Thời gian phản hồi fail-fast $< 0.01$ ms, tự động phục hồi qua trạng thái `HALF_OPEN` sau timeout 30s.
  8. **Viễn trắc phễu chuyển đổi & Thử nghiệm A/B (Full-Funnel Telemetry)**:
     - Theo dõi tỷ lệ chuyển đổi: Hiển thị (Impression) $\to$ Click $\to$ Xem chi tiết $\to$ Đặt vé $\to$ Sử dụng vé.
     - Hỗ trợ 4 nhóm thử nghiệm A/B: `CONTROL`, `VARIANT_B`, `LLM_EXPLAINER`, `BANDIT_EXPLORATION`.
  9. **Tuân thủ quyền riêng tư GDPR (Right-to-be-Forgotten)**:
     - Endpoint `DELETE /api/v1/recommendations/users/{user_id}/data` xoá sạch tương tác, đánh giá, lịch sử gợi ý và xoá cache ngay lập tức.

---

### 2.3. Tìm Kiếm Phim Thích Ứng Đa Tầng (Adaptive Semantic Search)
- **API Endpoint**: `POST /api/v1/search/adaptive`
- **Cơ chế hoạt động**:
  1. **Soft Utility Router (`soft_utility_router.py`)**:
     - Trích xuất đặc trưng truy vấn: độ dài câu hỏi, điểm tương đồng cao nhất, khoảng cách điểm, entropy.
     - Dự đoán phân phối định tuyến qua Softmax: $p(R0), p(R1), p(R2)$.
     - Tính entropy định tuyến $H(q) = -\sum p_i \log p_i$. Tự động kích hoạt Fallback khi độ bất định vượt ngưỡng.
  2. **Tầng R0 - First-Stage Retriever**:
     - Kết hợp Dense Semantic Vector (SBERT 384 dims) và Keyword Overlap (60% Dense + 40% Keyword).
  3. **Tầng R1 - Lightweight Neural Re-Ranker**:
     - Tái xếp hạng danh sách ứng viên, cộng điểm thưởng tương quan tiêu đề (+0.25) và thể loại (+0.15).
  4. **Tầng R2 - Heavy Late-Interaction Re-Ranker**:
     - Áp dụng kỹ thuật ColBERT-style Token-Level MaxSim để so khớp ma trận tương đồng từng token giữa câu hỏi và mô tả phim.

---

### 2.4. Trợ Lý Ảo Đàm Thoại PopBot (Docstring-Driven TAG Agent)
- **API Endpoint**: `POST /api/v1/chat/message`
- **Các ưu điểm kiến trúc**:
  1. **Single-Hop Bounded Latency**: Khống chế tối đa 1 lượt gọi tool. Câu hỏi giao tiếp/FAQ chỉ tốn 1 lượt LLM (~1s); câu hỏi tìm kiếm/gợi ý tốn tối đa 2 lượt LLM.
  2. **Docstring-Driven Function Calling**: Tự động chuyển đổi docstring hàm Python thành OpenAPI Tool Schemas:
     - `search_movies(query)`: Tự sửa lỗi chính tả, dịch tiếng lóng ('anh thon' $\to$ 'Thor').
     - `recommend_movies(user_id, mood_or_topic)`: Gợi ý theo tâm trạng, ngữ cảnh.
  3. **Cam kết 100% Zero-Hallucination**: Câu trả lời chỉ được giới thiệu các phim có thực trong danh mục được hệ thống trả về.
  4. **Quản lý phiên hội thoại đa lượt**: Lưu trữ lịch sử đàm thoại trong PostgreSQL (`chat_sessions.history`), hỗ trợ phân giải tỉnh lược câu hỏi phụ thuộc ("ai đóng phim này?").

---

### 2.5. Đồng Bộ Dữ Liệu Sự Kiện Thời Gian Thực (RabbitMQ Event Consumers)
Hệ thống lắng nghe trên message broker và tự động xóa cache liên quan:
1. **Catalog Consumer (`catalog_event_consumer.py`)**:
   - Lắng nghe `cinema.catalog.events` (`movie.published`, `movie.updated`).
   - Tạo vector nhúng 384 chiều qua SentenceTransformer, upsert vào `movie_embeddings`, và **xóa cache danh mục đề xuất** (`user:*`).
2. **Payment Consumer (`payment_event_consumer.py`)**:
   - Lắng nghe `cinema.payment.events` (`payment.succeeded`).
   - Ghi nhận tương tác mua vé (`interaction_type='BOOKING_PAID'`, trọng số 5.0) và **xóa cache đề xuất của người dùng đó** (`user:{user_id}:*`).

---

### 2.6. Bảo Mật, Viễn Trắc & Cơ Sở Hạ Tầng
1. **Zero-Trust Gateway Isolation**: Bắt buộc có header `X-Gateway-Secret` (cho API client) hoặc `X-Internal-Service-Secret` (cho giao tiếp nội bộ giữa các microservice).
2. **Distributed Tracing**: Gắn `X-Correlation-Id` vào tất cả các log line và response header.
3. **Immutability & Strict DTO Validation**: Tất cả DTOs dùng Pydantic `frozen=True` với chuẩn Envelope `ApiResponse<T>`.
4. **Flyway Migrations**:
   - `V1__init_ai_db.sql`: Khởi tạo bảng vector embeddings, index HNSW, sessions.
   - `V2__recommendation_enhancements.sql`: Bổ sung tracking recommendation sets, feedback, telemetry.
   - `V3__sentiment_and_reviews.sql`: Bảng review, aspect sentiment, experiment variants.
   - `V4__prompt_templates.sql`: Bảng quản lý template prompt động và nạp dữ liệu khởi tạo.

---

## 3. Danh Mục Đầy Đủ Các API Đang Cung Cấp

### 🔹 Nhóm Quản Trị Prompt Động (`/api/v1/prompts`)
| Phương thức | Đường dẫn API | Mô tả nghiệp vụ |
| :---: | :--- | :--- |
| `GET` | `/api/v1/prompts` | Lấy danh sách toàn bộ prompt templates (hỗ trợ `active_only`) |
| `GET` | `/api/v1/prompts/{prompt_code}` | Lấy chi tiết 1 prompt template theo mã định danh |
| `POST` | `/api/v1/prompts` | Tạo mới một prompt template |
| `PUT` | `/api/v1/prompts/{prompt_code}` | Cập nhật nội dung, model, temperature (tự động xóa cache hot-reload) |
| `DELETE` | `/api/v1/prompts/{prompt_code}` | Vô hiệu hoá template (soft delete) |
| `POST` | `/api/v1/prompts/{prompt_code}/test-render` | Chạy thử render biến mock và kiểm tra kết quả LLM dry-run |

### 🔹 Nhóm Gợi Ý Phim & Phản Hồi (`/api/v1/recommendations`)
| Phương thức | Đường dẫn API | Mô tả nghiệp vụ |
| :---: | :--- | :--- |
| `GET` | `/api/v1/recommendations/users/{user_id}` | Gợi ý phim cá nhân hóa theo người dùng (hỗ trợ `branch_id`, `limit`) |
| `GET` | `/api/v1/recommendations/movies/{movie_id}/similar` | Gợi ý phim tương đồng theo nội dung và embedding vector |
| `GET` | `/api/v1/recommendations/trending` | Danh sách phim thịnh hành đang chiếu tại rạp (Cold-start fallback) |
| `POST` | `/api/v1/recommendations/feedbacks` | Gửi tín hiệu tương tác: Đánh giá sao, Thích (Like), Ghét (Dislike) |
| `POST` | `/api/v1/recommendations/clicks` | Ghi nhận sự kiện click vào item gợi ý (đo CTR A/B Testing) |
| `POST` | `/api/v1/recommendations/reviews` | Gửi đánh giá phim kèm phân tích khía cạnh (Aspect Sentiment) |
| `GET` | `/api/v1/recommendations/metrics` | Thống kê viễn trắc phễu chuyển đổi và đo lường A/B testing |
| `DELETE` | `/api/v1/recommendations/users/{user_id}/data` | Xoá toàn bộ dữ liệu lịch sử của user (Tuân thủ GDPR / Quyền được quên) |

### 🔹 Nhóm Tìm Kiếm & Trợ Lý Ảo
| Phương thức | Đường dẫn API | Mô tả nghiệp vụ |
| :---: | :--- | :--- |
| `POST` | `/api/v1/search/adaptive` | Tìm kiếm ngữ nghĩa thích ứng 3 tầng (R0/R1/R2) |
| `POST` | `/api/v1/chat/message` | Trò chuyện với trợ lý ảo PopBot (Grounded Response Synthesis) |
| `GET` | `/api/v1/ai/metrics` | Báo cáo hiệu năng tìm kiếm và phân phối độ trễ (P50, P95) |
| `GET` | `/health` | Healthcheck công khai kiểm tra trạng thái hoạt động của service |

---

## 4. Kết Quả Kiểm Thử Nghiệm Thu (Quality Assurance)
- **100% Pass Pytest Test Suite**: Toàn bộ 14 bài kiểm thử tự động nội bộ chạy thành công trong ~2.2 giây.
- **100% Pass 9 Kịch Bản Tải Nặng & Biên Cực Hạn (Adversarial Stress Test)**:
  - Catalog Starvation (Dislike 100% danh mục phim): Không rò rỉ phim bị ghét.
  - Concurrency Storm: 20 request đồng thời bảo đảm tính bất biến (Idempotent), 0 bản ghi trùng.
  - Bảo mật & Fuzzing: Chặn 100% request thiếu secret và validate chặt chẽ tham số ngoài biên.
  - Phân tích cảm xúc châm biếm: Tách biệt khía cạnh âm thanh/hình ảnh (+1.0) và kịch bản/diễn xuất (-1.0).
  - Biên thời gian: Chống tràn số mũ float với timestamp tương lai và quá khứ 10 năm.
  - Circuit Breaker: Chuyển trạng thái FSM mượt mà, độ trễ fail-fast $< 0.01$ ms.
  - GDPR Purge: Xoá sạch dữ liệu 3 bảng và đưa tài khoản về cold-start không tàn dư.
