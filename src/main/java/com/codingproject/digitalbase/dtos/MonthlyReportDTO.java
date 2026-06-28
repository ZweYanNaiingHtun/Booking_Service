package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@NoArgsConstructor
public class MonthlyReportDTO {

    private String month;
    private Long totalBookings;
    private Double totalRevenue;

    // Native/JPQL Query Result Mapping အတွက် နဂို Custom Constructor ကို မပျက်မကွက် ထိန်းသိမ်းထားပါတယ်
    public MonthlyReportDTO(Object month, Object totalBookings, Object totalRevenue) {
        this.month = month != null ? month.toString() : null;
        this.totalBookings = totalBookings != null ? Long.valueOf(totalBookings.toString()) : 0L;
        this.totalRevenue = totalRevenue != null ? Double.valueOf(totalRevenue.toString()) : 0.0;
    }
}