package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableStaffResponse {
    private Long userId;
    private Long staffProfileId;
    private String fullName;
    private String profilePicture;
}
