package com.vetclinic.auth;

import com.vetclinic.user.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;

class SessionLoginServiceTest {

    private final SessionLoginService service = new SessionLoginService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storesPrincipalInSecurityContextAndSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.loginAs(AppUserPrincipalTest.user(5L, "Ayse Demir", Role.OWNER, true), request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(((AppUserPrincipal) auth.getPrincipal()).getId()).isEqualTo(5L);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_OWNER");

        SecurityContext saved = (SecurityContext) request.getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(saved).isNotNull();
        assertThat(saved.getAuthentication().getPrincipal()).isEqualTo(auth.getPrincipal());
    }

    @Test
    void changesSessionIdToPreventSessionFixation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        String oldId = session.getId();

        service.loginAs(AppUserPrincipalTest.user(1L, "Dr. Murat Kaya", Role.VET, true),
                request, new MockHttpServletResponse());

        assertThat(request.getSession(false).getId()).isNotEqualTo(oldId);
    }
}
