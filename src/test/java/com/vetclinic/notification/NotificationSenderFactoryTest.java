package com.vetclinic.notification;

import com.vetclinic.notification.domain.Channel;
import com.vetclinic.notification.repository.SmsOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NotificationSenderFactoryTest {

    private final EmailSender emailSender = new EmailSender(mock(JavaMailSender.class));
    private final MockSmsSender smsSender = new MockSmsSender(mock(SmsOutboxRepository.class));

    @Test
    void returnsTheSenderRegisteredForEachChannel() {
        NotificationSenderFactory factory = new NotificationSenderFactory(List.of(emailSender, smsSender));

        assertThat(factory.get(Channel.EMAIL)).isSameAs(emailSender);
        assertThat(factory.get(Channel.SMS)).isSameAs(smsSender);
    }

    @Test
    void failsForAChannelWithoutSender() {
        NotificationSenderFactory factory = new NotificationSenderFactory(List.of(emailSender));

        assertThatThrownBy(() -> factory.get(Channel.SMS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SMS");
    }

    @Test
    void rejectsTwoSendersForTheSameChannel() {
        EmailSender second = new EmailSender(mock(JavaMailSender.class));

        assertThatThrownBy(() -> new NotificationSenderFactory(List.of(emailSender, second)))
                .isInstanceOf(IllegalStateException.class);
    }
}
