package com.cinemaai.payment.service;

import com.cinemaai.payment.repository.CineWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletProvisioningService {

    private final CineWalletRepository walletRepository;

    @Transactional
    public boolean ensureWalletExists(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("A positive internal user ID is required");
        }
        return walletRepository.insertIfAbsent(userId) == 1;
    }
}
