//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.dtos.RevenueSummary;
import com.codingproject.digitalbase.dtos.StaffPerformance;
import com.codingproject.digitalbase.model.Booking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsRepository extends JpaRepository<Booking, Long> {


    @Query("SELECT new com.codingproject.digitalbase.dtos.StaffPerformance(s.id, s.user.fullName, SUM(CASE WHEN b.status = 'COMPLETED' THEN 1 ELSE 0 END), COALESCE(AVG(r.starRating), 5.0)) FROM StaffProfile s LEFT JOIN s.assignedBookings b LEFT JOIN s.reviews r GROUP BY s.id, s.user.fullName")
    List<StaffPerformance> getStaffPerformanceMetrics();
}
