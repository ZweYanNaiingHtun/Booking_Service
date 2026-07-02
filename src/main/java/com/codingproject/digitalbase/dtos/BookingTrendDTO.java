package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTrendDTO {

    private LocalDate bookingDate;
    private long bookingCount;
}