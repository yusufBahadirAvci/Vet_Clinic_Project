package com.vetclinic.pet.repository;

import com.vetclinic.pet.domain.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {

    /** Ownership check for the owner portal (CLAUDE.md rule 6): empty if the pet belongs to someone else. */
    Optional<Pet> findByIdAndOwnerId(Long id, Long ownerId);

    List<Pet> findAllByOwnerIdOrderByName(Long ownerId);

    List<Pet> findAllByOwnerIsNull();

    /** Pet list in the vet panel; the owner is fetched in the same query (no N+1). */
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Pet> findAll(Specification<Pet> spec, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    List<Pet> findTop5ByOrderByCreatedAtDescIdDesc();

    boolean existsByMicrochipNo(String microchipNo);

    boolean existsByMicrochipNoAndIdNot(String microchipNo, Long id);
}
