package com.nexus.commerce.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private OrderItem item(String productId, String sellerId, BigDecimal price, int qty) {
        return new OrderItem(productId, sellerId, "Product " + productId, price, qty);
    }

    @Test
    void create_startsAwaitingPaymentAndComputesTotal() {
        Order order = Order.create("buyer-1", null,
                List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 2)));

        assertThat(order.getId()).isNotBlank();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(order.getPaidAt()).isNull();
        assertThat(order.getAuctionId()).isNull();
    }

    @Test
    void create_fromAuctionCarriesAuctionId() {
        Order order = Order.create("buyer-1", "auction-1",
                List.of(item("product-1", "seller-1", new BigDecimal("150.00"), 1)));

        assertThat(order.getAuctionId()).isEqualTo("auction-1");
    }

    @Test
    void markPaid_setsStatusPaidAtAndPaymentReference() {
        Order order = Order.create("buyer-1", null,
                List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 1)));
        Instant now = Instant.now();

        Order paid = order.markPaid(now, "pi_123");

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getPaidAt()).isEqualTo(now);
        assertThat(paid.getPaymentReference()).isEqualTo("pi_123");
    }

    @Test
    void refund_setsStatusRefundedAndKeepsPaymentReference() {
        Order order = Order.create("buyer-1", null,
                        List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 1)))
                .markPaid(Instant.now(), "pi_123");

        Order refunded = order.refund();

        assertThat(refunded.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(refunded.getPaymentReference()).isEqualTo("pi_123");
        assertThat(refunded.getPaidAt()).isEqualTo(order.getPaidAt());
    }

    @Test
    void markShipped_setsShippedAt() {
        Order order = Order.create("buyer-1", null,
                        List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 1)))
                .markPaid(Instant.now(), "pi_123");
        Instant now = Instant.now();

        Order shipped = order.markShipped(now);

        assertThat(shipped.getShippedAt()).isEqualTo(now);
        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void cancel_setsStatusCancelled() {
        Order order = Order.create("buyer-1", null,
                List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 1)));

        assertThat(order.cancel().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void singleSellerIdOrNull_returnsSellerWhenAllItemsShareOne() {
        Order order = Order.create("buyer-1", null, List.of(
                item("product-1", "seller-1", new BigDecimal("100.00"), 1),
                item("product-2", "seller-1", new BigDecimal("50.00"), 1)));

        assertThat(order.singleSellerIdOrNull()).isEqualTo("seller-1");
    }

    @Test
    void singleSellerIdOrNull_returnsNullWhenItemsSpanMultipleSellers() {
        Order order = Order.create("buyer-1", null, List.of(
                item("product-1", "seller-1", new BigDecimal("100.00"), 1),
                item("product-2", "seller-2", new BigDecimal("50.00"), 1)));

        assertThat(order.singleSellerIdOrNull()).isNull();
    }

    @Test
    void refundLatePayment_movesCancelledOrderToRefundedWithChargeRecorded() {
        Instant when = Instant.parse("2026-10-16T10:00:00Z");
        Order order = Order.create("buyer-1", null, List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 1)))
                .cancel().refundLatePayment(when, "pi_late");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getPaymentReference()).isEqualTo("pi_late");
        assertThat(order.getPaidAt()).isEqualTo(when);
    }

    @Test
    void refundLatePayment_rejectsOrderThatIsNotCancelled() {
        Order order = Order.create("buyer-1", null, List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 1)));

        assertThatThrownBy(() -> order.refundLatePayment(Instant.now(), "pi_late"))
                .isInstanceOf(IllegalStateException.class);
    }
}
