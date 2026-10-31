package com.vetclinic.notification;

import com.vetclinic.notification.domain.SmsOutboxMessage;
import com.vetclinic.notification.repository.SmsOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsOutboxServiceTest {

    @Mock
    private SmsOutboxRepository smsOutboxRepository;

    @InjectMocks
    private SmsOutboxService service;

    @Test
    void mapsMessagesToViewsAndShowsTimeInClinicZone() {
        SmsOutboxMessage message = new SmsOutboxMessage();
        message.setPhone("+905321234567");
        message.setMessage("Your code is 123456");
        ReflectionTestUtils.setField(message, "createdAt", OffsetDateTime.of(2026, 1, 15, 9, 30, 5, 0, ZoneOffset.UTC));
        when(smsOutboxRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(message));

        List<SmsOutboxService.SmsOutboxView> views = service.latestMessages();

        assertThat(views).containsExactly(
                new SmsOutboxService.SmsOutboxView("+905321234567", "Your code is 123456", "2026-01-15 12:30:05"));
    }

    @Test
    void returnsEmptyListWhenNothingWasSent() {
        when(smsOutboxRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(service.latestMessages()).isEmpty();
    }
}
