package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStaffResponse {

    private Long staffProfileId;
    private String fullName;
    private String profilePicture;
    private String specializedName;
    private Double rating;
    private long bookingCount;
    private boolean isAvailable;
}