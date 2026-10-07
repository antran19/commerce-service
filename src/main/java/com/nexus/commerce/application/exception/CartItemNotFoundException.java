package com.nexus.commerce.application.exception;

import com.nexus.common.core.exception.NotFoundException;

public class CartItemNotFoundException extends NotFoundException {
    public CartItemNotFoundException(String productId) {
        super("CART_ITEM_NOT_FOUND", "Product not found in cart: " + productId);
    }
}
