package com.example.e_commerce_system.service;

import com.example.e_commerce_system.dto.*;
import com.example.e_commerce_system.entity.*;
import com.example.e_commerce_system.exception.ResourceNotFoundException;
import com.example.e_commerce_system.exception.UnauthorizedException;
import com.example.e_commerce_system.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, 
                        UserRepository userRepository,
                        CartRepository cartRepository, 
                        ProductRepository productRepository,
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public OrderResponse createOrder(String email, OrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        PaymentMethod paymentMethod = request.getPaymentMethod() != null 
                ? request.getPaymentMethod() 
                : PaymentMethod.COD;

        BigDecimal totalPrice = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(totalPrice);
        order.setPaymentMethod(paymentMethod);

        // CRITICAL: Use pessimistic locking to prevent race condition / overselling
        for (CartItem cartItem : cart.getItems()) {
            // This acquires a database lock (SELECT ... FOR UPDATE)
            Product product = productRepository.findByIdWithLock(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + cartItem.getProduct().getId()));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName() 
                        + ". Available: " + product.getStockQuantity());
            }

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtOrderTime(product.getPrice());
            order.getOrderItems().add(orderItem);

            // Deduct stock safely (inside locked transaction)
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        Order savedOrder = orderRepository.save(order);

        if (paymentMethod == PaymentMethod.STRIPE) {
            String paymentIntentId = paymentService.createPaymentIntent(totalPrice);
            savedOrder.setPaymentIntentId(paymentIntentId);
            orderRepository.save(savedOrder);
        }

        // Clear cart after successful order
        cart.getItems().clear();
        cartRepository.save(cart);

        return convertToOrderResponse(savedOrder);
    }

    private OrderResponse convertToOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemResponse resp = new OrderItemResponse();
                    resp.setProductId(item.getProduct().getId());
                    resp.setProductName(item.getProduct().getName());
                    resp.setQuantity(item.getQuantity());
                    resp.setPrice(item.getPriceAtOrderTime());
                    return resp;
                })
                .collect(Collectors.toList());

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setTotalPrice(order.getTotalPrice());
        response.setShippingAddress(order.getShippingAddress());
        response.setStatus(order.getStatus());
        response.setPaymentIntentId(order.getPaymentIntentId());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setItems(items);
        return response;
    }
}