package com.vetclinic.auth;

import com.vetclinic.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/** Development shortcut: log in as any seeded user without an OTP. Never active outside the dev profile. */
@Controller
@Profile("dev")
public class DevLoginController {

    public record DevUserView(Long id, String fullName, String role) {
    }

    private final DevLoginService devLoginService;
    private final SessionLoginService sessionLoginService;

    public DevLoginController(DevLoginService devLoginService, SessionLoginService sessionLoginService) {
        this.devLoginService = devLoginService;
        this.sessionLoginService = sessionLoginService;
    }

    @GetMapping("/dev/login")
    public String page(Model model) {
        List<DevUserView> users = devLoginService.listUsers().stream()
                .map(u -> new DevUserView(u.getId(), u.getFullName(), u.getRole().name()))
                .toList();
        model.addAttribute("users", users);
        return "dev/login";
    }

    @PostMapping("/dev/login/{id}")
    public String login(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        User user = devLoginService.getUser(id);
        sessionLoginService.loginAs(user, request, response);
        return "redirect:/";
    }
}
