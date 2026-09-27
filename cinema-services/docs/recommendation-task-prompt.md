# Recommendation Feature — Complete Task Specification

## 1. Bối cảnh dự án

Dự án sử dụng kiến trúc microservice cho hệ thống Cinema. Recommendation nằm trong **AI Service**, cùng với Semantic Search, Chatbot và Telemetry.

Các công nghệ hiện tại:

- Python
- FastAPI
- PostgreSQL
- pgvector
- RabbitMQ
- Sentence Transformers
- Cosine Similarity
- Pearson Shrinkage Collaborative Filtering

Các module Recommendation hiện có:

- `ContentBasedFilter`
- `PearsonShrinkageCollaborativeFilter`
- `HybridRecommendationEngine`
- Movie Embedding
- Recommendation API
- Payment Event Consumer

Mục tiêu là hoàn thiện Recommendation cho nghiệp vụ Cinema, không chỉ trả về danh sách phim mà phải cá nhân hóa theo hành vi, rating, thời gian, branch, khả năng đặt vé và phản hồi của Customer.

---

## 2. Mục tiêu tổng thể

Recommendation phải có khả năng:

- Đề xuất phim cho Guest.
- Đề xuất phim cá nhân hóa cho Customer.
- Đề xuất phim tương tự một phim cụ thể.
- Xử lý Customer mới và phim mới.
- Phân biệt rating tích cực và tiêu cực.
- Nhận biết Customer thay đổi sở thích theo thời gian.
- Không đề xuất phim Customer đã dislike.
- Chỉ đề xuất phim phù hợp với branch và showtime.
- Kết hợp nhiều thuật toán thành Hybrid Recommendation.
- Có fallback khi dữ liệu hoặc AI Service bị lỗi.
- Ghi nhận phản hồi sau recommendation để cải thiện kết quả.
- Đo lường được chất lượng Recommendation bằng metrics.

Không được triển khai các thuật toán một cách máy móc. Với mỗi kỹ thuật phải xác định rõ: phạm vi áp dụng, dữ liệu đầu vào, lý do lựa chọn, giới hạn và cách đánh giá.

---

## 3. Phạm vi chức năng

Phải phân tích các nhóm chức năng sau:

1. Content-Based Filtering.
2. Popularity-Based Recommendation.
3. Collaborative Filtering.
4. Hybrid Recommendation.
5. Cold-Start Handling.
6. Positive Feedback Handling.
7. Negative Feedback Handling.
8. Time Decay.
9. Preference Drift.
10. Genre và Sub-genre Preference.
11. Movie Embedding và Semantic Similarity.
12. Branch-Aware Recommendation.
13. Availability Filtering.
14. Diversity và Exploration.
15. Explainable Recommendation.
16. Recommendation Feedback Loop.
17. Recommendation Cache.
18. Fallback và Resilience.
19. Recommendation Metrics.
20. A/B Testing nếu đủ dữ liệu.

Phải phân loại thành:

### MVP — bắt buộc

- Content-Based Filtering.
- Popularity-Based Recommendation.
- Positive/Negative Feedback.
- Time Decay.
- Cold-Start.
- Availability Filtering.
- Explainable Reason.
- Fallback.
- Event Idempotency.

### Phase 2 — triển khai sau MVP

- Collaborative Filtering đầy đủ.
- Hybrid Ranking nâng cao.
- Branch-Aware Scoring.
- Diversity.
- Recommendation Metrics.
- Click-to-Booking Feedback Loop.
- Recommendation Cache.

### Phase 3 — có thể để sau

- Comment Sentiment.
- Aspect-Based Sentiment.
- LLM Explanation nâng cao.
- Deep Learning Recommendation.
- Reinforcement Learning.

---

## 4. Kiến trúc Recommendation

Pipeline đề xuất:

```text
Customer Behavior
        |
Movie Interaction
        |
User Preference Profile
        |
Candidate Generation
        |
+----------------------------+
| Content-Based Filtering     |
| Collaborative Filtering     |
| Popularity/Trending         |
+----------------------------+
        |
Hybrid Ranking
        |
Business Filtering
        |
Diversity Re-ranking
        |
Explainable Recommendation
        |
Recommendation API
```

Trách nhiệm giữa các service:

