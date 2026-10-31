package com.vetclinic.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

import java.time.OffsetDateTime;

/** An SMS "sent" by the mock SMS sender; stored instead of being delivered. */
@Entity
@Table(name = "sms_outbox")
@Getter
@Setter
@NoArgsConstructor
public class SmsOutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Normalized phone number (+905XXXXXXXXX). */
    @Column(nullable = false, length = 13)
    private String phone;

    @Column(nullable = false, length = 500)
    private String message;

    /** Filled by the database DEFAULT now() (CLAUDE.md rule 5). */
    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
