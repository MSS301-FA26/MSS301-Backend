package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.StaffCinemaAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffCinemaAssignmentRepository extends JpaRepository<StaffCinemaAssignment, Long> {
    List<StaffCinemaAssignment> findByUserId(Long userId);
    List<StaffCinemaAssignment> findByCinemaId(Long cinemaId);
    boolean existsByUserIdAndCinemaId(Long userId, Long cinemaId);
    void deleteByUserId(Long userId);
}
