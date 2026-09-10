package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.TicketCombo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketComboRepository extends JpaRepository<TicketCombo, Long> {

    List<TicketCombo> findByActiveTrue();

    Optional<TicketCombo> findByNameIgnoreCase(String name);

    Page<TicketCombo> findByActive(boolean active, Pageable pageable);

    Page<TicketCombo> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<TicketCombo> findByActiveAndNameContainingIgnoreCase(boolean active, String keyword, Pageable pageable);
}
