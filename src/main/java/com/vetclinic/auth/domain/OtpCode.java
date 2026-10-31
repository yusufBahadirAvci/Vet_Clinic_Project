package com.vetclinic.auth.domain;

import com.vetclinic.notification.domain.Channel;
import com.vetclinic.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

import java.time.OffsetDateTime;

/** A one-time login code. Only the hash of the code is stored; the code itself is never stored or logged. */
@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Channel channel;

    /** Normalized e-mail or phone the code was sent to. */
    @Column(nullable = false, length = 254)
    private String destination;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    /** Set by the service via the Clock. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /** Set by the service via the Clock when the code is used. */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /** Filled by the database DEFAULT now() (CLAUDE.md rule 5). */
    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
