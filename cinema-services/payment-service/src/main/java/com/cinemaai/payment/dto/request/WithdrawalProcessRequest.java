package com.cinemaai.payment.dto.request;

import jakarta.validation.constraints.Size;

public record WithdrawalProcessRequest(
        String method,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {}
