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
    // 🎯 ADMIN PANEL - INCOMING CUSTOMER INBOX QUERY (FIXED)
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.CUSTOMER " +
            "AND n.user IS NULL " + // 🌟 Admin Inbox အတွက် user = null ဖြစ်သော Noti များသာ ယူမည်
            "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
            "AND (" +
            "    /* 1. ORDERED TAB: Booking Status PENDING သို့မဟုတ် customerAction ORDERED */ " +
            "    (LOWER(:tab) = 'ordered' AND (" +
            "        n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.PENDING OR " +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.ORDERED" +
            "    )) OR " +
            "    /* 2. CANCEL TAB: Customer ကိုယ်တိုင် Cancel လုပ်ထားသော Noti များ */ " +
            "    (LOWER(:tab) = 'cancel' AND (" +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.CANCELLED" +
            "    )) OR " +
            "    /* 3. REVIEW TAB: Review / Rating အားလုံး ပါဝင်စေခြင်း */ " +
            "    (LOWER(:tab) = 'review' AND (" +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.REVIEW OR " +
            "        n.type = com.codingproject.digitalbase.enums.NotificationType.RATING" +
            "    )) OR " +
            "    /* 4. ALL TAB: Customer ဘက်မှ Incoming Noti အားလုံး ပြသပေးခြင်း */ " +
            "    ((:tab IS NULL OR LOWER(:tab) = 'all' OR TRIM(:tab) = '') AND (" +
            "        n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.PENDING OR " +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.ORDERED OR " +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.CANCELLED OR " +
            "        n.customerAction = com.codingproject.digitalbase.enums.CustomerAction.REVIEW OR " +
            "        n.type = com.codingproject.digitalbase.enums.NotificationType.RATING" +
            "    ))" +
            ") " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findAdminCustomerInbox(
            @Param("tab") String tab,
            @Param("startDate") Instant startDate,
            Pageable pageable);

    // 🎯 Incoming Staff Tab Filter (Only ALLOWS: Started, Completed)
    // ⚠️ 'CANCELLED' / 'CancelByCustomer' Noti များကို လုံးဝ ပယ်ထုတ်ထားပါသည်
    // 🎯 Incoming Staff Tab Filter (Allows: STAFF & BOTH audiences for Started, Completed)
    // 🎯 ADMIN PANEL - INCOMING STAFF INBOX QUERY (FIXED)
    @Query("SELECT n FROM Notification n WHERE " +
            "(n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.STAFF OR " +
            " n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.BOTH) " +
            "AND n.user IS NULL " + // 🌟 1. Staff မိုဘိုင်း App သို့ သီးသန့် ပို့ထားသော Personal Noti များ မပါစေရန်
            "AND n.type <> com.codingproject.digitalbase.enums.NotificationType.RATING " + // 🌟 2. Review/Rating Noti များ Incoming Staff ထဲ မဝင်စေရန်
            "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
            "AND (" +
            "    /* STARTED TAB: Staff လုပ်ငန်းစတင်သည့် Noti များ */ " +
            "    (LOWER(:tab) = 'started' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS) OR " +
            "    /* COMPLETED TAB: Staff လုပ်ငန်းပြီးစီးသည့် Operational Noti များ */ " +
            "    (LOWER(:tab) = 'completed' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.COMPLETED) OR " +
            "    /* ALL / INCOMING TAB */ " +
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

    // =========================================================================
// 📱 1. CUSTOMER INBOX QUERY
// (Confirm, Started/In-Progress, Completed များသာ ရရှိမည်)
// =========================================================================
    @Query("SELECT n FROM Notification n WHERE n.targetAudience IN :audiences " +
            "AND (n.customerAction IS NULL OR n.customerAction <> com.codingproject.digitalbase.enums.CustomerAction.CANCELLED) " +
            "AND (" +
            "    /* Global Broadcast Notifications (Announcement, Promo, Reminder, Alert) */ " +
            "    (n.type IN (" +
            "        com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT, " +
            "        com.codingproject.digitalbase.enums.NotificationType.PROMOTION, " +
            "        com.codingproject.digitalbase.enums.NotificationType.REMINDER, " +
            "        com.codingproject.digitalbase.enums.NotificationType.ALERT" +
            "    ) AND (n.user.id = :userId OR n.user IS NULL)) " +
            "    OR " +
            "    /* Personal Booking Updates ONLY for this Customer (Confirmed, Started, Completed) */ " +
            "    (n.bookingStatus IN (" +
            "        com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.COMPLETED" +
            "    ) AND n.user.id = :userId) " +
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
            @Param("audiences") List<TargetAudience> audiences,
            Pageable pageable);


    // =========================================================================
// 📱 2. STAFF INBOX QUERY
// (Confirm နှင့် Cancel State များသာ ရရှိမည်)
// =========================================================================
    @Query("SELECT n FROM Notification n WHERE n.targetAudience IN :audiences " +
            "AND (" +
            "    /* Global Broadcast Notifications */ " +
            "    (n.type IN (" +
            "        com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT, " +
            "        com.codingproject.digitalbase.enums.NotificationType.PROMOTION, " +
            "        com.codingproject.digitalbase.enums.NotificationType.REMINDER, " +
            "        com.codingproject.digitalbase.enums.NotificationType.ALERT" +
            "    ) AND (n.user.id = :userId OR n.user IS NULL)) " +
            "    OR " +
            "    /* Personal Rating/Review Notifications */ " +
            "    (n.type = com.codingproject.digitalbase.enums.NotificationType.RATING AND n.user.id = :userId) " +
            "    OR " +
            "    /* Personal Booking Updates ONLY for this Staff (Confirmed & Cancelled Only) */ " +
            "    (n.bookingStatus IN (" +
            "        com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.CANCELLED" +
            "    ) AND n.user.id = :userId) " +
            ") " +
            "AND (" +
            "    :tab IS NULL OR :tab = '' OR LOWER(:tab) = 'all' OR LOWER(:tab) = 'incoming' OR " +
            "    (LOWER(:tab) = 'confirmed' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED) OR " +
            "    (LOWER(:tab) = 'cancelled' AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.CANCELLED) OR " +
            "    (LOWER(:tab) = 'booking' AND n.bookingStatus IS NOT NULL) OR " +
            "    (LOWER(:tab) = 'promo' AND n.type = com.codingproject.digitalbase.enums.NotificationType.PROMOTION) OR " +
            "    (LOWER(:tab) = 'announcement' AND n.type = com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT) OR " +
            "    (LOWER(:tab) = 'review' AND n.type = com.codingproject.digitalbase.enums.NotificationType.RATING) OR " +
            "    (LOWER(:tab) = 'rating' AND n.type = com.codingproject.digitalbase.enums.NotificationType.RATING)" +
            ") " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findStaffNotificationsByTab(
            @Param("userId") Long userId,
            @Param("tab") String tab,
            @Param("audiences") List<TargetAudience> audiences,
            Pageable pageable);

    Page<Notification> findByTargetAudienceAndBookingStatusAndUserId(
            TargetAudience targetAudience,
            BookingStatus bookingStatus,
            Long userId,
            Pageable pageable
    );

    @Query("SELECT n FROM Notification n WHERE (n.targetAudience = :audience OR n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.BOTH) AND (n.user.id = :userId OR n.user IS NULL) ORDER BY n.createdAt DESC")
    Page<Notification> findNotificationsForUser(@Param("userId") Long userId, @Param("audience") TargetAudience audience, Pageable pageable);

    Page<Notification> findByTypeIsNotNullAndTargetAudienceInAndUserIsNull(
            List<TargetAudience> targetAudiences,
            Pageable pageable
    );
}