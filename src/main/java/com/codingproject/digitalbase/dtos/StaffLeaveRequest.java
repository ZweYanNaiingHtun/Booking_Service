package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant; // 🌟 Instant သို့ ပြောင်းလဲရန်
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StaffLeaveRequest {

    @NotNull(message = "Staff member must be selected")
    private Long staffProfileId;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    private Instant startDate; // 🌟 Instant သို့ ပြောင်းလဲခြင်း

    private Instant endDate; // 🌟 Optional Single-Day Leave အတွက် @NotNull ဖြုတ်ထားဆဲဖြစ်ပါသည်

    private String note;
}