- **Catalog Service**: cung cấp Movie, Genre, Actor, Director, Description, Status, Showtime.
- **Booking Service**: cung cấp booking, ticket, ticket used và hành vi đặt vé.
- **Payment Service**: cung cấp payment success, cancellation, refund.
- **Identity Service**: cung cấp `user_id` và quyền truy cập.
- **AI Service**: lưu interaction cần thiết, tạo embedding, tính điểm và trả recommendation.
- **RabbitMQ**: truyền event giữa các service.
- **PostgreSQL/pgvector**: lưu movie embedding, interaction, feedback và telemetry.

AI Service không được sao chép toàn bộ dữ liệu gốc từ service khác. Các liên kết tới User, Movie, Booking và Branch là logical reference.

---

## 5. Chiến lược theo loại người dùng

### Guest

Dùng:

- Global popularity.
- Trending.
- Now Showing.
- Popularity theo branch.

Không được yêu cầu user profile cá nhân.

### Customer mới

Dùng:

- Popularity.
- Onboarding preference nếu Customer chọn genre/actor.
- Content-Based nếu đã có preference ban đầu.

### Customer có ít interaction

Dùng:

```text
Content-Based + Popularity
```

Không được để Collaborative Filtering chi phối.

### Customer có nhiều interaction

Dùng:

```text
Content-Based + Collaborative + Popularity + Feedback + Recency
```

### Phim mới

Dùng metadata và embedding, không phụ thuộc số lượt booking.

### Branch mới

Dùng Global Popularity kết hợp Availability tại branch.

### AI Service lỗi

Dùng fallback theo thứ tự:

```text
Hybrid
→ Content-Based
→ Branch Popularity
→ Global Popularity
→ Now Showing
```

---

## 6. Content-Based Filtering

Content-Based Filtering phải sử dụng các thuộc tính:

- Genre.
- Sub-genre.
- Description.
- Actor/Performer.
- Director.
- Keyword.
- Language.
- Country.
- Release year.
- Age rating.

Phải mô tả:

- Cách tạo embedding.
- Model Sentence Transformer sử dụng.
- Kích thước vector.
- Cách lưu vector trong pgvector.
- Cách truy vấn vector.
- Cách tạo user profile vector.
- Cách tìm phim tương tự.
- Cách xử lý khi phim chưa có embedding.

Cosine Similarity được dùng để đo độ tương đồng giữa:

- Movie vector và Movie vector.
- User profile vector và Movie vector.

Phải có positive profile và negative profile.

Ví dụ:

```text
Customer đánh giá Movie A: 5★
Customer đánh giá Movie B: 1★

Movie mới giống Movie A → tăng điểm
Movie mới giống Movie B → giảm điểm
```

Không được loại bỏ toàn bộ Genre chỉ vì một phim bị đánh giá 1★.

---

## 7. Popularity-Based Recommendation

Popularity có thể tính từ:

- Booking count.
- Ticket used count.
- Rating average.
- Rating count.
- Recent booking trend.
- Branch popularity.
- Now Showing status.
- Availability.

Phải phân biệt:

- Global Popularity.
- Branch Popularity.
- Recent Trending.
- Top Rated.
- New Releases.

Popularity dùng cho Guest, Cold-Start, Fallback và làm một nguồn điểm trong Hybrid Ranking. Không được để Popularity khiến mọi Customer nhận cùng một danh sách.

---

## 8. Collaborative Filtering

Phải phân tích và so sánh:

- User-Based Collaborative Filtering.
- Item-Based Collaborative Filtering.
- Pearson Correlation.
- Pearson Shrinkage.
- Cosine Similarity.
- Matrix Factorization.

Code hiện tại đã có `PearsonShrinkageCollaborativeFilter`. Member phải giải thích:

- Công thức Pearson.
- Mean-centering.
- Common item overlap.
- `min_overlap`.
- Shrinkage factor.
- Rating deviation prediction.
- Xử lý dữ liệu thưa.
- Xử lý cold-start.

Ngưỡng đề xuất:

```text
0–4 interactions:
    Content-Based + Popularity

5–20 interactions:
    Content-Based + Collaborative nhẹ

Trên 20 interactions:
    Hybrid đầy đủ
```

Collaborative Filtering không được chi phối khi User chưa có đủ dữ liệu hoặc Pearson confidence thấp.

---

## 9. Hybrid Recommendation

Các score phải được normalize về cùng khoảng trước khi cộng.

Công thức khởi đầu:

```text
Final Score =
    Content Score * W_content
  + Collaborative Score * W_collaborative
  + Popularity Score * W_popularity
  + Feedback Score * W_feedback
  + Recency Score * W_recency
  + Availability Score * W_availability
  - Negative Penalty
```

