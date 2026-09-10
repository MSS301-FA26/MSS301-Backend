package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.CineWallet;
import com.sba301.cinemaai.entity.WalletTransaction;
import com.sba301.cinemaai.enums.WalletTransactionType;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletOrderByCreatedAtDesc(CineWallet wallet, Pageable pageable);

    @Query("SELECT tx FROM WalletTransaction tx WHERE tx.wallet = :wallet AND tx.type <> com.sba301.cinemaai.enums.WalletTransactionType.WITHDRAWAL_PAID ORDER BY tx.createdAt DESC")
    Page<WalletTransaction> findByWalletFiltered(@Param("wallet") CineWallet wallet, Pageable pageable);

    Page<WalletTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT tx FROM WalletTransaction tx WHERE tx.type <> com.sba301.cinemaai.enums.WalletTransactionType.WITHDRAWAL_PAID ORDER BY tx.createdAt DESC")
    Page<WalletTransaction> findAllFiltered(Pageable pageable);

    boolean existsByBookingAndType(Booking booking, WalletTransactionType type);

    @Query("""
            select coalesce(sum(tx.amount), 0)
            from WalletTransaction tx
            where tx.type = :type
            """)
    BigDecimal sumAmountByType(@Param("type") WalletTransactionType type);
}

