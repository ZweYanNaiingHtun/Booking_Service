package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorResponse {
    private Long id;
    private String name;
    private String logo;
    private String bannerTitle;
    private String bannerDescription;
    private String bannerImage;
    private Instant eventStartDate;
    private Instant eventEndDate;
    private boolean eventActive; // 🌟
}