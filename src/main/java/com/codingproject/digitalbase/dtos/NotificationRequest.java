package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class NotificationRequest {
    private String title;
    private String message;
    private NotificationType type;
    private TargetAudience targetAudience;
    private MultipartFile image;

    // 🌟 Request ကနေ ပို့လိုက်မယ့် JSON String အား လက်ခံမည့် နေရာသစ်
    private String metadata;
}