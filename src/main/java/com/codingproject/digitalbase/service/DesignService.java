package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ChartDataPoint;
import com.codingproject.digitalbase.dtos.DashboardStatsResponse;
import com.codingproject.digitalbase.dtos.DesignResponseDto;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DesignService {
    List<DesignResponseDto> getTrendingDesigns(String customerEmail);

    void toggleReactToDesign(Long designId, String customerEmail);

    List<DesignResponseDto> getCustomerFavorites(String customerEmail);

    DesignResponseDto uploadDesign(String title, MultipartFile file) throws IOException;


}