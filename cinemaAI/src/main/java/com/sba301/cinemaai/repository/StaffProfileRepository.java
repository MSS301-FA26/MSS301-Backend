package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.StaffProfile;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.StaffStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {

    Optional<StaffProfile> findByUser(User user);

    Optional<StaffProfile> findByEmployeeCode(String employeeCode);

    List<StaffProfile> findByCinema(Cinema cinema);

    List<StaffProfile> findByStatus(StaffStatus status);
}
