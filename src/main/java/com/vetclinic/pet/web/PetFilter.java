package com.vetclinic.pet.web;

import com.vetclinic.pet.domain.Species;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Filters of the pet list (query parameters of GET /vet/pets). */
@Getter
@Setter
@NoArgsConstructor
public class PetFilter {

    public enum Ownership { ALL, OWNED, OWNERLESS }

    /** null = all species. */
    private Species species;

    private Ownership ownership = Ownership.ALL;

    /** Searches the name and the microchip number. */
    private String q;

    private int page;
}
