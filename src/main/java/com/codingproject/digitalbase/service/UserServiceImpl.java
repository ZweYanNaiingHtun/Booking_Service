//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ChangePasswordRequest;
import com.codingproject.digitalbase.dtos.UpdateProfileRequest;
import com.codingproject.digitalbase.dtos.UserProfileResponse;
import com.codingproject.digitalbase.dtos.VerifyPhoneUpdateRequest;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.UserRepository;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Generated;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User)this.userRepository.findByEmail(currentEmail).orElseThrow(() -> new ResourceNotFoundException("Logged in user profile not found"));
        user.setFullName(request.getFullName());
        user.setGender(request.getGender());
        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            try {
                Path uploadPath = Paths.get("uploads/profile-pictures/");
                if (!Files.exists(uploadPath, new LinkOption[0])) {
                    Files.createDirectories(uploadPath);
                }

                if (user.getProfilePicture() != null && !user.getProfilePicture().equals("default-profile.png")) {
                    Path oldFilePath = uploadPath.resolve(user.getProfilePicture());
                    Files.deleteIfExists(oldFilePath);
                }

                String var10000 = UUID.randomUUID().toString();
                String newFileName = var10000 + "_" + request.getProfileImage().getOriginalFilename();
                Path filePath = uploadPath.resolve(newFileName);
                Files.copy(request.getProfileImage().getInputStream(), filePath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                user.setProfilePicture(newFileName);
            } catch (IOException e) {
                throw new BadRequestException("Failed to update profile image: " + e.getMessage());
            }
        }

        User updatedUser = (User)this.userRepository.save(user);
        return this.mapToProfileResponse(updatedUser);
    }

    @Transactional
    public UserProfileResponse updateProfilePhoto(String email, MultipartFile profileImage) {
        User user = (User)this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User profile not found with email: " + email));
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                Path uploadPath = Paths.get("uploads/profile-pictures/");
                if (!Files.exists(uploadPath, new LinkOption[0])) {
                    Files.createDirectories(uploadPath);
                }

                if (user.getProfilePicture() != null && !user.getProfilePicture().equals("default-profile.png")) {
                    Path oldFilePath = uploadPath.resolve(user.getProfilePicture());
                    Files.deleteIfExists(oldFilePath);
                }

                String var10000 = UUID.randomUUID().toString();
                String newFileName = var10000 + "_" + profileImage.getOriginalFilename();
                Path filePath = uploadPath.resolve(newFileName);
                Files.copy(profileImage.getInputStream(), filePath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                user.setProfilePicture(newFileName);
                User updatedUser = (User)this.userRepository.save(user);
                return this.mapToProfileResponse(updatedUser);
            } catch (IOException e) {
                throw new BadRequestException("Failed to store updated profile image: " + e.getMessage());
            }
        } else {
            throw new BadRequestException("Uploaded profile image is empty or invalid");
        }
    }

    @Transactional
    public UserProfileResponse changePassword(String email, ChangePasswordRequest request) {
        User user = (User)this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        if (!this.passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match!");
        } else if (this.passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as your current password!");
        } else {
            user.setPassword(this.passwordEncoder.encode(request.getNewPassword()));
            User updatedUser = (User)this.userRepository.save(user);
            return this.mapToProfileResponse(updatedUser);
        }
    }

    @Transactional
    public void sendPhoneUpdateOtp() {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User)this.userRepository.findByEmail(currentEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SecureRandom random = new SecureRandom();
        String otp = String.valueOf(100000 + random.nextInt(900000));
        user.setOtp(otp);
        user.setOtpGeneratedTime(Instant.now());
        this.userRepository.save(user);
        this.emailService.sendOtpEmail(user.getEmail(), otp);
    }

    @Transactional
    public void verifyAndUpdatePhone(VerifyPhoneUpdateRequest request) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User)this.userRepository.findByEmail(currentEmail).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getOtp() == null) {
            throw new BadRequestException("OTP not found or already used");
        } else if (!user.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP code");
        } else if (user.getOtpGeneratedTime().plus(5L, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            throw new BadRequestException("OTP code has expired");
        } else {
            user.setPhone(request.getNewPhoneNumber());
            user.setOtp((String)null);
            user.setOtpGeneratedTime((Instant)null);
            this.userRepository.save(user);
        }
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder().id(user.getId()).fullName(user.getFullName()).email(user.getEmail()).phone(user.getPhone()).gender(user.getGender()).profilePicture(user.getProfilePicture()).build();
    }

    @Generated
    public UserServiceImpl(final UserRepository userRepository, final EmailService emailService, final PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }
}
