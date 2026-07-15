package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.TargetAudience;
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

    // 📢 ၁။ Create Notification
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<NotificationDTO> createNotification(@ModelAttribute NotificationRequest request) {
        NotificationDTO response = notificationService.createNotification(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 🖥️ ၂။ [Admin Dashboard တွက်] Get ALL Staff Notifications History (Global View with Pagination)
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

    // 🖥️ ၃။ [Admin Dashboard တွက်] Get ALL Customer Notifications History (Global View with Pagination)
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

    // 📱 ၄။ [Customer Mobile App တွက်] Get Personalized Customer Inbox (🌟 Pagination စနစ်ဖြင့် ပြင်ဆင်ပြီး)
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

    // 📱 ၅။ [Staff Mobile App တွက်] Get Personalized Staff Inbox (🌟 Pagination စနစ်ဖြင့် ပြင်ဆင်ပြီး)
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
}