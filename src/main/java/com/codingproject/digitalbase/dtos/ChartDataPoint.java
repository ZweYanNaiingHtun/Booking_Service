package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChartDataPoint {
    private String label;
    private int bookingCount; // 📱 App/Web မှ Customer တင်သော ဘွတ်ကင်
    private int walkInCount;   // 🚶 ဆိုင်သို့ တိုက်ရိုက်လာသော ဘွတ်ကင်
    private int totalBooking;    // 📊 စုစုပေါင်း (Completed စာရင်းစုစုပေါင်း)
    private int cancelCount;
}