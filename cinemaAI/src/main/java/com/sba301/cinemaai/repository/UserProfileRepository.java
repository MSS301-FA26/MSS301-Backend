package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.UserProfile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    List<UserProfile> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
