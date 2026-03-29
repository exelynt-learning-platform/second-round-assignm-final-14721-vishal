package com.example.e_commerce_system.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.e_commerce_system.entity.Product;


public interface ProductRepository extends JpaRepository<Product, Long> {
	
	// In ProductRepository.java

	@Query("SELECT p FROM Product p WHERE p.id = :id")
	Optional<Product> findByIdWithLock(@Param("id") Long id);
}