package com.vetclinic.user.web;

import com.vetclinic.common.exception.FieldValidationException;
import com.vetclinic.user.service.OwnerService;
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

/**
 * Owner management in the vet panel. Ids are limited to digits ({@code \d{1,18}}): any other value matches no
 * handler and ends as the 404 page instead of a 400.
 */
@Controller
@RequestMapping("/vet/owners")
public class OwnerController {

    private static final String FORM_VIEW = "vet/owners/form";

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("owners", ownerService.search(query, page));
        model.addAttribute("q", query);
        return "vet/owners/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("ownerForm", new OwnerForm());
        return FORM_VIEW;
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("ownerForm") OwnerForm form, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            Long id = ownerService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Owner added.");
            return "redirect:/vet/owners/" + id;
        } catch (FieldValidationException e) {
            e.applyTo(bindingResult);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id:\\d{1,18}}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("owner", ownerService.getDetails(id));
        return "vet/owners/detail";
    }

    @GetMapping("/{id:\\d{1,18}}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("ownerForm", ownerService.getForm(id));
        model.addAttribute("ownerId", id);
        return FORM_VIEW;
    }

    @PostMapping("/{id:\\d{1,18}}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("ownerForm") OwnerForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("ownerId", id);
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            ownerService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Owner updated.");
            return "redirect:/vet/owners/" + id;
        } catch (FieldValidationException e) {
            e.applyTo(bindingResult);
            return FORM_VIEW;
        }
    }

    @PostMapping("/{id:\\d{1,18}}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ownerService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Owner deactivated. They can no longer log in.");
        return "redirect:/vet/owners/" + id;
    }

    @PostMapping("/{id:\\d{1,18}}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ownerService.reactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Owner reactivated. They can log in again.");
        return "redirect:/vet/owners/" + id;
    }
}
