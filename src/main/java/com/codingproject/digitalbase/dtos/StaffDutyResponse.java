package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.BookingStatus;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDutyResponse {

    private Long bookingId;
    private String customerName;
    private String serviceName;
    private Instant bookingDate;
    private Integer durationInMinutes;
    private BookingStatus status;
    private String notes;
}