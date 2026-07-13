package com.codingproject.digitalbase.model;

import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.TargetAudience;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(nullable = false )
    private String message;

    private String imageUrl; // Store relative path: /uploads/notifications/abc.jpg

    @Enumerated(EnumType.STRING)
    @Column(nullable = false , length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "target_audience" ,length = 50)
    private TargetAudience targetAudience;

    @Builder.Default
    private boolean isRead = false;

    // 🌟 UI ထဲက "View" ခလုတ်နှိပ်လျှင် သက်ဆိုင်ရာ Review UI သို့ သွားရန် (Deep Linking)
    // ဥပမာ - "/bookings/12/review" သို့မဟုတ် Screen Name "REVIEW_PAGE"
    private String actionUrl;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 🌟 မည်သည့် User အတွက် သီးသန့်ဖြစ်သည်ကို သတ်မှတ်ရန်
}