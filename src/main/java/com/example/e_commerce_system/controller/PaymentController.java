package com.example.e_commerce_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.e_commerce_system.service.OrderService;
import com.example.e_commerce_system.service.PaymentService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public PaymentController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    // Called by frontend after successful Stripe payment
    @PostMapping("/confirm")
    public ResponseEntity<String> confirmPayment(@RequestParam String paymentIntentId) {
        paymentService.confirmPayment(paymentIntentId);
        return ResponseEntity.ok("Payment confirmed successfully");
    }
}