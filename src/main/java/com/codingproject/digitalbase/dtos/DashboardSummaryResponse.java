package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalBookings;
    private long pendingBookings;
    private long completedBookings;
    private double totalRevenue;
    private List<CustomerBookingResponse> customerBookingSummary;
    private RevenueSummary revenueSummary;
    private List<StaffPerformance> staffPerformance;
    private List<MonthlyReportDTO> monthlyReports;
    private List<BookingTrendDTO> bookingTrends;
}