package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.OrderNotFoundException;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.common.core.exception.ConflictException;
import com.nexus.common.core.exception.ForbiddenException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

// An invoice is derived from a paid order, not stored: the order already holds everything an
// invoice needs (items, total, paidAt), and the number is a pure function of the order, so there
// is nothing to keep in sync and no migration. A refunded order keeps its invoice -- it was
// still issued; the orderStatus field tells the reader it was later refunded.
public class GetOrderInvoiceUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    public GetOrderInvoiceUseCase(OrderRepositoryPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public InvoiceResult getInvoice(String orderId, String callerId, boolean canViewAny) {
        Order order = orderRepositoryPort.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        // Authorize before revealing anything about the order's state.
        boolean isBuyer = order.getBuyerId().equals(callerId);
        boolean isSellerOfAnItem = order.getItems().stream().anyMatch(item -> item.getSellerId().equals(callerId));
        if (!canViewAny && !isBuyer && !isSellerOfAnItem) {
            throw new ForbiddenException("NOT_ALLOWED_TO_VIEW_INVOICE",
                    "Only the buyer, a seller in the order, or staff may view this invoice");
        }
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.REFUNDED) {
            throw new ConflictException("INVOICE_NOT_AVAILABLE",
                    "Order " + orderId + " has not been paid, so it has no invoice");
        }

        List<OrderItemResult> items = order.getItems().stream().map(OrderItemResult::from).toList();
        return new InvoiceResult(invoiceNumber(order), order.getId(), order.getBuyerId(), items,
                order.getTotalAmount(), order.getStatus().name(), order.getPaidAt());
    }

    // INV-<year paid>-<first 8 chars of the order id>, e.g. INV-2026-3F9A1C2B
    private static String invoiceNumber(Order order) {
        int year = order.getPaidAt().atZone(ZoneOffset.UTC).getYear();
        String compactId = order.getId().replace("-", "").toUpperCase(Locale.ROOT);
        return "INV-" + year + "-" + compactId.substring(0, Math.min(8, compactId.length()));
    }
}
