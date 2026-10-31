package com.vetclinic.notification;

import com.vetclinic.common.ContactNormalizer;
import com.vetclinic.notification.domain.Channel;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSenderFactory senderFactory;
    @Mock
    private NotificationSender emailSender;
    @Mock
    private NotificationSender smsSender;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(senderFactory, new ContactNormalizer());
    }

    @Test
    void notifyUserPrefersEmailWhenTheUserHasOne() {
        when(senderFactory.get(Channel.EMAIL)).thenReturn(emailSender);

        service.notifyUser(user("ayse@example.com", "+905321234567"), "Subject", "Body");

        verify(emailSender).send("ayse@example.com", "Subject", "Body");
        verifyNoInteractions(smsSender);
    }

    @Test
    void notifyUserFallsBackToSmsWithoutEmail() {
        when(senderFactory.get(Channel.SMS)).thenReturn(smsSender);

        service.notifyUser(user(null, "+905321234567"), "Subject", "Body");

        verify(smsSender).send("+905321234567", "Subject", "Body");
        verifyNoInteractions(emailSender);
    }

    @Test
    void notifyUserTreatsABlankEmailAsMissing() {
        when(senderFactory.get(Channel.SMS)).thenReturn(smsSender);

        service.notifyUser(user(" ", "+905321234567"), "Subject", "Body");

        verify(smsSender).send("+905321234567", "Subject", "Body");
    }

    @Test
    void notifyUserFailsWhenTheUserHasNoContact() {
        assertThatThrownBy(() -> service.notifyUser(user(null, null), "Subject", "Body"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(senderFactory);
    }

    @Test
    void notifyContactSendsEmailToAnEmailAddress() {
        when(senderFactory.get(Channel.EMAIL)).thenReturn(emailSender);

        service.notifyContact("ayse@example.com", "Subject", "Body");

        verify(emailSender).send("ayse@example.com", "Subject", "Body");
        verifyNoInteractions(smsSender);
    }

    @Test
    void notifyContactSendsSmsToAPhoneNumber() {
        when(senderFactory.get(Channel.SMS)).thenReturn(smsSender);

        service.notifyContact("+905321234567", "Subject", "Body");

        verify(smsSender).send("+905321234567", "Subject", "Body");
        verifyNoInteractions(emailSender);
    }

    private static User user(String email, String phone) {
        User user = new User();
        user.setId(1L);
        user.setFullName("Ayse Demir");
        user.setRole(Role.OWNER);
        user.setEmail(email);
        user.setPhone(phone);
        return user;
    }
}
