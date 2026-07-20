package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.TargetAudience;
import com.codingproject.digitalbase.service.FCMService;
import com.codingproject.digitalbase.service.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final FCMService fcmService;

    // 📢 ၁။ Create Notification
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<NotificationDTO> createNotification(@ModelAttribute NotificationRequest request) {
        NotificationDTO response = notificationService.createNotification(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================================
    // 🖥️ UI အပိုင်း (၁) - SENT NOTIFICATIONS HISTORY PAGE (Admin Direct Sent Only)
    // =========================================================================

    // ၂။ [Admin Dashboard] Get Staff Notifications History
    @GetMapping("/admin/staff")
    public ResponseEntity<Page<NotificationDTO>> getAllStaffNotificationsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationDTO> notifications = notificationService.getAllNotificationsByAudience(TargetAudience.STAFF, pageable);
        return ResponseEntity.ok(notifications);
    }

    // ၃။ [Admin Dashboard] Get Customer Notifications History
    @GetMapping("/admin/customer")
    public ResponseEntity<Page<NotificationDTO>> getAllCustomerNotificationsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationDTO> notifications = notificationService.getAllNotificationsByAudience(TargetAudience.CUSTOMER, pageable);
        return ResponseEntity.ok(notifications);
    }

    // =========================================================================
    // 📥 UI အပိုင်း (၂) - ညာဘက်ခြမ်း BELL INBOX DRAWER (NEW 🌟)
    // =========================================================================

    // 🎯 ၃.၁။ Incoming Customer Tab (Dropdown: ordered, cancel, review & Buttons: today, week, month)
    @GetMapping("/admin/inbox/customer")
    public ResponseEntity<Page<NotificationDTO>> getAdminInboxCustomer(
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(required = false, defaultValue = "all") String timeFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationDTO> notifications = notificationService.getAdminInboxCustomer(tab, timeFilter, pageable);
        return ResponseEntity.ok(notifications);
    }

    // 🎯 ၃.၂။ Incoming Staff Tab (Dropdown: started, completed & Buttons: today, week, month)
    @GetMapping("/admin/inbox/staff")
    public ResponseEntity<Page<NotificationDTO>> getAdminInboxStaff(
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(required = false, defaultValue = "all") String timeFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationDTO> notifications = notificationService.getAdminInboxStaff(tab, timeFilter, pageable);
        return ResponseEntity.ok(notifications);
    }

    // =========================================================================
    // 📱 MOBILE APPS ENDPOINTS & UTILITIES (အဟောင်းအတိုင်း ပြောင်းလဲမှုမရှိပါ)
    // =========================================================================

    // 📱 ၄။ [Customer Mobile App တွက်] Get Personalized Customer Inbox
    @GetMapping("/customer")
    public ResponseEntity<Page<NotificationDTO>> getCustomerNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationDTO> notifications = notificationService.getCustomerNotificationsByTab(userDetails.getUsername(), tab, pageable);
        return ResponseEntity.ok(notifications);
    }

    // 📱 ၅။ [Staff Mobile App တွက်] Get Personalized Staff Inbox
    @GetMapping("/staff")
    public ResponseEntity<Page<NotificationDTO>> getStaffNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationDTO> notifications = notificationService.getStaffNotificationsByTab(userDetails.getUsername(), tab, pageable);
        return ResponseEntity.ok(notifications);
    }

    // 🛡️ ၆။ Mark As Read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Principal principal) {
        notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    // 🗑️ ၇။ Delete Notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // 🚀 FCM Push Notification တိုက်ရိုက် စမ်းသပ်ရန် Endpoint
    @PostMapping("/test-push")
    public ResponseEntity<java.util.Map<String, String>> testDirectPush(@RequestBody java.util.Map<String, String> request) {
        String token = request.get("token");
        String title = request.get("title");
        String body = request.get("body");

        if (token == null || token.isEmpty()) {
            throw new com.codingproject.digitalbase.exception.BadRequestException("FCM Token cannot be null or empty!");
        }

        // FCM Service သို့ တိုက်ရိုက် လှမ်းပို့ခြင်း
        fcmService.sendPushNotification(token, title, body);

        return ResponseEntity.ok(java.util.Map.of("message", "Push notification triggered successfully!"));
    }
}