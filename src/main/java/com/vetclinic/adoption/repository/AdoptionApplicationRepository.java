package com.vetclinic.adoption.repository;

import com.vetclinic.adoption.domain.AdoptionApplication;
import com.vetclinic.adoption.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdoptionApplicationRepository extends JpaRepository<AdoptionApplication, Long> {

    List<AdoptionApplication> findAllByListingIdOrderByCreatedAt(Long listingId);

    List<AdoptionApplication> findAllByListingIdAndStatus(Long listingId, ApplicationStatus status);
}
