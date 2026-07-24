package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.service.DashboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(this.dashboardService.getDashboardStats(month, year));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/chart")
    public ResponseEntity<List<ChartDataPoint>> getDashboardChartData(@RequestParam(defaultValue = "weekly") String period) {
        return ResponseEntity.ok(this.dashboardService.getChartData(period));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/report-chart")
    public ResponseEntity<ReportSummaryResponse> getReportChart(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "weekly") String period) { // 🌟 weekly သို့မဟုတ် monthly လက်ခံရန်

        ReportSummaryResponse data = this.dashboardService.getReportChartData(year, month, period);
        return ResponseEntity.ok(data);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/service-trending")
    public ResponseEntity<List<TopServiceResponse>> getServiceTrending(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(this.dashboardService.getTopServicesTrending(month, year));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/daily-overview")
    public ResponseEntity<PaginatedDailyOverviewResponse> getDailyOverview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0") int page,   // 📄 Default: ပထမဆုံး စာမျက်နှာ (Page 0)
            @RequestParam(defaultValue = "10") int size) { // 📏 Default: တစ်မျက်နှာလျှင် ၁၀ ရက်စာ ပြသမည်

        // 🌟 Service ထဲက ပတ်လမ်းအသစ်ကို လှမ်းခေါ်ပြီး Response ပြန်ပေးခြင်း
        PaginatedDailyOverviewResponse response = this.dashboardService.getMonthlyDailyOverview(year, month, page, size);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/staff-performance")
    public ResponseEntity<StaffOverviewWrapper> getStaffPerformance(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(this.dashboardService.getStaffPerformanceRanking(month, year));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/staff-performance/{id}")
    public ResponseEntity<StaffPerformance> getStaffPerformanceById(@PathVariable Long id) {
        return ResponseEntity.ok(this.dashboardService.getStaffPerformanceById(id));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/today-bookings")
    public ResponseEntity<List<TodayBookingResponse>> getTodayBookings() {
        return ResponseEntity.ok(this.dashboardService.getTodayBookingsFeed());
    }
}