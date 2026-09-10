package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.AuditLog;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.AuditActionType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActor(User actor);

    List<AuditLog> findByAction(AuditActionType action);

    List<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId);

    // Lọc theo tiền tố loại đối tượng (vd "FOOD" khớp FOOD_ITEM + FOOD_COMBO + FOOD_ORDER)
    Page<AuditLog> findByTargetTypeStartingWith(String targetTypePrefix, Pageable pageable);
}
