package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalBookings;
    private double totalRevenue;
    private int todayActiveStaff;
    private long totalCustomers;
}