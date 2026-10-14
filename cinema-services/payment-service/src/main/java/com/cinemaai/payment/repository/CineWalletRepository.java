package com.cinemaai.payment.repository;

import com.cinemaai.payment.entity.CineWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CineWalletRepository extends JpaRepository<CineWallet, Long> {
    Optional<CineWallet> findByUserId(Long userId);

    // PostgreSQL's unique user_id constraint arbitrates concurrent deliveries without aborting the transaction.
    @Modifying
    @Query(value = """
            INSERT INTO cine_wallets (user_id, balance, created_at, updated_at)
            VALUES (:userId, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT w FROM CineWallet w WHERE w.userId = :userId")
    Optional<CineWallet> findByUserIdForUpdate(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(w.balance), 0) FROM CineWallet w")
    java.math.BigDecimal sumBalances();
}
