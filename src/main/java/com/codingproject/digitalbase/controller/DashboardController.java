//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.CustomerBookingResponse;
import com.codingproject.digitalbase.dtos.DashboardSummaryResponse;
import com.codingproject.digitalbase.repository.UserRepository;
import com.codingproject.digitalbase.service.DashboardService;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/admin/dashboard"})
public class DashboardController {
    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping({"/summary"})
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(this.dashboardService.getDashboardSummary());
    }

    @GetMapping({"/booking-summary"})
    public ResponseEntity<List<CustomerBookingResponse>> getCustomersBookingSummary() {
        return ResponseEntity.ok(this.userRepository.findAllCustomersWithBookingCount());
    }

}
