//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.RoleName;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    void signupInit(SignupInitRequest request);

    void completeProfile(CompleteProfileRequest request, String email);

    TokenPair verifyOtp(VerifyOtpRequest request);

    LoginResponse login(LoginRequest request);

    TokenPair firebaseLogin(String idToken);

    void updateFcmToken(String email, FcmTokenRequest request);

    TokenPair refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request, String email);

    String uploadProfilePicture(Long userId, MultipartFile file);

    String generateUserCode(RoleName roleName);
}
