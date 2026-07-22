package com.codingproject.digitalbase.dtos;

import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffHistoryResponse {

    private Long totalJobsDone;
    private BigDecimal totalCommission;
    private List<StaffHistoryDetailResponse> historyList;

    private int pageNumber;
    private int pageSize;
    private int totalPages;
}