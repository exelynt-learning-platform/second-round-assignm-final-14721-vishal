package com.example.e_commerce_system.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.e_commerce_system.entity.Order;
import com.example.e_commerce_system.entity.User;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}