package com.vetclinic.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vetclinic.common.AgeFormatter;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.support.TestData;
import com.vetclinic.user.domain.User;
import com.vetclinic.vaccination.VaccinationStatus;
import com.vetclinic.vaccination.VaccinationStatusCalculator;
import com.vetclinic.vaccination.domain.Vaccination;
import com.vetclinic.vaccination.domain.Vaccine;
import com.vetclinic.vaccination.repository.VaccinationRepository;

class PetProfileFacadeTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);
    private static final Long OWNER_ID = 10L;
    private static final Long PET_ID = 1L;

    private final PetRepository petRepository = mock(PetRepository.class);
    private final VaccinationRepository vaccinationRepository = mock(VaccinationRepository.class);
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
    private final PetProfileFacade facade = new PetProfileFacade(petRepository, vaccinationRepository,
            new VaccinationStatusCalculator(clock), new AgeFormatter(clock));

    private final User vet = vet();
    private final Vaccine rabies = vaccine(1L, "Rabies");
    private final Vaccine distemper = vaccine(2L, "Distemper");

    @Test
    void onlyTheLatestRecordOfAVaccineCounts() {
        Pet pet = pet();
        // The old dose is long overdue, but a newer rabies dose replaced it.
        Vaccination old = dose(1L, pet, rabies, TODAY.minusYears(2), TODAY.minusYears(1));
        Vaccination latest = dose(2L, pet, rabies, TODAY.minusMonths(1), TODAY.plusMonths(11));
        givenPetWith(pet, latest, old);

        PetProfileDto profile = facade.profileOf(PET_ID, OWNER_ID);

        assertThat(profile.overallStatus()).isEqualTo(VaccinationStatus.OK);
        assertThat(profile.upcomingDoses()).isEmpty();
        assertThat(profile.timeline()).hasSize(2);
        assertThat(profile.timeline().get(0).current()).isTrue();
        assertThat(profile.timeline().get(0).status()).isEqualTo(VaccinationStatus.OK);
        assertThat(profile.timeline().get(1).current()).isFalse();
        assertThat(profile.timeline().get(1).status()).isEqualTo(VaccinationStatus.NONE);
    }

    @Test
    void timelineIsNewestFirst() {
        Pet pet = pet();
        Vaccination older = dose(1L, pet, rabies, TODAY.minusYears(1), TODAY.plusDays(100));
        Vaccination newer = dose(2L, pet, distemper, TODAY.minusDays(5), TODAY.plusDays(200));
        givenPetWith(pet, older, newer);

        PetProfileDto profile = facade.profileOf(PET_ID, OWNER_ID);

        assertThat(profile.timeline()).extracting(PetProfileDto.VaccinationEntry::vaccineName)
                .containsExactly("Distemper", "Rabies");
    }

    @Test
    void overdueAndDueSoonDosesAreListedMostUrgentFirst() {
        Pet pet = pet();
        Vaccination dueSoon = dose(1L, pet, rabies, TODAY.minusMonths(11), TODAY.plusDays(7));
        Vaccination overdue = dose(2L, pet, distemper, TODAY.minusYears(1), TODAY.minusDays(3));
        givenPetWith(pet, dueSoon, overdue);

        PetProfileDto profile = facade.profileOf(PET_ID, OWNER_ID);

        assertThat(profile.overallStatus()).isEqualTo(VaccinationStatus.OVERDUE);
        assertThat(profile.upcomingDoses()).extracting(PetProfileDto.VaccinationEntry::vaccineName)
                .containsExactly("Distemper", "Rabies");
        assertThat(profile.upcomingDoses()).extracting(PetProfileDto.VaccinationEntry::status)
                .containsExactly(VaccinationStatus.OVERDUE, VaccinationStatus.DUE_SOON);
    }

    @Test
    void dueSoonOverallStatusWhenNearestDoseIsWithinThirtyDays() {
        Pet pet = pet();
        givenPetWith(pet, dose(1L, pet, rabies, TODAY.minusMonths(11), TODAY.plusDays(7)));

        assertThat(facade.profileOf(PET_ID, OWNER_ID).overallStatus()).isEqualTo(VaccinationStatus.DUE_SOON);
    }

    @Test
    void petWithoutVaccinationsHasNoStatusAndEmptyLists() {
        Pet pet = pet();
        givenPetWith(pet);

        PetProfileDto profile = facade.profileOf(PET_ID, OWNER_ID);

        assertThat(profile.overallStatus()).isEqualTo(VaccinationStatus.NONE);
        assertThat(profile.timeline()).isEmpty();
        assertThat(profile.upcomingDoses()).isEmpty();
    }

    @Test
    void identityFieldsAreMapped() {
        Pet pet = pet();
        pet.setBreed("Labrador");
        pet.setBirthDate(TODAY.minusMonths(3));
        givenPetWith(pet);

        PetProfileDto profile = facade.profileOf(PET_ID, OWNER_ID);

        assertThat(profile.name()).isEqualTo("Rex");
        assertThat(profile.speciesLabel()).isEqualTo("Dog");
        assertThat(profile.genderLabel()).isEqualTo("Unknown");
        assertThat(profile.age()).isEqualTo("3 months");
    }

    @Test
    void someoneElsesPetIsNotFound() {
        when(petRepository.findByIdAndOwnerId(PET_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.profileOf(PET_ID, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listForOwnerShowsTheNearestDoseStatusPerPet() {
        Pet pet = pet();
        when(petRepository.findAllByOwnerIdOrderByName(OWNER_ID)).thenReturn(List.of(pet));
        when(vaccinationRepository.findAllByPetIdOrderByAdministeredAtDesc(PET_ID))
                .thenReturn(List.of(dose(1L, pet, rabies, TODAY.minusYears(1), TODAY.minusDays(1))));

        List<PetSummaryDto> pets = facade.listForOwner(OWNER_ID);

        assertThat(pets).hasSize(1);
        assertThat(pets.get(0).status()).isEqualTo(VaccinationStatus.OVERDUE);
        assertThat(pets.get(0).name()).isEqualTo("Rex");
    }

    private void givenPetWith(Pet pet, Vaccination... vaccinations) {
        when(petRepository.findByIdAndOwnerId(PET_ID, OWNER_ID)).thenReturn(Optional.of(pet));
        when(vaccinationRepository.findAllByPetIdOrderByAdministeredAtDesc(PET_ID)).thenReturn(List.of(vaccinations));
    }

    private Pet pet() {
        Pet pet = TestData.pet("Rex", Species.DOG, TestData.owner("Owner", "owner@example.com"));
        pet.setId(PET_ID);
        pet.setBirthDate(TODAY.minusYears(3));
        return pet;
    }

    private User vet() {
        User user = TestData.vet("Dr. Vet", "vet@example.com");
        user.setId(5L);
        return user;
    }

    private static Vaccine vaccine(Long id, String name) {
        Vaccine vaccine = TestData.vaccine(name, Species.DOG);
        vaccine.setId(id);
        return vaccine;
    }

    private Vaccination dose(Long id, Pet pet, Vaccine vaccine, LocalDate administeredAt, LocalDate nextDueDate) {
        Vaccination v = new Vaccination();
        v.setId(id);
        v.setPet(pet);
        v.setVaccine(vaccine);
        v.setAdministeredAt(administeredAt);
        v.setNextDueDate(nextDueDate);
        v.setVet(vet);
        return v;
    }
}
