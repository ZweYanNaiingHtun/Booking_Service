//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ReviewRequest;
import com.codingproject.digitalbase.dtos.ReviewResponse;
import com.codingproject.digitalbase.dtos.StaffPerformanceResponse;
import java.util.List;

public interface ReviewService {
    ReviewResponse submitReview(ReviewRequest request);

    List<ReviewResponse> getReviewsByStaff(Long staffId);

    List<ReviewResponse> getReviewsByCustomer();

    List<StaffPerformanceResponse> getStaffPerformanceRanking();
}
