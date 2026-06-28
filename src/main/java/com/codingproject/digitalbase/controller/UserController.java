//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.ChangePasswordRequest;
import com.codingproject.digitalbase.dtos.UpdateProfileRequest;
import com.codingproject.digitalbase.dtos.UserProfileResponse;
import com.codingproject.digitalbase.dtos.VerifyPhoneUpdateRequest;
import com.codingproject.digitalbase.service.UserService;
import jakarta.validation.Valid;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/users"})
public class UserController {
    private final UserService userService;

    @PutMapping(
            value = {"/profile"},
            consumes = {"multipart/form-data"}
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> updateProfile(@ModelAttribute @Valid UpdateProfileRequest request) {
        return ResponseEntity.ok(this.userService.updateMyProfile(request));
    }

    @PatchMapping(
            value = {"/profile/photo"},
            consumes = {"multipart/form-data"}
    )
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> updateProfilePhoto(@AuthenticationPrincipal UserDetails userDetails, @RequestParam("profileImage") MultipartFile profileImage) {
        UserProfileResponse response = this.userService.updateProfilePhoto(userDetails.getUsername(), profileImage);
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/profile/change-password"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<UserProfileResponse> changePassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody @Valid ChangePasswordRequest request) {
        UserProfileResponse response = this.userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/profile/phone/request-otp"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<String> requestPhoneUpdateOtp() {
        this.userService.sendPhoneUpdateOtp();
        return ResponseEntity.ok("OTP code has been successfully sent to your registered email.");
    }

    @PutMapping({"/profile/phone/verify-update"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<String> verifyAndUpdatePhone(@RequestBody @Valid VerifyPhoneUpdateRequest request) {
        this.userService.verifyAndUpdatePhone(request);
        return ResponseEntity.ok("Phone number has been updated successfully.");
    }

    @Generated
    public UserController(final UserService userService) {
        this.userService = userService;
    }
}
