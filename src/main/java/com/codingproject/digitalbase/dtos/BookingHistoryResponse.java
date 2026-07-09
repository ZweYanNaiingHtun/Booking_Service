package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BookingHistoryResponse {
    private Long id;
    private String bookingCode;       // 🎯 UI ထဲက "BK-12345"
    private String serviceName;       // 🎯 "Manicure Cleansing"
    private int totalDuration;        // 🎯 "50" (Frontend မှ "mins" သို့မဟုတ် "Total time 50 mins" ဟု ပြသရန်)
    private Instant appointmentTime;  // 🎯 "Saturday, July 13 . 9:00 AM" အတွက်
    private BigDecimal totalPrice;        // 🎯 "5000" (Frontend မှ "Kyats" တွဲပြရန်)
    private String status;            // 🎯 PENDING, CONFIRMED, COMPLETED, CANCELLED
    private String staffName;         // 🎯 Detail Screen အတွက် "Myo Myo"
    private String staffRole;         // 🎯 Detail Screen အတွက် "Nail Artist"
}