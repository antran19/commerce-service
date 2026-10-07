package com.nexus.commerce.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    void markPaid_setsStatusAndPaidAt() {
        Order order = Order.create("buyer-1", null,
                List.of(item("product-1", "seller-1", new BigDecimal("100.00"), 1)));
        Instant now = Instant.now();

        Order paid = order.markPaid(now);

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getPaidAt()).isEqualTo(now);
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
}
