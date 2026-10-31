package com.vetclinic.pet.web;

import com.vetclinic.auth.SecurityConfig;
import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.pet.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vetclinic.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "VET")
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PetService petService;

    @Test
    void futureBirthDateIsShownOnTheBirthDateField() throws Exception {
        when(petService.create(any(PetForm.class)))
                .thenThrow(new FieldValidationException("birthDate", "The birth date cannot be in the future."));

        mockMvc.perform(validPet())
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("petForm", "birthDate"))
                .andExpect(content().string(containsString("The birth date cannot be in the future.")));
    }

    @Test
    void duplicateMicrochipIsShownOnTheMicrochipField() throws Exception {
        when(petService.create(any(PetForm.class)))
                .thenThrow(new FieldValidationException("microchipNo", "This microchip number is used by another pet."));

        mockMvc.perform(validPet().param("microchipNo", "792097000100001"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("petForm", "microchipNo"))
                .andExpect(content().string(containsString("This microchip number is used by another pet.")));
    }

    @Test
    void missingRequiredFieldsAndBadValuesAreFieldErrors() throws Exception {
        mockMvc.perform(post("/vet/pets/new").with(csrf())
                        .param("name", "").param("birthDate", "not-a-date")
                        .param("photoUrl", "javascript:alert(1)").param("microchipNo", "12-34"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("petForm", "name", "species", "birthDate", "photoUrl",
                        "microchipNo"))
                .andExpect(content().string(containsString("Please enter a valid date.")));
        verify(petService, never()).create(any());
    }

    @Test
    void newFormHasNoOwnerOption() throws Exception {
        when(petService.newForm(3L)).thenReturn(new PetForm());
        when(petService.ownerOptions(any())).thenReturn(List.of(new OwnerOption(3L, "Ayşe Demir (ayse@example.com)")));

        mockMvc.perform(get("/vet/pets/new").param("ownerId", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No owner")))
                .andExpect(content().string(containsString("Ayşe Demir (ayse@example.com)")));
    }

    @Test
    void nonNumericOrMissingIdShowsThe404Page() throws Exception {
        when(petService.getDetails(99L)).thenThrow(new NotFoundException("Pet not found: 99"));
        when(petService.getForm(99L)).thenThrow(new NotFoundException("Pet not found: 99"));

        mockMvc.perform(get("/vet/pets/abc"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
        mockMvc.perform(get("/vet/pets/99")).andExpect(status().isNotFound());
        mockMvc.perform(get("/vet/pets/99/edit")).andExpect(status().isNotFound());
        mockMvc.perform(get("/vet/pets/abc/edit")).andExpect(status().isNotFound());
    }

    @Test
    void adoptionButtonIsShownOnlyForPetsWithoutOwner() throws Exception {
        when(petService.getDetails(1L)).thenReturn(details(1L, null));
        when(petService.getDetails(2L)).thenReturn(details(2L, 3L));

        mockMvc.perform(get("/vet/pets/1"))
                .andExpect(content().string(containsString("Create adoption listing")))
                .andExpect(content().string(containsString("2 years")))
                .andExpect(content().string(containsString("No vaccination records yet.")));
        mockMvc.perform(get("/vet/pets/2"))
                .andExpect(content().string(not(containsString("Create adoption listing"))));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/vet/pets")).andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validPet() {
        return post("/vet/pets/new").with(csrf())
                .param("name", "Max").param("species", "DOG").param("gender", "MALE")
                .param("birthDate", "2022-05-01");
    }

    private static PetDetails details(Long id, Long ownerId) {
        return new PetDetails(id, "Buddy", "Dog", "Mixed breed", "Male", LocalDate.of(2024, 10, 31), "2 years",
                "Brown", null, true, null, ownerId, ownerId == null ? null : "Ayşe Demir",
                ownerId == null ? null : "ayse@example.com", null);
    }
}
