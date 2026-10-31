package com.vetclinic.pet.repository;

import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Query building blocks for the pet list in the vet panel. */
public final class PetSpecifications {

    private PetSpecifications() {
    }

    public static Specification<Pet> hasSpecies(Species species) {
        return (root, query, cb) -> cb.equal(root.get("species"), species);
    }

    public static Specification<Pet> hasOwner() {
        return (root, query, cb) -> cb.isNotNull(root.get("owner"));
    }

    public static Specification<Pet> hasNoOwner() {
        return (root, query, cb) -> cb.isNull(root.get("owner"));
    }

    /** Name contains the text (case-insensitive) or the microchip number contains it. */
    public static Specification<Pet> nameOrMicrochipContains(String text) {
        return (root, query, cb) -> {
            String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("microchipNo")), pattern, '\\'));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
