package com.vetclinic.notification;

import com.vetclinic.common.LogMasker;
import com.vetclinic.notification.domain.Channel;
import com.vetclinic.notification.domain.SmsOutboxMessage;
import com.vetclinic.notification.repository.SmsOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stand-in for a real SMS gateway: stores the message in {@code sms_outbox} instead of delivering it. */
@Component
public class MockSmsSender implements NotificationSender {

    static final int MAX_MESSAGE_LENGTH = 500;

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    private final SmsOutboxRepository smsOutboxRepository;

    public MockSmsSender(SmsOutboxRepository smsOutboxRepository) {
        this.smsOutboxRepository = smsOutboxRepository;
    }

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    /** An SMS has no subject, so only the body is stored (cut to the column length). */
    @Override
    public void send(String destination, String subject, String body) {
        SmsOutboxMessage message = new SmsOutboxMessage();
        message.setPhone(destination);
        message.setMessage(body.length() > MAX_MESSAGE_LENGTH ? body.substring(0, MAX_MESSAGE_LENGTH) : body);
        smsOutboxRepository.save(message);
        log.info("Mock SMS stored for {}", LogMasker.maskPhone(destination));
    }
}
