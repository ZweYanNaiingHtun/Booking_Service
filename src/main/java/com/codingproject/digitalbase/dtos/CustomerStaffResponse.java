package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStaffResponse {

    private Long userId;          // 🌟 ဤ Field အသစ်လေးအား ဖြည့်စွက်ပေးပါ (Booking Request အတွက် သုံးရန်)
    private Long staffProfileId;   // ရှိပြီးသား ကော်လံ
    private String fullName;
    private String profilePicture;
    private String specializedName;
    private Double rating;
    private int bookingCount;
    private boolean isAvailable;
}