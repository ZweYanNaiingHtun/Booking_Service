package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Notification;
import com.codingproject.digitalbase.enums.TargetAudience;
import com.codingproject.digitalbase.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final Path uploadPath = Paths.get("uploads/notifications/");

    @Override
    @Transactional
    public NotificationDTO createNotification(NotificationRequest request) {
        String relativeImagePath = null;
        MultipartFile image = request.getImage();

        // ၁။ DTO ထဲက ပုံပါလာပါက သိမ်းဆည်းခြင်း Logic
        if (image != null && !image.isEmpty()) {
            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                relativeImagePath = "/uploads/notifications/" + fileName;
            } catch (IOException e) {
                throw new BadRequestException("Failed to upload notification image: " + e.getMessage());
            }
        }

        // ၂။ Request DTO မှ ဒေတာများကို Entity သို့ ပြောင်းလဲသိမ်းဆည်းခြင်း
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .targetAudience(request.getTargetAudience())
                .imageUrl(relativeImagePath)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("📢 Notification sent out successfully to {}", request.getTargetAudience());
        return mapToDTO(saved);
    }

    // 🎯 Staff Notification သီးသန့်ဆွဲထုတ်ပေးမည့် Method
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getStaffNotifications() {
        return notificationRepository
                .findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.STAFF)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // 🎯 Customer Notification သီးသန့်ဆွဲထုတ်ပေးမည့် Method
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getCustomerNotifications() {
        return notificationRepository
                .findByTargetAudienceOrderByCreatedAtDesc(TargetAudience.CUSTOMER)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getCustomerNotificationsByTab(String email, String tab) {
        // 1. Target Audience က Customer ဖြစ်ကြောင်း သတ်မှတ်ခြင်း
        TargetAudience audience = TargetAudience.CUSTOMER;

        List<Notification> results;

        // 2. ပို့လိုက်သော Tab Parameter အပေါ်မူတည်ပြီး Filter လုပ်ခြင်း Logic
        if (tab == null || tab.trim().isEmpty() || "all".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceOrderByCreatedAtDesc(audience);
        } else if ("booking".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndTypeOrderByCreatedAtDesc(audience, NotificationType.BOOKING);
        } else if ("promotion".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndTypeOrderByCreatedAtDesc(audience, NotificationType.PROMOTION);
        } else {
            results = notificationRepository.findByTargetAudienceOrderByCreatedAtDesc(audience);
        }

        return results.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (notification.getImageUrl() != null) {
            try {
                String fileName = notification.getImageUrl().replace("/uploads/notifications/", "");
                Path filePath = uploadPath.resolve(fileName);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Failed to delete file: {}", e.getMessage());
            }
        }
        notificationRepository.delete(notification);
    }

    private NotificationDTO mapToDTO(Notification entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .imageUrl(entity.getImageUrl())
                .type(entity.getType())
                .targetAudience(entity.getTargetAudience())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}