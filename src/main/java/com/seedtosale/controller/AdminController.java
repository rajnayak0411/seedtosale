package com.seedtosale.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.seedtosale.dto.SignupDto;
import com.seedtosale.model.Seed;
import com.seedtosale.model.Tractor;
import com.seedtosale.service.SeedService;
import com.seedtosale.service.TractorService;
import com.seedtosale.repository.BuyerRepository;
import com.seedtosale.repository.OrderRepository;
import com.seedtosale.model.Order;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final SeedService seedService;
    private final TractorService tractorService;
    private final BuyerRepository buyerRepository;
    private final com.seedtosale.service.EnquiryService enquiryService;
    private final OrderRepository orderRepository;

    public AdminController(SeedService seedService, TractorService tractorService, BuyerRepository buyerRepository, com.seedtosale.service.EnquiryService enquiryService, OrderRepository orderRepository) {
        this.seedService = seedService;
        this.tractorService = tractorService;
        this.buyerRepository = buyerRepository;
        this.enquiryService = enquiryService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        // Fetch all seeds and tractors
        java.util.List<com.seedtosale.model.Seed> seeds = seedService.getAllSeeds();
        java.util.List<com.seedtosale.model.Tractor> tractors = tractorService.getAllTractors();

        // Calculate total seeds sold in kg and prepare list of seeds sold with quantity
        double totalSeedsSoldKg = seeds.stream()
            .mapToDouble(s -> s.getSoldQuantity())
            .sum();
        java.util.List<com.seedtosale.model.Seed> seedsSoldList = seeds.stream()
            .filter(s -> s.getSoldQuantity() > 0)
            .collect(Collectors.toList());

        // Prepare list of seeds sold with weight in kg
        java.util.List<java.util.Map<String, Object>> seedsSoldKgList = seeds.stream()
            .filter(s -> s.getSoldQuantity() > 0)
            .map(s -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("name", s.getName());
                map.put("soldWeightKg", s.getSoldQuantity());
                return map;
            })
            .collect(Collectors.toList());

        // Calculate total tractors rented (soldQuantity used as rented) and tractors left
        int totalTractorsRented = tractors.stream().mapToInt(t -> t.getSoldQuantity()).sum();
        int totalTractorsStock = tractors.stream().mapToInt(t -> t.getStockQuantity()).sum();
        int totalTractorsLeft = totalTractorsStock - totalTractorsRented;

        // Prepare list of seeds left with weight in kg (stockQuantity - soldQuantity)
        java.util.List<java.util.Map<String, Object>> seedsLeftKgList = seeds.stream()
            .filter(s -> s.getStockQuantity() > 0)
            .map(s -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("name", s.getName());
                map.put("weightLeftKg", s.getStockQuantity() - s.getSoldQuantity());
                return map;
            })
            .collect(Collectors.toList());

        // Prepare list of tractors left with quantity
        Stream<Object> tractorsLeftList = tractors.stream()
            .filter(t -> t.getStockQuantity() > 0)
            .map(t-> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("name", t.getName());
                map.put("tractorLeft", t.getStockQuantity() - t.getSoldQuantity());
                return map;
            });

        // Prepare list of rented tractors with quantity (soldQuantity > 0)
        java.util.List<java.util.Map<String, Object>> rentedTractorsList = tractors.stream()
            .filter(t -> t.getSoldQuantity() > 0)
            .map(t -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("name", t.getName());
                map.put("rentedQuantity", t.getSoldQuantity());
                return map;
            })
            .collect(Collectors.toList());

        model.addAttribute("totalSeedsSoldKg", totalSeedsSoldKg);
        model.addAttribute("seedsSoldList", seedsSoldList);
        model.addAttribute("seedsSoldKgList", seedsSoldKgList);
        model.addAttribute("totalTractorsRented", totalTractorsRented);
        model.addAttribute("totalTractorsLeft", totalTractorsLeft);
        model.addAttribute("seedsLeftKgList", seedsLeftKgList);
        model.addAttribute("tractorsLeftList", tractorsLeftList);
        model.addAttribute("rentedTractorsList", rentedTractorsList);

        return "Admin/dashboard";
    }

    @GetMapping("/Customer-details")
    public String customerDetails(Model model) {
        model.addAttribute("buyers", buyerRepository.findAll());
        return "Admin/Customer-details";
    }

    @GetMapping("/admin/customer-profile")
    public String customerProfile() {
        return "Admin/Customer-profile";
    }

    @GetMapping("/admin/customer-profile/{id}")
    public String customerProfileById(@PathVariable Long id, Model model) {
        SignupDto buyer = buyerRepository.findById(id).orElse(null);
        if (buyer == null) {
            return "redirect:/admin"; // Redirect to customer details if buyer not found
        }
        model.addAttribute("buyer", buyer);
        // Add orders and product details
        List<Order> orders = orderRepository.findByUserId(id);
        List<Map<String, Object>> ordersWithDetails = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("itemType", order.getItemType());
            orderMap.put("itemId", order.getItemId());
            orderMap.put("quantity", order.getQuantity());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("razorpayOrderId", order.getRazorpayOrderId());
            orderMap.put("razorpayPaymentId", order.getRazorpayPaymentId());
            orderMap.put("status", order.getStatus());
            if (order.getCreatedAt() != null) {
                java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy");
                java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
                java.time.format.DateTimeFormatter isoFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
                orderMap.put("orderDate", order.getCreatedAt().toLocalDate().format(dateFormatter));
                orderMap.put("orderDateIso", order.getCreatedAt().toLocalDate().format(isoFormatter));
                orderMap.put("orderTime", order.getCreatedAt().toLocalTime().format(timeFormatter));
            } else {
                orderMap.put("orderDate", "");
                orderMap.put("orderTime", "");
            }
            String productName = "";
            String location = "";
            String address = "";
            double price = 0.0;
            if ("seed".equalsIgnoreCase(order.getItemType())) {
                var seed = seedService.getSeedById(order.getItemId());
                if (seed != null) {
                    productName = seed.getName();
                    location = seed.getLocation();
                    price = seed.getPrice();
                    orderMap.put("productImage", "/uploads/seeds/" + seed.getImage());
                }
            } else if ("tractor".equalsIgnoreCase(order.getItemType())) {
                var tractor = tractorService.getTractorById(order.getItemId());
                if (tractor != null) {
                    productName = tractor.getName();
                    location = tractor.getLocation();
                    price = tractor.getPrice();
                    orderMap.put("productImage", "/uploads/tractors/" + tractor.getImage());
                }
            }
            orderMap.put("productName", productName);
            orderMap.put("location", location);
            orderMap.put("address", address);
            orderMap.put("price", price);
            ordersWithDetails.add(orderMap);
        }
        model.addAttribute("orders", ordersWithDetails);
        return "Admin/Customer-profile";
    }

    @GetMapping("/admin/seeds-list")
    public String seedsList() {
        return "Admin/seeds-list";
    }

    @GetMapping("/admin/tractor-list")
    public String tractorList() {
        return "Admin/tractor-list";
    }

    @GetMapping("/admin/upload-seeds")
    public String uploadSeeds() {
        return "Admin/upload-seeds";
    }

    @GetMapping("/admin/view-seeds")
    public String viewSeeds(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "location", required = false, defaultValue = "") String location,
            @RequestParam(value = "maxPrice", required = false, defaultValue = "1000000") double maxPrice,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            Model model) {
        model.addAttribute("seeds", seedService.getFilteredSeeds(type, location, maxPrice, search));
        model.addAttribute("filterType", type);
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterMaxPrice", maxPrice);
        model.addAttribute("filterSearch", search);
        return "viewseeds";
    }

    @PostMapping("/admin/upload-seeds")
    public String handleSeedUpload(@ModelAttribute Seed seed,
                                   @RequestParam("photo") MultipartFile photo,
                                   RedirectAttributes redirectAttributes) {
        try {
            seedService.saveSeed(seed, photo);
            redirectAttributes.addFlashAttribute("message", "Seed uploaded successfully!");
        } catch (Exception e) {
            logger.error("Failed to upload seed", e);
            redirectAttributes.addFlashAttribute("message", "Failed to upload seed.");
        }
        return "redirect:/admin/upload-seeds";
    }

    @GetMapping("/admin/upload-tractor")
    public String uploadTractor() {
        return "Admin/upload-tractor";
    }

    @PostMapping("/admin/upload-tractor")
    public String handleTractorUpload(@ModelAttribute Tractor tractor,
                                      @RequestParam("photo") MultipartFile photo,
                                      RedirectAttributes redirectAttributes) {
        try {
            tractorService.saveTractor(tractor, photo);
            redirectAttributes.addFlashAttribute("message", "Tractor uploaded successfully!");
        } catch (Exception e) {
            logger.error("Failed to upload tractor", e);
            redirectAttributes.addFlashAttribute("message", "Failed to upload tractor.");
        }
        return "redirect:/admin/upload-tractor";
    }

    @GetMapping("/admin/edit-tractor-list")
    public String editTractorList(Model model) {
        model.addAttribute("tractors", tractorService.getAllTractors());
        return "Admin/edit-tractor-list";
    }

    @GetMapping("/admin/edit-tractor/{id}")
    public String editTractorForm(@PathVariable Long id, Model model) {
        Tractor tractor = tractorService.getTractorById(id);
        if (tractor == null) {
            model.addAttribute("message", "Tractor not found.");
            return "redirect:/admin/edit-tractor-list";
        }
        model.addAttribute("tractor", tractor);
        model.addAttribute("locations", getLocations());
        return "Admin/edit-tractor";
    }

    @PostMapping("/admin/edit-tractor/{id}")
    public String updateTractor(@PathVariable Long id,
                                @ModelAttribute Tractor tractor,
                                @RequestParam("photo") MultipartFile photo,
                                RedirectAttributes redirectAttributes) {
        try {
            Tractor updated = tractorService.updateTractor(id, tractor, photo);
            if (updated == null) {
                redirectAttributes.addFlashAttribute("message", "Tractor not found.");
                return "redirect:/admin/edit-tractor-list";
            }
            redirectAttributes.addFlashAttribute("message", "Tractor updated successfully!");
        } catch (Exception e) {
            logger.error("Failed to update tractor", e);
            redirectAttributes.addFlashAttribute("message", "Failed to update tractor.");
        }
        return "redirect:/admin/edit-tractor-list";
    }
    @GetMapping("/admin/delete-tractor/{id}")
    public String deleteTractor(@PathVariable Long id){
        tractorService.deleteTractor(id);
        return "redirect:/admin/edit-tractor-list";
    }

    @GetMapping("/admin/edit-seed-list")
    public String editSeedList(Model model) {
        model.addAttribute("seeds", seedService.getAllSeeds());
        return "Admin/edit-seed-list";
    }

    @GetMapping("/admin/edit-seed/{id}")
    public String editSeedForm(@PathVariable Long id, Model model) {
        Seed seed = seedService.getSeedById(id);
        if (seed == null) {
            model.addAttribute("message", "Seed not found.");
            return "redirect:/admin/edit-seed-list";
        }
        model.addAttribute("seed", seed);
        model.addAttribute("locations", getLocations());
        return "Admin/edit-seed";
    }

    @PostMapping("/admin/edit-seed/{id}")
    public String updateSeed(@PathVariable Long id,
                             @ModelAttribute Seed seed,
                             @RequestParam("photo") MultipartFile photo,
                             RedirectAttributes redirectAttributes) {
        try {
            Seed updated = seedService.updateSeed(id, seed, photo);
            if (updated == null) {
                redirectAttributes.addFlashAttribute("message", "Seed not found.");
                return "redirect:/admin/edit-seed-list";
            }
            redirectAttributes.addFlashAttribute("message", "Seed updated successfully!");
        } catch (Exception e) {
            logger.error("Failed to update seed", e);
            redirectAttributes.addFlashAttribute("message", "Failed to update seed.");
        }
        return "redirect:/admin/edit-seed-list";
    }

    private java.util.List<String> getLocations() {
        return java.util.Arrays.asList(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat",
            "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
            "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh",
            "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands", "Chandigarh",
            "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Jammu and Kashmir", "Ladakh",
            "Lakshadweep", "Puducherry"
        );
    }
    @GetMapping("/admin/delete-seed/{id}")
    public String deleteSeed(@PathVariable Long id){
        seedService.deleteSeed(id);
        return "redirect:/admin/edit-seed-list";
    }

    @GetMapping("/enquiry-list")
    public String enquiryList(Model model) {
        model.addAttribute("enquiries", enquiryService.getAllEnquiry());
        return "Admin/enquiry-list";
    }
}
