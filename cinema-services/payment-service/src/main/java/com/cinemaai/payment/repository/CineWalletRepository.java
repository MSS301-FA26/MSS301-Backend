package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.CineWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CineWalletRepository extends JpaRepository<CineWallet, Long> {
    Optional<CineWallet> findByUserId(Long userId);
}
