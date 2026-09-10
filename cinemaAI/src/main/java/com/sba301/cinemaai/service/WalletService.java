package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.wallet.WithdrawalCreateRequest;
import com.sba301.cinemaai.dto.request.wallet.WithdrawalProcessRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletDashboardResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletResponse;
import com.sba301.cinemaai.dto.response.wallet.WalletTransactionResponse;
import com.sba301.cinemaai.dto.response.wallet.WithdrawalResponse;

public interface WalletService {

    // User endpoints
    WalletResponse getWallet(String email);

    PageResponse<WalletTransactionResponse> getTransactions(String email, int page, int size);

    WithdrawalResponse createWithdrawalRequest(String email, WithdrawalCreateRequest request);

    PageResponse<WithdrawalResponse> getMyWithdrawals(String email, int page, int size);

    // Admin endpoints
    PageResponse<WithdrawalResponse> getAllWithdrawals(String status, int page, int size);

    WithdrawalResponse approveWithdrawal(Long withdrawalId, WithdrawalProcessRequest request);

    WithdrawalResponse rejectWithdrawal(Long withdrawalId, WithdrawalProcessRequest request);

    WalletDashboardResponse getDashboard();
}
