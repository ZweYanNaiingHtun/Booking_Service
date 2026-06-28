//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ChangePasswordRequest;
import com.codingproject.digitalbase.dtos.UpdateProfileRequest;
import com.codingproject.digitalbase.dtos.UserProfileResponse;
import com.codingproject.digitalbase.dtos.VerifyPhoneUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserProfileResponse updateMyProfile(UpdateProfileRequest request);

    void sendPhoneUpdateOtp();

    void verifyAndUpdatePhone(VerifyPhoneUpdateRequest request);

    UserProfileResponse updateProfilePhoto(String email, MultipartFile profileImage);

    UserProfileResponse changePassword(String email, ChangePasswordRequest request);
}
