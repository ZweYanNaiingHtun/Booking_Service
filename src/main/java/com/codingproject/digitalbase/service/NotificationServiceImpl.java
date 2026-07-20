package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.NotificationType;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.enums.CustomerAction;
import com.codingproject.digitalbase.enums.TargetAudience;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Notification;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.NotificationRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    // ==========================================
    // 🖥️ UI အပိုင်း (၁) - SENT NOTIFICATIONS HISTORY PAGE
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getAllNotificationsByAudience(TargetAudience audience, Pageable pageable) {
        log.info("Admin UI: Fetching paged global notification history for audience: {}", audience);
        // 🎯 ပြင်ဆင်ချက်: System Events များမပါဘဲ Admin ကိုယ်တိုင် ပို့ထားသည့် Global Broadcasts သက်သက်သာ ဆွဲထုတ်ခြင်း
        Page<Notification> notificationPage = notificationRepository.findByTypeIsNotNullAndTargetAudienceAndUserIsNull(audience, pageable);
        return notificationPage.map(this::mapToDTO);
    }

    // ==========================================
    // 📥 UI အပိုင်း (၂) - ညာဘက်ခြမ်း BELL INBOX DRAWER (NEW 🌟)
    // ==========================================

    // 🎯 Incoming Customer Tab: Ordered (PENDING), Cancel (CANCELLED), Review (REVIEW)
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getAdminInboxCustomer(String tab, String timeFilter, Pageable pageable) {
        Instant startDate = calculateStartDate(timeFilter);
        String cleanTab = (tab == null) ? "all" : tab.trim().toLowerCase();

        log.info("Admin Inbox: Fetching Customer events with filter tab: {}, since: {}", cleanTab, startDate);
        return notificationRepository.findAdminCustomerInbox(cleanTab, startDate, pageable)
                .map(this::mapToDTO);
    }

    // 🎯 Incoming Staff Tab: Started (IN_PROGRESS), Completed (COMPLETED)
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getAdminInboxStaff(String tab, String timeFilter, Pageable pageable) {
        Instant startDate = calculateStartDate(timeFilter);
        String cleanTab = (tab == null) ? "all" : tab.trim().toLowerCase();

        log.info("Admin Inbox: Fetching Staff events with filter tab: {}, since: {}", cleanTab, startDate);
        return notificationRepository.findAdminStaffInbox(cleanTab, startDate, pageable)
                .map(this::mapToDTO);
    }

    // 💡 Helper Method: Today, This Week, This Month ခလုတ်များအတွက် Date တွက်ချက်ရန်
    private Instant calculateStartDate(String timeFilter) {
        if ("today".equalsIgnoreCase(timeFilter)) {
            return Instant.now().minus(1, ChronoUnit.DAYS);
        } else if ("week".equalsIgnoreCase(timeFilter)) {
            return Instant.now().minus(7, ChronoUnit.DAYS);
        } else if ("month".equalsIgnoreCase(timeFilter)) {
            return Instant.now().minus(30, ChronoUnit.DAYS);
        }
        return Instant.EPOCH; // "all" ဖြစ်ပါက အစကတည်းက ပြရန်
    }

    // ==========================================
    // 📱 MOBILE APPS & SYSTEM AUTO TRIGGERS
    // ==========================================

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
    @Async("notificationExecutor")
    @Transactional
    public void saveSystemNotification(String title, String message, NotificationType type,
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

    // 🎯 System Auto Event: Customer Action ဖြစ်ရပ်များ သီးသန့်လာသိမ်းရန် (Ordered, Cancel, Review)
    @Transactional
    public void saveCustomerEventNotification(String title, String message, CustomerAction action, BookingStatus status, User targetUser, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .title(title).message(message)
                .customerAction(action).bookingStatus(status)
                .targetAudience(TargetAudience.CUSTOMER).user(targetUser)
                .metadata(metadata).createdAt(Instant.now()).isRead(false).build();
        notificationRepository.save(notification);
    }

    // 🎯 System Auto Event: Staff Action ဖြစ်ရပ်များ သီးသန့်လာသိမ်းရန် (IN_PROGRESS, COMPLETED)
    @Transactional
    public void saveStaffEventNotification(String title, String message, BookingStatus status, User targetUser, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .title(title).message(message)
                .bookingStatus(status)
                .targetAudience(TargetAudience.STAFF).user(targetUser)
                .metadata(metadata).createdAt(Instant.now()).isRead(false).build();
        notificationRepository.save(notification);
    }

    private NotificationDTO mapToDTO(Notification entity) {
        return NotificationDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .imageUrl(entity.getImageUrl())
                .type(entity.getType())
                .targetAudience(entity.getTargetAudience())
                .bookingStatus(entity.getBookingStatus())   // 🌟 Added mapping
                .customerAction(entity.getCustomerAction()) // 🌟 Added mapping
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .metadata(entity.getMetadata())
                .build();
    }
}