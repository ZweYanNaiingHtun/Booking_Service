package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.BookingStatus;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long serviceId;
    private String serviceName;
    private Instant bookingDate;
    private String notes;
    private BookingStatus status;
    private String createdByCustomerOrStaffName;
    private Instant createdAt;
    private String cancelledBy;
    private String rejectionReason;

}