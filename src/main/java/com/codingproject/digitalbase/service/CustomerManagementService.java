package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import org.springframework.data.domain.Page;

public interface CustomerManagementService {

    CustomerDashboardMetrics getCustomerMetrics();

    Page<CustomerSummaryResponse> getCustomerList(String search, int page, int size);

    CustomerDetailResponse getCustomerDetail(Long customerId);

    WalkInDetailResponse getWalkInDetail(Long bookingId);

    Page<WalkInSummaryResponse> getWalkInList(int page, int size);

    // 🌟 [ADDED] Blocked Customer List ရယူရန်
    Page<CustomerSummaryResponse> getBlockedCustomerList(String search, int page, int size);

    // 🌟 [ADDED] Customer အား Block / Unblock ပြုလုပ်ရန် (Toggle Status)
    void toggleCustomerBlockStatus(Long customerId);
}