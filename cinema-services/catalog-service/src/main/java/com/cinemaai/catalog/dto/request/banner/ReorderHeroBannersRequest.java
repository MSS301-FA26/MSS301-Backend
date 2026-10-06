package com.cinemaai.catalog.dto.request.banner;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderHeroBannersRequest(
        @NotEmpty(message = "Danh sách banner IDs không được rỗng")
        List<Long> bannerIds
) {
}
