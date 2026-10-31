package com.vetclinic.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vetclinic.user.repository.UserRepository;

import com.vetclinic.common.web.HomeController;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void anonymousCanOpenHomeAndPrivacyPages() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/privacy")).andExpect(status().isOk());
    }

    @Test
    void anonymousIsRedirectedToLoginFromProtectedPages() throws Exception {
        mockMvc.perform(get("/my/pets")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/vet/owners")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCannotOpenVetArea() throws Exception {
        mockMvc.perform(get("/vet")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VET")
    void vetCannotOpenOwnerArea() throws Exception {
        mockMvc.perform(get("/my/pets")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VET")
    void vetPassesTheAccessRuleForVetArea() throws Exception {
        // No controller exists in this slice, so passing the rule shows up as 404 (not 302 or 403).
        mockMvc.perform(get("/vet/owners")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerPassesTheAccessRuleForOwnerArea() throws Exception {
        mockMvc.perform(get("/my/pets")).andExpect(status().isNotFound());
    }

    @Test
    void publicPathsAreNotRedirectedToLogin() throws Exception {
        // Controllers for these paths come in later tasks; 404 shows the security rule let the request through.
        mockMvc.perform(get("/login")).andExpect(status().isNotFound());
        mockMvc.perform(get("/login/verify")).andExpect(status().isNotFound());
        mockMvc.perform(get("/adoptions")).andExpect(status().isNotFound());
        mockMvc.perform(get("/adoptions/1")).andExpect(status().isNotFound());
        mockMvc.perform(post("/login/resend").with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post("/adoptions/1/apply").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void csrfIsRequiredForPublicPostEndpoints() throws Exception {
        mockMvc.perform(post("/adoptions/1/apply")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void logoutIsPostOnlyAndRedirectsHome() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/?logout"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void logoutWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/logout")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousNavbarShowsLoginOnly() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("Log in")))
                .andExpect(content().string(not(containsString("Log out"))))
                .andExpect(content().string(not(containsString("My pets"))))
                .andExpect(content().string(not(containsString("Owners"))));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerNavbarShowsMyPetsAndLogout() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("My pets")))
                .andExpect(content().string(containsString("Log out")))
                .andExpect(content().string(not(containsString("Log in"))))
                .andExpect(content().string(not(containsString("Owners"))));
    }

    @Test
    @WithMockUser(roles = "VET")
    void vetNavbarShowsVetMenu() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("Owners")))
                .andExpect(content().string(containsString("Vaccines")))
                .andExpect(content().string(containsString("Log out")))
                .andExpect(content().string(not(containsString("My pets"))));
    }
}
