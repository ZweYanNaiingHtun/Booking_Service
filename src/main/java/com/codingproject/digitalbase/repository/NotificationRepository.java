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

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 🌟 [Admin Framework တွက်] Target Audience တစ်ခုလုံးရဲ့ သမိုင်းကြောင်းအားလုံးကို ဆွဲထုတ်ရန်
    List<Notification> findByTargetAudienceOrderByCreatedAtDesc(TargetAudience targetAudience);

    // 🌟 Specific User အတွက် Type အလိုက် Noti Filter လုပ်ရန်
    List<Notification> findByTargetAudienceAndTypeAndUserIdOrderByCreatedAtDesc(
            TargetAudience targetAudience,
            NotificationType type,
            Long userId
    );

    // အမျိုးအစားအလိုက် သီးသန့်ဆွဲထုတ်ခြင်း
    List<Notification> findByTargetAudienceAndTypeOrderByCreatedAtDesc(TargetAudience audience, NotificationType type);

    // 🌟 Customer/Staff Mobile App Inbox အတွက် Personal + Public Broadcast ခွဲထုတ်ပေးမည့် Query
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

    // 🌟 ၂။ Derived Query Method များတွင် Pageable ပြောင်းလဲခြင်း (OrderBy စာသားများ ဖြုတ်လိုက်ပါ)
    Page<Notification> findByTargetAudienceAndTypeAndUserId(TargetAudience targetAudience, NotificationType type, Long userId, Pageable pageable);

    Page<Notification> findByTargetAudienceAndType(TargetAudience targetAudience, NotificationType type, Pageable pageable);
}