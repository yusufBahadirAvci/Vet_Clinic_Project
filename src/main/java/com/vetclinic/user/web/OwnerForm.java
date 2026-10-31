package com.vetclinic.user.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Create/edit form for an owner. Format checks only; normalization, "at least one contact" and
 * uniqueness are checked in {@code OwnerService}.
 */
@Getter
@Setter
@NoArgsConstructor
public class OwnerForm {

    @NotBlank(message = "Please enter the full name.")
    @Size(max = 100, message = "The full name can be at most 100 characters.")
    private String fullName;

    @Size(max = 254, message = "The e-mail address can be at most 254 characters.")
    private String email;

    @Size(max = 30, message = "The phone number is too long.")
    private String phone;

    @AssertTrue(message = "The owner's KVKK consent is required.")
    private boolean kvkkConsent;
}
