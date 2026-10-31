package com.vetclinic.adoption.repository;

import com.vetclinic.adoption.domain.AdoptionListing;
import com.vetclinic.adoption.domain.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdoptionListingRepository extends JpaRepository<AdoptionListing, Long> {

    List<AdoptionListing> findAllByStatusIn(Collection<ListingStatus> statuses);

    Optional<AdoptionListing> findByPetId(Long petId);
}
