package com.example.e_commerce_system.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.e_commerce_system.dto.CartItemRequest;
import com.example.e_commerce_system.dto.CartResponse;
import com.example.e_commerce_system.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody CartItemRequest request, 
                                                  Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.addToCart(email, request);
        return ResponseEntity.ok(response);
    }

    // NEW: Update quantity
    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateCartItem(@Valid @RequestBody CartItemRequest request, 
                                                       Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.updateCartItem(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.getCart(email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<CartResponse> removeFromCart(@PathVariable Long productId, 
                                                       Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.removeFromCart(email, productId);
        return ResponseEntity.ok(response);
    }
}