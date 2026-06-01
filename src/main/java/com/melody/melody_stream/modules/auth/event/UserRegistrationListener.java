package com.melody.melody_stream.modules.auth.event;

import com.melody.melody_stream.modules.auth.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final MailService mailService;

    // Listen Event, but ONLY RUN when Transaction before has commit successfully
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        mailService.sendVerificationEmail(event.email(), event.username(), event.verificationToken());
    }
}
