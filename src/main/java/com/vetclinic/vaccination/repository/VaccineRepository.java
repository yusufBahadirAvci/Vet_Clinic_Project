package com.vetclinic.vaccination.repository;

import com.vetclinic.pet.domain.Species;
import com.vetclinic.vaccination.domain.Vaccine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaccineRepository extends JpaRepository<Vaccine, Long> {

    /** Vaccines for the given species plus the ones for all species (target_species NULL). */
    List<Vaccine> findAllByTargetSpeciesOrTargetSpeciesIsNullOrderByName(Species species);
}
