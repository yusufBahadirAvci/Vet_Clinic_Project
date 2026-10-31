package com.vetclinic.pet.web;

import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.common.web.EnumLabels;
import com.vetclinic.pet.domain.Gender;
import com.vetclinic.pet.domain.Species;
import com.vetclinic.pet.service.PetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Pet management in the vet panel. Ids are limited to digits ({@code \d{1,18}}): any other value matches no
 * handler and ends as the 404 page instead of a 400.
 */
@Controller
@RequestMapping("/vet/pets")
public class PetController {

    private static final String FORM_VIEW = "vet/pets/form";

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @ModelAttribute("speciesOptions")
    public Map<String, String> speciesOptions() {
        return EnumLabels.options(Species.class);
    }

    @GetMapping
    public String list(@ModelAttribute("filter") PetFilter filter, Model model) {
        model.addAttribute("pets", petService.search(filter));
        return "vet/pets/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long ownerId, Model model) {
        PetForm form = petService.newForm(ownerId);
        model.addAttribute("petForm", form);
        return showForm(model, form, null);
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("petForm") PetForm form, BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(model, form, null);
        }
        try {
            Long id = petService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Pet added.");
            return "redirect:/vet/pets/" + id;
        } catch (FieldValidationException e) {
            e.applyTo(bindingResult);
            return showForm(model, form, null);
        }
    }

    @GetMapping("/{id:\\d{1,18}}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("pet", petService.getDetails(id));
        return "vet/pets/detail";
    }

    @GetMapping("/{id:\\d{1,18}}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PetForm form = petService.getForm(id);
        model.addAttribute("petForm", form);
        return showForm(model, form, id);
    }

    @PostMapping("/{id:\\d{1,18}}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("petForm") PetForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(model, form, id);
        }
        try {
            petService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Pet updated.");
            return "redirect:/vet/pets/" + id;
        } catch (FieldValidationException e) {
            e.applyTo(bindingResult);
            return showForm(model, form, id);
        }
    }

    private String showForm(Model model, PetForm form, Long petId) {
        model.addAttribute("petId", petId);
        model.addAttribute("genderOptions", EnumLabels.options(Gender.class));
        model.addAttribute("ownerOptions", petService.ownerOptions(form.getOwnerId()));
        return FORM_VIEW;
    }
}
