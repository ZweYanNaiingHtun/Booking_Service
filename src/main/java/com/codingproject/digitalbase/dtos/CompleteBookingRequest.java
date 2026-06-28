package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteBookingRequest {

    private BigDecimal amount;
    private BigDecimal extraAmount;
}