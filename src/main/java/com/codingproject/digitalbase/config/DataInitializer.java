//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.config;

import com.codingproject.digitalbase.enums.RoleName;
import com.codingproject.digitalbase.model.Role;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.BusinessServiceRepository;
import com.codingproject.digitalbase.repository.RoleRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessServiceRepository serviceRepository;

    @Transactional
    public void run(String... args) throws Exception {
        if (this.roleRepository.count() == 0L) {
            this.roleRepository.save(Role.builder().role(RoleName.CUSTOMER).build());
            this.roleRepository.save(Role.builder().role(RoleName.STAFF).build());
            this.roleRepository.save(Role.builder().role(RoleName.SUPER_ADMIN).build());
            log.info("✅ Roles Initialized!");
        }

        Role superAdminRole = this.roleRepository.findByRole(RoleName.SUPER_ADMIN).orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found!"));
        Role staffRole = this.roleRepository.findByRole(RoleName.STAFF).orElseThrow(() -> new RuntimeException("STAFF role not found!"));
        String superAdminEmail = "yannaing7269@gmail.com";
        if (!this.userRepository.existsByEmail(superAdminEmail)) {
            User superAdmin = User.builder().fullName("Super Administrator").code("ADMIN").email(superAdminEmail).password(this.passwordEncoder.encode("SuperAdmin@123")).phone("09123456789").gender("Male").profilePicture("default-profile.png").roles(new HashSet(Collections.singleton(superAdminRole))).enabled(true).createdAt(Instant.now()).build();
            this.userRepository.save(superAdmin);
            log.info("\ud83d\ude80 Super Admin Account created successfully! Code: ADMIN");
        }

    }
}
