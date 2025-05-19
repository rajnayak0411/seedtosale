package com.seedtosale.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.seedtosale.dto.SignupDto;
import com.seedtosale.service.BuyerService;
import com.seedtosale.repository.OrderRepository;
import com.seedtosale.model.Order;
import com.seedtosale.service.SeedService;
import com.seedtosale.service.TractorService;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller
public class AuthController {

    @Autowired
    private BuyerService buyerService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SeedService seedService;

    @Autowired
    private TractorService tractorService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("Buyer", new SignupDto());
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(@ModelAttribute("Buyer") SignupDto signupDto,
            RedirectAttributes redirectAttributes) {
        if (signupDto.getRoles() == null || signupDto.getRoles().isEmpty()) {
            signupDto.getRoles().add("ROLE_USER");
        }
        buyerService.registerBuyer(signupDto);
        redirectAttributes.addFlashAttribute("success", "Registration successful!");
        return "redirect:/login";
    }

}
