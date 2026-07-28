package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyStaffStatusResponse {

    // 🌟 Key အဖြစ် LocalDate (YYYY-MM-DD) သို့မဟုတ် String ရလဒ် ထွက်ပါမည်
    private Map<LocalDate, DailyStaffStatusResponse> dailyStatuses;
}