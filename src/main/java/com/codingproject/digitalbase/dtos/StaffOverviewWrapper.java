package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOverviewWrapper {
    private long availableCount;
    private long inProgressCount;
    private long unavailableCount;
    private long inactiveCount;
    private List<StaffPerformance> staffList;
}