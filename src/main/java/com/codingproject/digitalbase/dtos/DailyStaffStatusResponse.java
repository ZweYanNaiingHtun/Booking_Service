package com.codingproject.digitalbase.dtos;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyStaffStatusResponse {
    private List<StaffStatusDTO> activeStaff;  // Staff Today (အစိမ်းစက်)
    private List<StaffStatusDTO> dayOffStaff;  // Day Off (ပန်းရောင်စက်)
    private List<StaffStatusDTO> leaveStaff;   // Leave Staff (အနီစက်)

    @Data
    @Builder
    public static class StaffStatusDTO {
        private Long id;
        private String name;
        private String role; // e.g., "Nail Artist"
        private String profileImage;
    }
}