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
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY environment variable is missing. " +
                    "Stripe payments will not work until configured.");
        }

        if (stripeSecretKey.contains("your_stripe") || stripeSecretKey.startsWith("sk_test_your")) {
            throw new IllegalStateException("Invalid Stripe key detected (placeholder value). " +
                    "Please set a real STRIPE_SECRET_KEY from your Stripe dashboard.");
        }

        Stripe.apiKey = stripeSecretKey.trim();
        System.out.println("✅ Stripe initialized successfully with provided secret key.");
    }

    public String createPaymentIntent(BigDecimal amount) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            if (amountInCents < 50) {
                throw new IllegalArgumentException("Minimum payment amount is $0.50");
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

    public void confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if ("succeeded".equals(intent.getStatus())) {
                System.out.println("✅ Payment succeeded for PaymentIntent: " + paymentIntentId);
            } else {
                throw new RuntimeException("Payment not succeeded. Current status: " + intent.getStatus());
            }
        } catch (Exception e) {
            throw new RuntimeException("Payment verification failed: " + e.getMessage(), e);
        }
    }
}