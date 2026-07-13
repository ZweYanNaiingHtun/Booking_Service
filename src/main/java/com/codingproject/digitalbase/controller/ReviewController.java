package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.ReviewRequest;
import com.codingproject.digitalbase.dtos.ReviewResponse;
import com.codingproject.digitalbase.dtos.StaffPerformanceResponse;
import com.codingproject.digitalbase.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/reviews"})
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(@RequestBody @Valid ReviewRequest request) {
        return ResponseEntity.ok(this.reviewService.submitReview(request));
    }

    @GetMapping({"/my-history"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getMyReviewHistory(@AuthenticationPrincipal UserDetails userDetails) {
        // 🌟 Token ထဲမှ ပါလာသော Username (Email) ကို Service သို့ လှမ်းပို့လိုက်ခြင်း
        return ResponseEntity.ok(this.reviewService.getMyReviewHistory(userDetails.getUsername()));
    }
}
