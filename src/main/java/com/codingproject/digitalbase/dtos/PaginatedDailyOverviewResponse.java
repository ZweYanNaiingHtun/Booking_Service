package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedDailyOverviewResponse {
    private List<DailyOverviewDataPoint> content; // 📋 နေ့စဉ် ဒေတာစာရင်း Array
    private int pageNumber;                       // 📄 လက်ရှိ Page နံပါတ်
    private int pageSize;                         // 📏 တစ်မျက်နှာတွင် ပြသမည့် Row အရေအတွက်
    private int totalPages;                       // 🔢 စုစုပေါင်း စာမျက်နှာ အရေအတွက်
    private long totalElements;                   // 🗓️ ထိုလတွင်ရှိသော စုစုပေါင်း ရက်အရေအတွက် (ဥပမာ - ၃၀ ရက် သို့မဟုတ် ၃၁ ရက်)
}