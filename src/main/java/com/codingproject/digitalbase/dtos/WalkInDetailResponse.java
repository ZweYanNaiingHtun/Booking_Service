package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class WalkInDetailResponse {
    private Long id;                  // DB Booking ID
    private String walkInId;          // UI: "CUST-001"
    private String date;              // UI: "23/06/2026"
    private String staffName;         // UI: "Zwe Zwe"
    private String startTime;         // UI: "02:00 PM"
    private String serviceName;       // UI: "Simple Nail Art"
    private String duration;          // UI: "2 Hours"
    private BigDecimal basicAmount;   // UI: 20000
    private BigDecimal extraAmount;   // UI: 5000
    private BigDecimal totalCharges;  // UI: 25000
    private String extraChargesNote;  // UI: "Use Premium Nail Polish" (Notes ထဲကဒေတာ)
}