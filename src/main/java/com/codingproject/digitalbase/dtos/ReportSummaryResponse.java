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
public class ReportSummaryResponse {
    private RevenueBlock grandRevenue; // 🌟 Summary Card တစ်ခုတည်းမှာ ပြသရန် ပေါင်းစည်းထားသော ဝင်ငွေ Block
    private List<ReportChartDataPoint> chartData;
}
