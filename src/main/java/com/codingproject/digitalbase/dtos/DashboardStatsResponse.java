package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    // Total Bookings
    private long totalBookings;
    private double bookingsGrowthPercentage; // 🌟 ပြီးခဲ့သည့်လနှင့် နှိုင်းယှဉ်ချက် ရာခိုင်နှုန်း (e.g., 20.0)

    // Total Revenue
    private double totalRevenue;
    private double revenueGrowthPercentage;  // 🌟 (e.g., 20.0)

    // Active Staff
    private int todayActiveStaff;
    private double staffGrowthPercentage;    // 🌟 (e.g., 0.0)

    // Total Customers
    private long totalCustomers;
    private double customersGrowthPercentage; // 🌟 (e.g., 50.0)
}