package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private String profilePicture;
    // 🌟 🌟 🌟 Admin, Staff, Customer Role များကို Dynamic ပြသရန် ထပ်တိုးလိုက်ပါသည်
    private String role;
    private Double rating;                 // Staff ၏ ပျမ်းမျှ ကြယ်ပွင့်နှုန်း
    private Long completedBookingsCount;
}