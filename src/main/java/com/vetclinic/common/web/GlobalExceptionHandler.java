package com.vetclinic.common.web;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.ErrorResponse;
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

    /**
     * Catch-all. Spring's own 4xx errors (wrong method, missing parameter, unsupported media type ...) keep their
     * status; ErrorResponse is an interface, so it cannot be named in an @ExceptionHandler and is checked here.
     * TypeMismatchException and HttpMessageNotReadableException are not ErrorResponses but are 400s too.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpServletRequest request) {
        HttpStatusCode status = clientErrorStatus(e);
        if (status == null) {
            // Only the path is logged: the query string may contain personal data. Nothing is shown to the user.
            log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), e);
            return new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.warn("Client error {} on {} {}: {}", status.value(), request.getMethod(), request.getRequestURI(),
                e.getClass().getSimpleName());
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return new ModelAndView("error/404", HttpStatus.NOT_FOUND);
        }
        ModelAndView mav = new ModelAndView("error/error", status);
        mav.addObject("status", status.value());
        return mav;
    }

    /** The 4xx status of the exception, or null when it is not a client error. */
    private HttpStatusCode clientErrorStatus(Exception e) {
        HttpStatusCode status = null;
        if (e instanceof ErrorResponse errorResponse) {
            status = errorResponse.getStatusCode();
        } else if (e instanceof TypeMismatchException || e instanceof HttpMessageNotReadableException) {
            status = HttpStatus.BAD_REQUEST;
        }
        return status != null && status.is4xxClientError() ? status : null;
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
