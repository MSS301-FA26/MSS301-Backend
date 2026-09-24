package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.CineWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CineWalletRepository extends JpaRepository<CineWallet, Long> {
    Optional<CineWallet> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT w FROM CineWallet w WHERE w.userId = :userId")
    Optional<CineWallet> findByUserIdForUpdate(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(w.balance), 0) FROM CineWallet w")
    java.math.BigDecimal sumBalances();
}
