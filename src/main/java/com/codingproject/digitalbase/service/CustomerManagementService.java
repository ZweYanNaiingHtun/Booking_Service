package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

public interface CustomerManagementService {

    Page<CustomerSummaryResponse> getCustomerList(String search, int page, int size);

    CustomerDashboardMetrics getCustomerMetrics();

    // 🌟 [UPDATED] Modal အသစ်အတွက် အသေးစိတ် ဒေတာအကုန်ထုတ်ပေးမည့် မက်သဒ်
    CustomerDetailResponse getCustomerDetail(Long customerId);

    WalkInDetailResponse getWalkInDetail(Long bookingId);

    Page<WalkInSummaryResponse> getWalkInList(int page, int size);
}