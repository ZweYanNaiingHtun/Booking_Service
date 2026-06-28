package com.codingproject.digitalbase.dtos;

import java.time.Instant;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffTimeSlotResponse {

    private String timeLabel;
    private Instant bookingDate;
    private boolean isAvailable;
}