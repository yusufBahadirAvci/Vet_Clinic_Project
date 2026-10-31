package com.vetclinic.auth;

import com.vetclinic.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * The authenticated user stored in the session. Login is passwordless (OTP),
 * so there is no password; {@link #getUsername()} returns the display name shown in the navbar.
 */
public class AppUserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String displayName;
    private final String role;
    private final boolean active;

    public AppUserPrincipal(Long id, String displayName, String role, boolean active) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
    }

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(user.getId(), user.getFullName(), user.getRole().name(), user.isActive());
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return displayName;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
