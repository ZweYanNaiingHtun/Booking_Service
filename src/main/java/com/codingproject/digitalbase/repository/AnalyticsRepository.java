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
    @Query("SELECT new com.codingproject.digitalbase.dtos.RevenueSummary(SUM(p.amount), COUNT(p.id)) FROM Payment p WHERE p.status = 'COMPLETED'")
    RevenueSummary getRevenueSummary();

    @Query("SELECT new com.codingproject.digitalbase.dtos.StaffPerformance(s.id, s.user.fullName, SUM(CASE WHEN b.status = 'COMPLETED' THEN 1 ELSE 0 END), COALESCE(AVG(r.starRating), 5.0)) FROM StaffProfile s LEFT JOIN s.assignedBookings b LEFT JOIN s.reviews r GROUP BY s.id, s.user.fullName")
    List<StaffPerformance> getStaffPerformanceMetrics();

    @Query(
            value = "SELECT DATE_FORMAT(b.created_at, '%Y-%m') AS month, COUNT(b.id) AS totalBookings, SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END) AS totalRevenue FROM bookings b LEFT JOIN payments p ON b.id = p.booking_id GROUP BY DATE_FORMAT(b.created_at, '%Y-%m') ORDER BY month DESC",
            nativeQuery = true
    )
    List<Object[]> getRawMonthlyReports();

    @Query(
            value = "SELECT DATE(b.booking_date) AS bDate, COUNT(b.id) AS bCount FROM bookings b GROUP BY DATE(b.booking_date) ORDER BY bDate ASC",
            nativeQuery = true
    )
    List<Object[]> getRawBookingTrends();
}
