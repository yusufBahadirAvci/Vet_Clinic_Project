package com.vetclinic.auth;

import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(User user) {
        AppUserPrincipal principal = AppUserPrincipal.from(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    @Test
    void returnsIdAndUserOfLoggedInUser() {
        User user = AppUserPrincipalTest.user(9L, "Ayse Demir", Role.OWNER, true);
        loginAs(user);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        assertThat(service.currentUserId()).contains(9L);
        assertThat(service.requireUserId()).isEqualTo(9L);
        assertThat(service.requireUser()).isSameAs(user);
    }

    @Test
    void noAuthenticationMeansNoUser() {
        assertThat(service.currentUserId()).isEmpty();
        assertThatThrownBy(service::requireUserId).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(service::requireUser).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void anonymousAuthenticationMeansNoUser() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(service.currentUserId()).isEmpty();
    }

    @Test
    void requireUserThrowsNotFoundWhenUserWasDeleted() {
        loginAs(AppUserPrincipalTest.user(9L, "Ayse Demir", Role.OWNER, true));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(service::requireUser).isInstanceOf(NotFoundException.class);
    }
}
