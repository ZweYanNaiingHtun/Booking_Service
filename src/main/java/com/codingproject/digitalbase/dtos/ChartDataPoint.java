package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChartDataPoint {
    private String label; // Mon, Tue... သို့မဟုတ် January, February...
    private int bookingCount;
    private int cancelCount;
}
