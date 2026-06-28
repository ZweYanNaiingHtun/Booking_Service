package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLatestBookingResponse {

    private String bookingId;
    private String stage;
    private String service;
    private String staff;
}