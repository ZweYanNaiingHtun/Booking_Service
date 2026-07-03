package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ChangePasswordRequest;
import com.codingproject.digitalbase.dtos.UpdateProfileRequest;
import com.codingproject.digitalbase.dtos.UserProfileResponse;
import com.codingproject.digitalbase.dtos.VerifyPhoneUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    // 🌟 စာမျက်နှာဖွင့်ဖွင့်ချင်း Profile ဒေတာဆွဲထုတ်ရန် ထပ်တိုးလိုက်သည့် Method
    UserProfileResponse getMyProfile(String email);

    UserProfileResponse updateProfilePhoto(String email, MultipartFile profileImage);

    UserProfileResponse changePassword(String email, ChangePasswordRequest request);

    void updatePhoneDirect(String email, String newPhone);
}