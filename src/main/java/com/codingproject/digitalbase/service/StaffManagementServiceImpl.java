package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.LeaveType;
import com.codingproject.digitalbase.enums.RoleName;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.*;
import com.codingproject.digitalbase.repository.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

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
    private final StaffLeaveRepository staffLeaveRepository;

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

//    @Transactional
//    public StaffResponse linkStaffWithServices(Long staffId, List<Long> serviceIds) {
//        User staff = (User)this.userRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + staffId));
//        boolean isStaff = staff.getRoles().stream().anyMatch((r) -> r.getRole() == RoleName.STAFF);
//        if (!isStaff) {
//            throw new BadRequestException("Selected user is not a staff member");
//        } else {
//            List<BusinessService> services = serviceIds != null && !serviceIds.isEmpty() ? this.serviceRepository.findAllById(serviceIds) : Collections.emptyList();
//            if (staff.getStaffProfile() != null) {
//                staff.getStaffProfile().setSpecializedServices(new HashSet(services));
//            } else {
//                StaffProfile profile = StaffProfile.builder().user(staff).isAvailable(true).specializedServices(new HashSet(services)).rating((double)0.0F).joinedAt(Instant.now()).build();
//                staff.setStaffProfile(profile);
//            }
//
//            User updatedStaff = (User)this.userRepository.save(staff);
//            log.info("🎯 Successfully updated specialized services for staff code: {}", updatedStaff.getCode());
//            return this.mapToStaffResponse(updatedStaff);
//        }
//    }

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

    @Override
    @Transactional
    public StaffResponse updateStaffUser(Long staffId, StaffUpdateRequest request) {
        User staff = this.userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with id: " + staffId));

        boolean isStaff = staff.getRoles().stream().anyMatch((r) -> r.getRole() == RoleName.STAFF);
        if (!isStaff) {
            throw new BadRequestException("Selected user is not a staff member");
        } else {
            // Email နှင့် Phone နံပါတ် Duplicate ဖြစ်မဖြစ် စစ်ဆေးခြင်း
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

            // 🌟 လိုအပ်သော ကိုယ်ရေးအချက်အလက် (၄) ခုကိုသာ ရိုးရှင်းစွာ Update လုပ်ပါသည်
            staff.setFullName(request.getFullName());
            staff.setEmail(request.getEmail());
            staff.setPhone(request.getPhoneNumber());

            if (request.getDateOfBirth() != null) {
                staff.setDateOfBirth(request.getDateOfBirth());
            }

            // 💡 မှတ်ချက် - gender, profilePicture, specializedServices များကို ဘာမှမလုပ်ဘဲ နဂိုအတိုင်း ချန်ထားခဲ့ပါသည်

            User updatedStaff = this.userRepository.save(staff);
            return this.mapToStaffResponse(updatedStaff);
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
    public void assignStaffLeave(StaffLeaveRequest request) {
        // ၁။ ဝန်ထမ်း ရှိ/မရှိ စစ်ဆေးခြင်း
        StaffProfile staffProfile = staffProfileRepository.findById(request.getStaffProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found with ID: " + request.getStaffProfileId()));

        // 🌟 ၂။ Handling Single-Day Leave with Instant
        Instant startDate = request.getStartDate();
        Instant endDate = (request.getEndDate() != null) ? request.getEndDate() : startDate;

        // ၃။ Validation (စတင်ချိန်သည် ကုန်ဆုံးချိန်ထက် မကျော်လွန်စေရန် စစ်ဆေးခြင်း)
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        // ၄။ Entity ထဲသို့ ထည့်သွင်း၍ DB တွင် သိမ်းဆည်းခြင်း
        StaffLeave staffLeave = StaffLeave.builder()
                .staffProfile(staffProfile)
                .leaveType(request.getLeaveType())
                .startDate(startDate)
                .endDate(endDate)
                .note(request.getNote())
                .build();

        staffLeaveRepository.save(staffLeave);
    }

    @Override
    public DailyStaffStatusResponse getDailyStaffStatus(Instant targetDate) {
        List<StaffProfile> allStaff = staffProfileRepository.findAll();

        // Target နေ့တွင် ငြိနေသော ခွင့်များကို စစ်ထုတ်ခြင်း
        List<StaffLeave> activeLeaves = staffLeaveRepository.findAll().stream()
                .filter(leave -> !targetDate.isBefore(leave.getStartDate()) &&
                        (leave.getEndDate() == null || !targetDate.isAfter(leave.getEndDate())))
                .collect(Collectors.toList());

        List<DailyStaffStatusResponse.StaffStatusDTO> activeStaff = new ArrayList<>();
        List<DailyStaffStatusResponse.StaffStatusDTO> dayOffStaff = new ArrayList<>();
        List<DailyStaffStatusResponse.StaffStatusDTO> leaveStaff = new ArrayList<>();

        for (StaffProfile staff : allStaff) {
            StaffLeave staffLeave = activeLeaves.stream()
                    .filter(l -> l.getStaffProfile().getId().equals(staff.getId()))
                    .findFirst().orElse(null);

            User user = staff.getUser();

            // 🌟 User Object ထဲမှ Role Name ကို စာသားအဖြစ် သန့်သန့်လေး ပြောင်းလဲခြင်း
            String roleName = user.getRoles().stream()
                    .map(role -> role.getRole().name())
                    .collect(Collectors.joining(", "));

            DailyStaffStatusResponse.StaffStatusDTO dto = DailyStaffStatusResponse.StaffStatusDTO.builder()
                    .id(staff.getId())
                    .name(user.getFullName())
                    .role(roleName)
                    .profileImage(user.getProfilePicture())
                    .build();

            if (staffLeave == null) {
                activeStaff.add(dto);
            } else if (LeaveType.DAY_OFF == staffLeave.getLeaveType()) {
                dayOffStaff.add(dto);
            } else {
                leaveStaff.add(dto);
            }
        }

        return DailyStaffStatusResponse.builder()
                .activeStaff(activeStaff)
                .dayOffStaff(dayOffStaff)
                .leaveStaff(leaveStaff)
                .build();
    }

    // 🎯 ပြက္ခဒိန်အတွက် Range အလိုက် Loop ပတ်ပြီး Data ထုတ်ပေးခြင်း
    @Override
    public List<CalendarMonthResponse> getCalendarMonthOverview(Instant startDate, Instant endDate, Long staffId) {
        List<CalendarMonthResponse> monthlyData = new ArrayList<>();

        Instant currentDay = startDate;
        // Start Date မှ End Date အထိ တစ်ရက်ချင်းစီကို Loop ပတ်ပြီး စစ်ဆေးသည်
        while (!currentDay.isAfter(endDate)) {
            final Instant finalCurrentDay = currentDay;

            List<StaffLeave> dayLeaves = staffLeaveRepository.findAll().stream()
                    .filter(leave -> !finalCurrentDay.isBefore(leave.getStartDate()) &&
                            (leave.getEndDate() == null || !finalCurrentDay.isAfter(leave.getEndDate())))
                    .filter(leave -> staffId == null || leave.getStaffProfile().getId().equals(staffId))
                    .collect(Collectors.toList());

            // 🌟 Fix: .map() အတွင်း Lambda Body ကိုသုံး၍ Role Name အမှားကို ပြင်ဆင်ထားပါသည်
            List<CalendarMonthResponse.StaffLeaveEvent> events = dayLeaves.stream().map(leave -> {
                User user = leave.getStaffProfile().getUser();

                String roleName = user.getRoles().stream()
                        .map(role -> role.getRole().name())
                        .collect(Collectors.joining(", "));

                return CalendarMonthResponse.StaffLeaveEvent.builder()
                        .staffId(leave.getStaffProfile().getId())
                        .staffName(user.getFullName())
                        .role(roleName) // 🎯 ကွက်တိ စာသားအမှန် ထွက်လာပါလိမ့်မည်
                        .leaveType(leave.getLeaveType())
                        .note(leave.getNote())
                        .build();
            }).collect(Collectors.toList());

            monthlyData.add(CalendarMonthResponse.builder()
                    .date(finalCurrentDay)
                    .events(events)
                    .build());

            // နောက်တစ်ရက်သို့ ကူးပြောင်းခြင်း
            currentDay = currentDay.plus(1, ChronoUnit.DAYS);
        }
        return monthlyData;
    }

    @Override
    public List<StaffLeaveDetailResponse> getStaffLeavesBySelectedDate(Instant targetDate) {
        // ဒေတာအားလုံးကို List အလိုက် ဆွဲယူခြင်း
        List<StaffLeave> leaves = staffLeaveRepository.findActiveLeavesByDate(targetDate);

        return leaves.stream().map(leave -> {
            User user = leave.getStaffProfile().getUser();

            String roleName = user.getRoles().stream()
                    .map(role -> role.getRole().name())
                    .collect(Collectors.joining(", "));

            return StaffLeaveDetailResponse.builder()
                    .staffProfileId(leave.getStaffProfile().getId())
                    .staffName(user.getFullName())
                    .role(roleName)
                    .profileImage(user.getProfilePicture())
                    .leaveType(leave.getLeaveType().name())
                    .note(leave.getNote())
                    .build();
        }).collect(Collectors.toList());
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
        // ရက်စွဲ Format များ သတ်မှတ်ခြင်း
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a")
                .withZone(ZoneId.of("Asia/Yangon"));

        // 🌟 မွေးနေ့အတွက် Format သီးသန့် သတ်မှတ်ခြင်း (ဥပမာ - 2000-09-27)
        DateTimeFormatter dobFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("Asia/Yangon"));

        String formattedDate = user.getCreatedAt() != null ? formatter.format(user.getCreatedAt()) : null;

        // 🌟 User Entity ထဲမှ Instant dateOfBirth ကို String ပြောင်းလဲခြင်း
        String formattedDob = user.getDateOfBirth() != null ? dobFormatter.format(user.getDateOfBirth()) : null;

        boolean availableStatus = user.getStaffProfile() != null && user.getStaffProfile().isAvailable();

        List<Long> serviceIds = new java.util.ArrayList<>();
        if (user.getStaffProfile() != null && user.getStaffProfile().getSpecializedServices() != null) {
            serviceIds = user.getStaffProfile().getSpecializedServices().stream()
                    .map(BusinessService::getId)
                    .toList();
        }

        return StaffResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .code(user.getCode())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .profilePicture(user.getProfilePicture())
                .enabled(user.isEnabled())
                .isAvailable(availableStatus)
                .createdAt(formattedDate)
                .dateOfBirth(formattedDob) // 🌟 ဤနေရာတွင် မွေးနေ့ကို Response ထဲ ကွက်တိ ထည့်ပေးလိုက်ပါပြီဗျာ
                .specializedServiceIds(serviceIds)
                .build();
    }
}