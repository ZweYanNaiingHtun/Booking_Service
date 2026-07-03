package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;

import java.util.List;

public interface DashboardService {

    List<TopServiceResponse> getTopServicesTrending();

    List<StaffPerformance> getStaffPerformanceRanking();

    List<TodayBookingResponse> getTodayBookingsFeed();

    DashboardStatsResponse getDashboardStats();

    List<ChartDataPoint> getChartData(String period);

    StaffPerformance getStaffPerformanceById(Long staffId);
}