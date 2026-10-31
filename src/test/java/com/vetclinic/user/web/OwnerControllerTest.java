package com.vetclinic.user.web;

import com.vetclinic.auth.SecurityConfig;
import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.user.service.OwnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OwnerController.class)
@Import(SecurityConfig.class)
class OwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerService ownerService;

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/vet/owners")).andExpect(status().isForbidden());
        mockMvc.perform(get("/vet/owners/new")).andExpect(status().isForbidden());
        verify(ownerService, never()).search(any(), anyInt());
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/vet/owners")).andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "VET")
    void vetSeesOwnerList() throws Exception {
        when(ownerService.search("ayse", 0)).thenReturn(new PageImpl<>(
                List.of(new OwnerListItem(3L, "Ayşe Demir", "ayse.demir@example.com", null, true)),
                PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/vet/owners").param("q", "ayse"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ayşe Demir")))
                .andExpect(content().string(containsString("/vet/owners/3")));
    }

    @Test
    @WithMockUser(roles = "VET")
    void createRedirectsToDetailWithFlashMessage() throws Exception {
        when(ownerService.create(any(OwnerForm.class))).thenReturn(5L);

        mockMvc.perform(post("/vet/owners/new").with(csrf())
                        .param("fullName", "Ali Veli").param("phone", "0532 123 45 67").param("kvkkConsent", "true"))
                .andExpect(redirectedUrl("/vet/owners/5"))
                .andExpect(flash().attribute("successMessage", "Owner added."));
    }

    @Test
    @WithMockUser(roles = "VET")
    void duplicatePhoneIsShownOnThePhoneField() throws Exception {
        when(ownerService.create(any(OwnerForm.class)))
                .thenThrow(new FieldValidationException("phone", "This phone number is used by another record."));

        mockMvc.perform(post("/vet/owners/new").with(csrf())
                        .param("fullName", "Ali Veli").param("phone", "0532 123 45 67").param("kvkkConsent", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("vet/owners/form"))
                .andExpect(model().attributeHasFieldErrors("ownerForm", "phone"))
                .andExpect(content().string(containsString("This phone number is used by another record.")));
    }

    @Test
    @WithMockUser(roles = "VET")
    void missingKvkkConsentAndNameAreFieldErrors() throws Exception {
        mockMvc.perform(post("/vet/owners/new").with(csrf()).param("email", "ali@example.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("ownerForm", "kvkkConsent", "fullName"))
                .andExpect(content().string(containsString("KVKK consent is required")));
        verify(ownerService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "VET")
    void editWithDuplicateEmailIsShownOnTheEmailField() throws Exception {
        doThrow(new FieldValidationException("email", "This e-mail address is used by another record."))
                .when(ownerService).update(eq(5L), any(OwnerForm.class));

        mockMvc.perform(post("/vet/owners/5/edit").with(csrf())
                        .param("fullName", "Ali").param("email", "taken@example.com").param("kvkkConsent", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("ownerForm", "email"))
                .andExpect(content().string(containsString("/vet/owners/5/edit")));
    }

    @Test
    @WithMockUser(roles = "VET")
    void nonNumericOrMissingIdShowsThe404Page() throws Exception {
        when(ownerService.getDetails(99L)).thenThrow(new NotFoundException("Owner not found: 99"));

        mockMvc.perform(get("/vet/owners/abc"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
        mockMvc.perform(get("/vet/owners/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
        mockMvc.perform(get("/vet/owners/99999999999999999999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VET")
    void deactivateAndReactivate() throws Exception {
        mockMvc.perform(post("/vet/owners/5/deactivate").with(csrf()))
                .andExpect(redirectedUrl("/vet/owners/5"))
                .andExpect(flash().attributeExists("successMessage"));
        verify(ownerService).deactivate(5L);

        mockMvc.perform(post("/vet/owners/5/reactivate").with(csrf()))
                .andExpect(redirectedUrl("/vet/owners/5"))
                .andExpect(flash().attribute("successMessage", "Owner reactivated. They can log in again."));
        verify(ownerService).reactivate(5L);
    }

    @Test
    @WithMockUser(roles = "VET")
    void reactivateRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/vet/owners/5/reactivate")).andExpect(status().isForbidden());
        verify(ownerService, never()).reactivate(any());
    }

    @Test
    @WithMockUser(roles = "VET")
    void detailShowsReactivateButtonOnlyForInactiveOwner() throws Exception {
        when(ownerService.getDetails(5L)).thenReturn(
                new OwnerDetails(5L, "Ali Veli", "ali@example.com", null, false, null, List.of()));
        when(ownerService.getDetails(6L)).thenReturn(
                new OwnerDetails(6L, "Can Polat", "can@example.com", null, true, null, List.of()));

        mockMvc.perform(get("/vet/owners/5"))
                .andExpect(content().string(containsString("/vet/owners/5/reactivate")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/vet/owners/5/deactivate"))));
        mockMvc.perform(get("/vet/owners/6"))
                .andExpect(content().string(containsString("/vet/owners/6/deactivate")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/vet/owners/6/reactivate"))))
                .andExpect(content().string(containsString("/vet/pets/new?ownerId=6")));
    }
}
