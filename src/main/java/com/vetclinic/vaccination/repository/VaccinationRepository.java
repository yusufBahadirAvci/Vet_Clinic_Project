package com.vetclinic.vaccination.repository;

import com.vetclinic.vaccination.domain.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    List<Vaccination> findAllByPetIdOrderByAdministeredAtDesc(Long petId);

    /** Inclusive on both ends. */
    List<Vaccination> findAllByNextDueDateBetween(LocalDate from, LocalDate to);

    /**
     * Number of pets with a dose due between {@code from} and {@code to} (inclusive).
     * Only the latest dose of each vaccine counts for a pet (docs/decisions.md, decision 12):
     * a dose followed by a newer dose of the same vaccine is ignored. Same-day doses are ordered by id.
     */
    @Query("""
            select count(distinct v.pet.id) from Vaccination v
            where v.nextDueDate between :from and :to
              and not exists (
                  select 1 from Vaccination newer
                  where newer.pet = v.pet and newer.vaccine = v.vaccine
                    and (newer.administeredAt > v.administeredAt
                         or (newer.administeredAt = v.administeredAt and newer.id > v.id)))
            """)
    long countPetsDueBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
