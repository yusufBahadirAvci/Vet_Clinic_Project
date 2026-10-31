package com.vetclinic.auth;

import com.vetclinic.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/** Logs a user into the HTTP session; used by the OTP flow and the dev login. */
@Service
public class SessionLoginService {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public void loginAs(User user, HttpServletRequest request, HttpServletResponse response) {
        AppUserPrincipal principal = AppUserPrincipal.from(user);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Session fixation protection: a session id issued before login must not stay valid.
        request.getSession(true);
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);
    }
}
