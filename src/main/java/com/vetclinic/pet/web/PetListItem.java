package com.vetclinic.pet.web;

/** One row of the pet list in the vet panel. {@code ownerId}/{@code ownerName} are null for a pet without owner. */
public record PetListItem(Long id, String name, String species, String breed, String age, String microchipNo,
                          Long ownerId, String ownerName) {
}
