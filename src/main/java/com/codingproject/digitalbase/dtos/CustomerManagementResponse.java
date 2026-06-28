package com.codingproject.digitalbase.dtos;

import lombok.*;
import org.springframework.data.domain.Page;

@Data
@Builder
public class CustomerManagementResponse {
    private CustomerDashboardMetrics metrics;
    private Page<CustomerSummaryResponse> customerPage;
}