Một cấu hình MVP có thể là:

```text
Content Score       = 0.45
Popularity Score    = 0.15
Feedback Score      = 0.20
Recency Score       = 0.10
Availability Score  = 0.10
```

Khi dữ liệu Collaborative đủ:

```text
Content Score       = 0.40
Collaborative Score = 0.20
Feedback Score      = 0.15
Popularity Score    = 0.10
Recency Score       = 0.05
Availability Score  = 0.10
```

Mọi trọng số phải được ghi rõ là:

- Có nguồn học thuật.
- Hoặc initial heuristic.
- Hoặc được tuning bằng offline evaluation.

Không được trình bày heuristic weight như một công thức đã được chứng minh.

---

## 10. Rating và Feedback

Mapping đề xuất:

```text
5★ = +1.0
4★ = +0.7
3★ =  0.0
2★ = -0.7
1★ = -1.0
```

Các tín hiệu tích cực:

- Rating 4★/5★.
- Like.
- Booking paid.
- Ticket used.
- Xem detail nhiều lần.
- Click.
- Search.

Các tín hiệu tiêu cực:

- Rating 1★/2★.
- Dislike.
- Bỏ qua recommendation nhiều lần.
- Refund do Customer không hài lòng nếu có bằng chứng phù hợp.

Phải gán trọng số khác nhau:

```text
TICKET_USED    = tín hiệu rất mạnh
BOOKING_PAID   = tín hiệu mạnh
RATING_5       = tín hiệu rất mạnh
CLICK          = tín hiệu yếu
SEARCH         = tín hiệu yếu/trung bình
```

Refund do Cinema hủy showtime không được xem là Customer ghét phim.

---

## 11. Negative Feedback và Dislike

Đây là phần phải hoàn thiện ngay.

Yêu cầu:

- Rating 1★ phải tạo negative signal.
- Rating 5★ phải tạo positive signal.
- Tách `positive_profile` và `negative_profile`.
- Dislike phải tạo exclusion hoặc penalty.
- Phim đã dislike không được xuất hiện lại trong recommendation cá nhân.
- Phim tương tự phim bị dislike phải bị giảm điểm.
- Không được loại bỏ toàn bộ Genre vì một rating thấp.

Công thức:

```text
Final Score =
    Positive Similarity
  - Negative Similarity Penalty
```

---

## 12. Time Decay và Preference Drift

Time decay phải áp dụng cho toàn bộ interaction:

- Rating.
- Booking.
- Ticket used.
- Click.
- Search.
- Like.
- Dislike.

Công thức:

```text
effective_weight =
    original_weight * exp(-age / half_life)
```

Phải giải thích:

- Half-life được chọn là bao nhiêu.
- Vì sao chọn giá trị đó.
- Rating cũ giảm ảnh hưởng như thế nào.
- Rating mới được ưu tiên ra sao.

Phải hỗ trợ:

```text
Long-term Preference
Short-term Preference
```

Ví dụ:

```text
Trước đây Customer thích Anime.
Gần đây Customer đánh giá thấp Anime và đánh giá cao Action/Horror.
```

Hệ thống phải:

- Giữ lịch sử cũ.
- Giảm trọng số lịch sử cũ.
- Tăng trọng số hành vi mới.
- Không kết luận thay đổi chỉ từ một interaction.
- Phát hiện thay đổi khi có nhiều tín hiệu liên tiếp.

---

## 13. Genre và Sub-genre Preference

Không chỉ lưu preference ở mức Genre. Cần hỗ trợ:

- Genre.
- Sub-genre.
- Actor.
- Director.
- Theme.
- Keyword.

Ví dụ:

```text
Customer thích Psychological Horror.
Customer không thích Gore/Slasher.
Customer vẫn thích Mystery Horror.
```

---

## 14. Comment và Sentiment

Phần này có thể để Phase 3, nhưng phải ghi rõ trong roadmap.

Các trường hợp cần hỗ trợ sau này:

```text
1★ + comment khen phim
5★ + comment chê phim
Comment vừa khen vừa chê
Comment spam
Comment mỉa mai
```

Khi triển khai, cần lưu:

- Rating score.
- Sentiment score.
- Sentiment label.
- Feedback consistency.
- Confidence score.
- Aspect sentiment.

Feedback mâu thuẫn phải có confidence thấp và không được dùng làm tín hiệu cực mạnh.

---

## 15. Branch-Aware và Availability Filtering

Phải bổ sung score theo branch:

