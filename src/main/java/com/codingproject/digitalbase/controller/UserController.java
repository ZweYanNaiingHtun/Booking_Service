package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.ChangePasswordRequest;
import com.codingproject.digitalbase.dtos.UpdateProfileRequest;
import com.codingproject.digitalbase.dtos.UserProfileResponse;
import com.codingproject.digitalbase.dtos.VerifyPhoneUpdateRequest;
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

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/users"})
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    // 🌟 🌟 🌟 ၁။ Settings Page ဖွင့်လျှင် လက်ရှိ Login ဝင်ထားသူ (Admin/User) ၏ Profile အချက်အလက်ကို ဆွဲထုတ်မည့် Endpoint
    @GetMapping({"/profile"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        // userDetails.getUsername() မှတစ်ဆင့် လက်ရှိ Login ဝင်ထားသော email သို့မဟုတ် username ဖြင့် ဆွဲထုတ်ပါမည်
        return ResponseEntity.ok(this.userService.getMyProfile(userDetails.getUsername()));
    }

    // 🌟 ၂။ Settings Page ရှိ "Save Changes" ခလုတ်နှိပ်လျှင် Form Data (စာသား + ပုံ) ကို တစ်ခါတည်း Update လုပ်မည့် Endpoint
    @PutMapping(
            value = {"/profile"},
            consumes = {"multipart/form-data"}
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> updateProfile(@ModelAttribute @Valid UpdateProfileRequest request) {
        return ResponseEntity.ok(this.userService.updateMyProfile(request));
    }

    // ၃။ ပုံတစ်မျိုးတည်း သီးသန့် ချိန်းချင်လျှင် သုံးနိုင်သည့် Endpoint
    @PatchMapping(
            value = {"/profile/photo"},
            consumes = {"multipart/form-data"}
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> updateProfilePhoto(@AuthenticationPrincipal UserDetails userDetails, @RequestParam("profileImage") MultipartFile profileImage) {
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

    // ၅။ Phone Number အသစ်ပြင်ရန် OTP တောင်းဆိုသည့် Endpoint
    @PostMapping({"/profile/phone/request-otp"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> requestPhoneUpdateOtp() {
        this.userService.sendPhoneUpdateOtp();
        return ResponseEntity.ok("OTP code has been successfully sent to your registered email.");
    }

    // ၆။ OTP ကုဒ်မှန်ကန်ပါက ဖုန်းနံပါတ်ကို တရားဝင် ပြောင်းလဲပေးမည့် Endpoint
    @PutMapping({"/profile/phone/verify-update"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> verifyAndUpdatePhone(@RequestBody @Valid VerifyPhoneUpdateRequest request) {
        this.userService.verifyAndUpdatePhone(request);
        return ResponseEntity.ok("Phone number has been updated successfully.");
    }
}