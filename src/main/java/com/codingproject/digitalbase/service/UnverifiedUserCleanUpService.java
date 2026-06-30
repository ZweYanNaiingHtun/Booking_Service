//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnverifiedUserCleanUpService {
    private static final Logger log = LoggerFactory.getLogger(UnverifiedUserCleanUpService.class);
    private final UserRepository userRepository;

    @Scheduled(
            fixedRate = 60000L
    )
    @Transactional
    public void cleanupUnverifiedUsers() {
        log.info("Starting scheduled task: Cleaning up unverified users...");

        try {
            Instant timeLimit = Instant.now().minus(24L, ChronoUnit.HOURS);
            int deletedCount = this.userRepository.deleteByEnabledFalseAndCreatedAtBefore(timeLimit);
            if (deletedCount > 0) {
                log.info("Successfully deleted {} unverified user accounts from database.", deletedCount);
            } else {
                log.info("No unverified expired accounts found to delete.");
            }
        } catch (Exception e) {
            log.error("Error occurred while executing unverified users cleanup: {}", e.getMessage(), e);
        }

    }
}
