//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ReviewRequest;
import com.codingproject.digitalbase.dtos.ReviewResponse;
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

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse submitReview(ReviewRequest request) {
        Booking booking = this.bookingRepository.findById(request.getBookingId()).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentCustomer = this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
                Review savedReview = this.reviewRepository.save(review);
                List<Review> allReviewsForStaff = this.reviewRepository.findByStaffProfileId(staffProfile.getId());
                double totalStars = allReviewsForStaff.stream().mapToDouble(Review::getStarRating).sum();
                double averageRating = totalStars / (double)allReviewsForStaff.size();
                staffProfile.setRating((double)Math.round(averageRating * (double)10.0F) / (double)10.0F);
                this.staffProfileRepository.save(staffProfile);
                return this.mapToResponse(savedReview);
            }
        }
    }

    @Override
    @Transactional
    public List<ReviewResponse> getMyReviewHistory(String email) {
        // ၁။ Token မှ ရလာသော Email ဖြင့် လက်ရှိ User ကို ရှာဖွေခြင်း
        User currentUser = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // ၂။ လက်ရှိ User ထဲတွင် STAFF Role ပါဝင်နေခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
        boolean isStaff = currentUser.getRoles().stream()
                .anyMatch(role -> "STAFF".equals(role.getRole().name()));

        // 🎯 ၃။ အကယ်၍ ခေါ်ဆိုသူသည် STAFF ဖြစ်ပါက မိမိရရှိထားသော Review များကို ပြပေးခြင်း
        if (isStaff) {
            StaffProfile staffProfile = this.staffProfileRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + email));

            return this.reviewRepository.findByStaffProfileIdOrderByCreatedAtDesc(staffProfile.getId())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        // 🎯 ၄။ အကယ်၍ ခေါ်ဆိုသူသည် CUSTOMER ဖြစ်ပါက မိမိရေးသားခဲ့ဖူးသော Review များကို ပြပေးခြင်း
        return this.reviewRepository.findByCustomerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .customerName(review.getCustomer().getFullName())
                .staffName(review.getStaffProfile().getUser().getFullName())
                .starRating(review.getStarRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
