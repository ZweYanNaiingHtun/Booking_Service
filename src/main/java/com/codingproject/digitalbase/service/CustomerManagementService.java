package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.CustomerDashboardMetrics;
import com.codingproject.digitalbase.dtos.CustomerDetailResponse; // 🌟 ဤနေရာပြောင်းပါသည်
import com.codingproject.digitalbase.dtos.CustomerSummaryResponse;
import org.springframework.data.domain.Page;

public interface CustomerManagementService {

    Page<CustomerSummaryResponse> getCustomerList(String search, int page, int size);

    CustomerDashboardMetrics getCustomerMetrics();

    // 🌟 [UPDATED] Modal အသစ်အတွက် အသေးစိတ် ဒေတာအကုန်ထုတ်ပေးမည့် မက်သဒ်
    CustomerDetailResponse getCustomerDetail(Long customerId);
}