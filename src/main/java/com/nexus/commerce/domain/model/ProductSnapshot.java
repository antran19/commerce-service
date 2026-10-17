package com.nexus.commerce.domain.model;

import java.math.BigDecimal;

// What commerce-service knows about a catalog product: enough to price a cart item and tell
// whether it can still be bought, without calling catalog-service. Status is the catalog's own
// value (DRAFT/ACTIVE/INACTIVE/SOLD) kept as a string, since the catalog owns that enum.
public class ProductSnapshot {

    private static final String PURCHASABLE_STATUS = "ACTIVE";

    private final String productId;
    private final String sellerId;
    private final String name;
    private final BigDecimal price;
    private final String status;

    private ProductSnapshot(String productId, String sellerId, String name, BigDecimal price, String status) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.name = name;
        this.price = price;
        this.status = status;
    }

    // Catalog creates every product as DRAFT; it becomes buyable once a ProductStatusChanged
    // event moves it to ACTIVE.
    public static ProductSnapshot create(String productId, String sellerId, String name, BigDecimal price) {
        return new ProductSnapshot(productId, sellerId, name, price, "DRAFT");
    }

    public static ProductSnapshot reconstitute(String productId, String sellerId, String name, BigDecimal price,
                                               String status) {
        return new ProductSnapshot(productId, sellerId, name, price, status);
    }

    public ProductSnapshot withStatus(String newStatus) {
        return new ProductSnapshot(productId, sellerId, name, price, newStatus);
    }

    public ProductSnapshot withName(String newName) {
        return new ProductSnapshot(productId, sellerId, newName, price, status);
    }

    public boolean isPurchasable() {
        return PURCHASABLE_STATUS.equals(status);
    }

    public CartItem toCartItem(int quantity) {
        return CartItem.create(productId, sellerId, name, price, quantity);
    }

    public String getProductId() { return productId; }
    public String getSellerId() { return sellerId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
}
