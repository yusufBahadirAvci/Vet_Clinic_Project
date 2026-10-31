package com.vetclinic.pet.web;

import com.vetclinic.user.domain.User;

/** The owner's own contact details for the read-only profile page. */
public record OwnerProfileDto(String fullName, String email, String phone) {

    public static OwnerProfileDto from(User user) {
        return new OwnerProfileDto(user.getFullName(), user.getEmail(), user.getPhone());
    }
}
