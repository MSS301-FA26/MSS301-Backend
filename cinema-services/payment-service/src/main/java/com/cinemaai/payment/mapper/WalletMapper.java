package com.cinemaai.payment.mapper;

import com.cinemaai.payment.dto.response.WalletResponse;
import com.cinemaai.payment.dto.response.WalletTransactionResponse;
import com.cinemaai.payment.dto.response.WithdrawalResponse;
import com.cinemaai.payment.entity.CineWallet;
import com.cinemaai.payment.entity.WalletTransaction;
import com.cinemaai.payment.entity.WithdrawalRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    WalletResponse toResponse(CineWallet wallet);

    @Mapping(target = "walletId", source = "wallet.id")
    WalletTransactionResponse toTransactionResponse(WalletTransaction tx);

    @Mapping(target = "userName", expression = "java(\"User #\" + wr.getUserId())")
    @Mapping(target = "userEmail", source = "walletPhone")
    WithdrawalResponse toWithdrawalResponse(WithdrawalRequest wr);
}
