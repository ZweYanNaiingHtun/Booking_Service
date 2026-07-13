package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    private String title;
    private String message;
    private NotificationType type;
    private TargetAudience targetAudience;
    private MultipartFile image; // 🌟 စာသားတွေရော ပုံပါ DTO တစ်ခုတည်းမှာ စုစည်းလက်ခံခြင်း
}