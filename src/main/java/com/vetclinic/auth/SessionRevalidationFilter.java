package com.vetclinic.auth;

import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Re-checks the logged-in user against the database on every request. The session only holds a snapshot
 * taken at login, so a deactivated, deleted or re-roled user would otherwise keep their old access.
 * In those cases the session is invalidated and the user is sent to the login page.
 * Created in {@link SecurityConfig} (not a bean) so it only runs inside the security filter chain.
 */
public class SessionRevalidationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final String loginUrl;

    public SessionRevalidationFilter(UserRepository userRepository, String loginUrl) {
        this.userRepository = userRepository;
        this.loginUrl = loginUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal
                && !stillValid(principal)) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + loginUrl);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean stillValid(AppUserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .filter(User::isActive)
                .filter(u -> principal.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + u.getRole().name())))
                .isPresent();
    }
}
