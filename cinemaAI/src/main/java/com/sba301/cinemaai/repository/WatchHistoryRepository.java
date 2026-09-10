package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    List<WatchHistory> findByUserId(Long userId);

}
