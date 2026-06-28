package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
public class CustomerSummaryResponse {
    private Long id;
    private String customerId;     // UI ထဲက 'Cu-001' (User Code)
    private String customerName;   // UI ထဲက 'Ma Thandar' (Full Name)
    private String profilePicture; // Profile Image Path
    private String email;          // Gmail
    private String phoneNumber;    // Phone Number
    private String gender;         // Gender
    private String joinDate;       // UI ထဲက '23.6.2026' ပုံစံဖြင့်ပြသမည့် ရက်စွဲ
    private long bookingCount;     // ဝယ်ယူထားသည့် ဘွတ်ကင်အရေအတွက်
}