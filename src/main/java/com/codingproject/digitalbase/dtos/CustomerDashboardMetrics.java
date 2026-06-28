package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
public class CustomerDashboardMetrics {
    private long totalCustomers;
    private long todayBookings;
    private long blockedCustomers;
}