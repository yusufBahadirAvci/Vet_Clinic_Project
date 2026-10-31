package com.vetclinic.notification;

import com.vetclinic.common.ContactNormalizer;
import com.vetclinic.common.ContactType;
import com.vetclinic.notification.domain.Channel;
import com.vetclinic.user.domain.User;
import org.springframework.stereotype.Service;

/** The only entry point for sending notifications (CLAUDE.md rule 8). */
@Service
public class NotificationService {

    private final NotificationSenderFactory senderFactory;
    private final ContactNormalizer contactNormalizer;

    public NotificationService(NotificationSenderFactory senderFactory, ContactNormalizer contactNormalizer) {
        this.senderFactory = senderFactory;
        this.contactNormalizer = contactNormalizer;
    }

    /** E-mail if the user has one, otherwise SMS. */
    public void notifyUser(User user, String subject, String body) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            senderFactory.get(Channel.EMAIL).send(user.getEmail(), subject, body);
        } else if (user.getPhone() != null && !user.getPhone().isBlank()) {
            senderFactory.get(Channel.SMS).send(user.getPhone(), subject, body);
        } else {
            throw new IllegalStateException("User " + user.getId() + " has no e-mail address or phone number");
        }
    }

    /** @param normalizedContact an e-mail or phone number already normalized by {@link ContactNormalizer} */
    public void notifyContact(String normalizedContact, String subject, String body) {
        Channel channel = contactNormalizer.detectType(normalizedContact) == ContactType.EMAIL
                ? Channel.EMAIL
                : Channel.SMS;
        senderFactory.get(channel).send(normalizedContact, subject, body);
    }
}
