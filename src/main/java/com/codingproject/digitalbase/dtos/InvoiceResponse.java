package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private String invoiceNumber;
    private Long bookingId;
    private String customerName;
    private String customerPhone;
    private String staffName;
    private String serviceName;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String completedAt;
}