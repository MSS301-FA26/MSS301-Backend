# Coding Standards Cho Người Mới: Hiểu Đúng Ngay Từ Đầu

## Coding Standards for Beginners: Start Right to Code Like a Pro

Tài liệu này là bộ quy tắc coding standard cho backend **CinemaAI**. Mục tiêu là giúp thành viên mới hiểu cách viết code thống nhất trong dự án Java 17, Spring Boot, REST API, JPA, Security, DTO, Service, Controller và Integration Test.

Code không chỉ cần chạy được. Code còn phải dễ đọc, dễ review, dễ sửa và dễ mở rộng khi dự án lớn lên.

## 1. Coding Standards and Guidelines Là Gì?

**Định nghĩa**

Coding Standards and Guidelines là tập hợp các quy tắc được thống nhất trong team để mọi người viết code theo cùng một phong cách. Các quy tắc này bao gồm cách đặt tên, chia package, viết API, xử lý lỗi, viết service, tạo entity, viết test và cấu hình hệ thống.

**Trong CinemaAI, coding standard dùng để:**

- Giữ code đồng nhất dù nhiều người cùng phát triển.
- Giúp người mới đọc code nhanh hơn.
- Giảm lỗi khi review hoặc merge code.
- Tránh tạo endpoint, class, DTO hoặc service theo nhiều kiểu khác nhau.
- Giúp backend dễ bảo trì khi thêm module mới như movie, booking, payment, recommendation.

## 2. Mục Đích Của Coding Standard

### 2.1 Tính nhất quán

**Quy tắc**

Mọi module nên có cấu trúc và cách viết tương tự nhau. Nếu `MovieController` dùng `ApiResponse<T>`, `BookingController`, `FoodController`, `ShowtimeController` cũng nên làm như vậy.

**Vì sao cần**

Người đọc chỉ cần học một pattern là có thể hiểu các module khác.

**Ví dụ đúng**

```java
@GetMapping("/{movieId}")
public ApiResponse<MovieResponse> getMovie(@PathVariable Long movieId) {
    return ApiResponse.success(movieService.getPublic(movieId));
}
```

**Ví dụ sai**

```java
@GetMapping("/{movieId}")
public Movie getMovie(@PathVariable Long movieId) {
    return movieRepository.findById(movieId).orElseThrow();
}
```

**Ghi chú áp dụng cho CinemaAI**

Controller không trả entity trực tiếp. Controller trả DTO được bọc bởi `ApiResponse`.

### 2.2 Khả năng đọc và bảo trì

**Quy tắc**

Tên class, method, biến và endpoint phải nói rõ mục đích sử dụng.

**Vì sao cần**

Code đọc được nhanh thì sửa bug nhanh hơn và review ít hiểu nhầm hơn.

**Ví dụ đúng**

```java
public BookingResponse requestRefund(String email, Long bookingId, String reason) {
    // ...
}
```

**Ví dụ sai**

```java
public BookingResponse doThing(String x, Long y, String z) {
    // ...
}
```

**Ghi chú áp dụng cho CinemaAI**

Tên method trong service nên phản ánh nghiệp vụ: `holdSeats`, `createBooking`, `markRefunded`, `getMyBookings`.

### 2.3 Chất lượng code

**Quy tắc**

Business rule phải nằm ở service, không nằm ở controller, repository hoặc DTO.

**Vì sao cần**

Service là nơi dễ test, dễ review và dễ kiểm soát transaction.

**Ví dụ đúng**

```java
@Transactional
public BookingResponse createBooking(String email, CreateBookingRequest request) {
    releaseExpiredHolds();
    User user = userService.getByEmail(email);
    Booking booking = findBooking(request.holdBookingId());
    validateOwner(booking, user);
    // business rules continue here
}
```

**Ví dụ sai**

```java
@PostMapping
public ApiResponse<BookingResponse> createBooking(@RequestBody CreateBookingRequest request) {
    Booking booking = bookingRepository.findById(request.holdBookingId()).orElseThrow();
    if (booking.getStatus() != BookingStatus.HOLDING) {
        throw new RuntimeException("Invalid booking");
    }
    return ApiResponse.success(null);
}
```

**Ghi chú áp dụng cho CinemaAI**

Controller chỉ điều phối HTTP. Service mới là nơi chứa nghiệp vụ đặt vé, thanh toán, xác thực, recommendation.

