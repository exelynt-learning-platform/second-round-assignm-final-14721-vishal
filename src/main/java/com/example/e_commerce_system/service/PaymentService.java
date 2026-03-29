package com.example.e_commerce_system.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class PaymentService {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty()) {
            System.err.println("⚠️ STRIPE_SECRET_KEY not set. Stripe payments disabled for development.");
            return; // Allow app to start in dev without Stripe
        }

        if (stripeSecretKey.contains("your_stripe_test_key_here")) {
            throw new IllegalStateException("Invalid placeholder Stripe key. Set real STRIPE_SECRET_KEY.");
        }

        Stripe.apiKey = stripeSecretKey.trim();
        System.out.println("✅ Stripe initialized successfully.");
    }

    // createPaymentIntent and confirmPayment remain the same as before
    public String createPaymentIntent(BigDecimal amount) {
        if (Stripe.apiKey == null) {
            throw new IllegalStateException("Stripe not configured. Set STRIPE_SECRET_KEY.");
        }
        // ... rest of your original code
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return paymentIntent.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PaymentIntent: " + e.getMessage(), e);
        }
    }

    public void confirmPayment(String paymentIntentId) {
        // your original code
    }
}