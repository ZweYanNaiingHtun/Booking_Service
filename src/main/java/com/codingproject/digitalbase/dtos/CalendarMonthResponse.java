package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.LeaveType;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarMonthResponse {
    private Instant date; // 🎯 LocalDate မှ Instant သို့ ပြောင်းလဲထားပါသည်
    private List<StaffLeaveEvent> events;

    @Data
    @Builder
    public static class StaffLeaveEvent {
        private Long staffId;
        private String staffName;
        private String role;
        private LeaveType leaveType; // 🎯 String မှ Enum သို့ ပြောင်းလဲထားပါသည်
        private String note;
    }
}