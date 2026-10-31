package com.vetclinic.pet.service;

import com.vetclinic.pet.domain.Species;
import com.vetclinic.vaccination.VaccinationStatus;

import java.time.LocalDate;
import java.util.List;

/** Everything the pet detail page shows, assembled by {@link PetProfileFacade}. */
public record PetProfileDto(
        Long id,
        String name,
        Species species,
        String speciesLabel,
        String breed,
        String genderLabel,
        LocalDate birthDate,
        String age,
        String color,
        String microchipNo,
        boolean neutered,
        String photoUrl,
        VaccinationStatus overallStatus,
        List<VaccinationEntry> upcomingDoses,
        List<VaccinationEntry> timeline) {

    /**
     * One vaccination row. {@code current} is true only for the latest record of its vaccine;
     * older doses are shown as completed.
     */
    public record VaccinationEntry(
            String vaccineName,
            LocalDate administeredAt,
            LocalDate nextDueDate,
            String vetName,
            String lotNumber,
            VaccinationStatus status,
            boolean current) {
    }
}
