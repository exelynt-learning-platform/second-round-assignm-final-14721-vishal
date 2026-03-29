package com.example.e_commerce_system.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.e_commerce_system.entity.Cart;
import com.example.e_commerce_system.entity.User;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}