## 3. Nguyên Tắc Nền Tảng

### 3.1 KISS - Keep It Simple

**Quy tắc**

Viết code đơn giản nhất có thể, miễn là vẫn rõ nghiệp vụ và đúng yêu cầu.

**Vì sao cần**

Code đơn giản dễ đọc, dễ test và ít lỗi hơn code quá thông minh.

**Ví dụ đúng**

```java
private Pageable pageable(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(size, 100));
    return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "releaseDate"));
}
```

**Ví dụ sai**

```java
private Pageable pageable(int p, int s) {
    return PageRequest.of(p < 0 ? 0 : p, s < 1 ? 1 : s > 100 ? 100 : s);
}
```

**Ghi chú áp dụng cho CinemaAI**

Không cần nén quá nhiều logic vào một dòng. Ưu tiên code dễ đọc cho người mới.

### 3.2 DRY - Don't Repeat Yourself

**Quy tắc**

Không lặp lại cùng một logic ở nhiều nơi. Nếu cùng một nghiệp vụ xuất hiện nhiều lần, hãy đưa vào private method hoặc service phù hợp.

**Vì sao cần**

Khi rule thay đổi, chỉ cần sửa một chỗ.

**Ví dụ đúng**

```java
private Booking findBooking(Long id) {
    return bookingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Booking not found"));
}
```

**Ví dụ sai**

```java
Booking booking = bookingRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Booking not found"));

Booking anotherBooking = bookingRepository.findById(anotherId)
        .orElseThrow(() -> new NotFoundException("Booking not found"));
```

**Ghi chú áp dụng cho CinemaAI**

Các helper như `findById`, `validateOwner`, `toResponse`, `pageable` nên được tái sử dụng trong service.

### 3.3 SOLID Áp Dụng Vừa Đủ

**Quy tắc**

Mỗi class nên có một trách nhiệm chính. Không biến một service thành nơi làm mọi thứ.

**Vì sao cần**

Class nhỏ và rõ trách nhiệm thì dễ test, dễ sửa, dễ mở rộng.

**Ví dụ đúng**

```java
@Service
public class PaymentService {
    // xử lý payment
}

@Service
public class QrTicketService {
    // xử lý QR ticket
}
```

**Ví dụ sai**

```java
@Service
public class BookingService {
    // booking
    // payment
    // JWT
    // mail
    // recommendation
    // file upload
}
```

**Ghi chú áp dụng cho CinemaAI**

Tách nghiệp vụ theo module: `AuthService`, `BookingService`, `PaymentService`, `MovieService`, `RecommendationService`.

### 3.4 Dependency Injection

**Quy tắc**

Dùng constructor injection. Trong dự án này, ưu tiên `final` field kết hợp `@RequiredArgsConstructor`.

**Vì sao cần**

Dependency rõ ràng, dễ test và tránh object bị thiếu dependency khi chạy.

**Ví dụ đúng**

```java
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
}
```

**Ví dụ sai**

```java
@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;
}
```

**Ghi chú áp dụng cho CinemaAI**

Không dùng field injection cho code mới.

## 4. Quy Tắc Đặt Tên Trong Java

### 4.1 Class Name

**Quy tắc**

Class dùng `PascalCase`, tên phải thể hiện vai trò.

**Ví dụ đúng**

```java
public class MovieController {
}

public class BookingService {
}

public class MovieRepository {
}
```

**Ví dụ sai**

```java
public class moviecontroller {
}

public class BookingManagerThing {
}
```

**Ghi chú áp dụng cho CinemaAI**

Tên class nên đi theo hậu tố quen thuộc: `Controller`, `Service`, `Repository`, `Mapper`, `Request`, `Response`, `Exception`.

### 4.2 Method Và Variable Name

**Quy tắc**

Method và variable dùng `camelCase`. Method nên bắt đầu bằng động từ hoặc cụm hành động rõ nghĩa.

**Ví dụ đúng**

```java
public MovieResponse getPublic(Long movieId) {
    return toResponse(findById(movieId));
}

private Long movieId;
private String bookingCode;
```

**Ví dụ sai**

```java
public MovieResponse PublicMovie(Long Movie_ID) {
    return null;
}

private String data;
private Long x;
```

**Ghi chú áp dụng cho CinemaAI**

Tên biến ID nên dùng đúng resource: `movieId`, `bookingId`, `showtimeId`, `userId`.

