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
    private final UserRepository userRepository;
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

    // 🖥️ [Admin Panel သီးသန့်] Target Audience အလိုက် သမိုင်းကြောင်းအားလုံးကို စစ်ထုတ်ခြင်းမရှိဘဲ ဆွဲထုတ်ပြမည့် Method
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getAllNotificationsByAudience(TargetAudience audience) {
        log.info("Admin UI: Fetching all global notification history for audience: {}", audience);
        List<Notification> notifications = notificationRepository.findByTargetAudienceOrderByCreatedAtDesc(audience);
        return notifications.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // 📱 [Customer Mobile App တွက်] Tab အလိုက် ကိုယ်ပိုင်သီးသန့် Inbox Filter မက်သတ်
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getCustomerNotificationsByTab(String email, String tab) {
        User customerUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));

        TargetAudience audience = TargetAudience.CUSTOMER;
        List<Notification> results;

        if (tab == null || tab.trim().isEmpty() || "all".equalsIgnoreCase(tab)) {
            results = notificationRepository.findNotificationsForUser(customerUser.getId(), audience);
        } else if ("booking".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndTypeAndUserIdOrderByCreatedAtDesc(
                    audience, NotificationType.BOOKING, customerUser.getId());
        } else if ("promotion".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndTypeOrderByCreatedAtDesc(audience, NotificationType.PROMOTION);
        } else {
            results = notificationRepository.findNotificationsForUser(customerUser.getId(), audience);
        }

        return results.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // 📱 [Staff Mobile App တွက်] Tab အလိုက် ကိုယ်ပိုင်သီးသန့် Inbox Filter မက်သတ် (🌟 ဖြည့်စွက်ချက်အသစ်)
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getStaffNotificationsByTab(String email, String tab) {
        User staffUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found."));

        TargetAudience audience = TargetAudience.STAFF;
        List<Notification> results;

        if (tab == null || tab.trim().isEmpty() || "all".equalsIgnoreCase(tab) || "incoming".equalsIgnoreCase(tab)) {
            results = notificationRepository.findNotificationsForUser(staffUser.getId(), audience);
        } else if ("booking".equalsIgnoreCase(tab)) {
            results = notificationRepository.findByTargetAudienceAndTypeAndUserIdOrderByCreatedAtDesc(
                    audience, NotificationType.BOOKING, staffUser.getId());
        } else {
            results = notificationRepository.findNotificationsForUser(staffUser.getId(), audience);
        }

        return results.stream().map(this::mapToDTO).collect(Collectors.toList());
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