package com.vetclinic.adoption.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.adoption.domain.AdoptionApplication;
import com.vetclinic.adoption.domain.AdoptionListing;
import com.vetclinic.adoption.domain.ApplicationStatus;
import com.vetclinic.adoption.domain.ListingStatus;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static com.vetclinic.support.TestData.listing;
import static com.vetclinic.support.TestData.pet;
import static com.vetclinic.support.TestData.vet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AdoptionApplicationRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AdoptionApplicationRepository applicationRepository;

    @Autowired
    private AdoptionListingRepository listingRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AdoptionListing buddyListing;
    private AdoptionListing shadowListing;

    @BeforeEach
    void setUp() {
        User vet = userRepository.save(vet("Dr. Vet", "vet@example.com"));
        buddyListing = listingRepository.save(listing(petRepository.save(pet("Buddy", Species.DOG, null)), vet, ListingStatus.ACTIVE));
        shadowListing = listingRepository.save(listing(petRepository.save(pet("Shadow", Species.CAT, null)), vet, ListingStatus.ACTIVE));
    }

    @Test
    void findAllByListingIdOrderByCreatedAtReturnsOldestFirst() {
        AdoptionApplication second = save(buddyListing, "Second", ApplicationStatus.PENDING);
        AdoptionApplication first = save(buddyListing, "First", ApplicationStatus.PENDING);
        save(shadowListing, "Other listing", ApplicationStatus.PENDING);
        applicationRepository.flush();
        // created_at is DEFAULT now(), which is the same for the whole test transaction; set it explicitly.
        setCreatedAt(first, "2026-10-01T10:00:00+03:00");
        setCreatedAt(second, "2026-10-02T10:00:00+03:00");

        assertThat(applicationRepository.findAllByListingIdOrderByCreatedAt(buddyListing.getId()))
                .extracting(AdoptionApplication::getApplicantName)
                .containsExactly("First", "Second");
    }

    @Test
    void findAllByListingIdAndStatusFiltersByListingAndStatus() {
        AdoptionApplication pending = save(buddyListing, "Pending", ApplicationStatus.PENDING);
        save(buddyListing, "Rejected", ApplicationStatus.REJECTED);
        save(shadowListing, "Pending elsewhere", ApplicationStatus.PENDING);
        applicationRepository.flush();

        assertThat(applicationRepository.findAllByListingIdAndStatus(buddyListing.getId(), ApplicationStatus.PENDING))
                .containsExactly(pending);
    }

    @Test
    void applicationWithoutEmailAndPhoneIsRejected() {
        AdoptionApplication application = newApplication(buddyListing, "No Contact", ApplicationStatus.PENDING);
        application.setApplicantEmail(null);

        assertThatThrownBy(() -> applicationRepository.saveAndFlush(application))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AdoptionApplication save(AdoptionListing listing, String name, ApplicationStatus status) {
        return applicationRepository.save(newApplication(listing, name, status));
    }

    private static AdoptionApplication newApplication(AdoptionListing listing, String name, ApplicationStatus status) {
        AdoptionApplication application = new AdoptionApplication();
        application.setListing(listing);
        application.setApplicantName(name);
        application.setApplicantEmail("applicant@example.com");
        application.setKvkkConsent(true);
        application.setStatus(status);
        return application;
    }

    private void setCreatedAt(AdoptionApplication application, String timestamp) {
        jdbcTemplate.update("UPDATE adoption_applications SET created_at = ?::timestamptz WHERE id = ?",
                timestamp, application.getId());
    }
}
