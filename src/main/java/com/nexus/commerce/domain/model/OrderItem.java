package com.nexus.commerce.domain.model;

import java.math.BigDecimal;

// Immutable snapshot of a product at the moment an order was created -- price/name must never
// change retroactively even if the catalog entry changes later.
public class OrderItem {

    private final String productId;
    private final String sellerId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public OrderItem(String productId, String sellerId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderItem fromCartItem(CartItem item) {
        return new OrderItem(item.getProductId(), item.getSellerId(), item.getProductName(),
                item.getUnitPrice(), item.getQuantity());
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
