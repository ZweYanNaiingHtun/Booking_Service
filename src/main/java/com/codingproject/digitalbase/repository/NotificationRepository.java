package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.model.Notification;
import com.codingproject.digitalbase.enums.TargetAudience;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.Instant;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ==========================================
    // 🖥️ UI အပိုင်း (၁) - SENT NOTIFICATIONS PAGE (Admin Direct Send သက်သက်ပြရန်)
    // ==========================================

    // 🎯 Admin ကိုယ်တိုင် Type (ANNOUNCEMENT, PROMOTION စသည်) သတ်မှတ်ပြီး ပို့ထားသော Global Broadcasts (user IS NULL) သက်သက်သာ ပြရန်
    Page<Notification> findByTypeIsNotNullAndTargetAudienceAndUserIsNull(TargetAudience targetAudience, Pageable pageable);

    // ==========================================
    // 📥 UI အပိုင်း (၂) - ADMIN INBOX DRAWER (System Actions & Events များ ပြရန်)
    // ==========================================

    // 🎯 Incoming Customer Tab Filter: Ordered (PENDING), Cancel (CANCELLED), Review (REVIEW)
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.CUSTOMER " +
            "AND n.createdAt >= :startDate " +
            "AND (:tab = 'all' OR " +
            "    (:tab = 'ordered' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.PENDING) OR " +
            "    (:tab = 'cancel' AND n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.CANCELLED) OR " +
            "    (:tab = 'review' AND n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.REVIEW))")
    Page<Notification> findAdminCustomerInbox(
            @Param("tab") String tab,
            @Param("startDate") Instant startDate,
            Pageable pageable);

    // 🎯 Incoming Staff Tab Filter: Started (IN_PROGRESS), Completed (COMPLETED)
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.STAFF " +
            "AND n.createdAt >= :startDate " +
            "AND (:tab = 'all' OR " +
            "    (:tab = 'started' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS) OR " +
            "    (:tab = 'completed' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.COMPLETED))")
    Page<Notification> findAdminStaffInbox(
            @Param("tab") String tab,
            @Param("startDate") Instant startDate,
            Pageable pageable);

    // ==========================================
    // 📱 ကုဒ်ဟောင်းများ (Mobile App နှင့် အခြား Framework များအတွက် ချန်လှပ်ထားပါသည်)
    // ==========================================

    List<Notification> findByTargetAudienceOrderByCreatedAtDesc(TargetAudience targetAudience);

    List<Notification> findByTargetAudienceAndTypeAndUserIdOrderByCreatedAtDesc(
            TargetAudience targetAudience,
            NotificationType type,
            Long userId
    );

    List<Notification> findByTargetAudienceAndTypeOrderByCreatedAtDesc(TargetAudience audience, NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.targetAudience = :audience " +
            "AND (" +
            "  n.type IN (com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT, " +
            "             com.codingproject.digitalbase.enums.NotificationType.PROMOTION) " +
            "  OR " +
            "  (n.type IN (com.codingproject.digitalbase.enums.NotificationType.BOOKING, " +
            "              com.codingproject.digitalbase.enums.NotificationType.REMINDER, " +
            "              com.codingproject.digitalbase.enums.NotificationType.ALERT) " +
            "   AND n.user.id = :userId)" +
            ") " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsForUser(
            @Param("userId") Long userId,
            @Param("audience") TargetAudience audience
    );

    Page<Notification> findByTargetAudience(TargetAudience targetAudience, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.targetAudience = :audience AND (n.user.id = :userId OR n.user IS NULL)")
    Page<Notification> findNotificationsForUser(@Param("userId") Long userId, @Param("audience") TargetAudience audience, Pageable pageable);

    Page<Notification> findByTargetAudienceAndTypeAndUserId(TargetAudience targetAudience, NotificationType type, Long userId, Pageable pageable);

    Page<Notification> findByTargetAudienceAndType(TargetAudience targetAudience, NotificationType type, Pageable pageable);
}