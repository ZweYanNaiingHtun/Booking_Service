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
}