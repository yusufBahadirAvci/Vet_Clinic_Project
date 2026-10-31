package com.vetclinic.notification;

import com.vetclinic.common.config.TimeConfig;
import com.vetclinic.notification.repository.SmsOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Read side of the mock SMS outbox, for the vet panel. */
@Service
public class SmsOutboxService {

    private static final DateTimeFormatter SENT_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(TimeConfig.CLINIC_ZONE);

    public record SmsOutboxView(String phone, String message, String sentAt) {
    }

    private final SmsOutboxRepository smsOutboxRepository;

    public SmsOutboxService(SmsOutboxRepository smsOutboxRepository) {
        this.smsOutboxRepository = smsOutboxRepository;
    }

    /** The 50 most recent messages, newest first. */
    @Transactional(readOnly = true)
    public List<SmsOutboxView> latestMessages() {
        return smsOutboxRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(m -> new SmsOutboxView(m.getPhone(), m.getMessage(), SENT_AT_FORMAT.format(m.getCreatedAt())))
                .toList();
    }
}
