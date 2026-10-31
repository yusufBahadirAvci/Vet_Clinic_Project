package com.vetclinic.notification;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetclinic.notification.domain.Channel;
import com.vetclinic.notification.domain.SmsOutboxMessage;
import com.vetclinic.notification.repository.SmsOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MockSmsSenderTest {

    private static final String PHONE = "+905321234567";

    @Mock
    private SmsOutboxRepository smsOutboxRepository;

    @InjectMocks
    private MockSmsSender sender;

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    private Logger senderLogger;

    @BeforeEach
    void attachLogAppender() {
        senderLogger = (Logger) LoggerFactory.getLogger(MockSmsSender.class);
        logAppender.start();
        senderLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        senderLogger.detachAppender(logAppender);
    }

    @Test
    void usesTheSmsChannel() {
        assertThat(sender.channel()).isEqualTo(Channel.SMS);
    }

    @Test
    void writesTheMessageToTheOutbox() {
        sender.send(PHONE, "Subject", "Your code is 123456");

        ArgumentCaptor<SmsOutboxMessage> saved = ArgumentCaptor.forClass(SmsOutboxMessage.class);
        verify(smsOutboxRepository).save(saved.capture());
        assertThat(saved.getValue().getPhone()).isEqualTo(PHONE);
        assertThat(saved.getValue().getMessage()).isEqualTo("Your code is 123456");
    }

    @Test
    void cutsMessagesLongerThanTheColumn() {
        sender.send(PHONE, "Subject", "x".repeat(MockSmsSender.MAX_MESSAGE_LENGTH + 20));

        ArgumentCaptor<SmsOutboxMessage> saved = ArgumentCaptor.forClass(SmsOutboxMessage.class);
        verify(smsOutboxRepository).save(saved.capture());
        assertThat(saved.getValue().getMessage()).hasSize(MockSmsSender.MAX_MESSAGE_LENGTH);
    }

    @Test
    void logsOnlyTheMaskedPhoneNumber() {
        sender.send(PHONE, "Subject", "Your code is 123456");

        assertThat(logAppender.list).hasSize(1);
        String logged = logAppender.list.get(0).getFormattedMessage();
        assertThat(logged).contains("+90532*****67");
        assertThat(logged).doesNotContain(PHONE).doesNotContain("123456");
    }
}
