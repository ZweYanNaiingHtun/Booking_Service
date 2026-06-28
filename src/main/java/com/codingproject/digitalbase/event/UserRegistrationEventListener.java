//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.event;

import com.codingproject.digitalbase.service.EmailService;
import lombok.Generated;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegistrationEventListener {
    private final EmailService emailService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleUserRegistration(UserRegisterEvent event) {
        this.emailService.sendVerificationEmail(event.email(), event.token());
    }

    @Generated
    public UserRegistrationEventListener(final EmailService emailService) {
        this.emailService = emailService;
    }
}
