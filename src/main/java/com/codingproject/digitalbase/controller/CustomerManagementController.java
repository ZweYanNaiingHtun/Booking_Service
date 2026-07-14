package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.service.CustomerManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(this.customerService.getCustomerList(search, page, size));
    }

    // 🌟 [ADDED] Endpoint: Blocked ဖြစ်နေသော Customer များကို သီးသန့်ဆွဲထုတ်ရန်
    // GET -> /api/admin/customers/blocked
    @GetMapping("/blocked")
    public ResponseEntity<Page<CustomerSummaryResponse>> getBlockedCustomers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(this.customerService.getBlockedCustomerList(search, page, size));
    }

    // 🌟 [ADDED] Endpoint: Customer တစ်ဦးအား Block လုပ်ရန် သို့မဟုတ် Unblock ပြန်လုပ်ရန်
    // PATCH -> /api/admin/customers/{id}/toggle-block
    @PatchMapping("/{id}/toggle-block")
    public ResponseEntity<Void> toggleBlockCustomer(@PathVariable("id") Long customerId) {
        this.customerService.toggleCustomerBlockStatus(customerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/{id}/detail"})
    public ResponseEntity<CustomerDetailResponse> getCustomerDetail(@PathVariable("id") Long customerId) {
        return ResponseEntity.ok(this.customerService.getCustomerDetail(customerId));
    }

    @GetMapping("/walkin/{bookingId}")
    public ResponseEntity<WalkInDetailResponse> getWalkInDetail(@PathVariable Long bookingId) {
        return ResponseEntity.ok(this.customerService.getWalkInDetail(bookingId));
    }

    @GetMapping("/walkin")
    public ResponseEntity<Page<WalkInSummaryResponse>> getWalkInCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(this.customerService.getWalkInList(page, size));
    }
}