package com.vetclinic.common.web;

import java.time.LocalDate;
import java.util.List;

/** Numbers and the recent pets shown on the vet dashboard (/vet). */
public record DashboardStats(long ownerCount, long petCount, long petsDueSoonCount, long pendingApplicationCount,
                             List<RecentPet> recentPets) {

    /** {@code ownerName} is null for a pet without owner. */
    public record RecentPet(Long id, String name, String species, String ownerName, LocalDate addedOn) {
    }
}
