package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.StaffProfile;
import com.sba301.cinemaai.entity.StaffShift;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {

    List<StaffShift> findByStaffProfile(StaffProfile staffProfile);

    List<StaffShift> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);
}
