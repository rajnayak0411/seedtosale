package com.seedtosale.controller;

import com.seedtosale.model.Enquiry;
import com.seedtosale.model.Seed;
import com.seedtosale.model.Tractor;
import com.seedtosale.service.SeedService;
import com.seedtosale.service.TractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final SeedService seedService;
    private final TractorService tractorService;

    @Autowired
    public HomeController(SeedService seedService, TractorService tractorService) {
        this.seedService = seedService;
        this.tractorService = tractorService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("activePage", "home");
        model.addAttribute("enquiry", new Enquiry());
        List<Seed> latestSeeds = seedService.getLatestSeeds(3);
        List<Tractor> latestTractors = tractorService.getLatestTractors(3);

        model.addAttribute("latestSeeds", latestSeeds);
        model.addAttribute("latestTractors", latestTractors);

        return "index";
    }
}
