//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.service.CustomerManagementService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/admin/customers"})
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CustomerManagementController {
    private final CustomerManagementService customerService;

    @GetMapping({"/metrics"})
    public ResponseEntity<CustomerDashboardMetrics> getMetrics() {
        return ResponseEntity.ok(this.customerService.getCustomerMetrics());
    }

    @GetMapping
    public ResponseEntity<Page<CustomerSummaryResponse>> getAllCustomers(
            @RequestParam(value = "search",required = false) String search,
            @RequestParam(value = "page",defaultValue = "0") int page,
            @RequestParam(value = "size",defaultValue = "10") int size) {
        return ResponseEntity.ok(this.customerService.getCustomerList(search, page, size));
    }

    @GetMapping({"/{id}/detail"})
    public ResponseEntity<CustomerDetailResponse> getCustomerDetail(@PathVariable("id") Long customerId) {
        return ResponseEntity.ok(this.customerService.getCustomerDetail(customerId));
    }

    @GetMapping("/walkin/{bookingId}")
    public ResponseEntity<WalkInDetailResponse> getWalkInDetail(@PathVariable Long bookingId) {
        return ResponseEntity.ok(customerService.getWalkInDetail(bookingId));
    }

    @GetMapping("/walkin")
    public ResponseEntity<Page<WalkInSummaryResponse>> getWalkInCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(customerService.getWalkInList(page, size));
    }
}