```text
Final Score =
    Global Score
  + Branch Popularity Score
  + Branch Availability Score
  + Customer Branch Affinity
```

Trước khi trả kết quả phải lọc:

- Movie active.
- Movie đang chiếu.
- Movie có showtime.
- Movie có showtime tại branch đang chọn.
- Showtime chưa kết thúc.
- Showtime còn khả dụng.
- Movie phù hợp age/content restriction.
- Movie không bị Customer dislike.
- Movie không bị Customer ẩn.

Code hiện tại chỉ lọc `status = 'NOW_SHOWING'`, vì vậy phải bổ sung kiểm tra branch và showtime thực tế.

---

## 16. Diversity và Exploration

Sau khi ranking phải có diversity re-ranking.

Không nên trả 10 phim cùng Genre, Actor hoặc Director.

Có thể áp dụng:

- Tối đa 3 phim cùng Genre trong top 10.
- Giới hạn phim cùng Actor.
- Thêm phim mới.
- Thêm phim trending.
- Thêm phim exploration.

Phải giải thích trade-off:

- Exploitation: đề xuất thứ Customer chắc chắn thích.
- Exploration: thử nội dung mới.

---

## 17. Explainable Recommendation

Response nên có:

```json
{
  "movieId": 101,
  "score": 0.92,
  "rank": 1,
  "reason": "Similar to movies you rated highly",
  "source": "CONTENT_BASED",
  "genres": ["Action", "Sci-Fi"],
  "releaseYear": 2025,
  "posterUrl": "..."
}
```

Các reason hợp lệ:

- `Based on your recent Action bookings`.
- `Similar to movies you rated highly`.
- `Popular at your selected cinema`.
- `Because you watched movies by this performer`.

Reason phải phản ánh đúng nguồn điểm thật, không được sinh lý do sai lệch.

---

## 18. Event-Driven Update và Idempotency

Các event cần phân tích:

- `MOVIE_CREATED`.
- `MOVIE_UPDATED`.
- `MOVIE_DELETED`.
- `BOOKING_PAID`.
- `TICKET_USED`.
- `MOVIE_RATED`.
- `MOVIE_LIKED`.
- `MOVIE_DISLIKED`.
- `BOOKING_CANCELLED`.
- `REFUND_COMPLETED`.

Hiện tại `ON CONFLICT (user_id, movie_id)` chưa đủ để chống xử lý trùng event.

Phải bổ sung một trong các phương án:

```text
event_id UNIQUE
```

hoặc:

```text
processed_events
```

Yêu cầu:

- Cùng một event chỉ xử lý một lần.
- Retry không được nhân đôi interaction.
- Event đến trễ vẫn xử lý đúng.
- Có audit event.
- Có retry và dead-letter queue.

---

## 19. Database và Data Model

Phải rà soát các bảng:

### `movie_embeddings`

Lưu:

- `movie_id`.
- `title`.
- `genres`.
- `director`.
- `actors`.
- `description`.
- `poster_url`.
- `release_year`.
- `status`.
- `dense_vector`.

### `user_interactions`

Nên có:

- `id`.
- `user_id`.
- `movie_id`.
- `interaction_type`.
- `rating`.
- `weight`.
- `event_id`.
- `created_at`.
- `updated_at`.

### `movie_reviews` hoặc `user_movie_feedback`

Có thể bổ sung cho Phase 3:

- `review_id`.
- `user_id`.
- `movie_id`.
- `rating`.
- `comment`.
- `sentiment_label`.
- `sentiment_score`.
- `feedback_consistency`.
- `confidence_score`.

### `recommendation_sets`

Lưu:

- `recommendation_set_id`.
- `user_id`.
- `strategy`.
- `generated_at`.
- `expires_at`.

### `recommendations`

Lưu:

- `recommendation_id`.
- `recommendation_set_id`.
- `movie_id`.
- `score`.
- `rank`.
- `reason`.
- `source`.

### `ai_metrics_log`

Lưu metrics kỹ thuật và Recommendation metrics.

Nếu vẫn dùng unique `(user_id, movie_id)`, phải giải thích rõ đây là interaction tổng hợp hay interaction hiện hành. Nếu cần giữ lịch sử, phải có event history riêng.

---

## 20. API

Tối thiểu:

```http
GET /api/v1/recommendations/user/{user_id}?limit=10
GET /api/v1/recommendations/content/{movie_id}?limit=6
```

Có thể bổ sung:

```http
POST /api/v1/recommendations/feedback
GET /api/v1/recommendations/trending
GET /api/v1/recommendations/branch/{branch_id}
POST /api/v1/recommendations/refresh
```

