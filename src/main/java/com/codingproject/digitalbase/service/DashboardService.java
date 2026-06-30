//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.BookingTrendDTO;
import com.codingproject.digitalbase.dtos.CustomerBookingResponse;
import com.codingproject.digitalbase.dtos.DashboardSummaryResponse;
import com.codingproject.digitalbase.dtos.MonthlyReportDTO;
import com.codingproject.digitalbase.dtos.RevenueSummary;
import com.codingproject.digitalbase.dtos.StaffPerformance;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DashboardAnalyticsService dashboardAnalyticsService;

    public DashboardSummaryResponse getDashboardSummary() {
        long totalBookings = this.bookingRepository.count();
        long pendingBookings = this.bookingRepository.countByStatus(BookingStatus.PENDING);
        long completedBookings = this.bookingRepository.countByStatus(BookingStatus.COMPLETED);
        double totalRevenue = this.bookingRepository.calculateTotalRevenue();
        List<CustomerBookingResponse> customerBookingSummary = this.userRepository.findAllCustomersWithBookingCount();
        RevenueSummary revenueSummary = this.dashboardAnalyticsService.getRevenueSummary();
        List<StaffPerformance> staffPerformance = this.dashboardAnalyticsService.getStaffPerformance();
        List<MonthlyReportDTO> monthlyReports = this.dashboardAnalyticsService.getMonthlyReports();
        List<BookingTrendDTO> bookingTrends = this.dashboardAnalyticsService.getBookingTrends();
        return DashboardSummaryResponse.builder().totalBookings(totalBookings).pendingBookings(pendingBookings).completedBookings(completedBookings).totalRevenue(totalRevenue).customerBookingSummary(customerBookingSummary).revenueSummary(revenueSummary).staffPerformance(staffPerformance).monthlyReports(monthlyReports).bookingTrends(bookingTrends).build();
    }
}
