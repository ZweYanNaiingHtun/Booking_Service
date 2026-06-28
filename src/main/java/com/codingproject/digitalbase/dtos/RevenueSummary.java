package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSummary {

    private Double totalRevenue;
    private Long totalPaymentsCount;

    // Database Native Projection Mapping အတွက် Object Constructor ကို မပျက်မကွက် ထိန်းသိမ်းထားပါတယ်
    public RevenueSummary(Object totalRevenue, Object totalPaymentsCount) {
        this.totalRevenue = totalRevenue != null ? Double.valueOf(totalRevenue.toString()) : 0.0;
        this.totalPaymentsCount = totalPaymentsCount != null ? Long.valueOf(totalPaymentsCount.toString()) : 0L;
    }
}