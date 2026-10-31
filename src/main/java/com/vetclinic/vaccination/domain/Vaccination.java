package com.vetclinic.vaccination.domain;

import com.vetclinic.pet.domain.Pet;
import com.vetclinic.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** One administered vaccine dose. {@code nextDueDate} null = no further dose. */
@Entity
@Table(name = "vaccinations")
@Getter
@Setter
@NoArgsConstructor
public class Vaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    @Column(name = "administered_at", nullable = false)
    private LocalDate administeredAt;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    /** The veterinarian who administered the dose. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vet_id", nullable = false)
    private User vet;

    @Column(name = "lot_number", length = 40)
    private String lotNumber;

    @Column(length = 500)
    private String notes;

    /** Set by the reminder job via the Clock. */
    @Column(name = "reminder_sent_at")
    private OffsetDateTime reminderSentAt;

    /** Filled by the database DEFAULT now() (CLAUDE.md rule 5). */
    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
