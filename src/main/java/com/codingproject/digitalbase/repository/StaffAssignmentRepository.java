package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.model.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    // 🌟 LocalDate/LocalTime အစား Instant သို့ ပြောင်းလဲပြီး Overlap စစ်ဆေးခြင်း
    @Query("SELECT COUNT(sa) > 0 FROM StaffAssignment sa " +
            "WHERE sa.staffProfile.user.id = :staffId " +
            "AND sa.isBooked = true " +
            "AND sa.startTime < :endTime AND sa.endTime > :startTime")
    boolean isStaffBusy(@Param("staffId") Long staffId,
                        @Param("startTime") Instant startTime,
                        @Param("endTime") Instant endTime);

    @Query("SELECT COUNT(sa) FROM StaffAssignment sa WHERE sa.staffProfile.user.id = :userId")
    long countConfirmedByStaffUserId(@Param("userId") Long userId);
}