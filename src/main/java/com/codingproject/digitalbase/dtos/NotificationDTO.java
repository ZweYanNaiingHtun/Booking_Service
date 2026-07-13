package com.codingproject.digitalbase.dtos;

import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
import lombok.*;
import java.time.Instant;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private String imageUrl;
    private NotificationType type;
    private TargetAudience targetAudience;
    private boolean isRead;
    private Instant createdAt;
}