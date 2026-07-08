package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;

import java.util.List;

public interface DashboardService {

    List<TopServiceResponse> getTopServicesTrending(Integer month, Integer year);

    List<StaffPerformance> getStaffPerformanceRanking(Integer month, Integer year);

    ReportSummaryResponse getReportChartData(Integer year, Integer month, String period);

    List<TodayBookingResponse> getTodayBookingsFeed();

    DashboardStatsResponse getDashboardStats(Integer month, Integer year);

    List<ChartDataPoint> getChartData(String period);

    PaginatedDailyOverviewResponse getMonthlyDailyOverview(Integer year, Integer month, int page, int size);

    StaffPerformance getStaffPerformanceById(Long staffId);
}