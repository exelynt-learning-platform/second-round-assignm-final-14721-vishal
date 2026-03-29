package com.example.e_commerce_system.service;

import com.example.e_commerce_system.dto.CartItemRequest;
import com.example.e_commerce_system.dto.CartItemResponse;
import com.example.e_commerce_system.dto.CartResponse;
import com.example.e_commerce_system.entity.Cart;
import com.example.e_commerce_system.entity.CartItem;
import com.example.e_commerce_system.entity.Product;
import com.example.e_commerce_system.entity.User;
import com.example.e_commerce_system.exception.ResourceNotFoundException;
import com.example.e_commerce_system.repository.CartRepository;
import com.example.e_commerce_system.repository.ProductRepository;
import com.example.e_commerce_system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartResponse addToCart(String email, CartItemRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Find existing item (if any)
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int existingQty = (existingItem != null) ? existingItem.getQuantity() : 0;
        int newTotalQty = existingQty + request.getQuantity();

        // FIXED: Check stock against combined quantity
        if (product.getStockQuantity() < newTotalQty) {
            throw new RuntimeException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        if (existingItem != null) {
            existingItem.setQuantity(newTotalQty);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
        return getCart(email);
    }

    @Transactional
    public CartResponse updateCartItem(String email, CartItemRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        if (request.getQuantity() <= 0) {
            cart.getItems().remove(existingItem);
        } else {
            // Basic stock check on update
            if (product.getStockQuantity() < request.getQuantity()) {
                throw new RuntimeException("Insufficient stock for update");
            }
            existingItem.setQuantity(request.getQuantity());
        }

        cartRepository.save(cart);
        return getCart(email);
    }

    public CartResponse getCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    CartItemResponse response = new CartItemResponse();
                    response.setProductId(item.getProduct().getId());
                    response.setProductName(item.getProduct().getName());
                    response.setPrice(item.getProduct().getPrice());
                    response.setQuantity(item.getQuantity());
                    return response;
                })
                .collect(Collectors.toList());

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setItems(items);
        response.setTotalPrice(total);
        return response;
    }

    @Transactional
    public CartResponse removeFromCart(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
        return getCart(email);
    }
}