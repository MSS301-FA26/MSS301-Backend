package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.ManagerCinemaAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerCinemaAssignmentRepository extends JpaRepository<ManagerCinemaAssignment, Long> {
    List<ManagerCinemaAssignment> findByUserId(Long userId);
    boolean existsByUserIdAndCinemaId(Long userId, Long cinemaId);
    void deleteByUserId(Long userId);
}
