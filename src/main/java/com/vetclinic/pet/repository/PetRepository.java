package com.vetclinic.pet.repository;

import com.vetclinic.pet.domain.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {

    /** Ownership check for the owner portal (CLAUDE.md rule 6): empty if the pet belongs to someone else. */
    Optional<Pet> findByIdAndOwnerId(Long id, Long ownerId);

    List<Pet> findAllByOwnerIdOrderByName(Long ownerId);

    List<Pet> findAllByOwnerIsNull();
}
