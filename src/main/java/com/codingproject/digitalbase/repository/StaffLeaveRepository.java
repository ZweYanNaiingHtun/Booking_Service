package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.model.StaffLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Long> {

    @Query("SELECT sl FROM StaffLeave sl WHERE sl.startDate <= :monthEnd AND sl.endDate >= :monthStart")
    List<StaffLeave> findLeavesInPeriod(@Param("monthStart") Instant monthStart, @Param("monthEnd") Instant monthEnd);

    @Query("SELECT l FROM StaffLeave l WHERE :targetDate >= l.startDate AND (l.endDate IS NULL OR :targetDate <= l.endDate)")
    List<StaffLeave> findActiveLeavesByDate(@Param("targetDate") Instant targetDate);

    @Query("SELECT COUNT(sl) > 0 FROM StaffLeave sl WHERE sl.staffProfile.id = :staffId " +
            "AND sl.startDate <= :endDate AND sl.endDate >= :startDate")
    boolean existsOverlappingLeave(@Param("staffId") Long staffId,
                                   @Param("startDate") Instant startDate,
                                   @Param("endDate") Instant endDate);

    @Query("SELECT sl FROM StaffLeave sl WHERE :targetDate >= sl.startDate AND :targetDate <= sl.endDate")
    List<StaffLeave> findActiveLeavesAt(@Param("targetDate") Instant targetDate);
}