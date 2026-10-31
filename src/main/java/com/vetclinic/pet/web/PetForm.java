package com.vetclinic.pet.web;

import com.vetclinic.pet.domain.Gender;
import com.vetclinic.pet.domain.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Create/edit form for a pet. Format checks only; "birth date not in the future" (needs the Clock),
 * microchip uniqueness and the owner check are in {@code PetService}.
 */
@Getter
@Setter
@NoArgsConstructor
public class PetForm {

    @NotBlank(message = "Please enter the pet's name.")
    @Size(max = 50, message = "The name can be at most 50 characters.")
    private String name;

    @NotNull(message = "Please select the species.")
    private Species species;

    @Size(max = 60, message = "The breed can be at most 60 characters.")
    private String breed;

    @NotNull(message = "Please select the gender.")
    private Gender gender = Gender.UNKNOWN;

    @NotNull(message = "Please enter the birth date.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @Size(max = 40, message = "The color can be at most 40 characters.")
    private String color;

    @Size(max = 20, message = "The microchip number can be at most 20 characters.")
    @Pattern(regexp = "^\\s*[A-Za-z0-9]*\\s*$", message = "The microchip number can contain only letters and digits.")
    private String microchipNo;

    private boolean neutered;

    @Size(max = 500, message = "The photo URL can be at most 500 characters.")
    @Pattern(regexp = "^(\\s*|https?://\\S+)$", message = "The photo URL must start with http:// or https://.")
    private String photoUrl;

    /** null = no owner. */
    private Long ownerId;
}
