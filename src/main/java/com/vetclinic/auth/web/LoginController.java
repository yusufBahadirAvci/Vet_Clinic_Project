package com.vetclinic.auth.web;

import com.vetclinic.auth.OtpService;
import com.vetclinic.auth.SessionLoginService;
import com.vetclinic.auth.VerifyResult;
import com.vetclinic.common.ContactNormalizer;
import com.vetclinic.common.ContactType;
import com.vetclinic.common.InvalidContactException;
import com.vetclinic.common.LogMasker;
import com.vetclinic.user.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Passwordless login: request a one-time code, then verify it. */
@Controller
public class LoginController {

    static final String INVALID_CONTACT_MESSAGE = "Enter a valid e-mail address or mobile number";
    static final String INVALID_CODE_MESSAGE = "The code is invalid or has expired";
    static final String LOCKED_MESSAGE = "Too many failed attempts, please request a new code";
    static final String CODE_SENT_MESSAGE = "If your details are registered, a verification code has been sent.";

    static final String SESSION_CONTACT = "otpContact";

    private final OtpService otpService;
    private final SessionLoginService sessionLoginService;
    private final ContactNormalizer contactNormalizer;

    public LoginController(OtpService otpService, SessionLoginService sessionLoginService,
                           ContactNormalizer contactNormalizer) {
        this.otpService = otpService;
        this.sessionLoginService = sessionLoginService;
        this.contactNormalizer = contactNormalizer;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String requestCode(@Valid LoginForm loginForm, BindingResult bindingResult, HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        try {
            String contact = otpService.requestCode(loginForm.getContact());
            session.setAttribute(SESSION_CONTACT, contact);
        } catch (InvalidContactException e) {
            bindingResult.rejectValue("contact", "invalid", INVALID_CONTACT_MESSAGE);
            return "login";
        }
        redirectAttributes.addFlashAttribute("successMessage", CODE_SENT_MESSAGE);
        return "redirect:/login/verify";
    }

    @GetMapping("/login/verify")
    public String verifyPage(HttpSession session, Model model) {
        String contact = (String) session.getAttribute(SESSION_CONTACT);
        if (contact == null) {
            return "redirect:/login";
        }
        return verifyView(contact, new VerifyForm(), model);
    }

    @PostMapping("/login/verify")
    public String verify(@Valid VerifyForm verifyForm, BindingResult bindingResult, HttpSession session,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        String contact = (String) session.getAttribute(SESSION_CONTACT);
        if (contact == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            return verifyView(contact, verifyForm, model);
        }

        VerifyResult result = otpService.verifyCode(contact, verifyForm.getCode());
        switch (result.status()) {
            case SUCCESS -> {
                session.removeAttribute(SESSION_CONTACT);
                sessionLoginService.loginAs(result.user(), request, response);
                return result.user().getRole() == Role.VET ? "redirect:/vet" : "redirect:/my/pets";
            }
            case LOCKED -> model.addAttribute("errorMessage", LOCKED_MESSAGE);
            default -> model.addAttribute("errorMessage", INVALID_CODE_MESSAGE);
        }
        return verifyView(contact, new VerifyForm(), model);
    }

    @PostMapping("/login/resend")
    public String resend(HttpSession session, RedirectAttributes redirectAttributes) {
        String contact = (String) session.getAttribute(SESSION_CONTACT);
        if (contact == null) {
            return "redirect:/login";
        }
        otpService.requestCode(contact);
        redirectAttributes.addFlashAttribute("successMessage", CODE_SENT_MESSAGE);
        return "redirect:/login/verify";
    }

    private String verifyView(String contact, VerifyForm form, Model model) {
        model.addAttribute("verifyForm", form);
        model.addAttribute("maskedContact", mask(contact));
        return "login-verify";
    }

    private String mask(String contact) {
        return contactNormalizer.detectType(contact) == ContactType.EMAIL
                ? LogMasker.maskEmail(contact)
                : LogMasker.maskPhone(contact);
    }
}
