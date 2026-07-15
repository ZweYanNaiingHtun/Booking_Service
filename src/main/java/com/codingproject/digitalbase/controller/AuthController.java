//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.repository.UserRepository;
import com.codingproject.digitalbase.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/auth"})
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final AuthService authService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @GetMapping({"/test-oauth-config"})
    public String testOauthConfig() {
        return this.clientRegistrationRepository == null ? "❌ Spring Boot က OAuth2 Properties တွေကို မဖတ်မိပါဘူး သို့မဟုတ် Dependency လိုနေပါတယ်!" : "✅ OAuth2 Configuration အောင်မြင်စွာ အလုပ်လုပ်နေပါတယ်!";
    }

    @PostMapping({"/signup/init"})
    public ResponseEntity<?> signupInit(@RequestBody @Valid SignupInitRequest request) {
        this.authService.signupInit(request);
        return ResponseEntity.ok("Verification OTP sent to your email.");
    }

    @PostMapping(value = {"/signup/complete-profile"},consumes = {"multipart/form-data"}
    )
    public ResponseEntity<?> completeProfile(@ModelAttribute @Valid CompleteProfileRequest request, Principal principal) {
        this.authService.completeProfile(request, principal.getName());
        return ResponseEntity.ok("Profile created successfully! Sign In Successful.");
    }

    @PostMapping({"/login"})
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        LoginResponse loginResponse = this.authService.login(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping({"/firebase-login"})
    public ResponseEntity<?> verifyFirebaseToken(@RequestHeader("Authorization") String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String idToken = bearerToken.substring(7);
            TokenPair tokenPair = this.authService.firebaseLogin(idToken);
            return ResponseEntity.ok(tokenPair);
        } else {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }
    }

    @PostMapping({"/verify-otp"})
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(this.authService.verifyOtp(request));
    }

    @PostMapping({"/forgot-password"})
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        this.authService.forgotPassword(request);
        return ResponseEntity.ok("Password reset email sent");
    }

    @PostMapping({"/reset-password"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request, Principal principal) {
        this.authService.resetPassword(request, principal.getName());
        return ResponseEntity.ok("Password reset successful");
    }

    @PostMapping({"/refresh"})
    public ResponseEntity<?> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        TokenPair tokenPair = this.authService.refreshToken(request);
        return ResponseEntity.ok(tokenPair);
    }

    @PutMapping({"/{userId}/fcm-token"})
    public ResponseEntity<?> updateFcmToken(@PathVariable Long userId, @RequestParam String token) {
        return this.userRepository.findById(userId).map((user) -> {
            user.setFcmToken(token);
            this.userRepository.save(user);
            return ResponseEntity.ok("FCM Token updated successfully for user ID: " + userId);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = {"/{userId}/upload-image"}, consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadProfilePicture(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            String fileName = this.authService.uploadProfilePicture(userId, file);
            return ResponseEntity.ok("Image uploaded successfully! File Name: " + fileName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to upload image: " + e.getMessage());
        }
    }
}
