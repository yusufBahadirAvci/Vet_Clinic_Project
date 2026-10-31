package com.vetclinic.auth;

import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserPrincipalTest {

    @Test
    void mapsUserFieldsAndRoleAuthority() {
        User user = user(7L, "Dr. Elif Yilmaz", Role.VET, true);

        AppUserPrincipal principal = AppUserPrincipal.from(user);

        assertThat(principal.getId()).isEqualTo(7L);
        assertThat(principal.getDisplayName()).isEqualTo("Dr. Elif Yilmaz");
        assertThat(principal.getUsername()).isEqualTo("Dr. Elif Yilmaz");
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_VET");
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    void ownerGetsOwnerAuthorityAndInactiveUserIsDisabled() {
        AppUserPrincipal principal = AppUserPrincipal.from(user(1L, "Ayse Demir", Role.OWNER, false));

        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OWNER");
        assertThat(principal.isEnabled()).isFalse();
    }

    static User user(Long id, String name, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
