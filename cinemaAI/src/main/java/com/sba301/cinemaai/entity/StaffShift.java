package com.sba301.cinemaai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "staff_shifts",
        indexes = @Index(name = "idx_staff_shifts_staff_time", columnList = "staff_profile_id,start_time,end_time")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffShift extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_profile_id", nullable = false)
    private StaffProfile staffProfile;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 500)
    private String note;

    public StaffShift(StaffProfile staffProfile, LocalDateTime startTime, LocalDateTime endTime, String note) {
        this.staffProfile = staffProfile;
        this.startTime = startTime;
        this.endTime = endTime;
        this.note = note;
    }
}
