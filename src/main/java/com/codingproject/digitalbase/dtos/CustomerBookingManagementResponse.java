package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBookingManagementResponse {
    private Long id;
    private String code;
    private String bookingId;
    private String serviceName;
    private String customerName;
    private BigDecimal price;        // UI အတိုင်း "5000 kyats" ဟု ပြသရန်
    private String bookTime;     // UI အတိုင်း "9:00 AM" ဟု ပြသရန်
    private String date;         // UI အတိုင်း "July,13" ဟု ပြသရန်
    private Integer duringTime;   // UI အတိုင်း "50 mins" ဟု ပြသရန်
    private String staffName;    // ဝန်ထမ်းမရှိပါက "Any Available Staff" ဟု ပြသရန်
    private String status;

}
