package com.nexus.commerce.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class OrderItemEmbeddable {

    @Column(name = "product_id")
    private String productId;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "quantity")
    private int quantity;

    protected OrderItemEmbeddable() {
    }

    public OrderItemEmbeddable(String productId, String sellerId, String productName,
                                BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public String getSellerId() { return sellerId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