### 4.3 Constant Name

**Quy tắc**

Constant dùng `UPPER_SNAKE_CASE`, khai báo `private static final` nếu chỉ dùng trong class.

**Ví dụ đúng**

```java
private static final int HOLD_MINUTES = 10;
private static final int EMAIL_VERIFICATION_EXPIRES_IN_SECONDS = 90;
```

**Ví dụ sai**

```java
private static final int holdMinutes = 10;
private int MAX_RETRY = 3;
```

**Ghi chú áp dụng cho CinemaAI**

Không dùng biến global mutable. Nếu là cấu hình thay đổi theo môi trường, dùng `application.properties`, `.env` hoặc `@ConfigurationProperties`.

### 4.4 Package Name

**Quy tắc**

Package dùng chữ thường, ngắn gọn, theo layer hoặc module hiện có.

**Ví dụ đúng**

```text
com.sba301.cinemaai.controller
com.sba301.cinemaai.service
com.sba301.cinemaai.repository
com.sba301.cinemaai.dto.request.movie
com.sba301.cinemaai.dto.response.booking
```

**Ví dụ sai**

```text
com.sba301.cinemaai.Controllers
com.sba301.cinemaai.movieService
com.sba301.cinemaai.DTO.Response
```

**Ghi chú áp dụng cho CinemaAI**

Giữ đúng cấu trúc hiện có: controller, service, repository, entity, dto, mapper, exception, security, config, enums.

## 5. API Naming Standard

Đây là phần quan trọng nhất khi tạo endpoint mới.

### 5.1 Prefix API

**Quy tắc**

Tất cả REST API chính dùng prefix `/api/v1`.

**Vì sao cần**

Versioning giúp sau này có thể tạo `/api/v2` mà không phá client cũ.

**Ví dụ đúng**

```http
GET /api/v1/movies
GET /api/v1/bookings
POST /api/v1/auth/login
```

**Ví dụ sai**

```http
GET /movies
GET /api/movies
GET /v1/movies
```

**Ghi chú áp dụng cho CinemaAI**

Controller mới nên đặt base path bằng `@RequestMapping("/api/v1/...")`.

### 5.2 Dùng Danh Từ Cho Resource

**Quy tắc**

Mặc định dùng danh từ trong path. Hãy để HTTP method làm động từ.

**Vì sao cần**

