package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.model.Notification;
import com.codingproject.digitalbase.enums.TargetAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // CreatedAt Descending ဖြင့် နောက်ဆုံးပို့သမျှ အပေါ်ဆုံးကပြရန်
    List<Notification> findByTargetAudienceOrderByCreatedAtDesc(TargetAudience targetAudience);

    // အမျိုးအစားအလိုက် သီးသန့်ဆွဲထုတ်ခြင်း (Booking သို့မဟုတ် Promotion Tab အတွက်)
    List<Notification> findByTargetAudienceAndTypeOrderByCreatedAtDesc(TargetAudience audience, NotificationType type);

    // 🌟 မိမိအတွက် သီးသန့်ပို့ထားသော notification နှင့် အားလုံးအတွက် ပို့ထားသော global notification များကို တွဲထုတ်ရန်
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = :audience AND (n.user.id = :userId OR n.user IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findByAudienceAndUserId(@Param("audience") TargetAudience audience, @Param("userId") Long userId);

    // 🌟 Tab လိုက် စစ်ထုတ်သည့်အခါ သုံးရန်
    @Query("SELECT n FROM Notification n WHERE n.targetAudience = :audience AND n.type = :type AND (n.user.id = :userId OR n.user IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findByAudienceAndTypeAndUserId(@Param("audience") TargetAudience audience, @Param("type") NotificationType type, @Param("userId") Long userId);
}