package com.vetclinic.pet.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    void specificationsFilterBySpeciesOwnershipAndNameOrMicrochip() {
        Pet max = pet("Max", Species.DOG, alice);
        max.setMicrochipNo("792097000100001");
        petRepository.save(max);
        petRepository.save(pet("Maxi", Species.CAT, null));
        petRepository.save(pet("Bella", Species.DOG, bob));
        petRepository.flush();
        var byName = Sort.by("name");

        assertThat(petRepository.findAll(PetSpecifications.hasSpecies(Species.DOG), PageRequest.of(0, 20, byName)))
                .extracting(Pet::getName).containsExactly("Bella", "Max");
        assertThat(petRepository.findAll(PetSpecifications.hasNoOwner(), PageRequest.of(0, 20, byName)))
                .extracting(Pet::getName).containsExactly("Maxi");
        assertThat(petRepository.findAll(PetSpecifications.hasOwner()
                        .and(PetSpecifications.nameOrMicrochipContains("MAX")), PageRequest.of(0, 20, byName)))
                .extracting(Pet::getName).containsExactly("Max");
        assertThat(petRepository.findAll(PetSpecifications.nameOrMicrochipContains("0100001"),
                PageRequest.of(0, 20, byName)))
                .extracting(Pet::getName).containsExactly("Max");
    }

    @Test
    void microchipExistsChecksCanExcludeThePetItself() {
        Pet max = pet("Max", Species.DOG, alice);
        max.setMicrochipNo("792097000100001");
        max = petRepository.saveAndFlush(max);

        assertThat(petRepository.existsByMicrochipNo("792097000100001")).isTrue();
        assertThat(petRepository.existsByMicrochipNoAndIdNot("792097000100001", max.getId())).isFalse();
        assertThat(petRepository.existsByMicrochipNoAndIdNot("792097000100001", max.getId() + 1)).isTrue();
    }

    @Test
    void findTop5ReturnsNewestFirst() {
        for (int i = 1; i <= 6; i++) {
            petRepository.save(pet("Pet" + i, Species.CAT, null));
        }
        petRepository.flush();

        // same transaction -> same created_at; the id breaks the tie
        assertThat(petRepository.findTop5ByOrderByCreatedAtDescIdDesc())
                .extracting(Pet::getName).containsExactly("Pet6", "Pet5", "Pet4", "Pet3", "Pet2");
    }

    @Test
    void createdAtIsFilledByTheDatabaseAndNeuteredDefaultsToFalse() {
        Pet max = petRepository.saveAndFlush(pet("Max", Species.DOG, alice));

        assertThat(max.getCreatedAt()).isNotNull();
        assertThat(max.isNeutered()).isFalse();
    }
}
