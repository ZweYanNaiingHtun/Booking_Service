package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TodayBookingResponse {
    private String id;
    private String staffName;
    private String serviceNames;
    private String bookingTime;
    private BigDecimal price;
    private String status;
}