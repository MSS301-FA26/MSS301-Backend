package com.cinemaai.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WithdrawalCreateRequest(
        @NotNull(message = "Số tiền rút không được để trống")
        @DecimalMin(value = "10000", message = "Số tiền rút tối thiểu là 10.000 VNĐ")
        BigDecimal amount,

        @NotBlank(message = "Tên ngân hàng không được để trống")
        @Size(max = 100, message = "Tên ngân hàng tối đa 100 ký tự")
        String bankName,

        @NotBlank(message = "Số tài khoản không được để trống")
        @Size(max = 50, message = "Số tài khoản tối đa 50 ký tự")
        String accountNumber,

        @NotBlank(message = "Tên chủ tài khoản không được để trống")
        @Size(max = 120, message = "Tên chủ tài khoản tối đa 120 ký tự")
        String accountHolder,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String walletPhone
) {}
