package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.enums.CustomerAction;
import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
import com.codingproject.digitalbase.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ==========================================
    // 🖥️ UI အပိုင်း (၁) - SENT NOTIFICATIONS PAGE (Admin Direct Send သက်သက်ပြရန်)
    // ==========================================

    // 🎯 Admin ကိုယ်တိုင် Type သတ်မှတ်ပြီး ပို့ထားသော Global Broadcasts (user IS NULL) ပြရန်
    Page<Notification> findByTypeIsNotNullAndTargetAudienceAndUserIsNull(TargetAudience targetAudience, Pageable pageable);

    // ==========================================
    // 📥 UI အပိုင်း (၂) - ADMIN INBOX DRAWER (System Actions & Events Filter များ)
    // ==========================================

    // 🎯 Incoming Customer Tab Filter (Only ALLOWS: Ordered, Cancel, Review)
    // ⚠️ 'CONFIRMED' State Noti များကို 'All' ရွေးထားချိန်တွင် ပါမလာစေရန် ပိတ်ထားပါသည်
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.CUSTOMER " +
            "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
            "AND (" +
            "    (LOWER(:tab) = 'ordered' AND (n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.PENDING OR n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.ORDERED)) OR " +
            "    (LOWER(:tab) = 'cancel' AND (n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.CANCELLED OR n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.CANCELLED)) OR " +
            "    (LOWER(:tab) = 'review' AND n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.REVIEW) OR " +
            "    ((:tab IS NULL OR LOWER(:tab) = 'all' OR TRIM(:tab) = '') AND (" +
            "        n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.PENDING OR " +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.ORDERED OR " +
            "        n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.CANCELLED OR " +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.CANCELLED OR " +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.REVIEW" +
            "    ))" +
            ") " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findAdminCustomerInbox(
            @Param("tab") String tab,
            @Param("startDate") Instant startDate,
            Pageable pageable);

    // 🎯 Incoming Staff Tab Filter (Only ALLOWS: Started, Completed)
    // ⚠️ 'CANCELLED' / 'CancelByCustomer' Noti များကို လုံးဝ ပယ်ထုတ်ထားပါသည်
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.STAFF " +
            "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
            "AND (" +
            "    (LOWER(:tab) = 'started' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS) OR " +
            "    (LOWER(:tab) = 'completed' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.COMPLETED) OR " +
            "    ((:tab IS NULL OR LOWER(:tab) = 'all' OR LOWER(:tab) = 'incoming' OR TRIM(:tab) = '') AND (" +
            "        n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS OR " +
            "        n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.COMPLETED" +
            "    ))" +
            ") " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findAdminStaffInbox(
            @Param("tab") String tab,
            @Param("startDate") Instant startDate,
            Pageable pageable);

    // 📱 CUSTOMER INBOX QUERY
    @Query("SELECT n FROM Notification n WHERE n.targetAudience IN :audiences " +
            "AND (n.user.id = :userId OR n.user IS NULL) " +
            "AND (n.customerAction IS NULL OR n.customerAction <> com.codingproject.digitalbase.enums.CustomerAction.CANCELLED) " +
            "AND (" +
            "    n.type IN (com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT, com.codingproject.digitalbase.enums.NotificationType.PROMOTION, com.codingproject.digitalbase.enums.NotificationType.REMINDER, com.codingproject.digitalbase.enums.NotificationType.ALERT) " +
            "    OR n.bookingStatus IN (" +
            "        com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.CANCELLED, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.COMPLETED" +
            "    )" +
            ") " +
            "AND (" +
            "    :tab IS NULL OR LOWER(:tab) = 'all' OR TRIM(:tab) = '' OR " +
            "    (LOWER(:tab) = 'booking' AND n.bookingStatus IS NOT NULL) OR " +
            "    (LOWER(:tab) = 'promo' AND n.type = com.codingproject.digitalbase.enums.NotificationType.PROMOTION) OR " +
            "    (LOWER(:tab) = 'announcement' AND n.type = com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT)" +
            ") " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findCustomerNotificationsByTab(
            @Param("userId") Long userId,
            @Param("tab") String tab,
            @Param("audiences") List<TargetAudience> audiences, // 🌟 Parameter ထည့်သွင်းထားပါသည်
            Pageable pageable);

    // 📱 STAFF INBOX QUERY
    @Query("SELECT n FROM Notification n WHERE n.targetAudience IN :audiences " +
            "AND (n.user.id = :userId OR n.user IS NULL) " +
            "AND (" +
            "    n.type IN (com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT, com.codingproject.digitalbase.enums.NotificationType.PROMOTION, com.codingproject.digitalbase.enums.NotificationType.REMINDER, com.codingproject.digitalbase.enums.NotificationType.ALERT) " +
            "    OR n.bookingStatus IN (" +
            "        com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.CANCELLED" +
            "    )" +
            ") " +
            "AND (" +
            "    :tab IS NULL OR :tab = '' OR LOWER(:tab) = 'all' OR LOWER(:tab) = 'incoming' OR " +
            "    (LOWER(:tab) = 'confirmed' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED) OR " +
            "    (LOWER(:tab) = 'cancelled' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.CANCELLED) OR " +
            "    (LOWER(:tab) = 'booking' AND n.bookingStatus IS NOT NULL) OR " +
            "    (LOWER(:tab) = 'promo' AND n.type = com.codingproject.digitalbase.enums.NotificationType.PROMOTION) OR " +
            "    (LOWER(:tab) = 'announcement' AND n.type = com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT)" +
            ") " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findStaffNotificationsByTab(
            @Param("userId") Long userId,
            @Param("tab") String tab,
            @Param("audiences") List<TargetAudience> audiences, // 🌟 Parameter ထည့်သွင်းထားပါသည်
            Pageable pageable);

    Page<Notification> findByTargetAudienceAndBookingStatusAndUserId(
            TargetAudience targetAudience,
            BookingStatus bookingStatus,
            Long userId,
            Pageable pageable
    );

    @Query("SELECT n FROM Notification n WHERE (n.targetAudience = :audience OR n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.BOTH) AND (n.user.id = :userId OR n.user IS NULL) ORDER BY n.createdAt DESC")
    Page<Notification> findNotificationsForUser(@Param("userId") Long userId, @Param("audience") TargetAudience audience, Pageable pageable);
}