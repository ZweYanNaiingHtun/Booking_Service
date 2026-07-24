//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.RoleName;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Role;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.RoleRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    public String generateUserCode(RoleName roleName) {
        if (roleName == RoleName.SUPER_ADMIN) {
            return "ADMIN";
        } else {
            String prefix = roleName == RoleName.CUSTOMER ? "CU-" : "ST-";
            String maxCode = this.userRepository.findMaxCodeByPrefix(prefix);
            if (maxCode != null && !maxCode.trim().isEmpty()) {
                try {
                    String cleanedMaxCode = maxCode.trim();
                    String numericPart = cleanedMaxCode.substring(prefix.length());
                    int nextNumber = Integer.parseInt(numericPart) + 1;
                    return prefix + String.format("%03d", nextNumber);
                } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                    log.error("❌ Error parsing user code. DB MaxCode: '{}', Prefix: '{}'. Error Details: {}", new Object[]{maxCode, prefix, ((RuntimeException)e).getMessage()});
                    throw new BadRequestException("Data inconsistency detected in user codes. Please contact admin.");
                }
            } else {
                return prefix + "001";
            }
        }
    }

    @Transactional
    public void signupInit(SignupInitRequest request) {
        if (this.userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        } else {
            String otp = getOtp();
            Role userRole = this.roleRepository.findByRole(RoleName.CUSTOMER).orElseThrow(() -> new ResourceNotFoundException("Default role not found!"));
            User user = User.builder().email(request.getEmail()).password(this.passwordEncoder.encode(request.getPassword())).roles(new HashSet(Collections.singleton(userRole))).enabled(false).otp(otp).otpGeneratedTime(Instant.now()).createdAt(Instant.now()).profilePicture("default-profile.png").build();
            this.userRepository.save(user);
            this.emailService.sendVerificationEmail(user.getEmail(), otp);
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = this.userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email not found!"));

        // 🎯 အကောင့်က Block ခံထားရပါက (Enabled = false) လုံးဝဝင်ခွင့်မပြုပါ
        if (!user.isEnabled()) {
            throw new BadRequestException("Your account has been disabled. Please contact support.");
        }

        if (!this.passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid password request");
        } else {
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            String accessToken = this.jwtService.generateAccessToken(authentication);
            String refreshToken = this.jwtService.generateRefreshToken(authentication);
            TokenPair tokenPair = new TokenPair(accessToken, refreshToken);
            return LoginResponse.builder().message("Login successful").otpRequired(false).tokenPair(tokenPair).build();
        }
    }

    @Transactional
    public void completeProfile(CompleteProfileRequest request, String email) {
        User user = this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (this.userRepository.existsByPhone(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number is already in use!");
        } else {
            String customerCode = this.generateUserCode(RoleName.CUSTOMER);
            user.setFullName(request.getFullName());
            user.setCode(customerCode);
            user.setPhone(request.getPhoneNumber());
            user.setGender(request.getGender());
            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                try {
                    Path uploadPath = Paths.get("uploads/profile-pictures/");
                    if (!Files.exists(uploadPath, new LinkOption[0])) {
                        Files.createDirectories(uploadPath);
                    }

                    String var10000 = UUID.randomUUID().toString();
                    String fileName = var10000 + "_" + request.getProfileImage().getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(request.getProfileImage().getInputStream(), filePath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
                    user.setProfilePicture(fileName);
                } catch (IOException e) {
                    throw new BadRequestException("Could not store profile image: " + e.getMessage());
                }
            }

            this.userRepository.save(user);
        }
    }

    @Transactional
    public TokenPair verifyOtp(VerifyOtpRequest request) {
        User user = this.userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        if (user.getOtp() == null) {
            throw new BadRequestException("OTP not found or already used");
        } else if (!user.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        } else if (user.getOtpGeneratedTime().plus(5L, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
        } else {
            if (!user.isEnabled()) {
                user.setEnabled(true);
            }

            user.setOtp(null);
            user.setOtpGeneratedTime(null);
            this.userRepository.save(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, (Object)null, user.getAuthorities());
            String accessToken = this.jwtService.generateAccessToken(authentication);
            String refreshToken = this.jwtService.generateRefreshToken(authentication);
            return new TokenPair(accessToken, refreshToken);
        }
    }

    public TokenPair refreshToken(RefreshTokenRequest request) {
        String email = this.jwtService.extractEmailFromToken(request.getRefreshToken());
        User user = this.userRepository.findByEmail(email).orElseThrow();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, (Object)null, user.getAuthorities());
        String accessToken = this.jwtService.generateAccessToken(authentication);
        return new TokenPair(accessToken, request.getRefreshToken());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = this.userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        String otp = getOtp();
        user.setOtp(otp);
        user.setOtpGeneratedTime(Instant.now());
        this.userRepository.save(user);
        this.emailService.sendResetPasswordEmail(user.getEmail(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String email) {
        User user = this.userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(this.passwordEncoder.encode(request.getNewPassword()));
        this.userRepository.save(user);
    }

    @Transactional
    public TokenPair firebaseLogin(String idToken) {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String pictureUrl = decodedToken.getPicture();
            User user = this.userRepository.findByEmail(email).orElseGet(() -> {
                Role userRole = this.roleRepository.findByRole(RoleName.CUSTOMER).orElseThrow(() -> new ResourceNotFoundException("Default role not found!"));
                String firebaseUserCode = this.generateUserCode(RoleName.CUSTOMER);
                User newUser = User.builder()
                        .fullName(name != null ? name : email.split("@")[0])
                        .code(firebaseUserCode)
                        .email(email)
                        .password(this.passwordEncoder.encode(UUID.randomUUID().toString()))
                        .roles(new HashSet(Collections.singleton(userRole)))
                        .enabled(true) // အကောင့်အသစ်ဆိုလျှင် တန်းဖွင့်ပေးမည်
                        .profilePicture(pictureUrl != null ? pictureUrl : "default-profile.png")
                        .build();
                return this.userRepository.save(newUser);
            });

            // 🎯 ပြင်ဆင်လိုက်သည့်နေရာ: အကယ်၍ ရှိပြီးသားလူကို Admin က Block ထားပါက Firebase ဖြင့်လည်း ပေးမဝင်တော့ပါ
            if (!user.isEnabled()) {
                throw new BadRequestException("Your account has been disabled. Please contact support.");
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(user, (Object)null, user.getAuthorities());
            String accessToken = this.jwtService.generateAccessToken(authentication);
            String refreshToken = this.jwtService.generateRefreshToken(authentication);
            return new TokenPair(accessToken, refreshToken);
        } catch (FirebaseAuthException e) {
            throw new BadRequestException("Invalid Firebase Token: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateFcmToken(String email, FcmTokenRequest request) {
        String newToken = request.getToken();

        // 🎯 Valid Token မဟုတ်ပါက ရှေ့ဆက်မလုပ်ရန် စစ်ဆေးခြင်း
        if (newToken == null || newToken.trim().isEmpty()) {
            log.warn("FCM Token update bypassed: Token is null or blank for email: {}", email);
            return;
        }

        // ၁။ JWT မှတစ်ဆင့် ရရှိလာသော Email ဖြင့် လက်ရှိ အသုံးပြုသူအား ရှာဖွေခြင်း
        User currentUser = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // 🌟 ၂။ အဆိုပါ FCM Token သည် အခြား User တစ်ယောက်ယောက်ဆီမှာ ရှိနေပါက အရင် Clear (NULL) ပြုလုပ်ပေးခြင်း
        this.userRepository.findByFcmToken(newToken).ifPresent(otherUser -> {
            if (!otherUser.getId().equals(currentUser.getId())) {
                otherUser.setFcmToken(null);
                this.userRepository.save(otherUser);
                log.info("🔑 Duplicate FCM token cleared from previous user: {}", otherUser.getEmail());
            }
        });

        // ၃။ လက်ရှိ User ထံသို့ Token အသစ်အား သတ်မှတ်၍ DB တွင် သိမ်းဆည်းခြင်း
        currentUser.setFcmToken(newToken);
        this.userRepository.save(currentUser);
        log.info("✅ FCM Token successfully updated for user: {}", currentUser.getEmail());
    }

    @Transactional
    public String uploadProfilePicture(Long userId, MultipartFile file) {
        try {
            User user = this.userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            Path uploadPath = Paths.get("uploads/profile-pictures/");
            if (!Files.exists(uploadPath, new LinkOption[0])) {
                Files.createDirectories(uploadPath);
            }

            String var10000 = UUID.randomUUID().toString();
            String fileName = var10000 + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
            user.setProfilePicture(fileName);
            this.userRepository.save(user);
            return fileName;
        } catch (IOException e) {
            throw new BadRequestException("Could not store file: " + e.getMessage());
        }
    }

    private static String getOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
