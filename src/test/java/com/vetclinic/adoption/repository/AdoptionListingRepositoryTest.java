package com.vetclinic.adoption.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.adoption.domain.AdoptionListing;
import com.vetclinic.adoption.domain.ListingStatus;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;

import static com.vetclinic.support.TestData.listing;
import static com.vetclinic.support.TestData.pet;
import static com.vetclinic.support.TestData.vet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AdoptionListingRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AdoptionListingRepository listingRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User vet;

    @BeforeEach
    void setUp() {
        vet = userRepository.save(vet("Dr. Vet", "vet@example.com"));
    }

    @Test
    void findAllByStatusInReturnsOnlyMatchingStatuses() {
        AdoptionListing active = listingRepository.save(listing(newPet("Buddy"), vet, ListingStatus.ACTIVE));
        AdoptionListing reserved = listingRepository.save(listing(newPet("Daisy"), vet, ListingStatus.RESERVED));
        listingRepository.save(listing(newPet("Ginger"), vet, ListingStatus.DRAFT));
        listingRepository.save(listing(newPet("Shadow"), vet, ListingStatus.ADOPTED));
        listingRepository.flush();

        assertThat(listingRepository.findAllByStatusIn(EnumSet.of(ListingStatus.ACTIVE, ListingStatus.RESERVED)))
                .containsExactlyInAnyOrder(active, reserved);
    }

    @Test
    void findByPetIdReturnsTheListingOfThePet() {
        Pet buddy = newPet("Buddy");
        AdoptionListing listing = listingRepository.saveAndFlush(listing(buddy, vet, ListingStatus.ACTIVE));

        assertThat(listingRepository.findByPetId(buddy.getId())).contains(listing);
        assertThat(listingRepository.findByPetId(newPet("Other").getId())).isEmpty();
    }

    @Test
    void secondListingForTheSamePetIsRejected() {
        Pet buddy = newPet("Buddy");
        listingRepository.saveAndFlush(listing(buddy, vet, ListingStatus.CLOSED));

        assertThatThrownBy(() -> listingRepository.saveAndFlush(listing(buddy, vet, ListingStatus.DRAFT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatedAtIsFilledOnInsertAndAValueSetByTheServiceIsSaved() {
        AdoptionListing listing = listingRepository.saveAndFlush(listing(newPet("Buddy"), vet, ListingStatus.DRAFT));
        assertThat(listing.getCreatedAt()).isNotNull();
        assertThat(listing.getUpdatedAt()).isNotNull();

        OffsetDateTime later = OffsetDateTime.of(2030, 1, 15, 10, 30, 0, 0, ZoneOffset.ofHours(3));
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setUpdatedAt(later);
        listingRepository.saveAndFlush(listing);
        entityManager.clear();

        AdoptionListing reloaded = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(reloaded.getUpdatedAt()).isAtSameInstantAs(later);
    }

    private Pet newPet(String name) {
        return petRepository.save(pet(name, Species.DOG, null));
    }
}
