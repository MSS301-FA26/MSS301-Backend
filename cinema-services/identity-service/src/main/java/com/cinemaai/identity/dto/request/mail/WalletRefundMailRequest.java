package com.cinemaai.identity.dto.request.mail;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WalletRefundMailRequest(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email must be valid")
        String to,

        @NotBlank(message = "Booking code is required")
        String bookingCode,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "New balance is required")
        BigDecimal newBalance,

        String reason
) {}
