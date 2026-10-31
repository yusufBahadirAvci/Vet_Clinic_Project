package com.vetclinic.support;

import com.vetclinic.adoption.domain.AdoptionListing;
import com.vetclinic.adoption.domain.ListingStatus;
import com.vetclinic.pet.domain.Gender;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.vaccination.domain.Vaccine;

import java.time.LocalDate;

/** Factory methods for unsaved test entities with valid defaults. Tests save them with the repositories. */
public final class TestData {

    private TestData() {
    }

    public static User owner(String fullName, String email) {
        return user(fullName, email, Role.OWNER);
    }

    public static User vet(String fullName, String email) {
        return user(fullName, email, Role.VET);
    }

    private static User user(String fullName, String email, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    public static Pet pet(String name, Species species, User owner) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setSpecies(species);
        pet.setGender(Gender.UNKNOWN);
        pet.setBirthDate(LocalDate.of(2022, 5, 1));
        pet.setOwner(owner);
        return pet;
    }

    public static Vaccine vaccine(String name, Species targetSpecies) {
        Vaccine vaccine = new Vaccine();
        vaccine.setName(name);
        vaccine.setTargetSpecies(targetSpecies);
        vaccine.setRepeatIntervalDays(365);
        return vaccine;
    }

    public static AdoptionListing listing(Pet pet, User createdBy, ListingStatus status) {
        AdoptionListing listing = new AdoptionListing();
        listing.setPet(pet);
        listing.setTitle("Listing for " + pet.getName());
        listing.setDescription("A lovely pet looking for a home.");
        listing.setStatus(status);
        listing.setCreatedBy(createdBy);
        return listing;
    }
}
