package com.vetclinic.pet.web;

import com.vetclinic.auth.CurrentUserService;
import com.vetclinic.pet.service.PetProfileFacade;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Owner portal (access rule: /my/** requires OWNER in SecurityConfig). */
@Controller
public class OwnerPortalController {

    private final PetProfileFacade petProfileFacade;
    private final CurrentUserService currentUserService;

    public OwnerPortalController(PetProfileFacade petProfileFacade, CurrentUserService currentUserService) {
        this.petProfileFacade = petProfileFacade;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/my/pets")
    public String pets(Model model) {
        model.addAttribute("pets", petProfileFacade.listForOwner(currentUserService.requireUserId()));
        return "my/pets";
    }

    @GetMapping("/my/pets/{id}")
    public String pet(@PathVariable Long id, Model model) {
        model.addAttribute("pet", petProfileFacade.profileOf(id, currentUserService.requireUserId()));
        return "my/pet-detail";
    }

    @GetMapping("/my/profile")
    public String profile(Model model) {
        model.addAttribute("profile", OwnerProfileDto.from(currentUserService.requireUser()));
        return "my/profile";
    }
}
