package com.example.e_commerce_system.service;

import com.example.e_commerce_system.entity.OrderStatus;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {

    @Value("${stripe.secret-key}")   // Must be set as environment variable: STRIPE_SECRET_KEY
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            System.err.println("WARNING: Stripe secret key is not configured via environment variable!");
        }
        Stripe.apiKey = stripeSecretKey;
    }

    public String createPaymentIntent(BigDecimal amount) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            if (amountInCents < 50) {
                throw new RuntimeException("Amount too small. Minimum $0.50");
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
            throw new RuntimeException("Payment creation failed: " + e.getMessage(), e);
        }
    }

    // Improved confirmation with actual Stripe verification
    public void confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if ("succeeded".equals(intent.getStatus())) {
                // TODO: Update order status to PAID (inject OrderRepository if needed)
                System.out.println("Payment confirmed and succeeded for: " + paymentIntentId);
                // orderRepository.findByPaymentIntentId(...).ifPresent(... set PAID);
            } else {
                System.out.println("Payment status: " + intent.getStatus());
            }
        } catch (Exception e) {
            System.err.println("Failed to verify payment: " + e.getMessage());
        }
    }
}