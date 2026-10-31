package com.vetclinic.pet.web;

import java.time.LocalDate;

/** Pet detail page in the vet panel. The owner fields are null when the pet has no owner. */
public record PetDetails(Long id, String name, String species, String breed, String gender, LocalDate birthDate,
                         String age, String color, String microchipNo, boolean neutered, String photoUrl,
                         Long ownerId, String ownerName, String ownerEmail, String ownerPhone) {

    public boolean ownerless() {
        return ownerId == null;
    }
}
