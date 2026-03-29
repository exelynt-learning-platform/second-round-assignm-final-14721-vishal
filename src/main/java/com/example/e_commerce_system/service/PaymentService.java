package com.example.e_commerce_system.service;

import com.example.e_commerce_system.entity.OrderStatus;
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
    
    // You can add webhook handler later for real success/failure
    public void updateOrderStatus(String paymentIntentId, OrderStatus status) {
        // Implement logic to find order by paymentIntentId and update status
        // For assignment, you can call this from a success endpoint
    }
    public void confirmPayment(String paymentIntentId) {
        // In real project, verify with Stripe API
        // For now, just update status
        // You can inject OrderRepository and update status to PAID
        // orderRepository.findByPaymentIntentId(paymentIntentId).ifPresent(order -> {
        //     order.setStatus(OrderStatus.PAID);
        //     orderRepository.save(order);
        // });
        System.out.println("Payment confirmed for: " + paymentIntentId);
    }
}