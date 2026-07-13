package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationService {
    // 🌟 Parameter အများကြီးအစား DTO တစ်ခုတည်းသာ သုံးတော့မည်
    NotificationDTO createNotification(NotificationRequest request);

    // 🎯 List နှစ်ခုအား သီးခြားစီ ခွဲထုတ်လိုက်ခြင်း
    List<NotificationDTO> getStaffNotifications();

    List<NotificationDTO> getCustomerNotifications();

    List<NotificationDTO> getCustomerNotificationsByTab(String email, String tab);

    void markAsRead(Long id);

    void deleteNotification(Long id);
}