package com.vetclinic.notification.web;

import com.vetclinic.notification.SmsOutboxService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** VET-only view of the mock SMS outbox (access rule: /vet/** in SecurityConfig). */
@Controller
public class SmsOutboxController {

    private final SmsOutboxService smsOutboxService;

    public SmsOutboxController(SmsOutboxService smsOutboxService) {
        this.smsOutboxService = smsOutboxService;
    }

    @GetMapping("/vet/sms-outbox")
    public String page(Model model) {
        model.addAttribute("messages", smsOutboxService.latestMessages());
        return "vet/sms-outbox";
    }
}
