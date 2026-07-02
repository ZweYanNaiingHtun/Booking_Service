package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class WalkInSummaryResponse {
    private Long id;                // DB Booking ID
    private String walkInId;        // UI ထဲကပုံစံ "CUS-001", "CUS-002"
    private String serviceName;     // Simple Nail Art, Pedicure
    private String staffName;       // Zwe Zwe, Thu Thu
    private String date;            // 23/6/2026
    private String startTime;       // 02:00 PM
    private String totalTime;       // 2 hours (Duration ပြရန်)
    private String totalAmountDisplay; // "20000 MMK +5k Extra" ပုံစံမျိုး String ဖြင့် စုစည်းပြရန်
    private BigDecimal totalAmount;
    private BigDecimal extraAmount;
}