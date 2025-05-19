package com.seedtosale.controller;

import com.seedtosale.model.Seed;
import com.seedtosale.model.Tractor;
import com.seedtosale.model.Order;
import com.seedtosale.repository.OrderRepository;
import com.seedtosale.repository.TransactionRepository;
import com.seedtosale.repository.SignupDtoRepository;
import com.seedtosale.service.SeedService;
import com.seedtosale.service.TractorService;
import com.seedtosale.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final Path seedImageLocation = Paths.get("uploads/seeds");
    private final Path tractorImageLocation = Paths.get("uploads/tractors");

    private final SeedService seedService;
    private final TractorService tractorService;
    private final RazorpayService razorpayService;
    private final OrderRepository orderRepository;
    private final SignupDtoRepository signupDtoRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public OrderController(SeedService seedService, TractorService tractorService, RazorpayService razorpayService, OrderRepository orderRepository, SignupDtoRepository signupDtoRepository, TransactionRepository transactionRepository) {
        this.seedService = seedService;
        this.tractorService = tractorService;
        this.razorpayService = razorpayService;
        this.orderRepository = orderRepository;
        this.signupDtoRepository = signupDtoRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/image")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> getImage(@RequestParam String itemType, @RequestParam Long itemId) throws IOException {
        Path imagePath;
        if ("seed".equalsIgnoreCase(itemType)) {
            var seed = seedService.getSeedById(itemId);
            if (seed == null || seed.getImage() == null) {
                throw new IOException("Seed image not found");
            }
            imagePath = seedImageLocation.resolve(seed.getImage());
        } else if ("tractor".equalsIgnoreCase(itemType)) {
            var tractor = tractorService.getTractorById(itemId);
            if (tractor == null || tractor.getImage() == null) {
                throw new IOException("Tractor image not found");
            }
            imagePath = tractorImageLocation.resolve(tractor.getImage());
        } else {
            throw new IOException("Invalid item type");
        }
        byte[] imageBytes = java.nio.file.Files.readAllBytes(imagePath);
        String filename = imagePath.getFileName().toString().toLowerCase();
        String contentType = "image/jpeg"; // default
        if (filename.endsWith(".png")) {
            contentType = "image/png";
        } else if (filename.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (filename.endsWith(".webp")) {
            contentType = "image/webp";
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .body(imageBytes);
    }

    @GetMapping("/buy-seed/{id}")
    public String buySeed(@PathVariable("id") Long id, Model model) {
        Seed seed = seedService.getSeedById(id);
        if (seed == null) {
            model.addAttribute("message", "Seed not found.");
            return "error";
        }
        model.addAttribute("item", seed);
        model.addAttribute("itemType", "seed");
        model.addAttribute("availableQuantity", seed.getStockQuantity() - seed.getSoldQuantity());
        return "payment";
    }

    @GetMapping("/rent-tractor/{id}")
    public String rentTractor(@PathVariable("id") Long id, Model model) {
        Tractor tractor = tractorService.getTractorById(id);
        if (tractor == null) {
            model.addAttribute("message", "Tractor not found.");
            return "error";
        }
        model.addAttribute("item", tractor);
        model.addAttribute("itemType", "tractor");
        return "payment-tractor";
    }

    private Long getCurrentUserId() {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                var userDetails = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                System.out.println("Authenticated user: " + userDetails.getUsername());
                Optional<com.seedtosale.dto.SignupDto> userOpt = signupDtoRepository.findByEmailIgnoreCase(userDetails.getUsername());
                if (userOpt.isPresent()) {
                    System.out.println("Found user ID: " + userOpt.get().getId());
                    return userOpt.get().getId();
                } else {
                    System.out.println("User not found in repository for username: " + userDetails.getUsername());
                }
            } else {
                System.out.println("Authentication is null, not authenticated, or anonymous user");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("/process-payment")
    public String processPayment(@RequestParam String itemType,
                                 @RequestParam Long itemId,
                                 @RequestParam int quantity,
                                 Model model) throws IOException {
        Long userId = getCurrentUserId();
        System.out.println("processPayment userId: " + userId);

        // Calculate total price based on item type and quantity
        double totalPrice = 0.0;
        int availableQuantity = 0;
        if ("seed".equalsIgnoreCase(itemType)) {
            var seed = seedService.getSeedById(itemId);
            if (seed == null) {
                model.addAttribute("message", "Seed not found.");
                return "error";
            }
            availableQuantity = seed.getStockQuantity() - seed.getSoldQuantity();
            if (quantity > availableQuantity) {
                model.addAttribute("message", "Requested quantity exceeds available stock.");
                model.addAttribute("item", seed);
                model.addAttribute("itemType", itemType);
                model.addAttribute("availableQuantity", availableQuantity);
                return "payment";
            }
            totalPrice = seed.getPrice() * quantity;
            model.addAttribute("item", seed);
        } else if ("tractor".equalsIgnoreCase(itemType)) {
            var tractor = tractorService.getTractorById(itemId);
            if (tractor == null) {
                model.addAttribute("message", "Tractor not found.");
                return "error";
            }
            availableQuantity = tractor.getStockQuantity() - tractor.getSoldQuantity();
            if (quantity > availableQuantity) {
                model.addAttribute("message", "Requested quantity exceeds available stock.");
                model.addAttribute("item", tractor);
                model.addAttribute("itemType", itemType);
                model.addAttribute("availableQuantity", availableQuantity);
                return "payment-tractor";
            }
            totalPrice = tractor.getPrice() * quantity;
            model.addAttribute("item", tractor);
        } else {
            model.addAttribute("message", "Invalid item type.");
            return "error";
        }

        Order order = new Order();
        order.setItemType(itemType);
        order.setItemId(itemId);
        if ("tractor".equalsIgnoreCase(itemType)) {
            // Store rental hours in quantity field for tractor
            order.setQuantity(quantity);
        } else {
            order.setQuantity(quantity);
        }
        order.setTotalPrice(totalPrice);
        order.setStatus("PENDING");
        order.setUserId(userId);
        try {
            order = orderRepository.save(order);
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("orderId", order.getId());
        model.addAttribute("itemType", itemType);
        model.addAttribute("itemId", itemId);
        model.addAttribute("quantity", quantity);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("message", "Order created. Proceed to payment.");
        return "payment-initiate";
    }

    @PostMapping("/razorpay/confirm")
    public String confirmRazorpayPayment(@RequestParam Long orderId,
                                         @RequestParam String razorpayPaymentId,
                                         @RequestParam String razorpayOrderId,
                                         @RequestParam String razorpaySignature,
                                         Model model) {
        Long userId = getCurrentUserId();
        System.out.println("confirmRazorpayPayment userId: " + userId);

        boolean paymentSuccess = razorpayService.verifyPayment(razorpayPaymentId, razorpayOrderId, razorpaySignature);
        if (paymentSuccess) {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                order.setStatus("SOLD");
                order.setRazorpayPaymentId(razorpayPaymentId);
                order.setRazorpayOrderId(razorpayOrderId);
                order.setRazorpaySignature(razorpaySignature);
                order.setUserId(userId);
                try {
                    orderRepository.save(order);
                    com.seedtosale.model.Transaction transaction = new com.seedtosale.model.Transaction();
                    transaction.setOrderId(order.getId());
                    transaction.setRazorpayPaymentId(razorpayPaymentId);
                    transaction.setRazorpayOrderId(razorpayOrderId);
                    transaction.setRazorpaySignature(razorpaySignature);
                    transaction.setStatus("SOLD");
                    transaction.setAmount(order.getTotalPrice());
                    transactionRepository.save(transaction);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if ("seed".equalsIgnoreCase(order.getItemType())) {
                    var seed = seedService.getSeedById(order.getItemId());
                    if (seed != null) {
                        int newSoldQuantity = seed.getSoldQuantity() + order.getQuantity();
                        seed.setSoldQuantity(newSoldQuantity);
                        try {
                            seedService.updateSeed(seed.getId(), seed, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        model.addAttribute("item", seed);
                        model.addAttribute("itemType", "seed");
                    }
                } else if ("tractor".equalsIgnoreCase(order.getItemType())) {
                    var tractor = tractorService.getTractorById(order.getItemId());
                    if (tractor != null) {
                        // Add rental hours to hours field
                        int newHours = tractor.getHours() + order.getQuantity();
                        tractor.setHours(newHours);
                        // Increment soldQuantity by 1
                        int newSoldQuantity = tractor.getSoldQuantity() + 1;
                        tractor.setSoldQuantity(newSoldQuantity);
                        try {
                            tractorService.updateTractor(tractor.getId(), tractor, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        model.addAttribute("item", tractor);
                        model.addAttribute("itemType", "tractor");
                    }
                }
                model.addAttribute("quantity", order.getQuantity());
                model.addAttribute("totalPrice", order.getTotalPrice());
                model.addAttribute("message", "Razorpay payment successful! Your order has been placed.");
                model.addAttribute("order", order);
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
                String formattedCreatedAt = order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : "N/A";
                model.addAttribute("orderCreatedAt", order.getCreatedAt());
                var userOpt = signupDtoRepository.findById(order.getUserId());
                if (userOpt.isPresent()) {
                    model.addAttribute("userAddress", userOpt.get().getAddress());
                } else {
                    model.addAttribute("userAddress", "Address not available");
                }
            } else {
                model.addAttribute("message", "Order not found.");
            }
        } else {
            model.addAttribute("message", "Razorpay payment failed or pending.");
        }
        return "order-confirmation";
    }

    @PostMapping("/razorpay/initiate")
    @ResponseBody
    public Map<String, String> initiateRazorpayPayment(@RequestParam Long orderId) {
        Map<String, String> response = new HashMap<>();
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            String razorpayOrderId = razorpayService.initiatePayment(order.getItemType(), order.getItemId(), order.getQuantity());
            String keyId = razorpayService.getRazorpayConfig().getKeyId();
            System.out.println("Razorpay initiate payment keyId: " + keyId);
            System.out.println("Razorpay initiate payment orderId: " + razorpayOrderId);
            response.put("keyId", keyId);
            response.put("razorpayOrderId", razorpayOrderId);
        } else {
            response.put("error", "Order not found");
        }
        return response;
    }

    @PostMapping("/create-and-initiate")
    @ResponseBody
    public Map<String, Object> createAndInitiatePayment(@RequestParam String itemType,
                                                        @RequestParam Long itemId,
                                                        @RequestParam int quantity) {
        Map<String, Object> response = new HashMap<>();
        Long userId = getCurrentUserId();

        // Validate item and quantity
        double totalPrice = 0.0;
        int availableQuantity = 0;
        if ("seed".equalsIgnoreCase(itemType)) {
            var seed = seedService.getSeedById(itemId);
            if (seed == null) {
                response.put("error", "Seed not found");
                return response;
            }
            availableQuantity = seed.getStockQuantity() - seed.getSoldQuantity();
            if (quantity > availableQuantity) {
                response.put("error", "Requested quantity exceeds available stock");
                return response;
            }
            totalPrice = seed.getPrice() * quantity;
        } else if ("tractor".equalsIgnoreCase(itemType)) {
            var tractor = tractorService.getTractorById(itemId);
            if (tractor == null) {
                response.put("error", "Tractor not found");
                return response;
            }
            availableQuantity = tractor.getStockQuantity() - tractor.getSoldQuantity();
            if (quantity > availableQuantity) {
                response.put("error", "Requested quantity exceeds available stock");
                return response;
            }
            totalPrice = tractor.getPrice() * quantity;
        } else {
            response.put("error", "Invalid item type");
            return response;
        }

        // Create order
        Order order = new Order();
        order.setItemType(itemType);
        order.setItemId(itemId);
        order.setQuantity(quantity);
        order.setTotalPrice(totalPrice);
        order.setStatus("PENDING");
        order.setUserId(userId);
        try {
            order = orderRepository.save(order);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Failed to create order");
            return response;
        }

        // Initiate Razorpay payment
        String razorpayOrderId = razorpayService.initiatePayment(itemType, itemId, quantity);
        String keyId = razorpayService.getRazorpayConfig().getKeyId();

        response.put("keyId", keyId);
        response.put("razorpayOrderId", razorpayOrderId);
        response.put("orderId", order.getId());
        response.put("totalPrice", totalPrice);
        response.put("quantity", quantity);
        response.put("itemType", itemType);
        response.put("itemId", itemId);

        return response;
    }
}
    // Other methods unchanged...
