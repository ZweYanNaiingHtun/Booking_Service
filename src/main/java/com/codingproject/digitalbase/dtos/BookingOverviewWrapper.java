package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingOverviewWrapper {
    // 🌟 UI အပေါ်ဆုံးက Counter များအတွက်
    private long pendingCount;
    private long inProgressCount;
    private long confirmCount;
    private long completedCount;
    private long rejectedCount;

    // 🌟 Table ဒေတာစာရင်း
    private List<CustomerBookingManagementResponse> bookings;

    // 🌟 အောက်ခြေက Pagination (< 1 2 3 >) အတွက်
    private int currentPage;
    private int totalPages;
    private long totalElements;
}