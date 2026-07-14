package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.NotificationDTO;
import com.codingproject.digitalbase.dtos.NotificationRequest;
import com.codingproject.digitalbase.enums.TargetAudience;
import com.codingproject.digitalbase.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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

    // 🖥️ ၂။ [Admin Dashboard တွက်] Get ALL Staff Notifications History (Global View)
    @GetMapping("/admin/staff")
    public ResponseEntity<List<NotificationDTO>> getAllStaffNotificationsForAdmin() {
        // 🌟 Fix: Email သတ်မှတ်ချက်မလိုဘဲ သမိုင်းကြောင်း Noti အားလုံးကို Dashboard ပေါ်တင်ပေးပါမည်
        List<NotificationDTO> notifications = notificationService.getAllNotificationsByAudience(TargetAudience.STAFF);
        return ResponseEntity.ok(notifications);
    }

    // 🖥️ ၃။ [Admin Dashboard တွက်] Get ALL Customer Notifications History (Global View)
    @GetMapping("/admin/customer")
    public ResponseEntity<List<NotificationDTO>> getAllCustomerNotificationsForAdmin() {
        // 🌟 Fix: Customer တစ်ခုလုံးထံ ပို့ခဲ့သမျှ Noti အားလုံးကို ပြသပါမည်
        List<NotificationDTO> notifications = notificationService.getAllNotificationsByAudience(TargetAudience.CUSTOMER);
        return ResponseEntity.ok(notifications);
    }

    // 📱 ၄။ [Customer Mobile App တွက်] Get Personalized Customer Inbox
    @GetMapping("/customer")
    public ResponseEntity<List<NotificationDTO>> getCustomerNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String tab) {
        List<NotificationDTO> notifications = notificationService.getCustomerNotificationsByTab(userDetails.getUsername(), tab);
        return ResponseEntity.ok(notifications);
    }

    // 📱 ၅။ [Staff Mobile App တွက်] Get Personalized Staff Inbox (🌟 Endpoint အသစ်)
    @GetMapping("/staff")
    public ResponseEntity<List<NotificationDTO>> getStaffNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String tab) {
        // 🌟 Fix: ဝန်ထမ်းတစ်ဦးချင်းစီအလိုက် တာဝန်ကျသော Booking/Alert သီးသန့်များကို စစ်ထုတ်ကြည့်ရန်
        List<NotificationDTO> notifications = notificationService.getStaffNotificationsByTab(userDetails.getUsername(), tab);
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