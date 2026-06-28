package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDetailResponse {
    private Long id;
    private String customerId;     // Cu-001
    private String customerName;   // Ma Thandar
    private String email;          // userthandar12@gmail.com
    private String profilePicture;
    private String joinDate;       // 23.6.2026
    private String phoneNumber;    // 09 - 123 456 789
    private String status;         // Inprogress သို့မဟုတ် Blocked
    private long totalBookings;    // 10
    private String lastVisit;      // 23.6.2026
    private List<RecentBookingResponse> recentBookings;
}