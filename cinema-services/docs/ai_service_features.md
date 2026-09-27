# Báo Cáo Tính Năng Nhánh `feat/ai-service` (CinemaAI ai-service)

> **Nhánh Git**: `feat/ai-service`  
> **Service**: `MSS301-Backend/cinema-services/ai-service`  
> **Tech Stack**: Python 3.12, FastAPI, PostgreSQL (pgvector + HNSW), RabbitMQ (pika), Sentence-Transformers (`all-MiniLM-L6-v2`), OpenAI API, Pytest, UV.

---

## 1. Tổng Quan Kiến Trúc & Mục Tiêu

Nhánh `feat/ai-service` xây dựng một microservice độc lập chịu trách nhiệm cung cấp trí tuệ nhân tạo toàn diện cho nền tảng rạp chiếu phim **CinePremier** (CinemaAI). Service hoạt động theo kiến trúc Event-Driven kết hợp API-driven, hỗ trợ 3 trụ cột AI cốt lõi:
1. **Tìm kiếm phim thích ứng đa tầng (Adaptive Multi-Tier Semantic Search)**.
2. **Gợi ý phim cá nhân hóa kết hợp (Adaptive Hybrid Recommendation Engine)**.
3. **Trợ lý ảo đàm thoại thông minh (PopBot Conversational Assistant)** với kiến trúc **Docstring-Driven Single-Hop TAG Agent**.

Hệ thống được bảo vệ bởi lớp bảo mật **Zero-Trust Gateway Secret** và theo dõi hiệu năng liên tục qua **AI Telemetry & Metrics**.

