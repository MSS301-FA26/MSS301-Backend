package com.cinemaai.identity.controller;

import com.cinemaai.identity.dto.request.mail.WalletRefundMailRequest;
import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/mail")
@RequiredArgsConstructor
@Tag(name = "Internal - Mail", description = "Internal service-to-service mail dispatching")
public class InternalMailController {

    private final MailService mailService;

    @PostMapping("/refund-notice")
    @Operation(summary = "Gửi email thông báo hoàn tiền vào CineWallet")
    public ApiResponse<Void> sendRefundNotice(@Valid @RequestBody WalletRefundMailRequest request) {
        mailService.sendWalletRefundNotice(
                request.to(),
                request.bookingCode(),
                request.amount(),
                request.newBalance(),
                request.reason()
        );
        return ApiResponse.success(null, "Wallet refund email dispatched successfully");
    }
}
