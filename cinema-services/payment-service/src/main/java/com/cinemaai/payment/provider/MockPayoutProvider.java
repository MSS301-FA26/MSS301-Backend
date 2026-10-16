package com.cinemaai.payment.provider;

import com.cinemaai.payment.entity.WithdrawalRequest;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockPayoutProvider implements PayoutProvider {

    @Value("${payout.mock.mode:SUCCESS}")
    private String defaultMockMode;

    @Override
    public PayoutResult processPayout(WithdrawalRequest request) {
        if (request == null) {
            return PayoutResult.failure("INVALID_REQUEST", "Yêu cầu rút tiền không hợp lệ.");
        }

        // Test simulation hook: account numbers ending in "FAIL" or mode configured to FAIL
        String acc = request.getAccountNumber() != null ? request.getAccountNumber().trim().toUpperCase() : "";
        if ("FAIL".equalsIgnoreCase(defaultMockMode) || acc.endsWith("FAIL") || acc.contains("ERROR")) {
            log.warn("MockPayoutProvider simulating FAILED payout for request #{}", request.getId());
            return PayoutResult.failure("BANK_REJECTED", "Ngân hàng thụ hưởng từ chối giao dịch: Số tài khoản không hợp lệ hoặc tài khoản bị khóa.");
        }

        String txnId = "MOCK-PAYOUT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("MockPayoutProvider successfully processed payout for request #{}, amount: {}, txnId: {}",
                request.getId(), request.getAmount(), txnId);
        return PayoutResult.success(txnId);
    }
}
