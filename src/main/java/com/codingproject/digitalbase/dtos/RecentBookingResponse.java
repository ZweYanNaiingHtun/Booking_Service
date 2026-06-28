package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentBookingResponse {
    private String serviceName; // UI အရ lowercase ဖြင့်ပြရန် (ဥပမာ- manicure cleansing)
    private String bookingDate; // UI အရ '23.6.2026' ပုံစံပြရန်
}