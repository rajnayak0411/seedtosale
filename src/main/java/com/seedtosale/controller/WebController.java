package com.seedtosale.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.seedtosale.dto.SignupDto;
import com.seedtosale.model.Enquiry;
import com.seedtosale.model.Order;
import com.seedtosale.service.EnquiryService;
import com.seedtosale.service.SeedService;
import com.seedtosale.service.TractorService;

import jakarta.validation.Valid;

@Controller
public class WebController {
    @Autowired
    private final SeedService seedService;
    private final TractorService tractorService;
    private final EnquiryService enquiryService;
    private final com.seedtosale.repository.SignupDtoRepository signupDtoRepository;
    private final com.seedtosale.repository.OrderRepository orderRepository;

   
    public WebController(SeedService seedService, TractorService tractorService, EnquiryService enquiryService, com.seedtosale.repository.SignupDtoRepository signupDtoRepository, com.seedtosale.repository.OrderRepository orderRepository) {
        this.seedService = seedService;
        this.tractorService = tractorService;
        this.enquiryService = enquiryService;
        this.signupDtoRepository = signupDtoRepository;
        this.orderRepository = orderRepository;
    }


    @GetMapping("/seeds")
    public String viewSeeds(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "location", required = false, defaultValue = "") String location,
            @RequestParam(value = "maxPrice", required = false, defaultValue = "10000") double maxPrice,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            Model model) {
        model.addAttribute("seeds", seedService.getFilteredSeeds(type, location, maxPrice, search));
        model.addAttribute("filterType", type);
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterMaxPrice", maxPrice);
        model.addAttribute("filterSearch", search);
        return "viewseeds";
    }
    

    @GetMapping("/tractor")
    public String viewTractors(
            @RequestParam(value = "location", required = false, defaultValue = "") String location,
            @RequestParam(value = "maxPrice", required = false, defaultValue = "1000000") double maxPrice,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            Model model) {
        model.addAttribute("tractors", tractorService.getFilteredTractors(location, maxPrice, search));
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterMaxPrice", maxPrice);
        model.addAttribute("filterSearch", search);
        return "viewtractor";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("enquiry", new Enquiry());
        return "contact";
    }

    @PostMapping("/contact")
    public String submitEnquiry(@Valid @ModelAttribute("enquiry") Enquiry enquiry, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("enquiry", enquiry);
            return "index";
        }
        enquiry.setName(toTitleCase(enquiry.getName()));
        enquiryService.saveEnquiry(enquiry);
        model.addAttribute("message", "Your enquiry has been submitted successfully.");
        model.addAttribute("enquiry", new Enquiry());
        return "index";
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                titleCase.append(c);
                nextTitleCase = true;
            } else {
                if (nextTitleCase) {
                    titleCase.append(Character.toTitleCase(c));
                    nextTitleCase = false;
                } else {
                    titleCase.append(Character.toLowerCase(c));
                }
            }
        }
        return titleCase.toString();
    }


    // Existing methods unchanged...

    @GetMapping("/user-profile")
    public String getProfile(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        SignupDto user = signupDtoRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        System.out.println("User ID in getProfile: " + user.getId());
        model.addAttribute("buyer", user);

        // Fetch orders for the user
        List<Order> orders = orderRepository.findByUserId(user.getId());
        System.out.println("Number of orders found: " + orders.size());
        // Calculate delivery date (createdAt + 15 days) and fetch productName for each order
        List<java.util.Map<String, Object>> orderDetails = new ArrayList<>();
        for (com.seedtosale.model.Order order : orders) {
            if (!"sold".equalsIgnoreCase(order.getStatus())) {
                continue; // Skip orders not sold
            }
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("order", order);
            if (order.getCreatedAt() != null) {
                map.put("deliveryDate", order.getCreatedAt().plusDays(15));
            } else {
                map.put("deliveryDate", null);
            }
            // Fetch productName based on itemType and itemId
            String productName = "";
            String productImage = "";
            if ("seed".equalsIgnoreCase(order.getItemType())) {
                com.seedtosale.model.Seed seed = seedService.getSeedById(order.getItemId());
                    if (seed != null) {
                        productName = seed.getName();
                        productImage = "/uploads/seeds/" + seed.getImage(); // Add prefix for seed images
                    }
            } else if ("tractor".equalsIgnoreCase(order.getItemType())) {
                com.seedtosale.model.Tractor tractor = tractorService.getTractorById(order.getItemId());
                    if (tractor != null) {
                        productName = tractor.getName();
                        productImage = "/uploads/tractors/" + tractor.getImage(); // Add prefix for tractor images
                    }
            }
            map.put("productName", productName);
            map.put("productImage", productImage);

            orderDetails.add(map);
        }
        model.addAttribute("orderDetails", orderDetails);

        return "profile";
    }

    @PostMapping("/user-profile")
    public String updateProfile(@ModelAttribute("buyer") com.seedtosale.dto.SignupDto buyer, Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        com.seedtosale.dto.SignupDto existingUser = signupDtoRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existingUser == null) {
            return "redirect:/login";
        }

        // Basic validation
        if (buyer.getName() == null || buyer.getName().trim().isEmpty() ||
            buyer.getPhone() == null || buyer.getPhone().trim().isEmpty() ||
            buyer.getAddress() == null || buyer.getAddress().trim().isEmpty()) {
            model.addAttribute("buyer", buyer);
            model.addAttribute("errorMessage", "All fields are required.");
            return "profile";
        }

        // Update fields
        existingUser.setName(buyer.getName().trim());
        existingUser.setPhone(buyer.getPhone().trim());
        existingUser.setAddress(buyer.getAddress().trim());

        signupDtoRepository.save(existingUser);

        model.addAttribute("buyer", existingUser);
        model.addAttribute("successMessage", "Profile updated successfully.");

        return "profile";
    }

    @GetMapping("/orders")
    public String viewOrders(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        // Redirect to user-profile page with orders section active
        return "redirect:/user-profile?showOrders=true";
    }
}


