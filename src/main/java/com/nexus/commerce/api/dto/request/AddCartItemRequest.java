package com.nexus.commerce.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

// Only productId and quantity are used. sellerId/productName/unitPrice are still accepted so
// older clients keep working, but they are ignored: commerce prices the item from its own copy
// of the catalog (see AddCartItemUseCase).
public record AddCartItemRequest(
        @NotBlank String productId,
        String sellerId,
        String productName,
        BigDecimal unitPrice,
        @Min(value = 1, message = "quantity must be at least 1") int quantity) {
}
