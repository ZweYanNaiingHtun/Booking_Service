package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.StaffCreateRequest;
import com.codingproject.digitalbase.dtos.StaffResponse;
import com.codingproject.digitalbase.dtos.StaffUpdateRequest;
import com.codingproject.digitalbase.enums.RoleName;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.BusinessService;
import com.codingproject.digitalbase.model.Role;
import com.codingproject.digitalbase.model.StaffProfile;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.BusinessServiceRepository;
import com.codingproject.digitalbase.repository.RoleRepository;
import com.codingproject.digitalbase.repository.StaffProfileRepository;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffManagementServiceImpl implements StaffManagementService {

    private static final Logger log = LoggerFactory.getLogger(StaffManagementServiceImpl.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessServiceRepository serviceRepository;
    private final EmailService emailService;
    private final StaffProfileRepository staffProfileRepository;

    private String generateUserCode(RoleName roleName) {
        if (roleName == RoleName.SUPER_ADMIN) {
            return "ADMIN";
        } else {
            String prefix = roleName == RoleName.CUSTOMER ? "CU-" : "ST-";
            String maxCode = this.userRepository.findMaxCodeByPrefix(prefix);
            if (maxCode == null) {
                return prefix + "001";
            } else {
                try {
                    String numericPart = maxCode.substring(prefix.length());
                    int nextNumber = Integer.parseInt(numericPart) + 1;
                    return prefix + String.format("%03d", nextNumber);
                } catch (Exception var6) {
                    return prefix + "001";
                }
            }
        }
    }

    @Transactional
    public StaffResponse createStaffUser(StaffCreateRequest request) {
        if (this.userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        } else if (this.userRepository.existsByPhone(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number is already registered");
        } else {
            Role staffRole = (Role)this.roleRepository.findByRole(RoleName.STAFF).orElseThrow(() -> new ResourceNotFoundException("STAFF role not found!"));
            String temporaryPassword = this.generateStrongPassword();
            String staffCode = this.generateUserCode(RoleName.STAFF);
            String imageFileName = "default-profile.png";
            User staff = User.builder().fullName(request.getFullName()).code(staffCode).email(request.getEmail()).password(this.passwordEncoder.encode(temporaryPassword)).phone(request.getPhoneNumber()).dateOfBirth(request.getDateOfBirth()).profilePicture(imageFileName).roles(new HashSet(Collections.singleton(staffRole))).enabled(true).createdAt(Instant.now()).build();
            List<BusinessService> services = Collections.emptyList();
            if (request.getSpecializedServiceIds() != null && !request.getSpecializedServiceIds().isEmpty()) {
                services = this.serviceRepository.findAllById(request.getSpecializedServiceIds());
            }

            StaffProfile profile = StaffProfile.builder().user(staff).isAvailable(true).specializedServices(new HashSet(services)).rating((double)0.0F).joinedAt(Instant.now()).build();
            staff.setStaffProfile(profile);
            User savedStaff = (User)this.userRepository.save(staff);

            try {
                this.emailService.sendStaffWelcomeEmail(savedStaff.getEmail(), temporaryPassword);
                log.info("🚀 Temporary password successfully sent to staff email: {}", savedStaff.getEmail());
            } catch (Exception e) {
                log.error("❌ Failed to send credential email to {}. Error: {}", savedStaff.getEmail(), e.getMessage());
            }

            return this.mapToStaffResponse(savedStaff);
        }
    }

    @Transactional
    public StaffResponse linkStaffWithServices(Long staffId, List<Long> serviceIds) {
        User staff = (User)this.userRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + staffId));
        boolean isStaff = staff.getRoles().stream().anyMatch((r) -> r.getRole() == RoleName.STAFF);
        if (!isStaff) {
            throw new BadRequestException("Selected user is not a staff member");
        } else {
            List<BusinessService> services = serviceIds != null && !serviceIds.isEmpty() ? this.serviceRepository.findAllById(serviceIds) : Collections.emptyList();
            if (staff.getStaffProfile() != null) {
                staff.getStaffProfile().setSpecializedServices(new HashSet(services));
            } else {
                StaffProfile profile = StaffProfile.builder().user(staff).isAvailable(true).specializedServices(new HashSet(services)).rating((double)0.0F).joinedAt(Instant.now()).build();
                staff.setStaffProfile(profile);
            }

            User updatedStaff = (User)this.userRepository.save(staff);
            log.info("🎯 Successfully updated specialized services for staff code: {}", updatedStaff.getCode());
            return this.mapToStaffResponse(updatedStaff);
        }
    }

    private String generateStrongPassword() {
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialCharacters = "!@#$%^&*()-_=+[{]};:',<.>/?";
        String combinedChars = upperCaseLetters + lowerCaseLetters + numbers + specialCharacters;
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
        password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialCharacters.charAt(random.nextInt(specialCharacters.length())));

        for(int i = 4; i < 12; ++i) {
            password.append(combinedChars.charAt(random.nextInt(combinedChars.length())));
        }

        List<Character> charList = new ArrayList();
        for(char c : password.toString().toCharArray()) {
            charList.add(c);
        }

        Collections.shuffle(charList);
        StringBuilder shuffledPassword = new StringBuilder();
        for(char c : charList) {
            shuffledPassword.append(c);
        }

        return shuffledPassword.toString();
    }

    public List<StaffResponse> getAllStaffs() {
        return this.userRepository.findAll().stream()
                .filter((user) -> user.getRoles().stream().anyMatch((role) -> role.getRole() == RoleName.STAFF))
                .map(this::mapToStaffResponse)
                .toList();
    }

    @Transactional
    public StaffResponse updateStaffUser(Long staffId, StaffUpdateRequest request) {
        User staff = (User)this.userRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + staffId));
        boolean isStaff = staff.getRoles().stream().anyMatch((r) -> r.getRole() == RoleName.STAFF);
        if (!isStaff) {
            throw new BadRequestException("Selected user is not a staff member");
        } else {
            this.userRepository.findByEmail(request.getEmail()).ifPresent((existingUser) -> {
                if (!existingUser.getId().equals(staffId)) {
                    throw new BadRequestException("Email is already registered by another user");
                }
            });
            this.userRepository.findByPhone(request.getPhoneNumber()).ifPresent((existingUser) -> {
                if (!existingUser.getId().equals(staffId)) {
                    throw new BadRequestException("Phone number is already registered by another user");
                }
            });
            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                try {
                    Path uploadPath = Paths.get("uploads/profile-pictures/");
                    if (!Files.exists(uploadPath, new LinkOption[0])) {
                        Files.createDirectories(uploadPath);
                    }

                    String var10000 = UUID.randomUUID().toString();
                    String imageFileName = var10000 + "_" + request.getProfileImage().getOriginalFilename();
                    Path filePath = uploadPath.resolve(imageFileName);
                    Files.copy(request.getProfileImage().getInputStream(), filePath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                    staff.setProfilePicture(imageFileName);
                } catch (IOException e) {
                    throw new BadRequestException("Failed to store updated staff profile image: " + e.getMessage());
                }
            }

            staff.setFullName(request.getFullName());
            staff.setEmail(request.getEmail());
            staff.setPhone(request.getPhoneNumber());
            staff.setGender(request.getGender());
            List<BusinessService> services;
            if (request.getSpecializedServiceIds() != null && !request.getSpecializedServiceIds().isEmpty()) {
                services = this.serviceRepository.findAllById(request.getSpecializedServiceIds());
            } else {
                services = this.serviceRepository.findAll();
            }

            if (staff.getStaffProfile() != null) {
                staff.getStaffProfile().setSpecializedServices(new HashSet(services));
            } else {
                StaffProfile profile = StaffProfile.builder().user(staff).isAvailable(true).specializedServices(new HashSet(services)).rating((double)0.0F).joinedAt(Instant.now()).build();
                staff.setStaffProfile(profile);
            }

            return this.mapToStaffResponse((User)this.userRepository.save(staff));
        }
    }

    @Transactional
    public StaffResponse toggleStaffStatus(Long staffId, boolean enable) {
        User staff = (User)this.userRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        boolean isStaff = staff.getRoles().stream().anyMatch((r) -> r.getRole() == RoleName.STAFF);
        if (!isStaff) {
            throw new BadRequestException("Selected user is not a staff member");
        } else {
            staff.setEnabled(enable);
            return this.mapToStaffResponse((User)this.userRepository.save(staff));
        }
    }

    @Override
    @Transactional
    public StaffResponse toggleStaffAvailability(Long staffProfileId, boolean available) {
        StaffProfile staffProfile = staffProfileRepository.findById(staffProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found with id: " + staffProfileId));

        staffProfile.setAvailable(available);
        StaffProfile updatedProfile = staffProfileRepository.save(staffProfile);

        // 🌟 ပြင်ဆင်ချက်: mapToStaffResponse ထဲသို့ user entity ကို ပြန်ထည့်ပေးခြင်းဖြင့် အားလုံးအဆင်ပြေသွားပါမည်
        return this.mapToStaffResponse(updatedProfile.getUser());
    }

    // 🌟 တည်ငြိမ်ပြီး စိတ်ချရသော တစ်ခုတည်းသော Mapper Method
    private StaffResponse mapToStaffResponse(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a")
                .withZone(ZoneId.of("Asia/Yangon"));

        String formattedDate = user.getCreatedAt() != null ? formatter.format(user.getCreatedAt()) : null;

        // 🌟 User ထဲမှတစ်ဆင့် StaffProfile ကို ယူပြီး isAvailable ကို Null-safe ဖြစ်အောင် ဆွဲထုတ်ခြင်း
        boolean availableStatus = user.getStaffProfile() != null && user.getStaffProfile().isAvailable();

        return StaffResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .code(user.getCode())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .profilePicture(user.getProfilePicture())
                .enabled(user.isEnabled())
                .isAvailable(availableStatus) // 🌟 ကွက်တိ Map ဖြစ်သွားပါပြီ
                .createdAt(formattedDate)
                .build();
    }
}