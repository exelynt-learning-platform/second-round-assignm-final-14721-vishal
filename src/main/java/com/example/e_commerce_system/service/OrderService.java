package com.example.e_commerce_system.service;

import com.example.e_commerce_system.dto.OrderItemResponse;
import com.example.e_commerce_system.dto.OrderRequest;
import com.example.e_commerce_system.dto.OrderResponse;
import com.example.e_commerce_system.entity.*;
import com.example.e_commerce_system.exception.ResourceNotFoundException;
import com.example.e_commerce_system.exception.UnauthorizedException;
import com.example.e_commerce_system.repository.CartRepository;
import com.example.e_commerce_system.repository.OrderRepository;
import com.example.e_commerce_system.repository.ProductRepository;
import com.example.e_commerce_system.repository.UserRepository;
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
    private final ProductRepository productRepository;   // Added for stock deduction
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        CartRepository cartRepository, ProductRepository productRepository,
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
            throw new RuntimeException("Cart is empty. Please add items before checkout.");
        }

        BigDecimal totalPrice = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // FIXED: Safe PaymentMethod handling (resolves Critical Bug)
        PaymentMethod method = (request.getPaymentMethod() != null) 
                ? request.getPaymentMethod() 
                : PaymentMethod.COD;

        // Create Order
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(totalPrice);
        order.setPaymentMethod(method);

        // Add Order Items + Deduct Stock
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtOrderTime(cartItem.getProduct().getPrice());
            order.getOrderItems().add(orderItem);

            // Deduct stock
            Product product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        Order savedOrder = orderRepository.save(order);

        // Handle Payment
        if (method == PaymentMethod.STRIPE) {
            try {
                String paymentIntentId = paymentService.createPaymentIntent(totalPrice);
                savedOrder.setPaymentIntentId(paymentIntentId);
                orderRepository.save(savedOrder);
            } catch (Exception e) {
                throw new RuntimeException("Payment initialization failed: " + e.getMessage());
            }
        } else {
            savedOrder.setStatus(OrderStatus.PENDING); // COD
            orderRepository.save(savedOrder);
        }

        // Clear cart only after successful order creation
        cart.getItems().clear();
        cartRepository.save(cart);

        return convertToOrderResponse(savedOrder);
    }

    // getMyOrders - Already implemented, kept with minor improvement
    public List<OrderResponse> getMyOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return orderRepository.findByUser(user).stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long orderId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only view your own orders");
        }
        return convertToOrderResponse(order);
    }

    private OrderResponse convertToOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemResponse response = new OrderItemResponse();
                    response.setProductId(item.getProduct().getId());
                    response.setProductName(item.getProduct().getName());
                    response.setQuantity(item.getQuantity());
                    response.setPrice(item.getPriceAtOrderTime());
                    return response;
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