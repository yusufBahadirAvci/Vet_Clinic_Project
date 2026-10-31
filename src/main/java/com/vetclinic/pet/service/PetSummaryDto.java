package com.vetclinic.pet.service;

import com.vetclinic.pet.domain.Species;
import com.vetclinic.vaccination.VaccinationStatus;

/** One pet card on the "My pets" page. {@code status} is the status of the nearest dose. */
public record PetSummaryDto(
        Long id,
        String name,
        Species species,
        String speciesLabel,
        String breed,
        String age,
        String photoUrl,
        VaccinationStatus status) {
}
