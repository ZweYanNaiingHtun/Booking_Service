//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.dtos.MonthlyReportDTO;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.model.Booking;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByCustomerId(Long customerId, Pageable pageable);

    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    @Query("SELECT b.assignedStaff.id FROM Booking b WHERE b.bookingDate = :bookingDate AND b.status = com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED")
    List<Long> findBusyStaffIdsByDateTime(@Param("bookingDate") Instant bookingDate);

    List<Booking> findByRequestedStaffIdAndStatusAndBookingDateBetween(Long staffProfileId, BookingStatus status, Instant start, Instant end);

    List<Booking> findByStatusAndBookingDateBetween(BookingStatus status, Instant start, Instant end);

    long countByStatus(BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.businessService.price), 0.0) FROM Booking b WHERE b.status = com.codingproject.digitalbase.enums.BookingStatus.COMPLETED")
    double calculateTotalRevenue();

    @Query("SELECT new com.codingproject.digitalbase.dtos.MonthlyReportDTO(FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m'), COUNT(b), SUM(CASE WHEN b.status = com.codingproject.digitalbase.enums.BookingStatus.COMPLETED THEN b.businessService.price ELSE 0.0 END)) FROM Booking b GROUP BY FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m') ORDER BY FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m') ASC")
    List<MonthlyReportDTO> getMonthlyReportsAndTrends();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.requestedStaff.id = :profileId AND b.status = 'PENDING'")
    long countPendingByStaffProfileId(@Param("profileId") Long profileId);

    boolean existsByCustomerIdAndBookingDateAndStatusIn(Long customerId, Instant bookingDate, Collection<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.assignedStaff.id = :staffProfileId AND b.bookingDate >= :startOfDay AND b.bookingDate <= :endOfDay AND b.status = :status ORDER BY b.bookingDate ASC")
    List<Booking> findStaffDutiesByDateAndStatus(@Param("staffProfileId") Long staffProfileId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay, @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.assignedStaff.id = :staffProfileId AND b.status = :status AND b.bookingDate BETWEEN :start AND :end ORDER BY b.bookingDate DESC")
    List<Booking> findStaffHistory(@Param("staffProfileId") Long staffProfileId, @Param("status") BookingStatus status, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate >= :startOfDay AND b.bookingDate <= :endOfDay")
    long countTodayBookings(@Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId ORDER BY b.createdAt DESC LIMIT 1")
    Optional<Booking> findNewestBookingByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId ORDER BY b.createdAt DESC")
    List<Booking> findTopRecentBookings(@Param("customerId") Long customerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.customer.code = 'CU-WALKIN' ORDER BY b.bookingDate DESC")
    Page<Booking> findWalkInBookings(Pageable pageable);
}
