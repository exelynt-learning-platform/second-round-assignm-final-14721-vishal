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

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty() || 
            stripeSecretKey.contains("your_stripe_test_key_here")) {
            throw new IllegalStateException("Stripe secret key is not configured properly. " +
                    "Please set the STRIPE_SECRET_KEY environment variable.");
        }
        Stripe.apiKey = stripeSecretKey.trim();
        System.out.println("Stripe API key initialized successfully.");
    }

    public String createPaymentIntent(BigDecimal amount) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            if (amountInCents < 50) {
                throw new IllegalArgumentException("Amount too small. Minimum $0.50");
            }

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return paymentIntent.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Stripe PaymentIntent: " + e.getMessage(), e);
        }
    }

    // Called after frontend confirms payment
    public void confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if ("succeeded".equals(intent.getStatus())) {
                System.out.println("✅ Payment confirmed successfully for: " + paymentIntentId);
                // TODO: Update order status to PAID here (inject OrderRepository if needed)
            } else {
                throw new RuntimeException("Payment not succeeded. Status: " + intent.getStatus());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify payment: " + e.getMessage(), e);
        }
    }
}