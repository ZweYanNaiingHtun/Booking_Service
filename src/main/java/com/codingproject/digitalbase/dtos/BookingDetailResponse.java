package com.codingproject.digitalbase.dtos;

import lombok.*;

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
    private String serviceNameAndDuration; // ဥပမာ - "Manicure Cleansing (50 mins)"
    private String totalCharges;       // ဥပမာ - "5000 Kyats"
    private String status;             // "Confirm", "Inprogress", "Completed"
}