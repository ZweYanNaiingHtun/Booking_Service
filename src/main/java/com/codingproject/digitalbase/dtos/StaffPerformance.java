package com.codingproject.digitalbase.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformance {

    private Long staffId;
    private String staffName;
    private Long completedJobsCount;
    private Double ratingAverage;

    // Database Native Projection Mapping အတွက် Object Constructor ကို နဂိုအတိုင်း ထည့်သွင်းထားပါတယ်
    public StaffPerformance(Object staffId, Object staffName, Object completedJobsCount, Object ratingAverage) {
        this.staffId = staffId != null ? Long.valueOf(staffId.toString()) : null;
        this.staffName = staffName != null ? staffName.toString() : null;
        this.completedJobsCount = completedJobsCount != null ? Long.valueOf(completedJobsCount.toString()) : 0L;
        this.ratingAverage = ratingAverage != null ? Double.valueOf(ratingAverage.toString()) : 0.0;
    }
}