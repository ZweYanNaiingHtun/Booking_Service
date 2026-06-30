//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.BookingTrendDTO;
import com.codingproject.digitalbase.dtos.MonthlyReportDTO;
import com.codingproject.digitalbase.dtos.RevenueSummary;
import com.codingproject.digitalbase.dtos.StaffPerformance;
import com.codingproject.digitalbase.repository.AnalyticsRepository;
import java.sql.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardAnalyticsService {
    private final AnalyticsRepository analyticsRepository;

    public RevenueSummary getRevenueSummary() {
        return this.analyticsRepository.getRevenueSummary();
    }

    public List<StaffPerformance> getStaffPerformance() {
        return this.analyticsRepository.getStaffPerformanceMetrics();
    }

    public List<MonthlyReportDTO> getMonthlyReports() {
        return this.analyticsRepository.getRawMonthlyReports().stream()
                .map((row) -> new MonthlyReportDTO(row[0], ((Number)row[1]).longValue(), row[2] != null ? ((Number)row[2]).doubleValue() : (double)0.0F)).collect(Collectors.toList());
    }

    public List<BookingTrendDTO> getBookingTrends() {
        return this.analyticsRepository.getRawBookingTrends().stream()
                .map((row) -> new BookingTrendDTO(row[0] != null ? ((Date)row[0]).toLocalDate() : null, ((Number)row[1]).longValue())).collect(Collectors.toList());
    }
}
