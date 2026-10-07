package com.nexus.commerce.application.exception;

import com.nexus.common.core.exception.NotFoundException;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(String id) {
        super("ORDER_NOT_FOUND", "Order not found: " + id);
    }
}
