package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.commerce.domain.model.ShippingAddress;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;
import org.springframework.transaction.annotation.Transactional;

// The buyer tells us where to ship before paying. One use case for both order types: a direct
// checkout order and an order auto-created from a won auction (which has no address yet) are
// both AWAITING_PAYMENT, so the same rule covers them. Calling it again replaces the address
// as long as the order is still unpaid.
public class SetShippingAddressUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public SetShippingAddressUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    // @Transactional is required: findByIdForUpdate takes a PESSIMISTIC_WRITE lock.
    @Transactional
    public OrderResult setShippingAddress(String orderId, String callerId, String recipientName, String phone,
                                          String addressLine, String city) {
        Order order = orderRepositoryPort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getBuyerId().equals(callerId)) {
            throw new ForbiddenException("NOT_THE_BUYER", "Only the order's buyer can set its shipping address");
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new ConflictException("ORDER_NOT_AWAITING_PAYMENT",
                    "Order " + orderId + " is no longer awaiting payment, its shipping address can't be changed");
        }
        ShippingAddress address = ShippingAddress.create(recipientName, phone, addressLine, city);
        return OrderResult.from(orderRepositoryPort.save(order.withShippingAddress(address)));
    }
}
