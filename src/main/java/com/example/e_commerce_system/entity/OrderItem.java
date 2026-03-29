package com.example.e_commerce_system.entity;

import jakarta.persistence.*;


import java.math.BigDecimal;

@Entity

public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getPriceAtOrderTime() {
		return priceAtOrderTime;
	}

	public void setPriceAtOrderTime(BigDecimal priceAtOrderTime) {
		this.priceAtOrderTime = priceAtOrderTime;
	}

	@ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    private Product product;

    private int quantity;

    private BigDecimal priceAtOrderTime;  // Snapshot for data integrity
}