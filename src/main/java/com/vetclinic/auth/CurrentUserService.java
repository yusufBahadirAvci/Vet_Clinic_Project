package com.vetclinic.auth;

import com.vetclinic.common.exception.NotFoundException;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return Optional.of(principal.getId());
        }
        return Optional.empty();
    }

    public Long requireUserId() {
        return currentUserId().orElseThrow(
                () -> new AuthenticationCredentialsNotFoundException("No authenticated user"));
    }

    public User requireUser() {
        Long id = requireUserId();
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
