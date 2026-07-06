package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeStaffResponse {
    private Long userId;
    private Long staffProfileId;
    private String fullName;
    private String profilePicture;
    private String specializedName; // UI: "Nail Artist"
    private Double rating;          // UI: 4.5
    private int bookingCount;
    private boolean isAvailable;
}