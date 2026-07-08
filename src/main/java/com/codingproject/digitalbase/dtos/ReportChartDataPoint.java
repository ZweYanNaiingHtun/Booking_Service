package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportChartDataPoint {
    private String label;             // 📅 "Mon", "Tue", ..., "Sun"
    private long bookingCount;        // 📱 App Bookings Count
    private long walkInCount;         // 🚶 Walk-in Bookings Count
    private long cancelCount;         // ❌ Cancelled Bookings Count
    private long totalBooking;        // 📊 Total Bookings Count
    private RevenueBlock revenueBlock; // 💅 Top Trending Service တစ်ခုတည်းမှရသော ဝင်ငွေ
    private String topServiceName;    // 🏷️ Top Service နာမည်
}