package com.vetclinic.pet.service;

import com.vetclinic.common.AgeFormatter;
import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.pet.domain.Gender;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.pet.web.OwnerOption;
import com.vetclinic.pet.web.PetDetails;
import com.vetclinic.pet.web.PetForm;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    /** 2026-10-31 in Istanbul. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-31T09:00:00Z"), ZoneId.of("Europe/Istanbul"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Mock
    private PetRepository petRepository;

    @Mock
    private UserRepository userRepository;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(petRepository, userRepository, new AgeFormatter(CLOCK), CLOCK);
    }

    @Test
    void birthDateInTheFutureGivesFieldError() {
        PetForm form = form("Max");
        form.setBirthDate(TODAY.plusDays(1));

        assertThatThrownBy(() -> petService.create(form))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsEntry("birthDate", PetService.BIRTH_DATE_IN_FUTURE);
        verify(petRepository, never()).save(any());
    }

    @Test
    void birthDateTodayIsAllowed() {
        PetForm form = form("Newborn");
        form.setBirthDate(TODAY);
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

        petService.create(form);

        verify(petRepository).save(any(Pet.class));
    }

    @Test
    void duplicateMicrochipGivesFieldError() {
        PetForm form = form("Max");
        form.setMicrochipNo(" 792097000100001 ");
        when(petRepository.existsByMicrochipNo("792097000100001")).thenReturn(true);

        assertThatThrownBy(() -> petService.create(form))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsEntry("microchipNo", PetService.MICROCHIP_TAKEN);
        verify(petRepository, never()).save(any());
    }

    @Test
    void editingKeepsOwnMicrochip() {
        Pet existing = pet(5L, null);
        existing.setMicrochipNo("ABC123");
        when(petRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(petRepository.existsByMicrochipNoAndIdNot("ABC123", 5L)).thenReturn(false);
        PetForm form = form("Max");
        form.setMicrochipNo("abc123");

        petService.update(5L, form);

        assertThat(existing.getMicrochipNo()).isEqualTo("ABC123");
        verify(petRepository, never()).existsByMicrochipNo(anyString());
    }

    @Test
    void ownerlessPetIsSavedWithBlankOptionalFieldsAsNull() {
        PetForm form = form(" Buddy ");
        form.setBreed("  ");
        form.setColor("");
        form.setMicrochipNo(" ");
        form.setPhotoUrl("");
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> {
            Pet p = inv.getArgument(0);
            p.setId(11L);
            return p;
        });

        Long id = petService.create(form);

        ArgumentCaptor<Pet> saved = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(saved.capture());
        Pet pet = saved.getValue();
        assertThat(id).isEqualTo(11L);
        assertThat(pet.getName()).isEqualTo("Buddy");
        assertThat(pet.getOwner()).isNull();
        assertThat(pet.getBreed()).isNull();
        assertThat(pet.getColor()).isNull();
        assertThat(pet.getMicrochipNo()).isNull();
        assertThat(pet.getPhotoUrl()).isNull();
    }

    @Test
    void petIsLinkedToSelectedOwner() {
        User owner = user(3L, Role.OWNER, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));
        PetForm form = form("Max");
        form.setOwnerId(3L);

        petService.create(form);

        ArgumentCaptor<Pet> saved = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(saved.capture());
        assertThat(saved.getValue().getOwner()).isSameAs(owner);
    }

    @Test
    void unknownOwnerOrVetAsOwnerGivesFieldError() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.VET, true)));

        PetForm unknown = form("Max");
        unknown.setOwnerId(99L);
        PetForm vet = form("Max");
        vet.setOwnerId(1L);

        assertThatThrownBy(() -> petService.create(unknown))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsEntry("ownerId", PetService.UNKNOWN_OWNER);
        assertThatThrownBy(() -> petService.create(vet))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsEntry("ownerId", PetService.UNKNOWN_OWNER);
    }

    @Test
    void inactiveOwnerCannotGetANewPetButKeepsAnExistingOne() {
        User inactive = user(3L, Role.OWNER, false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(inactive));
        PetForm form = form("Max");
        form.setOwnerId(3L);

        assertThatThrownBy(() -> petService.create(form))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsEntry("ownerId", PetService.INACTIVE_OWNER);

        Pet existing = pet(5L, inactive);
        when(petRepository.findById(5L)).thenReturn(Optional.of(existing));
        petService.update(5L, form);
        assertThat(existing.getOwner()).isSameAs(inactive);
    }

    @Test
    void newFormPreselectsActiveOwnerOnly() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, Role.OWNER, true)));
        when(userRepository.findById(4L)).thenReturn(Optional.of(user(4L, Role.OWNER, false)));

        assertThat(petService.newForm(3L).getOwnerId()).isEqualTo(3L);
        assertThat(petService.newForm(4L).getOwnerId()).isNull();
        assertThat(petService.newForm(null).getOwnerId()).isNull();
    }

    @Test
    void ownerOptionsContainActiveOwnersAndTheSelectedInactiveOne() {
        when(userRepository.findAllByRoleOrderByFullName(Role.OWNER)).thenReturn(List.of(
                user(3L, Role.OWNER, true), user(4L, Role.OWNER, false), user(5L, Role.OWNER, false)));

        assertThat(petService.ownerOptions(4L)).extracting(OwnerOption::id).containsExactly(3L, 4L);
        assertThat(petService.ownerOptions(null)).extracting(OwnerOption::id).containsExactly(3L);
    }

    @Test
    void detailsShowAgeAndLabels() {
        Pet pet = pet(5L, null);
        pet.setBirthDate(TODAY.minusMonths(3));
        when(petRepository.findById(5L)).thenReturn(Optional.of(pet));

        PetDetails details = petService.getDetails(5L);

        assertThat(details.age()).isEqualTo("3 months");
        assertThat(details.species()).isEqualTo("Dog");
        assertThat(details.ownerless()).isTrue();
    }

    @Test
    void missingPetIsNotFound() {
        when(petRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.getDetails(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> petService.update(99L, form("Max"))).isInstanceOf(NotFoundException.class);
    }

    private static PetForm form(String name) {
        PetForm form = new PetForm();
        form.setName(name);
        form.setSpecies(Species.DOG);
        form.setGender(Gender.MALE);
        form.setBirthDate(LocalDate.of(2022, 5, 1));
        return form;
    }

    private static Pet pet(Long id, User owner) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName("Max");
        pet.setSpecies(Species.DOG);
        pet.setGender(Gender.MALE);
        pet.setBirthDate(LocalDate.of(2022, 5, 1));
        pet.setOwner(owner);
        return pet;
    }

    private static User user(Long id, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFullName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
