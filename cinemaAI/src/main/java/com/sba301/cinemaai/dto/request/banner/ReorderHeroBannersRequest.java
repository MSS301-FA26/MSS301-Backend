package com.sba301.cinemaai.dto.request.banner;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderHeroBannersRequest(
        @NotEmpty(message = "Danh sách banner IDs không được rỗng")
        List<Long> bannerIds
) {
}
