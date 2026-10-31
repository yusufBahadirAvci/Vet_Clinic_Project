package com.vetclinic.common.service;

import com.vetclinic.adoption.domain.ApplicationStatus;
import com.vetclinic.adoption.repository.AdoptionApplicationRepository;
import com.vetclinic.common.web.DashboardStats;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.vaccination.repository.VaccinationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.vetclinic.support.TestData.owner;
import static com.vetclinic.support.TestData.pet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    /** 2026-10-31 00:30 in Istanbul (still 2026-10-30 in UTC). */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-30T21:30:00Z"), ZoneId.of("Europe/Istanbul"));

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private VaccinationRepository vaccinationRepository;
    @Mock
    private AdoptionApplicationRepository applicationRepository;

    @Test
    void collectsCountsAndRecentPets() {
        Pet max = pet("Max", Species.DOG, owner("Ayşe Demir", "ayse@example.com"));
        max.setId(1L);
        max.setCreatedAt(OffsetDateTime.parse("2026-10-30T22:00:00Z"));
        Pet buddy = pet("Buddy", Species.DOG, null);
        buddy.setId(2L);

        when(userRepository.countByRoleAndActiveTrue(Role.OWNER)).thenReturn(6L);
        when(petRepository.count()).thenReturn(15L);
        when(vaccinationRepository.countPetsDueBetween(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 7)))
                .thenReturn(3L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING)).thenReturn(2L);
        when(petRepository.findTop5ByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(max, buddy));

        DashboardStats stats = new DashboardService(userRepository, petRepository, vaccinationRepository,
                applicationRepository, CLOCK).getStats();

        assertThat(stats.ownerCount()).isEqualTo(6);
        assertThat(stats.petCount()).isEqualTo(15);
        assertThat(stats.petsDueSoonCount()).isEqualTo(3);
        assertThat(stats.pendingApplicationCount()).isEqualTo(2);
        assertThat(stats.recentPets()).containsExactly(
                new DashboardStats.RecentPet(1L, "Max", "Dog", "Ayşe Demir", LocalDate.of(2026, 10, 31)),
                new DashboardStats.RecentPet(2L, "Buddy", "Dog", null, null));
    }
}
