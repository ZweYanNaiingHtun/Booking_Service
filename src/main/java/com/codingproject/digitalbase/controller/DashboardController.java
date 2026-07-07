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
    @GetMapping("/service-trending")
    public ResponseEntity<List<TopServiceResponse>> getServiceTrending(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(this.dashboardService.getTopServicesTrending(month, year));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/staff-performance")
    public ResponseEntity<List<StaffPerformance>> getStaffPerformance(
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