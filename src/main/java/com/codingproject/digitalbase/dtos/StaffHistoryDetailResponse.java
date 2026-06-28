package com.codingproject.digitalbase.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffHistoryDetailResponse {

    private Long bookingId;
    private String serviceName;
    private String customerName;
    private Instant bookingDate;
    private BigDecimal commission;
}