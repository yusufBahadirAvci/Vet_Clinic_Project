package com.vetclinic.user.web;

/** One row of the owner list in the vet panel. */
public record OwnerListItem(Long id, String fullName, String email, String phone, boolean active) {
}