REST API mô tả tài nguyên. Hành động đã được thể hiện bằng `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.

**Ví dụ đúng**

```http
GET /api/v1/movies
POST /api/v1/movies
GET /api/v1/movies/{movieId}
PUT /api/v1/movies/{movieId}
DELETE /api/v1/movies/{movieId}
```

**Ví dụ sai**

```http
GET /api/v1/getMovies
POST /api/v1/createMovie
PUT /api/v1/updateMovie/{movieId}
DELETE /api/v1/deleteMovie/{movieId}
```

**Ghi chú áp dụng cho CinemaAI**

Endpoint CRUD mới không được thêm verb như `get`, `create`, `update`, `delete` vào path.

### 5.3 Dùng Danh Từ Số Nhiều Cho Collection

**Quy tắc**

Collection dùng danh từ số nhiều.

**Vì sao cần**

`/movies` biểu diễn tập hợp movie. `/movies/{movieId}` biểu diễn một movie cụ thể.

**Ví dụ đúng**

```http
/api/v1/movies
/api/v1/bookings
/api/v1/showtimes
/api/v1/actors
/api/v1/genres
/api/v1/notifications
```

**Ví dụ sai**

```http
/api/v1/movie
/api/v1/booking
/api/v1/showtime
```

**Ghi chú áp dụng cho CinemaAI**

Một vài endpoint legacy như `/api/v1/cinema` đang tồn tại như alias. API mới nên ưu tiên dạng số nhiều `/api/v1/cinemas`.

### 5.4 Dùng Nested Resource Khi Có Quan Hệ Sở Hữu

**Quy tắc**

Khi resource con thuộc resource cha, dùng nested path.

**Vì sao cần**

Path thể hiện rõ quan hệ dữ liệu.

**Ví dụ đúng**

```http
GET /api/v1/actors/{actorId}/movies
GET /api/v1/showtimes/{showtimeId}/seat-map
GET /api/v1/admin/rooms/{roomId}/seats
PUT /api/v1/admin/movies/{movieId}/actors
```

**Ví dụ sai**

```http
GET /api/v1/getMoviesByActor/{actorId}
GET /api/v1/seatMapByShowtime/{showtimeId}
PUT /api/v1/updateMovieActors/{movieId}
```

**Ghi chú áp dụng cho CinemaAI**

Nested resource nên dùng tối đa 2 đến 3 cấp. Nếu path quá dài, cân nhắc tạo resource riêng.

### 5.5 Dùng Action Endpoint Chỉ Khi Cần

**Quy tắc**

Chỉ dùng động từ hoặc action trong path khi hành động đó không phải CRUD thông thường.

**Vì sao cần**

Một số nghiệp vụ như login, logout, validate, check-in không biểu diễn tự nhiên bằng CRUD đơn giản.

**Ví dụ đúng**

```http
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
POST /api/v1/ticket-pricing/validate
POST /api/v1/bookings/{bookingId}/check-in
POST /api/v1/bookings/{bookingId}/refund-request
```

**Ví dụ sai**

```http
GET /api/v1/movies/searchMovies
POST /api/v1/bookings/createBooking
POST /api/v1/users/updateProfile
```

**Ghi chú áp dụng cho CinemaAI**

Nếu action có thể biến thành resource, ưu tiên danh từ:

```http
POST /api/v1/bookings/{bookingId}/refund-requests
```

thay vì:

```http
POST /api/v1/bookings/{bookingId}/requestRefund
```

### 5.6 Path Dùng Kebab Case

**Quy tắc**

Path dùng `kebab-case`, không dùng `camelCase` hoặc `snake_case`.

**Ví dụ đúng**

```http
/api/v1/ticket-pricing
/api/v1/favorite-actors
/api/v1/refund-request
/api/v1/check-in
```

**Ví dụ sai**

```http
/api/v1/ticketPricing
/api/v1/favorite_actors
/api/v1/refundRequest
/api/v1/checkIn
```

**Ghi chú áp dụng cho CinemaAI**

Path variable vẫn dùng `camelCase`, ví dụ `{movieId}`, `{bookingId}`.

### 5.7 Query Parameter Và Path Variable

**Quy tắc**

Query parameter và path variable dùng `camelCase`.

**Ví dụ đúng**

```http
GET /api/v1/movies?genreId=1&fromDate=2026-01-01&toDate=2026-12-31&page=0&size=20
GET /api/v1/movies/{movieId}
GET /api/v1/admin/users/{userId}
```

**Ví dụ sai**

```http
GET /api/v1/movies?genre_id=1&from_date=2026-01-01
GET /api/v1/movies/{id}
GET /api/v1/admin/users/{UserID}
```

**Ghi chú áp dụng cho CinemaAI**

Path variable nên có tên rõ resource, không dùng `{id}` nếu path có thể gây mơ hồ.

### 5.8 Admin Và Staff Namespace

**Quy tắc**

API cho admin đặt dưới `/api/v1/admin`. API cho staff đặt dưới `/api/v1/staff`.

**Ví dụ đúng**

```http
GET /api/v1/admin/movies
POST /api/v1/admin/showtimes
POST /api/v1/staff/check-in
```

**Ví dụ sai**

```http
GET /api/v1/movies/admin
POST /api/v1/showtimes/create-by-admin
POST /api/v1/check-in/staff
```

**Ghi chú áp dụng cho CinemaAI**

Namespace quyền truy cập nên nằm ngay sau version để dễ cấu hình security.

## 6. Controller Standard

### 6.1 Controller Chỉ Xử Lý HTTP

**Quy tắc**

Controller chỉ nhận request, validate input, lấy authenticated user nếu cần, gọi service và trả response.

**Vì sao cần**

Controller mỏng giúp business logic tập trung trong service.

**Ví dụ đúng**

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ApiResponse<BookingResponse> createBooking(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateBookingRequest request
) {
    return ApiResponse.success(
            bookingService.createBooking(user.getUsername(), request),
            "Booking created successfully"
    );
}
```

**Ví dụ sai**

```java
@PostMapping
public ApiResponse<BookingResponse> createBooking(@RequestBody CreateBookingRequest request) {
    Booking booking = bookingRepository.save(new Booking());
    booking.markPaid("QR");
    return ApiResponse.success(bookingMapper.toResponse(booking));
}
```

**Ghi chú áp dụng cho CinemaAI**

Không inject repository trực tiếp vào controller cho nghiệp vụ thông thường.

