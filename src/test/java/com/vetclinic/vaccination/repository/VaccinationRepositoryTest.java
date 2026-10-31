package com.vetclinic.vaccination.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.vaccination.domain.Vaccination;
import com.vetclinic.vaccination.domain.Vaccine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.vetclinic.support.TestData.owner;
import static com.vetclinic.support.TestData.pet;
import static com.vetclinic.support.TestData.vaccine;
import static com.vetclinic.support.TestData.vet;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class VaccinationRepositoryTest extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Autowired
    private VaccinationRepository vaccinationRepository;

    @Autowired
    private VaccineRepository vaccineRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    private User vet;
    private Pet max;
    private Pet luna;
    private Vaccine rabies;

    @BeforeEach
    void setUp() {
        vet = userRepository.save(vet("Dr. Vet", "vet@example.com"));
        User owner = userRepository.save(owner("Owner", "owner@example.com"));
        max = petRepository.save(pet("Max", Species.DOG, owner));
        luna = petRepository.save(pet("Luna", Species.CAT, owner));
        rabies = vaccineRepository.save(vaccine("Rabies", null));
    }

    @Test
    void findAllByPetIdOrderByAdministeredAtDescReturnsNewestFirst() {
        Vaccination oldest = save(max, TODAY.minusDays(700), TODAY.minusDays(335));
        Vaccination newest = save(max, TODAY.minusDays(10), TODAY.plusDays(355));
        Vaccination middle = save(max, TODAY.minusDays(335), TODAY.minusDays(10));
        save(luna, TODAY.minusDays(5), TODAY.plusDays(360));
        vaccinationRepository.flush();

        assertThat(vaccinationRepository.findAllByPetIdOrderByAdministeredAtDesc(max.getId()))
                .containsExactly(newest, middle, oldest);
    }

    @Test
    void findAllByNextDueDateBetweenIsInclusiveAndSkipsOutsideAndNull() {
        Vaccination dueToday = save(max, TODAY.minusDays(365), TODAY);
        Vaccination dueInSevenDays = save(luna, TODAY.minusDays(358), TODAY.plusDays(7));
        save(max, TODAY.minusDays(400), TODAY.minusDays(1));   // overdue, before the range
        save(luna, TODAY.minusDays(300), TODAY.plusDays(8));   // after the range
        save(max, TODAY.minusDays(30), null);                  // single dose
        vaccinationRepository.flush();

        assertThat(vaccinationRepository.findAllByNextDueDateBetween(TODAY, TODAY.plusDays(7)))
                .containsExactlyInAnyOrder(dueToday, dueInSevenDays);
    }

    @Test
    void countPetsDueBetweenCountsOnlyTheLatestDoseOfEachVaccineAndEachPetOnce() {
        Vaccine combo = vaccineRepository.save(vaccine("Dog Combo", Species.DOG));
        User owner = userRepository.save(owner("Other", "other@example.com"));
        Pet rocky = petRepository.save(pet("Rocky", Species.DOG, owner));
        Pet bella = petRepository.save(pet("Bella", Species.DOG, owner));
        Pet kiwi = petRepository.save(pet("Kiwi", Species.BIRD, owner));

        // Max: Rabies and Combo both due within 7 days -> Max is counted once
        save(max, rabies, TODAY.minusDays(360), TODAY.plusDays(5));
        save(max, combo, TODAY.minusDays(365), TODAY);
        // Luna: an old dose was due in 3 days but a newer dose of the same vaccine exists -> not counted
        save(luna, rabies, TODAY.minusDays(362), TODAY.plusDays(3));
        save(luna, rabies, TODAY.minusDays(1), TODAY.plusDays(364));
        // Rocky: due exactly on the last day of the range -> counted
        save(rocky, rabies, TODAY.minusDays(358), TODAY.plusDays(7));
        // Bella: overdue -> not counted; Kiwi: due after the range -> not counted
        save(bella, rabies, TODAY.minusDays(370), TODAY.minusDays(5));
        save(kiwi, rabies, TODAY.minusDays(300), TODAY.plusDays(8));
        vaccinationRepository.flush();

        assertThat(vaccinationRepository.countPetsDueBetween(TODAY, TODAY.plusDays(7))).isEqualTo(2);
    }

    private Vaccination save(Pet pet, LocalDate administeredAt, LocalDate nextDueDate) {
        return save(pet, rabies, administeredAt, nextDueDate);
    }

    private Vaccination save(Pet pet, Vaccine vaccine, LocalDate administeredAt, LocalDate nextDueDate) {
        Vaccination vaccination = new Vaccination();
        vaccination.setPet(pet);
        vaccination.setVaccine(vaccine);
        vaccination.setVet(vet);
        vaccination.setAdministeredAt(administeredAt);
        vaccination.setNextDueDate(nextDueDate);
        return vaccinationRepository.save(vaccination);
    }
}
