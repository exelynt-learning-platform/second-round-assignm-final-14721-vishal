package com.example.e_commerce_system.service;

import com.example.e_commerce_system.dto.OrderItemResponse;
import com.example.e_commerce_system.dto.OrderRequest;
import com.example.e_commerce_system.dto.OrderResponse;
import com.example.e_commerce_system.entity.*;
import com.example.e_commerce_system.exception.ResourceNotFoundException;
import com.example.e_commerce_system.exception.UnauthorizedException;
import com.example.e_commerce_system.repository.CartRepository;
import com.example.e_commerce_system.repository.OrderRepository;
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
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        CartRepository cartRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
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

        // Create Order
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(totalPrice);
     //   order.setPaymentMethod(request.getPaymentMethod());   // NEW

     // Replace this line:
     // order.setPaymentMethod(request.getPaymentMethod());

     // With this safe code:
     PaymentMethod method = (request.getPaymentMethod() != null) ? request.getPaymentMethod() : PaymentMethod.COD;
     order.setPaymentMethod(method);
        
        // Add Order Items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtOrderTime(cartItem.getProduct().getPrice());
            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Handle Payment based on method
        if (request.getPaymentMethod() == PaymentMethod.STRIPE) {
            try {
                String paymentIntentId = paymentService.createPaymentIntent(totalPrice);
                savedOrder.setPaymentIntentId(paymentIntentId);
                orderRepository.save(savedOrder);
                System.out.println("Stripe PaymentIntent created: " + paymentIntentId);
            } catch (Exception e) {
                System.err.println("Stripe failed, order saved as PENDING: " + e.getMessage());
            }
        } else if (request.getPaymentMethod() == PaymentMethod.COD) {
            // For COD, we can mark it as CONFIRMED or keep PENDING
            savedOrder.setStatus(OrderStatus.PENDING); // or PAID if you want
            orderRepository.save(savedOrder);
            System.out.println("COD Order created successfully");
        }

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return convertToOrderResponse(savedOrder);
    }
    
    
 // ✅ This method was missing - Now Added
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
        response.setItems(items);
        
        // ADD THIS LINE
        response.setPaymentMethod(order.getPaymentMethod());

        return response;
    }
}