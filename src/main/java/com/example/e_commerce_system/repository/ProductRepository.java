package com.example.e_commerce_system.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.e_commerce_system.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}