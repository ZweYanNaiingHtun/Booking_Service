package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long bookingId;
    private String customerName;
    private String staffName;
    private int starRating;
    private String comment;
    private Instant createdAt;
}