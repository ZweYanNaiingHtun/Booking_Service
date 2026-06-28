//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.CompleteProfileRequest;
import com.codingproject.digitalbase.dtos.ForgotPasswordRequest;
import com.codingproject.digitalbase.dtos.LoginRequest;
import com.codingproject.digitalbase.dtos.LoginResponse;
import com.codingproject.digitalbase.dtos.ResetPasswordRequest;
import com.codingproject.digitalbase.dtos.SignupInitRequest;
import com.codingproject.digitalbase.dtos.TokenPair;
import com.codingproject.digitalbase.dtos.VerifyOtpRequest;
import com.codingproject.digitalbase.enums.RoleName;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    void signupInit(SignupInitRequest request);

    void completeProfile(CompleteProfileRequest request, String email);

    TokenPair verifyOtp(VerifyOtpRequest request);

    LoginResponse login(LoginRequest request);

    TokenPair firebaseLogin(String idToken);

    TokenPair refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request, String email);

    String uploadProfilePicture(Long userId, MultipartFile file);

    String generateUserCode(RoleName roleName);
}
