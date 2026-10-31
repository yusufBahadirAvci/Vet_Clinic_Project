package com.vetclinic.notification.repository;

import com.vetclinic.notification.domain.SmsOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsOutboxRepository extends JpaRepository<SmsOutboxMessage, Long> {

    List<SmsOutboxMessage> findTop50ByOrderByCreatedAtDesc();
}
