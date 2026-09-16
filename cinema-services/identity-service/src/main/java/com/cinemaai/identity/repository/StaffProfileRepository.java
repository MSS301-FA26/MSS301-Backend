package com.cinemaai.identity.repository;

import com.cinemaai.identity.entity.StaffProfile;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.StaffStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {

    Optional<StaffProfile> findByUser(User user);

    Optional<StaffProfile> findByEmployeeCode(String employeeCode);

    List<StaffProfile> findByCinemaId(Long cinemaId);

    List<StaffProfile> findByStatus(StaffStatus status);
}
