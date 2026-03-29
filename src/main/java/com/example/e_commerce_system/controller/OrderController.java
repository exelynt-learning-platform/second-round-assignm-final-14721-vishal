package com.example.e_commerce_system.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.e_commerce_system.dto.OrderRequest;
import com.example.e_commerce_system.dto.OrderResponse;
import com.example.e_commerce_system.service.OrderService;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request, 
                                                     Authentication authentication) {
        String email = authentication.getName();
        OrderResponse response = orderService.createOrder(email, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        List<OrderResponse> orders = orderService.getMyOrders(email);
        return ResponseEntity.ok(orders);
    }

    // NEW: Get single order
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId, 
                                                      Authentication authentication) {
        String email = authentication.getName();
        OrderResponse order = orderService.getOrderById(orderId, email);
        return ResponseEntity.ok(order);
    }
}