package com.vetclinic.pet.service;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.common.AgeFormatter;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.vaccination.VaccinationStatus;
import com.vetclinic.vaccination.VaccinationStatusCalculator;
import com.vetclinic.vaccination.domain.Vaccination;
import com.vetclinic.vaccination.domain.Vaccine;
import com.vetclinic.vaccination.repository.VaccinationRepository;
import com.vetclinic.vaccination.repository.VaccineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.vetclinic.support.TestData.owner;
import static com.vetclinic.support.TestData.pet;
import static com.vetclinic.support.TestData.vaccine;
import static com.vetclinic.support.TestData.vet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Only the latest dose per vaccine counts" is implemented twice: in {@link PetProfileFacade} (owner portal)
 * and in {@code VaccinationRepository.countPetsDueBetween} (vet dashboard). Both run here on the same data and
 * must agree on which pets have a current dose that is due soon.
 */
@Transactional
class LatestDoseRuleParityTest extends AbstractIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);
    private static final int DUE_SOON_DAYS = 30;

    @Autowired
    private VaccinationRepository vaccinationRepository;
    @Autowired
    private VaccineRepository vaccineRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void ownerPortalAndVetDashboardAgreeOnWhichPetsHaveACurrentDoseDueSoon() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
        PetProfileFacade facade = new PetProfileFacade(petRepository, vaccinationRepository,
                new VaccinationStatusCalculator(clock), new AgeFormatter(clock));

        User vet = userRepository.save(vet("Dr. Vet", "vet@example.com"));
        User owner = userRepository.save(owner("Owner", "owner@example.com"));
        Vaccine rabies = vaccineRepository.save(vaccine("Rabies", null));
        Vaccine combo = vaccineRepository.save(vaccine("Combo", null));

        Pet both = newPet("Both", owner);
        dose(both, rabies, vet, TODAY.minusDays(360), TODAY.plusDays(5));
        dose(both, combo, vet, TODAY.minusDays(365), TODAY);
        Pet superseded = newPet("Superseded", owner);
        dose(superseded, rabies, vet, TODAY.minusDays(362), TODAY.plusDays(3));
        dose(superseded, rabies, vet, TODAY.minusDays(1), TODAY.plusDays(364));
        Pet lastDay = newPet("LastDay", owner);
        dose(lastDay, rabies, vet, TODAY.minusDays(335), TODAY.plusDays(DUE_SOON_DAYS));
        Pet overdue = newPet("Overdue", owner);
        dose(overdue, rabies, vet, TODAY.minusDays(370), TODAY.minusDays(5));
        Pet later = newPet("Later", owner);
        dose(later, rabies, vet, TODAY.minusDays(300), TODAY.plusDays(DUE_SOON_DAYS + 1));
        Pet sameDay = newPet("SameDay", owner);
        dose(sameDay, rabies, vet, TODAY.minusDays(10), TODAY.plusDays(4));      // lower id, same day
        dose(sameDay, rabies, vet, TODAY.minusDays(10), TODAY.plusDays(300));    // higher id wins
        Pet supersededByNoSchedule = newPet("SingleDoseNow", owner);
        dose(supersededByNoSchedule, rabies, vet, TODAY.minusDays(350), TODAY.plusDays(10));
        dose(supersededByNoSchedule, rabies, vet, TODAY.minusDays(2), null);    // newer dose with no next date
        vaccinationRepository.flush();

        List<Pet> pets = List.of(both, superseded, lastDay, overdue, later, sameDay, supersededByNoSchedule);
        List<String> dueSoonInPortal = pets.stream()
                .filter(p -> facade.profileOf(p.getId(), owner.getId()).timeline().stream()
                        .anyMatch(e -> e.current() && e.status() == VaccinationStatus.DUE_SOON))
                .map(Pet::getName)
                .toList();

        long dueInDashboard = vaccinationRepository.countPetsDueBetween(TODAY, TODAY.plusDays(DUE_SOON_DAYS));

        assertThat(dueSoonInPortal).containsExactlyInAnyOrder("Both", "LastDay");
        assertThat(dueInDashboard).isEqualTo(dueSoonInPortal.size());
    }

    private Pet newPet(String name, User owner) {
        Pet pet = pet(name, Species.DOG, owner);
        pet.setBirthDate(TODAY.minusYears(2));
        return petRepository.save(pet);
    }

    private void dose(Pet pet, Vaccine vaccine, User vet, LocalDate administeredAt, LocalDate nextDueDate) {
        Vaccination v = new Vaccination();
        v.setPet(pet);
        v.setVaccine(vaccine);
        v.setVet(vet);
        v.setAdministeredAt(administeredAt);
        v.setNextDueDate(nextDueDate);
        vaccinationRepository.save(v);
    }
}
