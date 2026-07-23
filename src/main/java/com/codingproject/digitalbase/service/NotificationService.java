package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.enums.CustomerAction;
import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
import com.codingproject.digitalbase.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface NotificationService {

    // 📢 ၁။ Notification အသစ် ဖန်တီးပေးပို့ရန်
    NotificationDTO createNotification(NotificationRequest request);

    // 🖥️ ၂။ [Admin Dashboard တွက်] Target Audience အလိုက် Noti သမိုင်းကြောင်းအားလုံးကို ဆွဲထုတ်ရန် (🌟 Pagination ဖြင့် ပြင်ဆင်ပြီး)
    Page<NotificationDTO> getAllNotificationsByAudience(TargetAudience audience, Pageable pageable);

    Page<NotificationDTO> getAdminInboxCustomer(String tab, String timeFilter, Pageable pageable);

    Page<NotificationDTO> getAdminInboxStaff(String tab, String timeFilter, Pageable pageable);

    // 📱 ၃။ [Customer Mobile App တွက်] Tab အလိုက် မိမိနှင့်သက်ဆိုင်သော Inbox Noti များ ဆွဲထုတ်ရန်
    Page<NotificationDTO> getCustomerNotificationsByTab(String email, String tab, Pageable pageable);

    Page<NotificationDTO> getStaffNotificationsByTab(String email, String tab, Pageable pageable);


    // 🛡️ ၅။ Noti ကို ဖတ်ပြီးကြောင်း Mark လုပ်ရန် (ပိုင်ရှင်မှန်ကန်ကြောင်းပါ စစ်ဆေးမည်)
    void markAsRead(Long id, String userEmail);

    // 🗑️ ၆။ Notification ကို စနစ်ထဲမှ ဖျက်သိမ်းရန် (ပုံပါက ပုံပါတွဲဖျက်မည်)
    void deleteNotification(Long id);

    void saveSystemNotification(String title, String message, NotificationType type,
                                TargetAudience audience, User targetUser, Map<String, Object> metadata);

    // 🌟 Customer Event Notification ကြေညာချက် ထည့်သွင်းပေးရန်
    void saveCustomerEventNotification(String title, String message,NotificationType type, CustomerAction action, BookingStatus status, User targetUser, Map<String, Object> metadata);

    // 🌟 Staff Event Notification ကြေညာချက် ပါတစ်ခါတည်း ထည့်ထားပေးပါ
    void saveStaffEventNotification(
            String title,
            String message,
            NotificationType type,          // 🌟 NotificationType တိုးလိုက်ပါ
            BookingStatus status,
            User targetUser,
            Map<String, Object> metadata
    );
}