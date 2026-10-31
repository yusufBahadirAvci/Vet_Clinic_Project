package com.vetclinic.notification.web;

import com.vetclinic.auth.SecurityConfig;
import com.vetclinic.notification.SmsOutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vetclinic.user.repository.UserRepository;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SmsOutboxController.class)
@Import(SecurityConfig.class)
class SmsOutboxControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SmsOutboxService smsOutboxService;

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerGetsForbidden() throws Exception {
        mockMvc.perform(get("/vet/sms-outbox")).andExpect(status().isForbidden());

        verifyNoInteractions(smsOutboxService);
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/vet/sms-outbox"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "VET")
    void vetSeesTheLatestMessages() throws Exception {
        when(smsOutboxService.latestMessages()).thenReturn(List.of(
                new SmsOutboxService.SmsOutboxView("+905321234567", "Your code is 123456", "2026-01-15 12:30:05")));

        mockMvc.perform(get("/vet/sms-outbox"))
                .andExpect(status().isOk())
                .andExpect(view().name("vet/sms-outbox"))
                .andExpect(content().string(containsString("+905321234567")))
                .andExpect(content().string(containsString("Your code is 123456")))
                .andExpect(content().string(containsString("2026-01-15 12:30:05")));
    }

    @Test
    @WithMockUser(roles = "VET")
    void vetSeesAnEmptyStateWhenNothingWasSent() throws Exception {
        when(smsOutboxService.latestMessages()).thenReturn(List.of());

        mockMvc.perform(get("/vet/sms-outbox"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No SMS messages have been sent yet.")));
    }
}
