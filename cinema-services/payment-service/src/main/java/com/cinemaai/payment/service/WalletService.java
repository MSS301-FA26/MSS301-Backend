package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.request.WithdrawalCreateRequest;
import com.cinemaai.payment.dto.request.WithdrawalProcessRequest;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.dto.response.WalletDashboardResponse;
import com.cinemaai.payment.dto.response.WalletResponse;
import com.cinemaai.payment.dto.response.WalletTransactionResponse;
import com.cinemaai.payment.dto.response.WithdrawalResponse;

public interface WalletService {

    // User endpoints
    WalletResponse getWallet(Long userId);

    PageResponse<WalletTransactionResponse> getTransactions(Long userId, int page, int size);

    WithdrawalResponse createWithdrawalRequest(Long userId, WithdrawalCreateRequest request);

    PageResponse<WithdrawalResponse> getMyWithdrawals(Long userId, int page, int size);

    // Admin / Staff endpoints
    PageResponse<WithdrawalResponse> getAllWithdrawals(String status, int page, int size);

    WithdrawalResponse approveWithdrawal(Long withdrawalId, WithdrawalProcessRequest request);

    WithdrawalResponse rejectWithdrawal(Long withdrawalId, WithdrawalProcessRequest request);

    WalletDashboardResponse getDashboard();
}
