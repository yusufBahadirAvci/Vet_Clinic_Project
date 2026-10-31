package com.vetclinic.common.service;

import com.vetclinic.adoption.domain.ApplicationStatus;
import com.vetclinic.adoption.repository.AdoptionApplicationRepository;
import com.vetclinic.common.web.DashboardStats;
import com.vetclinic.common.web.EnumLabels;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.vaccination.repository.VaccinationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** Builds the vet dashboard from the user, pet, vaccination and adoption repositories. */
@Service
public class DashboardService {

    /** "Vaccination due soon" card: doses due from today up to this many days ahead (overdue ones not counted). */
    static final int DUE_SOON_DAYS = 7;

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final VaccinationRepository vaccinationRepository;
    private final AdoptionApplicationRepository applicationRepository;
    private final Clock clock;

    public DashboardService(UserRepository userRepository, PetRepository petRepository,
                            VaccinationRepository vaccinationRepository,
                            AdoptionApplicationRepository applicationRepository, Clock clock) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        LocalDate today = LocalDate.now(clock);
        var recentPets = petRepository.findTop5ByOrderByCreatedAtDescIdDesc().stream()
                .map(p -> new DashboardStats.RecentPet(p.getId(), p.getName(), EnumLabels.of(p.getSpecies()),
                        p.getOwner() == null ? null : p.getOwner().getFullName(),
                        p.getCreatedAt() == null ? null
                                : p.getCreatedAt().atZoneSameInstant(clock.getZone()).toLocalDate()))
                .toList();
        return new DashboardStats(
                userRepository.countByRoleAndActiveTrue(Role.OWNER),
                petRepository.count(),
                vaccinationRepository.countPetsDueBetween(today, today.plusDays(DUE_SOON_DAYS)),
                applicationRepository.countByStatus(ApplicationStatus.PENDING),
                recentPets);
    }
}
