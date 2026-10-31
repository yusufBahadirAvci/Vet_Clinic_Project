package com.vetclinic.vaccination.repository;

import com.vetclinic.vaccination.domain.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    List<Vaccination> findAllByPetIdOrderByAdministeredAtDesc(Long petId);

    /** Inclusive on both ends. */
    List<Vaccination> findAllByNextDueDateBetween(LocalDate from, LocalDate to);
}
