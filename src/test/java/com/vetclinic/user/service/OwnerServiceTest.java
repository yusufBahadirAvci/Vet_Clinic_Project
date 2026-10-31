package com.vetclinic.user.service;

import com.vetclinic.common.AgeFormatter;
import com.vetclinic.common.ContactNormalizer;
import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.user.web.OwnerForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
class OwnerServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-31T09:00:00Z"), ZoneId.of("Europe/Istanbul"));

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepository petRepository;

    private OwnerService ownerService;

    @BeforeEach
    void setUp() {
        ownerService = new OwnerService(userRepository, petRepository, new ContactNormalizer(),
                new AgeFormatter(CLOCK), CLOCK);
    }

    @Test
    void createStoresNormalizedContactsAndConsentTimeFromClock() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        Long id = ownerService.create(form("  Ayşe Demir ", " Ayse.Demir@Example.COM ", "0532 123 45 67"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User owner = saved.getValue();
        assertThat(id).isEqualTo(42L);
        assertThat(owner.getFullName()).isEqualTo("Ayşe Demir");
        assertThat(owner.getEmail()).isEqualTo("ayse.demir@example.com");
        assertThat(owner.getPhone()).isEqualTo("+905321234567");
        assertThat(owner.getRole()).isEqualTo(Role.OWNER);
        assertThat(owner.isActive()).isTrue();
        assertThat(owner.getKvkkConsentAt()).isEqualTo(OffsetDateTime.now(CLOCK));
    }

    @Test
    void createWithPhoneOnlyLeavesEmailNull() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ownerService.create(form("Zeynep", "   ", "+90 533 444 55 66"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isNull();
        assertThat(saved.getValue().getPhone()).isEqualTo("+905334445566");
    }

    @Test
    void duplicatePhoneGivesPhoneFieldError() {
        when(userRepository.existsByPhone("+905321234567")).thenReturn(true);

        assertThatThrownBy(() -> ownerService.create(form("Ali", null, "0532 123 45 67")))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsExactly(java.util.Map.entry("phone", OwnerService.PHONE_TAKEN));
        verify(userRepository, never()).save(any());
    }

    @Test
    void duplicateEmailGivesEmailFieldError() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> ownerService.create(form("Ali", "TAKEN@example.com", null)))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsEntry("email", OwnerService.EMAIL_TAKEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void missingBothContactsIsRejected() {
        assertThatThrownBy(() -> ownerService.create(form("Ali", "", " ")))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsEntry("email", OwnerService.CONTACT_REQUIRED)
                .containsEntry("phone", OwnerService.CONTACT_REQUIRED);
    }

    @Test
    void invalidPhoneGivesPhoneFieldError() {
        assertThatThrownBy(() -> ownerService.create(form("Ali", null, "0212 123 45 67")))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsOnlyKeys("phone");
    }

    @Test
    void missingKvkkConsentIsRejected() {
        OwnerForm form = form("Ali", "ali@example.com", null);
        form.setKvkkConsent(false);

        assertThatThrownBy(() -> ownerService.create(form))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsOnlyKeys("kvkkConsent");
    }

    @Test
    void updateDoesNotCountOwnContactsAsDuplicateAndKeepsConsentTime() {
        OffsetDateTime consentedAt = OffsetDateTime.parse("2025-01-01T10:00:00+03:00");
        User existing = owner(7L, "ali@example.com", "+905321234567");
        existing.setKvkkConsentAt(consentedAt);
        when(userRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("ali@example.com", 7L)).thenReturn(false);
        when(userRepository.existsByPhoneAndIdNot("+905321234567", 7L)).thenReturn(false);

        ownerService.update(7L, form("Ali Veli", "ali@example.com", "05321234567"));

        assertThat(existing.getFullName()).isEqualTo("Ali Veli");
        assertThat(existing.getKvkkConsentAt()).isEqualTo(consentedAt);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByPhone(anyString());
    }

    @Test
    void updateWithPhoneOfAnotherUserGivesPhoneFieldError() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner(7L, "ali@example.com", null)));
        when(userRepository.existsByEmailAndIdNot("ali@example.com", 7L)).thenReturn(false);
        when(userRepository.existsByPhoneAndIdNot("+905334445566", 7L)).thenReturn(true);

        assertThatThrownBy(() -> ownerService.update(7L, form("Ali", "ali@example.com", "0533 444 55 66")))
                .isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFieldErrors(), MAP)
                .containsOnlyKeys("phone");
    }

    @Test
    void deactivateAndReactivateToggleActiveFlag() {
        User owner = owner(7L, "ali@example.com", null);
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));

        ownerService.deactivate(7L);
        assertThat(owner.isActive()).isFalse();

        ownerService.reactivate(7L);
        assertThat(owner.isActive()).isTrue();
    }

    @Test
    void missingOwnerIsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerService.getDetails(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> ownerService.deactivate(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> ownerService.reactivate(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void vetIsNotTreatedAsOwner() {
        User vet = owner(1L, "vet@example.com", null);
        vet.setRole(Role.VET);
        when(userRepository.findById(1L)).thenReturn(Optional.of(vet));

        assertThatThrownBy(() -> ownerService.getForm(1L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> ownerService.deactivate(1L)).isInstanceOf(NotFoundException.class);
        assertThat(vet.isActive()).isTrue();
    }

    private static OwnerForm form(String fullName, String email, String phone) {
        OwnerForm form = new OwnerForm();
        form.setFullName(fullName);
        form.setEmail(email);
        form.setPhone(phone);
        form.setKvkkConsent(true);
        return form;
    }

    private static User owner(Long id, String email, String phone) {
        User user = new User();
        user.setId(id);
        user.setFullName("Owner " + id);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(Role.OWNER);
        return user;
    }
}
