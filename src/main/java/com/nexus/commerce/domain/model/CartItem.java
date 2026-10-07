package com.nexus.commerce.domain.model;

import java.math.BigDecimal;

public class CartItem {

    private final String productId;
    private final String sellerId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    private CartItem(String productId, String sellerId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static CartItem create(String productId, String sellerId, String productName,
                                   BigDecimal unitPrice, int quantity) {
        return new CartItem(productId, sellerId, productName, unitPrice, quantity);
    }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, sellerId, productName, unitPrice, newQuantity);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getProductId() { return productId; }
    public String getSellerId() { return sellerId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
