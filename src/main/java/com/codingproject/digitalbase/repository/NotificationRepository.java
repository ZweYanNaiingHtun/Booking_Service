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

    // 🎯 Admin ကိုယ်တိုင် ပို့ထားသော Global Broadcast Notification များကိုသာ ဆွဲထုတ်ရန်
    Page<Notification> findByIsBroadcastTrueAndTargetAudienceInAndUserIsNull(
            List<TargetAudience> targetAudiences,
            Pageable pageable
    );

    // ==========================================
    // 📥 UI - ADMIN INBOX DRAWER (System Actions & Events Filter များ)
    // ==========================================

    // 🎯 ADMIN PANEL - INCOMING CUSTOMER INBOX QUERY
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.CUSTOMER " +
            "AND n.user IS NULL " +
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

    // 🎯 ADMIN PANEL - INCOMING STAFF INBOX QUERY (FIXED Bug for Started Tab)
    @Query("SELECT n FROM Notification n WHERE " +
            "(n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.STAFF OR " +
            " n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.BOTH) " +
            "AND (n.type IS NULL OR n.type <> com.codingproject.digitalbase.enums.NotificationType.RATING) " + // 🌟 Rating Noti မပါစေရန် & Null Safe Check
            "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
            "AND (" +
            "    /* STARTED TAB: Staff လုပ်ငန်းစတင်သည့် Noti များ ('started' သို့မဟုတ် 'in_progress' နှစ်ခုလုံး လက်ခံသည်) */ " +
            "    ((LOWER(:tab) = 'started' OR LOWER(:tab) = 'in_progress') AND n.bookingStatus = com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS) OR " +
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
    // =========================================================================
    @Query("SELECT n FROM Notification n WHERE n.targetAudience IN :audiences " +
            "AND (n.customerAction IS NULL OR n.customerAction <> com.codingproject.digitalbase.enums.CustomerAction.CANCELLED) " +
            "AND (" +
            "    /* Global Broadcast Notifications */ " +
            "    (n.type IN (" +
            "        com.codingproject.digitalbase.enums.NotificationType.ANNOUNCEMENT, " +
            "        com.codingproject.digitalbase.enums.NotificationType.PROMOTION, " +
            "        com.codingproject.digitalbase.enums.NotificationType.REMINDER, " +
            "        com.codingproject.digitalbase.enums.NotificationType.ALERT" +
            "    ) AND (n.user.id = :userId OR n.user IS NULL)) " +
            "    OR " +
            "    /* Personal Booking Updates ONLY for this Customer (CANCELLED အပါအဝင်) */ " +
            "    (n.bookingStatus IN (" +
            "        com.codingproject.digitalbase.enums.BookingStatus.CONFIRMED, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.COMPLETED, " +
            "        com.codingproject.digitalbase.enums.BookingStatus.CANCELLED" + // 🌟 ဒီနေရာတွင် CANCELLED ကို ထည့်သွင်းထားပါသည်
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
            "    /* Personal Booking Updates ONLY for this Staff */ " +
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

    @Query("SELECT n FROM Notification n WHERE (n.targetAudience = :audience OR n.targetAudience = com.codingproject.digitalbase.enums.TargetAudience.BOTH) AND (n.user.id = :userId OR n.user IS NULL) ORDER BY n.createdAt DESC")
    Page<Notification> findNotificationsForUser(@Param("userId") Long userId, @Param("audience") TargetAudience audience, Pageable pageable);

    Page<Notification> findByTypeIsNotNullAndTargetAudienceInAndUserIsNull(
            List<TargetAudience> targetAudiences,
            Pageable pageable
    );

}