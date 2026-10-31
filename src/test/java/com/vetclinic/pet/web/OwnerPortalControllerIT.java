package com.vetclinic.pet.web;

import static com.vetclinic.support.TestData.owner;
import static com.vetclinic.support.TestData.pet;
import static com.vetclinic.support.TestData.vaccine;
import static com.vetclinic.support.TestData.vet;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.auth.AppUserPrincipal;
import com.vetclinic.pet.domain.Pet;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.repository.PetRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import com.vetclinic.vaccination.domain.Vaccination;
import com.vetclinic.vaccination.domain.Vaccine;
import com.vetclinic.vaccination.repository.VaccinationRepository;
import com.vetclinic.vaccination.repository.VaccineRepository;

@AutoConfigureMockMvc
class OwnerPortalControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private VaccineRepository vaccineRepository;
    @Autowired
    private VaccinationRepository vaccinationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private Clock clock;

    private User alice;
    private User bob;
    private Pet alicesPet;
    private Pet bobsPet;

    @BeforeEach
    void setUp() {
        cleanUp();
        alice = userRepository.save(owner("Alice Owner", "alice@portal.test"));
        bob = userRepository.save(owner("Bob Owner", "bob@portal.test"));
        User theVet = userRepository.save(vet("Portal Vet", "vet@portal.test"));
        alicesPet = petRepository.save(pet("Rex", Species.DOG, alice));
        bobsPet = petRepository.save(pet("Misty", Species.CAT, bob));

        Vaccine rabies = vaccineRepository.save(vaccine("Portal Rabies", Species.DOG));
        Vaccination overdue = new Vaccination();
        overdue.setPet(alicesPet);
        overdue.setVaccine(rabies);
        overdue.setVet(theVet);
        overdue.setAdministeredAt(LocalDate.now(clock).minusYears(1).minusDays(10));
        overdue.setNextDueDate(LocalDate.now(clock).minusDays(10));
        overdue.setLotNumber("LOT-77");
        vaccinationRepository.save(overdue);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM vaccinations WHERE pet_id IN (SELECT id FROM pets WHERE owner_id IN "
                + "(SELECT id FROM users WHERE email LIKE '%@portal.test'))");
        jdbcTemplate.update("DELETE FROM pets WHERE owner_id IN (SELECT id FROM users WHERE email LIKE '%@portal.test')");
        jdbcTemplate.update("DELETE FROM vaccines WHERE name = 'Portal Rabies'");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE '%@portal.test'");
    }

    @Test
    void ownerSeesOwnPetsOnTheListPage() throws Exception {
        mockMvc.perform(get("/my/pets").with(user(AppUserPrincipal.from(alice))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rex")))
                .andExpect(content().string(containsString("Overdue")))
                .andExpect(content().string(not(containsString("Misty"))));
    }

    @Test
    void ownerWithoutPetsSeesTheEmptyState() throws Exception {
        User carol = userRepository.save(owner("Carol Owner", "carol@portal.test"));

        mockMvc.perform(get("/my/pets").with(user(AppUserPrincipal.from(carol))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You have no registered pets. Please contact the clinic.")));
    }

    @Test
    void ownerSeesOwnPetDetailWithTheVaccinationCard() throws Exception {
        mockMvc.perform(get("/my/pets/" + alicesPet.getId()).with(user(AppUserPrincipal.from(alice))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rex")))
                .andExpect(content().string(containsString("Upcoming and overdue doses")))
                .andExpect(content().string(containsString("Portal Rabies")))
                .andExpect(content().string(containsString("LOT-77")))
                .andExpect(content().string(containsString("Portal Vet")));
    }

    @Test
    void datesUseEnglishMonthNamesEvenWithATurkishLocale() throws Exception {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            Vaccine other = vaccineRepository.findAll().stream()
                    .filter(v -> v.getName().equals("Portal Rabies")).findFirst().orElseThrow();
            Vaccination dose = new Vaccination();
            dose.setPet(alicesPet);
            dose.setVaccine(other);
            dose.setVet(userRepository.findAll().stream()
                    .filter(u -> "vet@portal.test".equals(u.getEmail())).findFirst().orElseThrow());
            dose.setAdministeredAt(LocalDate.of(2025, 11, 5));
            dose.setNextDueDate(LocalDate.of(2026, 7, 23));
            vaccinationRepository.save(dose);

            mockMvc.perform(get("/my/pets/" + alicesPet.getId())
                            .header("Accept-Language", "tr-TR")
                            .with(user(AppUserPrincipal.from(alice))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("05 Nov 2025")))
                    .andExpect(content().string(containsString("23 Jul 2026")))
                    .andExpect(content().string(not(containsString("Kas"))))
                    .andExpect(content().string(not(containsString("Tem"))));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void anotherOwnersPetIsNotFound()throws Exception {
        mockMvc.perform(get("/my/pets/" + bobsPet.getId()).with(user(AppUserPrincipal.from(alice))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownPetIsNotFound() throws Exception {
        mockMvc.perform(get("/my/pets/999999").with(user(AppUserPrincipal.from(alice))))
                .andExpect(status().isNotFound());
    }

    @Test
    void profileShowsOwnContactDetails() throws Exception {
        mockMvc.perform(get("/my/profile").with(user(AppUserPrincipal.from(alice))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alice Owner")))
                .andExpect(content().string(containsString("alice@portal.test")))
                .andExpect(content().string(containsString("If your details are wrong, please contact the clinic.")));
    }

    @Test
    void ownerDeactivatedAfterLoginIsRedirectedToLogin() throws Exception {
        AppUserPrincipal session = AppUserPrincipal.from(alice);
        alice.setActive(false);
        userRepository.save(alice);

        mockMvc.perform(get("/my/pets").with(user(session)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void ownerWhoseRoleChangedAfterLoginIsRedirectedToLogin() throws Exception {
        AppUserPrincipal session = AppUserPrincipal.from(alice);
        alice.setRole(com.vetclinic.user.domain.Role.VET);
        userRepository.save(alice);

        mockMvc.perform(get("/my/pets").with(user(session)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void activeOwnerIsNotAffectedByTheRevalidation() throws Exception {
        mockMvc.perform(get("/my/pets").with(user(AppUserPrincipal.from(alice))))
                .andExpect(status().isOk());
    }
}
