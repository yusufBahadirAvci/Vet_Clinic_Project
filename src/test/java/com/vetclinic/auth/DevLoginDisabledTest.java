package com.vetclinic.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Outside the dev profile /dev/login is not permitted by SecurityConfig. */
@WebMvcTest
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class DevLoginDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devLoginIsProtectedOutsideDevProfile() throws Exception {
        mockMvc.perform(get("/dev/login")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/dev/login/1").with(csrf())).andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }
}
