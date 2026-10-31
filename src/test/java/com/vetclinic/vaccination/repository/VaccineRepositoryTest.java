package com.vetclinic.vaccination.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.vaccination.domain.Vaccine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.vetclinic.support.TestData.vaccine;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class VaccineRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private VaccineRepository vaccineRepository;

    @Test
    void returnsVaccinesForTheSpeciesAndForAllSpeciesSortedByName() {
        vaccineRepository.save(vaccine("Rabies", null));
        vaccineRepository.save(vaccine("Dog Combo (DHPPi)", Species.DOG));
        vaccineRepository.save(vaccine("Leptospirosis", Species.DOG));
        vaccineRepository.save(vaccine("Cat Combo (FVRCP)", Species.CAT));
        vaccineRepository.flush();

        assertThat(vaccineRepository.findAllByTargetSpeciesOrTargetSpeciesIsNullOrderByName(Species.DOG))
                .extracting(Vaccine::getName)
                .containsExactly("Dog Combo (DHPPi)", "Leptospirosis", "Rabies");

        assertThat(vaccineRepository.findAllByTargetSpeciesOrTargetSpeciesIsNullOrderByName(Species.BIRD))
                .extracting(Vaccine::getName)
                .containsExactly("Rabies");
    }
}
