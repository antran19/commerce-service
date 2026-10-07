package com.nexus.commerce.application.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentGatewayPort {

    CheckoutSession createCheckoutSession(String orderId, String buyerId, List<LineItem> lineItems,
                                           String successUrl, String cancelUrl);

    PaymentStatus retrieveSessionStatus(String sessionId);

    record LineItem(String name, BigDecimal unitPrice, int quantity) {
    }

    record CheckoutSession(String sessionId, String checkoutUrl) {
    }

    record PaymentStatus(boolean paid, String orderId, String buyerId) {
    }
}
