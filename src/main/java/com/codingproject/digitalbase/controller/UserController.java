package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/users"})
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    // 🔄 အဆင့် (၁) - Personal Information စာမျက်နှာ ဖွင့်လိုက်လျှင် ဒေတာအားလုံး (Name, Phone, Gmail, Gender) ကို ဆွဲထုတ်ပြမည့် Endpoint
    @GetMapping({"/profile"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(this.userService.getMyProfile(userDetails.getUsername()));
    }
    // 📸 အဆင့် (၂) - "Save" ခလုတ်နှိပ်လျှင် Profile Photo တစ်ခုတည်းကိုသာ သီးသန့် Update လုပ်မည့် Endpoint
    @PutMapping(
            value = {"/profile/photo"},
            consumes = {"multipart/form-data"}
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> updateProfilePhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("profileImage") MultipartFile profileImage) {

        // ပုံကို အောင်မြင်စွာ လဲလှယ်ပြီးနောက် UI ထဲတွင် ဒေတာများ ပြန်လည် Refresh ဖြစ်စေရန် UserProfileResponse ကို ပြန်ပေးပါသည်
        UserProfileResponse response = this.userService.updateProfilePhoto(userDetails.getUsername(), profileImage);
        return ResponseEntity.ok(response);
    }

    // ၄။ Security Tab တွင် Password ချိန်းရန် သုံးနိုင်သည့် Endpoint
    @PutMapping({"/profile/change-password"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> changePassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody @Valid ChangePasswordRequest request) {
        UserProfileResponse response = this.userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile/change-phone-direct")
    public ResponseEntity<?> changePhoneDirect(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid ChangePhoneRequest request) {
        // Security Context ထဲမှ လက်ရှိ User ၏ Email ကို ယူပါသည်
        String currentUsersEmail = userDetails.getUsername();

        // Service လှမ်းခေါ်ပြီး ဖုန်းနံပါတ် တိုက်ရိုက် အပ်ဒိတ်လုပ်ခြင်း
        this.userService.updatePhoneDirect(currentUsersEmail, request.getNewPhone());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Phone number updated successfully"
        ));
    }
}