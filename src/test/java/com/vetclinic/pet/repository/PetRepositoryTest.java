package com.vetclinic.pet.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.vetclinic.support.TestData.owner;
import static com.vetclinic.support.TestData.pet;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PetRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(owner("Alice", "alice@example.com"));
        bob = userRepository.save(owner("Bob", "bob@example.com"));
    }

    @Test
    void findByIdAndOwnerIdReturnsOwnPet() {
        Pet max = petRepository.saveAndFlush(pet("Max", Species.DOG, alice));

        assertThat(petRepository.findByIdAndOwnerId(max.getId(), alice.getId())).contains(max);
    }

    @Test
    void findByIdAndOwnerIdIsEmptyForAnotherOwnersPet() {
        Pet max = petRepository.saveAndFlush(pet("Max", Species.DOG, alice));

        assertThat(petRepository.findByIdAndOwnerId(max.getId(), bob.getId())).isEmpty();
    }

    @Test
    void findByIdAndOwnerIdIsEmptyForPetWithoutOwner() {
        Pet stray = petRepository.saveAndFlush(pet("Stray", Species.CAT, null));

        assertThat(petRepository.findByIdAndOwnerId(stray.getId(), alice.getId())).isEmpty();
    }

    @Test
    void findAllByOwnerIdOrderByNameReturnsOnlyThatOwnersPetsSortedByName() {
        petRepository.save(pet("Rocky", Species.DOG, alice));
        petRepository.save(pet("Luna", Species.CAT, alice));
        petRepository.save(pet("Bella", Species.DOG, bob));
        petRepository.flush();

        assertThat(petRepository.findAllByOwnerIdOrderByName(alice.getId()))
                .extracting(Pet::getName)
                .containsExactly("Luna", "Rocky");
    }

    @Test
    void findAllByOwnerIsNullReturnsPetsWithoutOwner() {
        petRepository.save(pet("Max", Species.DOG, alice));
        petRepository.save(pet("Buddy", Species.DOG, null));
        petRepository.save(pet("Thumper", Species.RABBIT, null));
        petRepository.flush();

        assertThat(petRepository.findAllByOwnerIsNull())
                .extracting(Pet::getName)
                .containsExactlyInAnyOrder("Buddy", "Thumper");
    }

    @Test
    void createdAtIsFilledByTheDatabaseAndNeuteredDefaultsToFalse() {
        Pet max = petRepository.saveAndFlush(pet("Max", Species.DOG, alice));

        assertThat(max.getCreatedAt()).isNotNull();
        assertThat(max.isNeutered()).isFalse();
    }
}
