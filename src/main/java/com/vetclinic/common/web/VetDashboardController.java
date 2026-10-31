package com.vetclinic.common.web;

import com.vetclinic.common.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VetDashboardController {

    private final DashboardService dashboardService;

    public VetDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/vet")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "vet/dashboard";
    }
}
