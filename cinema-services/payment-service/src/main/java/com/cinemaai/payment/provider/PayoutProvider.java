package com.cinemaai.payment.provider;

import com.cinemaai.payment.entity.WithdrawalRequest;

public interface PayoutProvider {
    PayoutResult processPayout(WithdrawalRequest request);
}
