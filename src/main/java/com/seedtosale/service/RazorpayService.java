package com.seedtosale.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seedtosale.config.RazorpayConfig;
import com.seedtosale.model.Seed;
import com.seedtosale.model.Tractor;

@Service
public class RazorpayService {

    private final RazorpayConfig razorpayConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SeedService seedService;
    private final TractorService tractorService;

    @Autowired
    public RazorpayService(RazorpayConfig razorpayConfig, SeedService seedService, TractorService tractorService) {
        this.razorpayConfig = razorpayConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.seedService = seedService;
        this.tractorService = tractorService;
    }

    public RazorpayConfig getRazorpayConfig() {
        return razorpayConfig;
    }

    public String initiatePayment(String itemType, Long itemId, int quantity) {
        try {
            // Calculate total amount in paise
            double unitPrice = 0.0;
            if ("seed".equalsIgnoreCase(itemType)) {
                Seed seed = seedService.getSeedById(itemId);
                if (seed != null) {
                    unitPrice = seed.getPrice();
                }
            } else if ("tractor".equalsIgnoreCase(itemType)) {
                Tractor tractor = tractorService.getTractorById(itemId);
                if (tractor != null) {
                    unitPrice = tractor.getPrice();
                }
            }
            int amountInPaise = (int) Math.round(unitPrice * quantity * 100);

            // Prepare payload for Razorpay order creation
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("amount", amountInPaise); // Amount in paise
            payloadMap.put("currency", "INR");
            payloadMap.put("receipt", "rcpt_" + System.currentTimeMillis());
            payloadMap.put("payment_capture", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = razorpayConfig.getKeyId() + ":" + razorpayConfig.getKeySecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payloadMap, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    razorpayConfig.getBaseUrl() + "/v1/orders",
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
                String orderId = (String) responseBody.get("id");
                // Return orderId to be used in frontend Razorpay checkout
                return orderId;
            } else {
                return "ERROR: Failed to create Razorpay order.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: Exception during Razorpay payment initiation.";
        }
    }

    public boolean verifyPayment(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String generatedSignature = generateHMACSHA256(payload, razorpayConfig.getKeySecret());
            System.out.println("Verifying Razorpay payment:");
            System.out.println("Payload: " + payload);
            System.out.println("Generated Signature: " + generatedSignature);
            System.out.println("Received Signature: " + razorpaySignature);
            return generatedSignature.equals(razorpaySignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handleWebhook(String payload) {
        // Log or parse JSON and handle accordingly
        System.out.println("Received Razorpay Webhook Payload: " + payload);

        // TODO: Validate signature, parse JSON, update DB accordingly
        try {
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            String event = (String) webhookData.get("event");

            if ("payment.captured".equalsIgnoreCase(event)) {
                System.out.println("Payment captured: " + webhookData);
                // Update payment status in DB
            } else {
                System.out.println("Unhandled event: " + event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateHMACSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes());
        // Convert to hex string instead of Base64
        StringBuilder hexString = new StringBuilder(2 * rawHmac.length);
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
