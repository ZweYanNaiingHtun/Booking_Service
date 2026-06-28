package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkInBookingRequest {

    @NotNull(message = "Service ID is required for walk-in booking")
    private Long serviceId;

    @NotNull(message = "Booking date is required for walk-in booking")
    @Future(message = "Booking date must be futured")
    private Instant bookingDate;

    private BigDecimal amount;
    private BigDecimal extraAmount;

    @NotBlank(message = "Payment type is required")
    private String paymentType;

    private String notes;
}