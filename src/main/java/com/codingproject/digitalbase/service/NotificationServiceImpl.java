package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Notification;
import com.codingproject.digitalbase.enums.TargetAudience;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.NotificationRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Path uploadPath = Paths.get("uploads/notifications/");

    @Override
    @Transactional
    public NotificationDTO createNotification(NotificationRequest request) {
        String relativeImagePath = null;
        MultipartFile image = request.getImage();

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

        java.util.Map<String, Object> metadataMap = new java.util.HashMap<>();
        if (request.getMetadata() != null && !request.getMetadata().trim().isEmpty()) {
            try {
                metadataMap = objectMapper.readValue(request.getMetadata(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            } catch (IOException e) {
                log.error("Failed to parse metadata JSON from request", e);
            }
        }

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .targetAudience(request.getTargetAudience())
                .imageUrl(relativeImagePath)
                .metadata(metadataMap)
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("📢 Notification sent out successfully to {}", request.getTargetAudience());
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getAllNotificationsByAudience(TargetAudience audience, Pageable pageable) {
        log.info("Admin UI: Fetching paged global notification history for audience: {}", audience);
        Page<Notification> notificationPage = notificationRepository.findByTargetAudience(audience, pageable);
        return notificationPage.map(this::mapToDTO);
    }

    // 📱 [Customer Mobile App တွက်] 🌟 Pagination စနစ်ဖြင့် အဆင့်မြှင့်တင်ထားသော Inbox Filter
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getCustomerNotificationsByTab(String email, String tab, Pageable pageable) {
        User customerUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        TargetAudience audience = TargetAudience.CUSTOMER;
        Page<Notification> results;

        if (tab == null || tab.trim().isEmpty() || "all".equalsIgnoreCase(tab)) {
            results = notificationRepository.findNotificationsForUser(customerUser.getId(), audience, pageable);
        } else if ("booking".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndTypeAndUserId(
                    audience, NotificationType.BOOKING, customerUser.getId(), pageable);
        } else if ("promotion".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndType(audience, NotificationType.PROMOTION, pageable);
        } else {
            results = notificationRepository.findNotificationsForUser(customerUser.getId(), audience, pageable);
        }

        return results.map(this::mapToDTO);
    }

    // 📱 [Staff Mobile App တွက်] 🌟 Pagination စနစ်ဖြင့် အဆင့်မြှင့်တင်ထားသော Inbox Filter
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getStaffNotificationsByTab(String email, String tab, Pageable pageable) {
        User staffUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found."));

        TargetAudience audience = TargetAudience.STAFF;
        Page<Notification> results;

        if (tab == null || tab.trim().isEmpty() || "all".equalsIgnoreCase(tab) || "incoming".equalsIgnoreCase(tab)) {
            results = notificationRepository.findNotificationsForUser(staffUser.getId(), audience, pageable);
        } else if ("booking".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndTypeAndUserId(
                    audience, NotificationType.BOOKING, staffUser.getId(), pageable);
        } else {
            results = notificationRepository.findNotificationsForUser(staffUser.getId(), audience, pageable);
        }

        return results.map(this::mapToDTO);
    }

    @Override
    @Transactional
    public void markAsRead(Long id, String userEmail) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!notification.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("You are not authorized to mark this notification as read.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        log.info("Notification ID: {} successfully marked as read by user: {}", id, userEmail);
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

    @Override
    @Transactional
    public void sendSystemNotification(String title, String message, NotificationType type,
                                       TargetAudience audience, User targetUser, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .type(type)
                .targetAudience(audience)
                .user(targetUser)
                .metadata(metadata)
                .createdAt(Instant.now())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("🚀 Auto-Notification triggered for {} ({})", targetUser.getEmail(), audience);
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
                .metadata(entity.getMetadata())
                .build();
    }
}