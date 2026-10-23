package com.example.MeterMind.ui;

import com.example.MeterMind.identity.CustomUserDetails;
import com.example.MeterMind.identity.Role;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootRedirectController {

    @GetMapping("/")
    public String redirectRoot(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getRole() == Role.CUSTOMER) {
            return "redirect:/customer/dashboard";
        } else {
            return "redirect:/owner/dashboard";
        }
    }
}
