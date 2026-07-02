package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MonthlyReportResponse {
    private List<TopServiceResponse> topServices;
    private List<StaffPerformance> staffPerformance;
}
