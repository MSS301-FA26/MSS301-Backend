package com.cinemaai.identity.repository;

import com.cinemaai.identity.entity.UserCinemaAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCinemaAssignmentRepository extends JpaRepository<UserCinemaAssignment, Long> {

    Optional<UserCinemaAssignment> findByUserId(Long userId);

    List<UserCinemaAssignment> findByCinemaId(Long cinemaId);

    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