### 6.2 Response Format

**Quy tắc**

API thành công trả `ApiResponse<T>`. API phân trang dùng `PageResponse<T>` bên trong `ApiResponse`.

**Ví dụ đúng**

```java
public ApiResponse<PageResponse<MovieResponse>> searchMovies(...) {
    return ApiResponse.success(movieService.searchPublic(...));
}
```

**Ví dụ sai**

```java
public List<MovieResponse> searchMovies(...) {
    return movieService.searchPublic(...).items();
}
```

**Ghi chú áp dụng cho CinemaAI**

Giữ response thống nhất với `success`, `data`, `message`, `timestamp`.

### 6.3 Status Code

**Quy tắc**

Tạo mới thành công dùng `201 Created`. Lấy, cập nhật, xóa mềm thành công thường dùng `200 OK`.

**Ví dụ đúng**

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ApiResponse<MovieResponse> createMovie(@Valid @RequestBody MovieCreateRequest request) {
    return ApiResponse.success(movieService.create(request), "Movie created successfully");
}
```

**Ví dụ sai**

```java
@PostMapping
public ApiResponse<MovieResponse> createMovie(@RequestBody MovieCreateRequest request) {
    return ApiResponse.success(movieService.create(request));
}
```

**Ghi chú áp dụng cho CinemaAI**

Nếu endpoint tạo resource mới, thêm `@ResponseStatus(HttpStatus.CREATED)`.

## 7. Service Standard

### 7.1 Service Chứa Business Logic

**Quy tắc**

Các rule như kiểm tra trạng thái booking, trùng title, quyền sở hữu, hết hạn hold seat phải nằm trong service.

**Ví dụ đúng**

```java
if (booking.getStatus() != BookingStatus.HOLDING) {
    throw new BadRequestException("Booking is not in holding status");
}
```

**Ví dụ sai**

```java
if (booking.getStatus() != BookingStatus.HOLDING) {
    return ApiResponse.failure("Invalid booking");
}
```

**Ghi chú áp dụng cho CinemaAI**

Service throw custom exception. `GlobalExceptionHandler` sẽ lo format lỗi.

### 7.2 Transaction

**Quy tắc**

Method đọc dữ liệu dùng `@Transactional(readOnly = true)`. Method ghi dữ liệu dùng `@Transactional`.

**Vì sao cần**

Transaction giúp dữ liệu nhất quán, đặc biệt trong booking, payment, seat hold.

**Ví dụ đúng**

```java
@Transactional(readOnly = true)
public MovieResponse getPublic(Long movieId) {
    return toResponse(findById(movieId));
}

@Transactional
public MovieResponse updateStatus(Long movieId, MovieStatusUpdateRequest request) {
    Movie movie = findById(movieId);
    movie.changeStatus(request.status());
    return toResponse(movie);
}
```

**Ví dụ sai**

```java
public MovieResponse updateStatus(Long movieId, MovieStatusUpdateRequest request) {
    Movie movie = movieRepository.findById(movieId).get();
    movie.changeStatus(request.status());
    return toResponse(movie);
}
```

**Ghi chú áp dụng cho CinemaAI**

Các nghiệp vụ nhiều bước như booking, payment, refund, check-in bắt buộc cần transaction.

### 7.3 Exception Trong Service

**Quy tắc**

Dùng custom exception theo đúng ngữ nghĩa.

| Trường hợp | Exception nên dùng |
| --- | --- |
| Request sai nghiệp vụ | `BadRequestException` |
| Không tìm thấy resource | `NotFoundException` |
| Trùng dữ liệu | `ConflictException` |
| Chưa đăng nhập hoặc token sai | `UnauthorizedException` |
| Không đủ quyền | `ForbiddenException` |

**Ví dụ đúng**

```java
private Movie findById(Long movieId) {
    return movieRepository.findById(movieId)
            .orElseThrow(() -> new NotFoundException("Movie not found"));
}
```

**Ví dụ sai**

```java
private Movie findById(Long movieId) {
    return movieRepository.findById(movieId).orElseThrow(RuntimeException::new);
}
```

**Ghi chú áp dụng cho CinemaAI**

Không throw `RuntimeException` chung chung cho lỗi nghiệp vụ.

## 8. DTO Standard

### 8.1 Request Và Response DTO

**Quy tắc**

Không expose entity trực tiếp ra API. Tạo DTO riêng cho request và response.

**Ví dụ đúng**

```java
public record MovieCreateRequest(
        @NotBlank(message = "Title is required")
        String title,

        @Min(value = 1, message = "Duration must be positive")
        int durationMinutes
) {
}
```

**Ví dụ sai**

```java
@PostMapping
public ApiResponse<Movie> createMovie(@RequestBody Movie movie) {
    return ApiResponse.success(movieRepository.save(movie));
}
```

**Ghi chú áp dụng cho CinemaAI**

DTO đang được chia theo `dto.request.*` và `dto.response.*`. Tiếp tục giữ cấu trúc này.

### 8.2 Validation

**Quy tắc**

Request body cần được validate bằng Jakarta Validation khi field có ràng buộc.

**Ví dụ đúng**

```java
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}
```

**Ví dụ sai**

```java
public record LoginRequest(
        String username,
        String password
) {
}
```

**Ghi chú áp dụng cho CinemaAI**

Controller nhận request body phải dùng `@Valid @RequestBody`.

## 9. Entity Standard

### 9.1 Entity Không Nên Có Setter Tràn Lan

**Quy tắc**

Entity dùng `@Getter`, hạn chế public setter. Thay đổi trạng thái qua domain method.

**Vì sao cần**

Domain method giúp kiểm soát cách dữ liệu thay đổi.

**Ví dụ đúng**

```java
public void changeStatus(MovieStatus status) {
    this.status = status;
}

