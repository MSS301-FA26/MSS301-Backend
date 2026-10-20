package com.cinemaai.catalog.dto.request.cinema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RoomLayoutConfigRequest(
        @Min(value = 1, message = "Số hàng phải lớn hơn hoặc bằng 1")
        @Max(value = 52, message = "Số hàng tối đa là 52")
        Integer rowCount,

        @Min(value = 1, message = "Số ghế trên mỗi hàng phải lớn hơn hoặc bằng 1")
        @Max(value = 50, message = "Số ghế trên mỗi hàng tối đa là 50")
        Integer columnCount,

        @Min(value = 0, message = "Vị trí lối đi không thể âm")
        @Max(value = 50, message = "Vị trí lối đi không được vượt quá số ghế")
        Integer aislePosition
) {
}
