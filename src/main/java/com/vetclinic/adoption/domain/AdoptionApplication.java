package com.vetclinic.adoption.domain;

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

/**
 * A visitor's application for an adoption listing.
 * At least one of {@code applicantEmail} / {@code applicantPhone} is set (database CHECK), normalized.
 */
@Entity
@Table(name = "adoption_applications")
@Getter
@Setter
@NoArgsConstructor
public class AdoptionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private AdoptionListing listing;

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "applicant_email", length = 254)
    private String applicantEmail;

    @Column(name = "applicant_phone", length = 13)
    private String applicantPhone;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "kvkk_consent", nullable = false)
    private boolean kvkkConsent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApplicationStatus status;

    /** Filled by the database DEFAULT now() (CLAUDE.md rule 5). */
    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Set by the service via the Clock when the application is approved or rejected. */
    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;
}
