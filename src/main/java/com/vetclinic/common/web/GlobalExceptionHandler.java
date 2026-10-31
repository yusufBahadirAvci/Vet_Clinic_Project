package com.vetclinic.common.web;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.vetclinic.common.InvalidContactException;
import com.vetclinic.common.exception.NotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class})
    public ModelAndView handleNotFound() {
        return new ModelAndView("error/404", HttpStatus.NOT_FOUND);
    }

    /**
     * Safety net for contact errors a controller did not handle itself. Controllers with a form should
     * report the problem on the field instead (bindingResult.rejectValue).
     */
    @ExceptionHandler(InvalidContactException.class)
    public String handleInvalidContact(InvalidContactException e, HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:" + sameSiteRefererPath(request);
    }

    /** Lets Spring Security answer with the login redirect or the 403 page. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurityException(Exception e) throws Exception {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception e, HttpServletRequest request) {
        // Only the path is logged: the query string may contain personal data. Nothing is shown to the user.
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), e);
        return new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** Path of the Referer when it points to this same host, otherwise "/" (prevents open redirects). */
    private String sameSiteRefererPath(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            boolean sameHost = request.getServerName().equalsIgnoreCase(uri.getHost());
            if (sameHost && path != null && path.startsWith("/") && !path.startsWith("//")) {
                return path;
            }
        } catch (IllegalArgumentException ignored) {
            // malformed Referer: fall back to the home page
        }
        return "/";
    }
}
