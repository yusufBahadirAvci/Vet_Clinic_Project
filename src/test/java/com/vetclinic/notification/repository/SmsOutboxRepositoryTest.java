package com.vetclinic.notification.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.notification.domain.SmsOutboxMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SmsOutboxRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SmsOutboxRepository smsOutboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findTop50ByOrderByCreatedAtDescReturnsTheNewest50() {
        OffsetDateTime base = OffsetDateTime.parse("2026-10-31T12:00:00+03:00");
        for (int i = 1; i <= 55; i++) {
            SmsOutboxMessage message = new SmsOutboxMessage();
            message.setPhone("+905321234567");
            message.setMessage("Message " + i);
            smsOutboxRepository.saveAndFlush(message);
            // created_at is DEFAULT now(), identical inside one test transaction; message i is i minutes later.
            jdbcTemplate.update("UPDATE sms_outbox SET created_at = ? WHERE id = ?", base.plusMinutes(i), message.getId());
        }

        List<SmsOutboxMessage> newest = smsOutboxRepository.findTop50ByOrderByCreatedAtDesc();

        assertThat(newest).hasSize(50);
        assertThat(newest.getFirst().getMessage()).isEqualTo("Message 55");
        assertThat(newest.getLast().getMessage()).isEqualTo("Message 6");
    }
}
