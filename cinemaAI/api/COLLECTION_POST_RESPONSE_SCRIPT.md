# Collection Post-response Script

Dán script này vào **Collection SBA -> Scripts -> Post-response** trong Postman.

Script này dùng `pm.collectionVariables`, phù hợp khi Postman đang để `No environment`.

```javascript
let body = {};

try {
  body = pm.response.json();
} catch (error) {
  body = {};
}

pm.collectionVariables.set("lastStatus", pm.response.code);

if (body.message) {
  pm.collectionVariables.set("lastMessage", body.message);
}

if (body.success !== undefined) {
  pm.collectionVariables.set("lastSuccess", body.success);
}

if (body.data?.accessToken) {
  pm.collectionVariables.set("accessToken", body.data.accessToken);
  pm.collectionVariables.set("tokenType", body.data.tokenType || "Bearer");
}

if (body.data?.refreshToken) {
  pm.collectionVariables.set("refreshToken", body.data.refreshToken);
}

if (body.data?.user?.id) {
  pm.collectionVariables.set("userId", body.data.user.id);
}

if (body.data?.user?.email) {
  pm.collectionVariables.set("userEmail", body.data.user.email);
}

if (body.data?.user?.roles?.includes("ADMIN") && body.data?.accessToken) {
  pm.collectionVariables.set("adminToken", body.data.accessToken);
}

if (body.data?.id) {
  const requestName = pm.info.requestName.toLowerCase();
  const isMovieActorAssignment = requestName.includes("actor") && requestName.includes("movie");

  if (requestName.includes("genre")) {
    pm.collectionVariables.set("genreId", body.data.id);
  }

  if (requestName.includes("movie")) {
    pm.collectionVariables.set("movieId", body.data.id);
  }

  if (requestName.includes("actor") && !isMovieActorAssignment) {
    pm.collectionVariables.set("actorId", body.data.id);
  }

  if (requestName.includes("cinema")) {
    pm.collectionVariables.set("cinemaId", body.data.id);
  }

  if (requestName.includes("phòng") || requestName.includes("room")) {
    pm.collectionVariables.set("roomId", body.data.id);
  }

  if (requestName.includes("suất") || requestName.includes("showtime")) {
    pm.collectionVariables.set("showtimeId", body.data.id);
  }

  if (requestName.includes("booking") || requestName.includes("hold")) {
    pm.collectionVariables.set("bookingId", body.data.id);
  }
}

if (Array.isArray(body.data) && body.data.length > 0) {
  const first = body.data[0];

  if (first.id) {
    pm.collectionVariables.set("firstId", first.id);
  }

  if (first.seatId || first.id) {
    const requestName = pm.info.requestName.toLowerCase();
    if (requestName.includes("ghế") || requestName.includes("seat")) {
      pm.collectionVariables.set("seatId", first.seatId || first.id);
    }
  }
}

if (body.data?.seats?.[0]) {
  const firstSeat = body.data.seats[0];
  pm.collectionVariables.set("seatId", firstSeat.seatId || firstSeat.id);
}

if (body.data?.qrCode) {
  pm.collectionVariables.set("qrCode", body.data.qrCode);
}

if (body.data?.bookingCode) {
  pm.collectionVariables.set("bookingCode", body.data.bookingCode);
}
```

## Authorization ở các request

Ở tab Authorization hoặc Headers của request cần token, dùng:

```http
Authorization: Bearer {{accessToken}}
```

Nếu request chỉ admin dùng:

```http
Authorization: Bearer {{adminToken}}
```

## baseUrl

Vì đang dùng collection variables, tạo thêm biến collection:

```text
baseUrl = http://localhost:8080
```