Feedback request mẫu:

```json
{
  "userId": 1,
  "movieId": 10,
  "rating": 5,
  "comment": "Very good movie",
  "interactionType": "MOVIE_RATED"
}
```

Phải mô tả:

- Request.
- Response.
- Error response.
- Authorization.
- Guest handling.
- Rate limit nếu cần.

---

## 21. Feedback Loop

Phải ghi nhận các event:

```text
RECOMMENDATION_SHOWN
RECOMMENDATION_CLICKED
MOVIE_DETAIL_VIEWED
BOOKING_CREATED
BOOKING_PAID
TICKET_USED
RECOMMENDATION_DISMISSED
MOVIE_DISLIKED
```

Luồng:

```text
Recommendation shown
→ Customer click
→ Movie detail
→ Booking
→ Payment
→ Ticket used
→ Movie rating
→ Update user profile
```

Các event này dùng cho:

- Preference update.
- Ranking quality.
- CTR.
- Booking conversion.
- Model evaluation.

---

## 22. Cache và Resilience

Có thể cache:

```text
user:{id}:recommendations
movie:{id}:similar-movies
branch:{id}:trending
```

Cache phải được invalidation khi:

- Customer rating mới.
- Customer dislike.
- Booking mới.
- Movie cập nhật.
- Showtime thay đổi.
- Movie bị ẩn.

AI Service cần có:

- Timeout.
- Retry.
- Circuit breaker.
- Fallback.
- Correlation ID.
- Logging.
- Telemetry.

Recommendation lỗi không được làm hỏng Movie Listing, Movie Detail, Showtime, Booking hoặc Payment.

---

## 23. Academic Citation và Formula Research

Với mỗi công thức hoặc thuật toán phải cung cấp:

1. Tên công thức.
2. Công thức toán học.
3. Giải thích từng biến.
4. Bài báo hoặc tài liệu gốc.
5. Tác giả.
6. Năm xuất bản.
7. DOI hoặc link chính thống.
8. Vị trí áp dụng trong code.
9. Lý do lựa chọn.
10. Phương pháp thay thế.
11. Lý do không chọn phương pháp thay thế.
12. Giới hạn.
13. Điều kiện áp dụng.

Cần nghiên cứu tối thiểu:

- Cosine Similarity.
- Pearson Correlation.
- Pearson Shrinkage.
- Weighted Hybrid Scoring.
- Exponential Time Decay.
- Precision@K.
- Recall@K.
- NDCG@K.
- Coverage.
- Diversity.

Ưu tiên nguồn:

- Bài báo gốc.
- ACM.
- IEEE.
- Springer.
- ScienceDirect.
- DOI.
- Tài liệu chính thức của framework.

Không dùng blog cá nhân hoặc nội dung không có nguồn gốc thay cho bài báo học thuật.

Sau khi hoàn thành nghiên cứu, phải cập nhật đồng bộ các tài liệu kiến trúc,
API specification, database schema/migration, event contract, code và test.
Không được để công thức trong tài liệu khác với công thức đang chạy trong code.
Nếu một công thức mới chỉ được nghiên cứu nhưng chưa triển khai, phải ghi rõ
trạng thái `PROPOSED` hoặc `NOT_IMPLEMENTED`, không được mô tả như chức năng đã có.

Phải phân biệt:

- Academic formula.
- Heuristic.
- Business rule.
- Configuration weight.

---

## 24. Baseline và Evaluation

Phải xây dựng hoặc mô phỏng các baseline:

```text
Baseline 1: Popularity only
Baseline 2: Content-Based only
Baseline 3: Collaborative only
Baseline 4: Hybrid
```

Metrics cần có:

- Precision@K.
- Recall@K.
- NDCG@K.
- MAP@K nếu phù hợp.
- Coverage.
- Diversity.
- CTR.
- Movie detail view rate.
- Booking conversion.
- Ticket used conversion.
- Latency.
- Fallback rate.

Không được kết luận Hybrid tốt hơn nếu chưa có số liệu so sánh.

---

## 25. Test Cases bắt buộc

Phải viết test cho:

