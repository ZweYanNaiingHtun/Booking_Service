package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.TargetAudience;
import java.util.List;

public interface NotificationService {

    // 📢 ၁။ Notification အသစ် ဖန်တီးပေးပို့ရန်
    NotificationDTO createNotification(NotificationRequest request);

    // 🖥️ ၂။ [Admin Dashboard တွက်] Target Audience အလိုက် Noti သမိုင်းကြောင်းအားလုံးကို ဆွဲထုတ်ရန်
    List<NotificationDTO> getAllNotificationsByAudience(TargetAudience audience);

    // 📱 ၃။ [Customer Mobile App တွက်] Tab အလိုက် မိမိနှင့်သက်ဆိုင်သော Inbox Noti များ ဆွဲထုတ်ရန်
    List<NotificationDTO> getCustomerNotificationsByTab(String email, String tab);

    // 📱 ၄။ [Staff Mobile App တွက်] Tab အလိုက် မိမိနှင့်သက်ဆိုင်သော Inbox Noti များ ဆွဲထုတ်ရန်
    List<NotificationDTO> getStaffNotificationsByTab(String email, String tab);

    // 🛡️ ၅။ Noti ကို ဖတ်ပြီးကြောင်း Mark လုပ်ရန် (ပိုင်ရှင်မှန်ကန်ကြောင်းပါ စစ်ဆေးမည်)
    void markAsRead(Long id, String userEmail);

    // 🗑️ ၆။ Notification ကို စနစ်ထဲမှ ဖျက်သိမ်းရန် (ပုံပါက ပုံပါတွဲဖျက်မည်)
    void deleteNotification(Long id);
}