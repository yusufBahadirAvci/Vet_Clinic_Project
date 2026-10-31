package com.vetclinic.auth;

import com.vetclinic.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vetclinic.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DevLoginController.class)
@ActiveProfiles("dev")
@Import({SecurityConfig.class, SessionLoginService.class})
class DevLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DevLoginService devLoginService;

    @Test
    void listsSeededUsersForAnonymousVisitor() throws Exception {
        when(devLoginService.listUsers()).thenReturn(List.of(
                AppUserPrincipalTest.user(1L, "Dr. Elif Yilmaz", Role.VET, true),
                AppUserPrincipalTest.user(2L, "Ayse Demir", Role.OWNER, true)));

        mockMvc.perform(get("/dev/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dr. Elif Yilmaz")))
                .andExpect(content().string(containsString("Ayse Demir")))
                .andExpect(content().string(containsString("/dev/login/2")));
    }

    @Test
    void loginStoresSecurityContextInSessionAndRedirectsHome() throws Exception {
        when(devLoginService.getUser(2L)).thenReturn(AppUserPrincipalTest.user(2L, "Ayse Demir", Role.OWNER, true));

        var result = mockMvc.perform(post("/dev/login/2").with(csrf()).session(new MockHttpSession()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        Object context = result.getRequest().getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context).isNotNull();
    }

    @Test
    void loginRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/dev/login/2")).andExpect(status().isForbidden());
    }
}
