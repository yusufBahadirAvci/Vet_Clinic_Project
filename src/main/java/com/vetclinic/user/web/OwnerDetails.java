package com.vetclinic.user.web;

import java.time.LocalDate;
import java.util.List;

/** Owner detail page in the vet panel: contact details and the owner's pets. */
public record OwnerDetails(Long id, String fullName, String email, String phone, boolean active,
                           LocalDate kvkkConsentDate, List<OwnerPet> pets) {

    public record OwnerPet(Long id, String name, String species, String breed, String age) {
    }
}
