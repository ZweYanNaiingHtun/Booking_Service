package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyOverviewDataPoint {
    private String date;              // 📅 Format: "1.6.2026", "2.6.2026"
    private long totalBookings;       // 📊 Total Bookings
    private long cancelledBookings;   // ❌ Cancelled Bookings
    private long walkInCustomers;     // 🚶 Walk-in Customers
    private long activeStaff;         // 👥 Active Staff Count (ထိုနေ့တွင် အလုပ်လုပ်သော ဝန်ထမ်းအရေအတွက်)
    private double totalRevenue;      // 💰 Total Revenue
    private String topService;        // 💅 Top Service Name
}