1. Guest.
2. Customer mới.
3. Customer có ít interaction.
4. Customer có nhiều interaction.
5. Rating 5★.
6. Rating 1★.
7. Rating 3★.
8. Rating và comment mâu thuẫn.
9. Customer thay đổi sở thích.
10. Phim mới.
11. Phim đã xem.
12. Phim không còn chiếu.
13. Phim không có tại branch.
14. Booking cancelled.
15. Refund do Cinema hủy showtime.
16. Event bị gửi trùng.
17. Event đến trễ.
18. AI Service timeout.
19. Không có embedding.
20. Không có collaborative data.
21. Danh sách bị trùng Genre.
22. User truy cập recommendation của User khác.
23. Xóa dữ liệu Customer.
24. Phim bị inactive.

---

## 26. Đối chiếu với code hiện tại

Phải tạo bảng Gap Analysis:

| Requirement | Current Code | Status | Missing Part | File/Module | Proposed Fix |
|---|---|---|---|---|---|

Chỉ được sử dụng các trạng thái:

```text
IMPLEMENTED
PARTIAL
NOT_IMPLEMENTED
```

Không được đánh dấu `IMPLEMENTED` nếu chỉ có:

- Comment trong code.
- Class nhưng chưa được gọi.
- Database column nhưng chưa có business logic.
- API trả dữ liệu mock.
- Telemetry chung nhưng chưa có Recommendation metric.

Mỗi dòng phải chỉ ra:

- File.
- Class.
- Method.
- Database table.
- Test tương ứng.

Các gap hiện cần kiểm tra đặc biệt:

### Đã có hoặc có một phần

- Content-Based Filtering.
- Movie Embedding.
- Cosine Similarity.
- Pearson Shrinkage Collaborative Filtering.
- Hybrid Scoring.
- Cold-Start Fallback.
- Recent Genre Booster.
- Một phần Time Decay.
- Một phần Telemetry.
- Adaptive Collaborative Weight.

### Chưa hoàn thiện

- Rating 1★ thành negative signal.
- Negative profile.
- Dislike exclusion.
- Time decay cho toàn bộ profile.
- Branch-aware scoring.
- Showtimes availability.
- Diversity.
- Click-to-booking feedback loop.
- Recommendation business metrics.
- Event idempotency bằng `event_id`.
- Comment sentiment.

---

## 27. Thứ tự ưu tiên triển khai

### Ưu tiên 1 — phải hoàn thiện ngay

1. Rating 1★ tạo negative signal.
2. Rating 5★ tạo positive signal.
3. Tách positive và negative profile.
4. Time decay cho toàn bộ profile.
5. Dislike exclusion.
6. Availability filtering.
7. Fallback rõ ràng.
8. Event idempotency bằng `event_id`.

### Ưu tiên 2 — triển khai sau MVP

1. Branch-aware scoring.
2. Diversity re-ranking.
3. Recommendation business metrics.
4. Click-to-booking feedback loop.
5. Adaptive Collaborative Filtering.
6. Recommendation cache.

### Ưu tiên 3 — có thể để sau

1. Comment sentiment.
2. Aspect sentiment.
3. LLM explanation nâng cao.
4. Deep learning.
5. Reinforcement learning.

Không được chuyển sang ưu tiên 2 nếu các mục ưu tiên 1 chưa có test và chưa được chứng minh hoạt động.

---

## 28. Tiêu chí nghiệm thu

Task chỉ được xem là hoàn thành khi:

- Có Content-Based hoạt động.
- Có Popularity fallback.
- Có Positive/Negative Feedback.
- Rating 1★ thực sự làm giảm điểm nội dung tương tự.
- Rating 5★ thực sự làm tăng điểm nội dung tương tự.
- Có Time Decay cho toàn bộ profile.
- Có cold-start.
- Có availability filtering theo branch/showtime.
- Không đề xuất lại phim đã dislike.
- Có fallback rõ ràng.
- Event retry không tạo duplicate interaction.
- Có explainable reason.
- Có baseline so sánh.
- Có citation cho công thức chính.
- Công thức trong tài liệu khớp với code.
- Có unit test và integration test.
- Có metrics đánh giá offline hoặc online.
- Có Gap Analysis với trạng thái chính xác.
- Tài liệu kiến trúc, API, database, event contract, code và test phải phản ánh
  cùng một phiên bản Recommendation; mọi điểm khác biệt phải được ghi trong Gap Analysis.
- Không trình bày heuristic như công thức đã được chứng minh.
- Không để Collaborative Filtering chi phối khi dữ liệu User chưa đủ.

Mối liên hệ bắt buộc phải chứng minh là:

```text
Công thức lý thuyết
→ Code thực tế
→ Dữ liệu Cinema
→ Kết quả đánh giá
→ Quyết định nghiệp vụ
```

