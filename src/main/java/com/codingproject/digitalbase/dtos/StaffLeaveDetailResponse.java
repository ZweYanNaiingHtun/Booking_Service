package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffLeaveDetailResponse {
    private Long staffProfileId;
    private String staffName;
    private String role;
    private String profileImage;
    private String leaveType;
    private String note;
}