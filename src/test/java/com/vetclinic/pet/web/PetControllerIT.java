package com.vetclinic.pet.web;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.vetclinic.support.TestData.owner;
import static com.vetclinic.support.TestData.pet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full stack (MockMvc + real PostgreSQL): the vet adds a pet and finds it in the list. Rolled back after each test. */
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "VET")
class PetControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void vetAddsPetAndItShowsInTheList() throws Exception {
        User ayse = userRepository.save(owner("Ayşe Demir", "ayse@example.com"));

        mockMvc.perform(post("/vet/pets/new").with(csrf())
                        .param("name", "Pamuk").param("species", "CAT").param("gender", "FEMALE")
                        .param("birthDate", "2024-03-15").param("microchipNo", "900000000000123")
                        .param("ownerId", ayse.getId().toString()))
                .andExpect(status().is3xxRedirection());

        Pet saved = petRepository.findAll().stream().filter(p -> p.getName().equals("Pamuk")).findFirst().orElseThrow();
        assertThat(saved.getOwner().getId()).isEqualTo(ayse.getId());

        mockMvc.perform(get("/vet/pets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pamuk")))
                .andExpect(content().string(containsString("Ayşe Demir")));

        mockMvc.perform(get("/vet/pets").param("q", "900000000000123"))
                .andExpect(content().string(containsString("Pamuk")));
        mockMvc.perform(get("/vet/pets").param("ownership", "OWNERLESS"))
                .andExpect(content().string(not(containsString("Pamuk"))));
        mockMvc.perform(get("/vet/pets").param("species", "DOG"))
                .andExpect(content().string(not(containsString("Pamuk"))));

        mockMvc.perform(get("/vet/pets/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pamuk")));
    }

    @Test
    void duplicateMicrochipIsAFieldErrorAndNothingIsSaved() throws Exception {
        Pet existing = pet("Max", Species.DOG, null);
        existing.setMicrochipNo("792097000100001");
        petRepository.saveAndFlush(existing);
        long before = petRepository.count();

        mockMvc.perform(post("/vet/pets/new").with(csrf())
                        .param("name", "Copy").param("species", "DOG").param("gender", "MALE")
                        .param("birthDate", "2023-01-01").param("microchipNo", "792097000100001"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("petForm", "microchipNo"))
                .andExpect(content().string(containsString("This microchip number is used by another pet.")));

        assertThat(petRepository.count()).isEqualTo(before);
    }

    @Test
    void missingPetShowsThe404Page() throws Exception {
        mockMvc.perform(get("/vet/pets/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
    }
}
