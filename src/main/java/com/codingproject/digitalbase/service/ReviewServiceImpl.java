//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ReviewRequest;
import com.codingproject.digitalbase.dtos.ReviewResponse;
import com.codingproject.digitalbase.dtos.StaffPerformanceResponse;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Booking;
import com.codingproject.digitalbase.model.Review;
import com.codingproject.digitalbase.model.StaffProfile;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.ReviewRepository;
import com.codingproject.digitalbase.repository.StaffProfileRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse submitReview(ReviewRequest request) {
        Booking booking = (Booking)this.bookingRepository.findById(request.getBookingId()).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentCustomer = (User)this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!booking.getCustomer().getId().equals(currentCustomer.getId())) {
            throw new BadRequestException("Unauthorized! You can only review your own bookings.");
        } else if (!"COMPLETED".equals(booking.getStatus().name())) {
            throw new BadRequestException("You can only review completed service bookings.");
        } else if (this.reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new BadRequestException("You have already submitted a review for this booking.");
        } else {
            StaffProfile staffProfile = booking.getAssignedStaff();
            if (staffProfile == null) {
                throw new BadRequestException("No staff member was assigned to this booking.");
            } else {
                Review review = Review.builder().booking(booking).customer(currentCustomer).staffProfile(staffProfile).starRating(request.getStarRating()).comment(request.getComment()).build();
                Review savedReview = (Review)this.reviewRepository.save(review);
                List<Review> allReviewsForStaff = this.reviewRepository.findByStaffProfileId(staffProfile.getId());
                double totalStars = allReviewsForStaff.stream().mapToDouble(Review::getStarRating).sum();
                double averageRating = totalStars / (double)allReviewsForStaff.size();
                staffProfile.setRating((double)Math.round(averageRating * (double)10.0F) / (double)10.0F);
                this.staffProfileRepository.save(staffProfile);
                return this.mapToResponse(savedReview);
            }
        }
    }

    public List<ReviewResponse> getReviewsByCustomer() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentCustomer = (User)this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return this.reviewRepository.findByCustomerIdOrderByCreatedAtDesc(currentCustomer.getId()).stream().map(this::mapToResponse).toList();
    }

    public List<StaffPerformanceResponse> getStaffPerformanceRanking() {
        Pageable topFive = PageRequest.of(0, 5);
        List<Object[]> results = this.reviewRepository.findTopStaffPerformance(topFive);
        return results.stream().map((result) -> {
            StaffProfile staff = (StaffProfile)result[0];
            Double avgRating = (Double)result[1];
            return StaffPerformanceResponse.builder().staffId(staff.getId()).staffName(staff.getUser().getFullName()).averageRating((double)Math.round(avgRating * (double)10.0F) / (double)10.0F).build();
        }).toList();
    }

    public List<ReviewResponse> getReviewsByStaff(Long staffId) {
        if (!this.staffProfileRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff profile not found with ID: " + staffId);
        } else {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(new String[]{"createdAt"}).descending());
            Page<Review> reviewPage = this.reviewRepository.findByStaffProfileId(staffId, pageable);
            return reviewPage.getContent().stream().map(this::mapToResponse).toList();
        }
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder().id(review.getId()).bookingId(review.getBooking().getId()).customerName(review.getCustomer().getFullName()).staffName(review.getStaffProfile().getUser().getFullName()).starRating(review.getStarRating()).comment(review.getComment()).createdAt(review.getCreatedAt()).build();
    }

    @Generated
    public ReviewServiceImpl(final ReviewRepository reviewRepository, final BookingRepository bookingRepository, final StaffProfileRepository staffProfileRepository, final UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.userRepository = userRepository;
    }
}
