package com.nexus.commerce.application.port.out;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentGatewayPort {

    CheckoutSession createCheckoutSession(String orderId, String buyerId, List<LineItem> lineItems,
                                           String successUrl, String cancelUrl);

    PaymentStatus retrieveSessionStatus(String sessionId);

    // paymentReference is the gateway's own id for the completed charge (Stripe's
    // PaymentIntent id) -- persisted on the Order once paid, so RefundOrderUseCase can
    // refund the exact charge later without needing the original checkout session again.
    void refund(String paymentReference);

    record LineItem(String name, BigDecimal unitPrice, int quantity) {
    }

    record CheckoutSession(String sessionId, String checkoutUrl) {
    }

    record PaymentStatus(boolean paid, String orderId, String buyerId, String paymentReference) {
    }
}
