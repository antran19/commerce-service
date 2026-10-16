package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;

import java.util.List;

public class CreateCheckoutSessionUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;

    public CreateCheckoutSessionUseCase(OrderRepositoryPort orderRepositoryPort,
                                         PaymentGatewayPort paymentGatewayPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
    }

    public PaymentGatewayPort.CheckoutSession createSession(String orderId, String callerId,
                                                             String successUrl, String cancelUrl) {
        Order order = orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getBuyerId().equals(callerId)) {
            throw new ForbiddenException("NOT_THE_BUYER", "Only the order's buyer can pay for it");
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new ConflictException("ORDER_NOT_AWAITING_PAYMENT",
                    "Order " + orderId + " is not awaiting payment");
        }

        // The seller needs to know where to ship, so no payment before an address is set.
        if (order.getShippingAddress() == null) {
            throw new ConflictException("SHIPPING_ADDRESS_REQUIRED",
                    "Set a shipping address for order " + orderId + " before paying");
        }
        List<PaymentGatewayPort.LineItem> lineItems = order.getItems().stream()
                .map(i -> new PaymentGatewayPort.LineItem(i.getProductName(), i.getUnitPrice(), i.getQuantity()))
                .toList();

        return paymentGatewayPort.createCheckoutSession(orderId, callerId, lineItems, successUrl, cancelUrl);
    }
}