public void updateMedia(String trailerUrl, String posterUrl, String avatarUrl) {
    this.trailerUrl = trailerUrl;
    this.posterUrl = posterUrl;
    this.avatarUrl = avatarUrl;
}
```

**Ví dụ sai**

```java
movie.setStatus(MovieStatus.INACTIVE);
movie.setTitle(null);
movie.setDurationMinutes(-1);
```

**Ghi chú áp dụng cho CinemaAI**

Code mới nên giữ style entity hiện tại: `@Getter`, constructor rõ nghĩa, method như `markPaid`, `cancel`, `expire`, `checkIn`.

### 9.2 Constructor Cho JPA

**Quy tắc**

Entity cần no-args constructor cho JPA, nhưng nên để `protected`.

**Ví dụ đúng**

```java
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie extends BaseEntity {
    // ...
}
```

**Ví dụ sai**

```java
@Entity
@NoArgsConstructor
@Setter
public class Movie {
    // ...
}
```

**Ghi chú áp dụng cho CinemaAI**

Entity có audit fields nên extend `BaseEntity`.

### 9.3 Mapping Database

**Quy tắc**

Tên table và column trong database dùng `snake_case`. Tên field Java dùng `camelCase`.

**Ví dụ đúng**

```java
@Column(name = "release_date")
private LocalDate releaseDate;

@Column(name = "duration_minutes", nullable = false)
private int durationMinutes;
```

**Ví dụ sai**

```java
@Column(name = "releaseDate")
private LocalDate release_date;
```

**Ghi chú áp dụng cho CinemaAI**

Giữ naming database thống nhất với các bảng hiện có như `movies`, `booking_seats`, `ticket_pricing_rules`.

## 10. Repository Standard

### 10.1 Repository Chỉ Truy Vấn Dữ Liệu

**Quy tắc**

Repository không chứa business rule. Repository chỉ định nghĩa cách truy vấn.

**Ví dụ đúng**

```java
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    Optional<Movie> findByTitle(String title);

    boolean existsByTitle(String title);
}
```

**Ví dụ sai**

```java
public interface MovieRepository extends JpaRepository<Movie, Long> {

