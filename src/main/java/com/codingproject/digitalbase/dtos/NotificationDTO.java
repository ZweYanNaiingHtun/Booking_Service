package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.enums.CustomerAction;
import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
import lombok.*;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private String imageUrl;
    private NotificationType type; // BOOKING, PROMOTION, ANNOUNCEMENT, ALERT, REVIEW
    private TargetAudience targetAudience;
    private BookingStatus bookingStatus;
    private CustomerAction customerAction;
    private boolean isRead;
    private Instant createdAt;

    // 🌟 နေရာစုံ၊ Type စုံအတွက် Dynamic Data အားလုံးကို သိမ်းမည့် နေရာသစ်
    private Map<String, Object> metadata;
}