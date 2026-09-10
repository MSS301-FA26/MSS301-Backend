package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.CineWallet;
import com.sba301.cinemaai.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CineWalletRepository extends JpaRepository<CineWallet, Long> {

    Optional<CineWallet> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from CineWallet wallet where wallet.user = :user")
    Optional<CineWallet> findByUserForUpdate(@Param("user") User user);

    @Query("select coalesce(sum(wallet.balance), 0) from CineWallet wallet")
    java.math.BigDecimal sumBalances();
}
