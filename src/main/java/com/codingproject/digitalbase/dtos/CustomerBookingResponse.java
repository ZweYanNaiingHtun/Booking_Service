package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBookingResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private boolean enabled;
    private String profilePicture;
    private long bookingCount;
}