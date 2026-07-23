package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.BookingResponse;
import com.codingproject.digitalbase.dtos.ChangePasswordRequest;
import com.codingproject.digitalbase.dtos.DeleteAccountRequest;
import com.codingproject.digitalbase.dtos.UserProfileResponse;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.StaffProfile;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.StaffProfileRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;

    // 🌟 🌟 🌟 ၁။ စာမျက်နှာဖွင့်ချိန်တွင် Email ဖြင့် Profile ဒေတာ ရှာဖွေပေးမည့် Method အသစ်
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        // ၁။ အီးမေးလ်ဖြင့် အသုံးပြုသူအား ရှာဖွေခြင်း
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with email: " + email));

        Double rating = null;
        Long completedBookingsCount = null;

        // ၂။ အသုံးပြုသူသည် STAFF Role ဟုတ်မဟုတ် စစ်ဆေးခြင်း
        boolean isStaff = user.getRoles().stream()
                .anyMatch(role -> "STAFF".equals(role.getRole().name()));

        // 🎯 ၃။ အကယ်၍ ဝန်ထမ်းဖြစ်ပါက Rating နှင့် Completed Bookings Count အား ရှာဖွေတွက်ချက်ခြင်း
        if (isStaff) {
            StaffProfile staffProfile = this.staffProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user email: " + email));

            // DB ထဲရှိ လက်ရှိ Staff ၏ Rating အား ရယူခြင်း
            rating = staffProfile.getRating();

            // 💡 Booking Table ထဲမှ အဆိုပါ ဝန်ထမ်း တာဝန်ယူပြီး COMPLETED ဖြစ်သွားသော အရေအတွက်အား Count လုပ်ခြင်း
            // (ဆရာကြီးတို့၏ Booking Status Enum အမျိုးအစားပေါ်မူတည်၍ လိုအပ်သလို ပြောင်းလဲနိုင်ပါသည်)
            completedBookingsCount = this.bookingRepository.countCompletedBookingsByStaffId(staffProfile.getId());
        }
        // ၄။ DTO ထဲသို့ ဒေတာများ ထည့်သွင်းကာ ပြန်လည်ပေးပို့ခြင်း
        return this.mapToProfileResponse(user, rating, completedBookingsCount);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfilePhoto(String email, MultipartFile profileImage) {
        User user = this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User profile not found with email: " + email));
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
                return this.mapToProfileResponse(updatedUser , null , null);
            } catch (IOException e) {
                throw new BadRequestException("Failed to store updated profile image: " + e.getMessage());
            }
        } else {
            throw new BadRequestException("Uploaded profile image is empty or invalid");
        }
    }

    @Override
    @Transactional
    public UserProfileResponse changePassword(String email, ChangePasswordRequest request) {
        User user = this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        if (!this.passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match!");
        } else if (this.passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as your current password!");
        } else {
            user.setPassword(this.passwordEncoder.encode(request.getNewPassword()));
            User updatedUser = this.userRepository.save(user);
            return this.mapToProfileResponse(updatedUser , null , null);
        }
    }

    @Override
    @Transactional
    public void updatePhoneDirect(String email, String newPhone) {
        // ၁။ လက်ရှိ Login ဝင်ထားသော အသုံးပြုသူကို ရှာဖွေပါတယ်
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ၂။ ပြောင်းလဲမည့် ဖုန်းနံပါတ်အသစ်သည် အခြားသူတစ်ယောက်ယောက် သုံးထားပြီးသား ဖြစ်နေလား စစ်ဆေးခြင်း (Duplicate Check)
        if (userRepository.existsByPhone(newPhone)) {
            if (!newPhone.equals(user.getPhone())) {
                throw new BadRequestException("This phone number is already registered by another user");
            }
        }

        // ၃။ ဖုန်းနံပါတ်အသစ်ကို တိုက်ရိုက် သတ်မှတ်ပြီး သိမ်းဆည်းလိုက်ပါသည်
        user.setPhone(newPhone);
        userRepository.save(user);

        log.info("📱 Successfully updated phone number directly for user: {}", email);
    }

    // 🌟 🌟 🌟 ၃။ UI ဒီဇိုင်းနှင့် ကိုက်ညီအောင် Role ပါ တစ်ပါတည်း Map လုပ်ပေးခြင်း
    // 🌟 ပြင်ဆင်လိုက်သည့် မက်သတ်: Parameter ၃ ခု (user, rating, completedBookingsCount) လက်ခံရန် ပြောင်းလဲထားပါသည်
    private UserProfileResponse mapToProfileResponse(User user, Double rating, Long completedBookingsCount) {
        String relativeImagePath = null;

        if (user.getProfilePicture() != null) {
            // 🎯 ဆရာကြီး လိုချင်တဲ့အတိုင်း Host တွေ Port တွေမပါဘဲ Relative Path သန့်သန့်လေးပဲ ပေါင်းစပ်ခြင်း
            relativeImagePath = "/uploads/profile-pictures/" + user.getProfilePicture();
        } else {
            // ပုံမရှိရင်လည်း Relative Path အတိုင်းပဲ Default ပြန်ပေးခြင်း
            relativeImagePath = "/uploads/profile-pictures/default-profile.png";
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .profilePicture(relativeImagePath)
                .role(user.getRoles() != null && !user.getRoles().isEmpty() ?
                        user.getRoles().stream()
                                .findFirst()
                                .map(r -> r.getRole().name())
                                .orElse(null) : null)
                .rating(rating)                         // 🌟 ယခုအခါ ဝင်လာသော Parameter ကြောင့် စုတ်ယူနိုင်သွားပါပြီ
                .completedBookingsCount(completedBookingsCount) // 🌟 ယခုအခါ ဝင်လာသော Parameter ကြောင့် စုတ်ယူနိုင်သွားပါပြီ
                .build();
    }
    // 🔓 1. Admin မှ User ကို Unblock ပြုလုပ်ခြင်း (အကောင့် ပြန်ဖွင့်ပေးခြင်း)
    @Override
    @Transactional
    public void unblockUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.isEnabled()) {
            throw new BadRequestException("User account is already active and unblocked.");
        }

        user.setEnabled(true); // 🎯 User status ကို ပြန်လည် Active (true) လုပ်ပေးခြင်း
        userRepository.save(user);

        log.info("🔓 Admin successfully UNBLOCKED User ID: {} ({})", userId, user.getEmail());
    }

    @Override
    @Transactional
    public void deactivateMyAccount(String email, DeleteAccountRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // 🔒 ၁။ ရိုက်ထည့်လိုက်သော Password မှန်/မမှန် တိုက်စစ်ခြင်း
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect password. Please enter your correct password to delete account.");
        }

        // 🚫 ၂။ Password မှန်ပါက Account Status အား Disable ပြုလုပ်ခြင်း
        user.setEnabled(false);
        userRepository.save(user);

        log.info("🚫 User {} has successfully deactivated their own account after password verification.", email);
    }

    // 🚫 2. Admin မှ User ကို Block ပြုလုပ်ခြင်း
    @Override
    @Transactional
    public void blockUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!user.isEnabled()) {
            throw new BadRequestException("User account is already blocked.");
        }

        user.setEnabled(false); // 🎯 User status ကို Disable (false) ပြုလုပ်ခြင်း
        userRepository.save(user);

        log.info("🚫 Admin successfully BLOCKED User ID: {} ({})", userId, user.getEmail());
    }
}