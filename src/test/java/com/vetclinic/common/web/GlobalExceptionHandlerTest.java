package com.vetclinic.common.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.vetclinic.common.InvalidContactException;
import com.vetclinic.common.exception.NotFoundException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundExceptionShows404Page() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl("error/404"));
    }

    @Test
    void missingResourceShows404Page() throws Exception {
        mockMvc.perform(get("/test/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl("error/404"));
    }

    @Test
    void unexpectedExceptionShows500PageWithoutDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(forwardedUrl("error/500"));
    }

    @Test
    void typeMismatchReturns400NotFiveHundred() throws Exception {
        mockMvc.perform(get("/test/item/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(forwardedUrl("error/error"));
    }

    @Test
    void unknownSubPathReturns404Page() throws Exception {
        mockMvc.perform(get("/test/not-found/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(post("/test/boom"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(forwardedUrl("error/error"));
    }

    @Test
    void missingParameterReturns400() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(forwardedUrl("error/error"));
    }

    @Test
    void invalidContactRedirectsBackToSameSiteReferer() throws Exception {
        mockMvc.perform(get("/test/bad-contact").header(HttpHeaders.REFERER, "http://localhost/vet/owners/new"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/vet/owners/new"))
                .andExpect(flash().attribute("errorMessage", "Please enter a valid e-mail address."));
    }

    @Test
    void invalidContactIgnoresForeignReferer() throws Exception {
        mockMvc.perform(get("/test/bad-contact").header(HttpHeaders.REFERER, "http://evil.example/steal"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void invalidContactIgnoresMalformedReferer() throws Exception {
        mockMvc.perform(get("/test/bad-contact").header(HttpHeaders.REFERER, "not a url"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void invalidContactWithoutRefererGoesHome() throws Exception {
        mockMvc.perform(get("/test/bad-contact")).andExpect(redirectedUrl("/"));
    }

    @Test
    void accessDeniedIsLeftToSpringSecurity() {
        assertThatThrownBy(() -> mockMvc.perform(get("/test/denied")))
                .hasCauseInstanceOf(AccessDeniedException.class);
    }

    @Controller
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new NotFoundException("Pet not found");
        }

        @GetMapping("/test/no-resource")
        void noResource() throws Exception {
            throw new NoResourceFoundException(HttpMethod.GET, "/missing", "missing");
        }

        @GetMapping("/test/item/{id}")
        void item(@PathVariable Long id) {
        }

        @GetMapping("/test/param")
        void param(@RequestParam String q) {
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/test/bad-contact")
        void badContact() {
            throw new InvalidContactException("Please enter a valid e-mail address.");
        }

        @GetMapping("/test/denied")
        void denied() {
            throw new AccessDeniedException("denied");
        }
    }
}
