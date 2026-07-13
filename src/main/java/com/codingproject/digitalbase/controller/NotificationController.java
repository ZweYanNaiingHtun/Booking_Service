package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 🌟 ၁။ Create Notification (သန့်ရှင်းသွားသော ကုဒ်)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<NotificationDTO> createNotification(@ModelAttribute NotificationRequest request) {
        NotificationDTO response = notificationService.createNotification(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 🎯 ၂။ Get Staff Notifications Only
    @GetMapping("/admin/staff")
    public ResponseEntity<List<NotificationDTO>> getStaffNotifications() {
        return ResponseEntity.ok(notificationService.getStaffNotifications());
    }

    // 🎯 ၃။ Get Customer Notifications Only
    @GetMapping("/admin/customer")
    public ResponseEntity<List<NotificationDTO>> getCustomerNotifications() {
        return ResponseEntity.ok(notificationService.getCustomerNotifications());
    }

    @GetMapping("/customer")
    public ResponseEntity<List<NotificationDTO>> getCustomerNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String tab) {

        List<NotificationDTO> notifications = notificationService.getCustomerNotificationsByTab(userDetails.getUsername(), tab);
        return ResponseEntity.ok(notifications);
    }

    // 🌟 Notification တစ်ခုကို နှိပ်လိုက်ရင် အနီစက် ပျောက်သွားစေရန် (Mark as Read)
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    // ၄။ Delete Notification
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}