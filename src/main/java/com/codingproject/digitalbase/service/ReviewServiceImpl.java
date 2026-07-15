package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ReviewRequest;
import com.codingproject.digitalbase.dtos.ReviewResponse;
import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;

    // 🌟 Notification Service အား အသုံးပြုရန် Inject နှိုးခြင်း
    private final NotificationService notificationService;

    @Transactional
    public ReviewResponse submitReview(ReviewRequest request) {
        Booking booking = this.bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentCustomer = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!booking.getCustomer().getId().equals(currentCustomer.getId())) {
            throw new BadRequestException("Unauthorized! You can only review your own bookings.");
        } else if (!"COMPLETED".equals(booking.getStatus().name())) {
            throw new BadRequestException("You can only review completed service bookings.");
        } else if (this.reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new BadRequestException("You have already submitted a review for this booking.");
        }

        StaffProfile staffProfile = booking.getAssignedStaff();
        if (staffProfile == null) {
            throw new BadRequestException("No staff member was assigned to this booking.");
        }

        // ၁။ Review အား စနစ်တကျ DB တွင် သိမ်းဆည်းခြင်း
        Review review = Review.builder()
                .booking(booking)
                .customer(currentCustomer)
                .staffProfile(staffProfile)
                .starRating(request.getStarRating())
                .comment(request.getComment())
                .build();

        Review savedReview = this.reviewRepository.save(review);

        // ၂။ ဝန်ထမ်း၏ Average Star Rating အား ပြန်လည်တွက်ချက်ပြင်ဆင်ခြင်း
        List<Review> allReviewsForStaff = this.reviewRepository.findByStaffProfileId(staffProfile.getId());
        double totalStars = allReviewsForStaff.stream().mapToDouble(Review::getStarRating).sum();
        double averageRating = totalStars / (double)allReviewsForStaff.size();
        staffProfile.setRating((double)Math.round(averageRating * (double)10.0F) / (double)10.0F);
        this.staffProfileRepository.save(staffProfile);

        // 🚀 ၃။ Shared Metadata Payload တည်ဆောက်ခြင်း (Frontend မှ ခွဲထုတ်ဖတ်ရှုနိုင်ရန်)
        Map<String, Object> reviewMetadata = new HashMap<>();
        reviewMetadata.put("bookingId", booking.getId());
        reviewMetadata.put("reviewId", savedReview.getId());
        reviewMetadata.put("starRating", request.getStarRating());
        reviewMetadata.put("customerName", currentCustomer.getFullName());

        // 📢 ၄။ Customer ထံသို့ Review တင်ပေးမှု အောင်မြင်ကြောင်း Noti ပို့ခြင်း
        try {
            notificationService.sendSystemNotification(
                    "Review Submitted! ⭐",
                    "Thank you for your feedback! You gave a " + request.getStarRating() + "-star rating to " + staffProfile.getUser().getFullName() + ".",
                    NotificationType.BOOKING, // 💡 အကယ်၍ Enum ထဲတွင် REVIEW သီးသန့်ရှိက လဲလှယ်နိုင်ပါသည်
                    TargetAudience.CUSTOMER,
                    currentCustomer,
                    reviewMetadata
            );
        } catch (Exception e) {
            log.error("Failed to send review notification to customer", e);
        }

        // 📢 ၅။ သက်ဆိုင်ရာ ဝန်ထမ်း (Staff) ထံသို့ Review အသစ်ရရှိကြောင်း Noti ပို့ခြင်း
        try {
            notificationService.sendSystemNotification(
                    "New Review Received! 📝",
                    currentCustomer.getFullName() + " submitted a " + request.getStarRating() + "-star review for your service.",
                    NotificationType.BOOKING,
                    TargetAudience.STAFF,
                    staffProfile.getUser(), // StaffProfile ၏ User Entity အား တိုက်ရိုက်ရယူခြင်း
                    reviewMetadata
            );
        } catch (Exception e) {
            log.error("Failed to send review notification to staff", e);
        }

        return this.mapToResponse(savedReview);
    }

    @Override
    @Transactional
    public List<ReviewResponse> getMyReviewHistory(String email) {
        User currentUser = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        boolean isStaff = currentUser.getRoles().stream()
                .anyMatch(role -> "STAFF".equals(role.getRole().name()));

        if (isStaff) {
            StaffProfile staffProfile = this.staffProfileRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + email));

            return this.reviewRepository.findByStaffProfileIdOrderByCreatedAtDesc(staffProfile.getId())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

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