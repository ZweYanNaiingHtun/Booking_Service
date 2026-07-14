package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {
    private String bookingId;
    private String customerName;
    private String phoneNumber;
    private String appointmentDetails; // ဥပမာ - "Saturday, July 13 . 9:00 AM"
    private String staffName;          // ဥပမာ - "Nail artist, Myo Myo"
    private String serviceName;        // ဥပမာ - "Manicure Cleansing"
    private BigDecimal duration;       // ဥပမာ - 50
    private BigDecimal price;          // 🌟 String အစား ကိန်းဂဏန်းသီးသန့် (ဥပမာ - 5000.00) ပြောင်းလဲလိုက်ပါသည်
    private String status;             // "Confirm", "Inprogress", "Completed"
}