    default void deleteMovieByBusinessRule(Movie movie) {
        movie.changeStatus(MovieStatus.INACTIVE);
    }
}
```

**Ghi chú áp dụng cho CinemaAI**

Soft delete, check permission, validate status phải nằm ở service.

## 11. Mapper Standard

### 11.1 Mapper Chuyển Entity Sang DTO

**Quy tắc**

Mapper chỉ chuyển đổi dữ liệu. Không đặt business rule nặng trong mapper.

**Ví dụ đúng**

```java
public MovieResponse toMovieResponse(Movie movie, List<Genre> genres, List<ActorResponse> actors) {
    return new MovieResponse(
            movie.getId(),
            movie.getTitle(),
            movie.getDescription(),
            genres.stream().map(this::toGenreResponse).toList(),
            actors
    );
}
```

**Ví dụ sai**

```java
public MovieResponse toMovieResponse(Movie movie) {
    if (movie.getStatus() == MovieStatus.INACTIVE) {
        throw new NotFoundException("Movie not found");
    }
    // mapping
}
```

**Ghi chú áp dụng cho CinemaAI**

Business rule như ẩn phim inactive phải nằm ở service, không nằm trong mapper.

## 12. Error Handling Standard

### 12.1 Response Lỗi Thống Nhất

**Quy tắc**

Tất cả lỗi API nên đi qua `GlobalExceptionHandler` và trả `ErrorResponse`.

**Ví dụ đúng**

```java
@ExceptionHandler(NotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(
        NotFoundException exception,
        HttpServletRequest request
) {
    return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
}
```

**Ví dụ sai**

```java
try {
    return ApiResponse.success(service.doSomething());
} catch (Exception exception) {
    return ApiResponse.failure(exception.getMessage());
}
```

**Ghi chú áp dụng cho CinemaAI**

Không try-catch trong controller để format lỗi nghiệp vụ. Hãy throw exception và để handler xử lý.

## 13. Security Và Config Standard

### 13.1 Không Hardcode Secret Mới

**Quy tắc**

Không commit thêm password, API key, JWT secret, payment secret thật vào source code.

**Vì sao cần**

Secret bị commit có thể bị lộ và gây rủi ro bảo mật.

**Ví dụ đúng**

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY:dummy-openai-key}
app.jwt.secret=${JWT_SECRET:cineai-development-secret-key-please-change-123456}
```

**Ví dụ sai**

```properties
spring.ai.openai.api-key=sk-real-secret-key
app.jwt.secret=my-production-secret
```

**Ghi chú áp dụng cho CinemaAI**

Dùng `.env.example` để hướng dẫn biến môi trường. File `.env` local không commit.

### 13.2 Phân Quyền API

**Quy tắc**

API public, authenticated, admin, staff phải được đặt path rõ ràng và cấu hình trong security.

**Ví dụ đúng**

```http
GET /api/v1/movies
GET /api/v1/users/me
GET /api/v1/admin/users
POST /api/v1/staff/check-in
```

**Ví dụ sai**

```http
GET /api/v1/users/admin-list
POST /api/v1/check-in-admin
```

**Ghi chú áp dụng cho CinemaAI**

Admin API đặt dưới `/api/v1/admin`. Staff API đặt dưới `/api/v1/staff`.

## 14. Logging Standard

### 14.1 Dùng Slf4j

**Quy tắc**

Dùng `@Slf4j` để log. Không dùng `System.out.println` trong code backend.

**Ví dụ đúng**

```java
@Slf4j
@Service
public class PaymentService {

    public void confirmPayment(Payment payment) {
        log.info("Payment {} confirmed for booking {}", payment.getId(), payment.getBooking().getBookingCode());
    }
}
```

**Ví dụ sai**

```java
System.out.println("Payment confirmed: " + payment.getId());
```

**Ghi chú áp dụng cho CinemaAI**

Không log password, OTP, access token, refresh token, payment hash secret hoặc API key.

## 15. Comment Và Documentation

### 15.1 Comment Khi Logic Khó Hiểu

**Quy tắc**

Không comment những gì code đã nói rõ. Chỉ comment khi cần giải thích nghiệp vụ hoặc lý do kỹ thuật.

**Ví dụ đúng**

```java
// VNPAY expects amount in VND multiplied by 100.
BigDecimal vnpAmount = amount.multiply(BigDecimal.valueOf(100));
```

**Ví dụ sai**

```java
// Set title to title
this.title = title;
```

**Ghi chú áp dụng cho CinemaAI**

Không bắt buộc module header dài ở đầu mỗi file Java. Ưu tiên tên class/method rõ nghĩa và OpenAPI annotation cho API cần mô tả.

### 15.2 API Documentation

**Quy tắc**

API quan trọng nên có `@Operation`, `@Tag` hoặc được ghi trong tài liệu API phase khi cần test bằng Postman/Swagger.

**Ví dụ đúng**

```java
@Operation(summary = "Create new movie", description = "Create a new movie for admin")
@PostMapping
public ApiResponse<MovieResponse> createMovie(@Valid @RequestBody MovieCreateRequest request) {
    return ApiResponse.success(movieService.create(request));
}
```

**Ví dụ sai**

```java
@PostMapping("/do")
public Object doSomething(@RequestBody Object request) {
    return null;
}
```

**Ghi chú áp dụng cho CinemaAI**

Endpoint mới nên rõ tên ngay từ path, không phụ thuộc hoàn toàn vào Swagger description.

## 16. Testing Standard

### 16.1 Integration Test Cho API Quan Trọng

**Quy tắc**

API quan trọng nên có integration test với `@SpringBootTest` và `@AutoConfigureMockMvc`.

**Ví dụ đúng**

```java
@SpringBootTest
@AutoConfigureMockMvc
class MovieIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldManageGenresAndMoviesThroughAdminAndPublicApis() throws Exception {
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk());
    }
}
```

**Ví dụ sai**

```java
class MovieTests {

    @Test
    void test() {
        assertTrue(true);
    }
}
```

**Ghi chú áp dụng cho CinemaAI**

Test profile dùng H2 trong `src/test/resources/application.properties`. Không phụ thuộc database local của developer.

### 16.2 Dữ Liệu Test

**Quy tắc**

Dữ liệu test nên unique để tránh đụng dữ liệu giữa các test.

**Ví dụ đúng**

```java
String email = "phase3.admin." + System.nanoTime() + "@example.com";
```

**Ví dụ sai**

```java
String email = "admin@example.com";
```

**Ghi chú áp dụng cho CinemaAI**

Khi test tạo user, movie, genre, booking, nên tạo dữ liệu riêng trong test thay vì phụ thuộc seeder.

## 17. Formatting Và Style Chung

### 17.1 Indentation

**Quy tắc**

Dùng 4 spaces cho Java. Không dùng tab lẫn spaces lộn xộn.

**Ví dụ đúng**

```java
public ApiResponse<MovieResponse> getMovie(@PathVariable Long movieId) {
    return ApiResponse.success(movieService.getPublic(movieId));
}
```

**Ví dụ sai**

```java
public ApiResponse<MovieResponse> getMovie(@PathVariable Long movieId) {
return ApiResponse.success(movieService.getPublic(movieId));
}
```

**Ghi chú áp dụng cho CinemaAI**

Repo hiện chưa cấu hình Checkstyle/Spotless, nên developer cần tự giữ style nhất quán khi code và review.

### 17.2 Import

**Quy tắc**

Không để unused import. Import static chỉ dùng khi giúp test dễ đọc.

**Ví dụ đúng**

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

**Ví dụ sai**

```java
import java.util.*;
import com.sba301.cinemaai.entity.*;
```

**Ghi chú áp dụng cho CinemaAI**

Tránh wildcard import trong code mới.

## 18. Checklist Trước Khi Merge

Trước khi tạo pull request hoặc merge code, hãy tự kiểm tra:

- API mới có bắt đầu bằng `/api/v1` không?
- API path có dùng danh từ số nhiều không?
- API CRUD có tránh các path như `/getX`, `/createX`, `/updateX`, `/deleteX` không?
- Action endpoint có thật sự là nghiệp vụ đặc biệt không?
- Controller có mỏng không, hay đang chứa business logic?
- Request body có `@Valid` và DTO có validation chưa?
- Response có dùng `ApiResponse<T>` không?
- Service method đọc có `@Transactional(readOnly = true)` chưa?
- Service method ghi có `@Transactional` chưa?
- Có dùng custom exception thay vì `RuntimeException` chung chung không?
- Entity có tránh public setter tràn lan không?
- Có expose entity trực tiếp ra response không?
- Có hardcode secret, password, token, API key mới không?
- Có dùng `@Slf4j` thay vì `System.out.println` không?
- API quan trọng có integration test bằng MockMvc chưa?
- Tên class, method, biến, package có đúng convention không?

## 19. Kết Luận

Coding standard không phải để làm code dài hơn hay khó viết hơn. Coding standard giúp team viết code cùng một ngôn ngữ, giảm hiểu nhầm, giảm lỗi và giúp người mới vào dự án bắt nhịp nhanh hơn.

Với CinemaAI, hãy nhớ quy tắc quan trọng nhất khi viết API:

```text
Mặc định dùng danh từ trong path.
Để HTTP method làm động từ.
Chỉ dùng action endpoint khi đó là nghiệp vụ đặc biệt, không phải CRUD thông thường.
```