```
                       ┌────────────────────────┐
                       │      API Gateway       │
                       └───────────┬────────────┘
                                   │ (X-Gateway-Secret, X-Correlation-Id)
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          ai-service (FastAPI)                          │
│                                                                        │
│  ┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────┐ │
│  │   Adaptive Search     │ │ Hybrid Recommendation │ │ PopBot TAG    │ │
│  │  - Soft Utility Router│ │ - Pearson Shrinkage CF│ │   Agent       │ │
│  │  - R0/R1/R2 Re-rankers│ │ - SBERT Content Filter│ │ - Docstring   │ │
│  │  - HNSW pgvector      │ │ - Time-Decay Booster  │ │   Tools       │ │
│  │                       │ │ - Cold-Start Fallback │ │ - Grounded LLM│ │
│  └───────────────────────┘ └───────────────────────┘ └───────────────┘ │
│                                  │                                     │
│            ┌─────────────────────┴──────────────────────┐              │
│            ▼                                            ▼              │
│  ┌───────────────────────┐                    ┌──────────────────────┐ │
│  │ PostgreSQL + pgvector │                    │  RabbitMQ Consumers  │ │
│  │ - movie_embeddings    │                    │ - Catalog events     │ │
│  │ - user_interactions   │                    │ - Payment events     │ │
│  │ - chat_sessions       │                    └──────────────────────┘ │
│  │ - ai_metrics_log      │                                             │
│  └───────────────────────┘                                             │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Chi Tiết Các Tính Năng Đang Có

### 2.1. Tìm kiếm phim thích ứng đa tầng (Adaptive Semantic Search)
- **API Endpoint**: `POST /api/v1/search/adaptive`
- **Các thành phần cốt lõi**:
  1. **Soft Utility Router (`soft_utility_router.py`)**:
     - Trích xuất đặc trưng thống kê truy vấn $x_q = [\text{query\_len}, \text{top\_score}, \text{score\_gap}, \text{score\_entropy}]$.
     - Dự đoán phân phối xác suất định tuyến: $p(R0), p(R1), p(R2)$ qua hàm Softmax.
     - Tính entropy định tuyến $H(q) = -\sum p_i \log p_i$.
     - Tự động kích hoạt cơ chế Fallback nếu entropy vượt ngưỡng bất định.
  2. **Tầng R0 - First-Stage Retriever (`route_r0_first_stage`)**:
     - Kết hợp tìm kiếm ngữ nghĩa Dense Vector (SBERT 384 dims qua Cosine Similarity) và Keyword Overlap (trọng số 60% Dense + 40% Keyword).
     - Lọc trạng thái phim `NOW_SHOWING` để tối ưu tài nguyên quét.
  3. **Tầng R1 - Lightweight Neural Re-Ranker (`route_r1_lightweight_reranker`)**:
     - Tái sử dụng danh sách ứng viên từ R0, cross-scoring với điểm thưởng tương quan tiêu đề (Title match +0.25) và thể loại (Genre match +0.15).
  4. **Tầng R2 - Heavy Late-Interaction Re-Ranker (`route_r2_heavy_adap_colbert`)**:
     - Cơ chế ColBERT-style Token-Level MaxSim: So khớp ma trận tương đồng token câu truy vấn với token tóm tắt phim.
     - Cân bằng 50% điểm candidate + 50% điểm Late-Interaction.
  5. **Đo lường & Logging**:
     - Ghi nhận độ trễ, tuyến đường lựa chọn (R0/R1/R2), entropy và cờ fallback vào bảng `ai_metrics_log`.

---

### 2.2. Gợi ý phim cá nhân hóa kết hợp (Adaptive Hybrid Recommendation Engine)
- **API Endpoints**:
  - `GET /api/v1/recommendations/user/{user_id}?limit=10` (Gợi ý cho người dùng)
  - `GET /api/v1/recommendations/content/{movie_id}?limit=6` (Gợi ý phim tương tự)
- **Các thành phần cốt lõi**:
  1. **User-User Collaborative Filtering với Pearson Shrinkage (`collaborative_filter.py`)**:
     - Tính tương quan Pearson có **Mean-Centering** chuẩn xác trên tập phim chung giữa 2 người dùng $I_{uv}$.
     - Áp dụng kỹ thuật điều chuẩn **Overlap Shrinkage Regularization**:
       $$s'_{uv} = s_{uv} \cdot \frac{|I_{uv}|}{|I_{uv}| + \lambda} \quad (\lambda = 5.0, |I_{uv}| \ge 2)$$
     - Dự đoán điểm từ độ lệch xếp hạng:
       $$\hat{r}_{ui} = \bar{r}_u + \frac{\sum_{v \in \text{top-K}} s'_{uv}(r_{vi} - \bar{r}_v)}{\sum_{v \in \text{top-K}} |s'_{uv}|}$$
     - Tối ưu truy vấn SQL: Chỉ lấy người dùng có phim đánh giá chung thay vì quét toàn bộ bảng (tránh full-table scan).
  2. **Content-Based Filtering (`content_filter.py`)**:
     - Xây dựng User Preference Vector trọng số từ các phim người dùng đã tương tác.
     - Đo khoảng cách ngữ nghĩa Cosine Similarity với kho vector phim trong `movie_embeddings`.
  3. **Real-time Exponential Time-Decay Booster (`hybrid_engine.py`)**:
     - Phân tích tương tác đặt vé gần nhất của user.
     - Tăng điểm ưu tiên cho các phim cùng thể loại với hệ số suy giảm mũ theo thời gian:
       $$\text{boost} = 0.20 \cdot e^{-\Delta t / 24.0} \quad (\text{half-life} \approx 24\text{ giờ})$$
  4. **Cold-Start Fallback**:
     - Nếu người dùng mới chưa có lịch sử, hệ thống tự động fallback về danh sách phim thịnh hành đang chiếu (`NOW_SHOWING`), đảm bảo không trả về rỗng.

---

### 2.3. Trợ lý ảo đàm thoại PopBot (Conversational TAG Assistant)
- **API Endpoint**: `POST /api/v1/chat/message`
- **Kiến trúc Docstring-Driven Single-Hop TAG Agent (`modules/chatbot/agent/`)**:
  1. **Single-Hop Bounded Latency (`SingleHopTagExecutor`)**:
     - Khống chế tối đa 1 lượt gọi tool.
     - Đối với câu hỏi chào hỏi/chém gió ngoài lề (Chit-chat / FAQ): Chỉ tốn **1 lần gọi LLM** (~1s).
     - Đối với câu hỏi tra cứu / gợi ý: Tối đa **2 lần gọi LLM** (1 lần phân loại & trích xuất tham số, 1 lần tổng hợp câu trả lời).
  2. **100% Docstring-Driven Tool Definition (`tools.py`, `tool.py`, `registry.py`)**:
     - Định nghĩa công cụ bằng docstring chuẩn Python tự động chuyển thành JSON Schema OpenAI functions:
       - `search_movies(query: str)`: Tự sửa lỗi chính tả, dịch tiếng lóng (ví dụ: 'anh thon' -> 'Thor', 'fim ma' -> 'phim ma').
       - `recommend_movies(user_id: int, mood_or_topic: Optional[str])`: Gợi ý theo cảm xúc, hoàn cảnh (buồn, vui, hẹn hò...).
     - System prompt thuần Persona, không hardcode hướng dẫn tool.
  3. **Cam kết Zero-Hallucination (Grounded Response Synthesis)**:
     - LLM chỉ được phép giới thiệu các bộ phim có trong danh sách kết quả hệ thống trả về.
     - Tích hợp fallback template tự động nếu OpenAI API gặp sự cố mạng hoặc timeout.
  4. **Multi-turn Context & Session Persistence**:
     - Quản lý lịch sử hội thoại nhiều lượt lưu trữ trong PostgreSQL (`chat_sessions.history` dưới dạng JSONB).
     - Tự động duy trì `last_movie_id` để xử lý các câu hỏi ngữ cảnh nối tiếp ("Ai đóng phim này?", "Chiếu rạp nào?").

---

### 2.4. Đồng bộ dữ liệu sự kiện thời gian thực (RabbitMQ Event Consumers)
Tích hợp trực tiếp vào message broker của toàn hệ thống cinema-services:
1. **Catalog Consumer (`catalog_event_consumer.py`)**:
   - Lắng nghe exchange `cinema.catalog.events` (`movie.published`, `movie.updated`).
   - Ghép nối thông tin (Title, Description, Director, Genres, Actors), tính toán vector dense 384 chiều qua SentenceTransformer, và upsert vào bảng `movie_embeddings` (`dense_vector`).
2. **Payment Consumer (`payment_event_consumer.py`)**:
   - Lắng nghe exchange `cinema.payment.events` (`payment.succeeded`).
   - Tự động ghi nhận tín hiệu tương tác mạnh (`interaction_type='BOOKING_PAID'`, `weight += 5.0`) vào bảng `user_interactions` một cách bất đồng bộ và lũy kế.

---

### 2.5. Bảo mật, Cơ sở hạ tầng & Giám sát (Security, Infra & Telemetry)
1. **Zero-Trust Gateway Security (`GatewaySecretMiddleware`)**:
   - Chặn toàn bộ truy cập trực tiếp từ bên ngoài vào `/api/v1/**` nếu thiếu header `X-Gateway-Secret`.
   - Bảo vệ endpoints nội bộ `/internal/**` với `X-Internal-Service-Secret`.
   - Sử dụng `hmac.compare_digest` để chống tấn công Timing Attack.
2. **Distributed Tracing**:
   - Middleware `CorrelationIdMiddleware` tự động sinh hoặc chuyển tiếp header `X-Correlation-Id`.
   - `CorrelationIdLogFilter` gắn correlation_id vào từng log line.
3. **Telemetry Metrics API (`telemetry_api.py`)**:
   - Endpoint `GET /api/v1/ai/metrics` tính toán tức thời phân phối độ trễ (Mean, P50, P95), tỉ lệ các route R0/R1/R2, số lượng fallback.
4. **PostgreSQL Migration (`V1__init_ai_db.sql`)**:
   - Tự động kích hoạt extension `vector`.
   - Tạo index vector tăng tốc HNSW `idx_movie_embeddings_hnsw` trên cột `dense_vector`.
5. **Bộ kiểm thử toàn diện (Pytest)**:
   - `test_chatbot_context.py`: Kiểm thử phân giải ngữ cảnh độc lập/phụ thuộc, in-memory dispatcher.
   - `test_recommendation_algo.py`: Kiểm thử thuật toán Pearson Shrinkage CF.
   - `test_search_router.py`: Kiểm thử Soft Utility Router.
   - `test_security.py`: Kiểm thử xác thực Gateway Secret header và whitelist `/health`.

---

## 3. Tổng Kết Danh Sách API Đang Cung Cấp

| Phương Thức | Đường Dẫn | Mô Tả |
| :--- | :--- | :--- |
| `GET` | `/health` | Healthcheck công khai trạng thái service |
| `POST` | `/api/v1/search/adaptive` | Tìm kiếm phim thích ứng 3 tầng (R0/R1/R2) |
| `GET` | `/api/v1/recommendations/user/{user_id}` | Gợi ý phim cá nhân hóa cho người dùng |
| `GET` | `/api/v1/recommendations/content/{movie_id}`| Gợi ý danh sách phim tương tự theo nội dung |
| `POST` | `/api/v1/chat/message` | Trợ lý ảo PopBot đàm thoại đa lượt (TAG Agent) |
| `GET` | `/api/v1/ai/metrics` | Thống kê telemetry (P50, P95 latency, routes) |
