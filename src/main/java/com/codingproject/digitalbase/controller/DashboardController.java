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
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(this.dashboardService.getDashboardStats());
    }

    // 🌟 ၂။ Chart Data သီးသန့် (Weekly သို့မဟုတ် Monthly Toggle အတွက်)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/chart")
    public ResponseEntity<List<ChartDataPoint>> getChartData(@RequestParam(defaultValue = "weekly") String period) {
        return ResponseEntity.ok(this.dashboardService.getChartData(period));
    }

    // 🌟 ၂။ Top Services + Staff Performance Table (Monthly စာရင်းများအတွက်)
    // 🌟 ၁။ ဝန်ဆောင်မှု Trending (Pie Chart) အတွက် သီးသန့် Endpoint
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/service-trending")
    public ResponseEntity<List<TopServiceResponse>> getServiceTrending() {
        return ResponseEntity.ok(this.dashboardService.getTopServicesTrending());
    }

    // 🌟 ၂။ ဝန်ထမ်းစွမ်းဆောင်ရည် (Staff Management Table) အတွက် သီးသန့် Endpoint
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/staff-performance")
    public ResponseEntity<List<StaffPerformance>> getStaffPerformance() {
        return ResponseEntity.ok(this.dashboardService.getStaffPerformanceRanking());
    }

    // 🌟 ၃။ Dashboard အောက်ဆုံးက "Today's Bookings" Real-time Feed အတွက်
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/today-bookings")
    public ResponseEntity<List<TodayBookingResponse>> getTodayBookings() {
        return ResponseEntity.ok(this.dashboardService.getTodayBookingsFeed());
    }
}