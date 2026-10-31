package com.vetclinic.vaccination.domain;

import com.vetclinic.pet.domain.Species;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A vaccine in the clinic catalog.
 * {@code targetSpecies} null = suitable for all species; {@code repeatIntervalDays} null = single dose.
 */
@Entity
@Table(name = "vaccines")
@Getter
@Setter
@NoArgsConstructor
public class Vaccine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_species", length = 10)
    private Species targetSpecies;

    @Column(name = "repeat_interval_days")
    private Integer repeatIntervalDays;

    @Column(length = 500)
    private String description;
}
