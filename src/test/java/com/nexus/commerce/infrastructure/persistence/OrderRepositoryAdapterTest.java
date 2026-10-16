package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.commerce.domain.model.ShippingAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderRepositoryAdapter.class)
class OrderRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("commerce_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private OrderRepositoryAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    private Order newOrder(String buyerId, String auctionId) {
        return Order.create(buyerId, auctionId,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1)));
    }

    private Order orderCreatedAt(Instant createdAt, String auctionId, OrderStatus status) {
        return Order.reconstitute(java.util.UUID.randomUUID().toString(), "buyer-1", auctionId,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1)),
                new BigDecimal("999.00"), status, createdAt, null, null, null);
    }

    @Test
    void findUnpaidDirectOrdersCreatedBefore_returnsOnlyOldUnpaidDirectOrders() {
        Instant now = Instant.now();
        Order oldUnpaid = orderCreatedAt(now.minusSeconds(3600), null, OrderStatus.AWAITING_PAYMENT);
        Order recentUnpaid = orderCreatedAt(now.minusSeconds(60), null, OrderStatus.AWAITING_PAYMENT);
        Order oldAuctionOrder = orderCreatedAt(now.minusSeconds(3600), "auction-9", OrderStatus.AWAITING_PAYMENT);
        Order oldCancelled = orderCreatedAt(now.minusSeconds(3600), null, OrderStatus.CANCELLED);
        for (Order o : List.of(oldUnpaid, recentUnpaid, oldAuctionOrder, oldCancelled)) {
            adapter.save(o);
        }
        entityManager.flush();
        entityManager.clear();

        List<Order> due = adapter.findUnpaidDirectOrdersCreatedBefore(now.minusSeconds(1800));

        assertThat(due).extracting(Order::getId).containsExactly(oldUnpaid.getId());
    }

    @Test
    void save_thenFindById_roundTripsTheShippingAddress() {
        Order order = newOrder("buyer-1", null)
                .withShippingAddress(ShippingAddress.create("Nguyen Van A", "0901234567", "12 Le Loi", "HCM"));
        adapter.save(order);
        entityManager.flush();
        entityManager.clear();

        Order reloaded = adapter.findById(order.getId()).orElseThrow();

        assertThat(reloaded.getShippingAddress()).isEqualTo(order.getShippingAddress());
    }

    @Test
    void save_withoutAnAddress_reloadsWithANullAddress() {
        Order order = newOrder("buyer-1", null);
        adapter.save(order);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findById(order.getId()).orElseThrow().getShippingAddress()).isNull();
    }

    @Test
    void save_thenFindById_roundTripsAllFields() {
        Order order = newOrder("buyer-1", null);
        adapter.save(order);
        // See CartRepositoryAdapterTest for why flush+clear matters here.
        entityManager.flush();
        entityManager.clear();

        Optional<Order> reloaded = adapter.findById(order.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getBuyerId()).isEqualTo("buyer-1");
        assertThat(reloaded.get().getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(reloaded.get().getItems()).hasSize(1);
        assertThat(reloaded.get().getTotalAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    void findByAuctionId_locatesTheOrderCreatedFromThatAuction() {
        Order order = newOrder("buyer-1", "auction-1");
        adapter.save(order);

        Optional<Order> reloaded = adapter.findByAuctionId("auction-1");

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getId()).isEqualTo(order.getId());
    }

    @Test
    void findByBuyerId_returnsOrdersNewestFirst() {
        Order first = newOrder("buyer-2", null);
        adapter.save(first);
        Order second = newOrder("buyer-2", null);
        adapter.save(second);

        List<Order> orders = adapter.findByBuyerId("buyer-2");

        assertThat(orders).hasSize(2);
    }

    @Test
    void findAll_returnsOrdersAcrossEveryBuyer() {
        Order fromBuyerA = newOrder("buyer-4", null);
        Order fromBuyerB = newOrder("buyer-5", null);
        adapter.save(fromBuyerA);
        adapter.save(fromBuyerB);

        List<Order> orders = adapter.findAll();

        assertThat(orders).extracting(Order::getId).contains(fromBuyerA.getId(), fromBuyerB.getId());
    }

    @Test
    void save_persistsPaidStatusAndPaidAt() {
        Order order = newOrder("buyer-3", null);
        adapter.save(order);
        Instant paidAt = Instant.now();

        adapter.save(order.markPaid(paidAt, "pi_test"));

        Order reloaded = adapter.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);
        // Postgres TIMESTAMPTZ has microsecond precision, Instant.now() can carry nanoseconds --
        // compare truncated to the column's precision instead of exact equality.
        assertThat(reloaded.getPaidAt()).isCloseTo(paidAt, within(1, java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    void findBySellerId_returnsOnlyOrdersContainingThatSellersItems() {
        Order order = Order.create("buyer-7", null,
                List.of(new OrderItem("product-1", "seller-target", "Widget", new BigDecimal("50.00"), 1)));
        adapter.save(order);
        Order other = newOrder("buyer-7", null);
        adapter.save(other);

        List<Order> orders = adapter.findBySellerId("seller-target");

        assertThat(orders).extracting(Order::getId).containsExactly(order.getId());
    }

    @Test
    void save_persistsShippedAt() {
        Order order = newOrder("buyer-6", null).markPaid(Instant.now(), "pi_test");
        adapter.save(order);
        Instant shippedAt = Instant.now();

        adapter.save(order.markShipped(shippedAt));

        Order reloaded = adapter.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getShippedAt()).isCloseTo(shippedAt, within(1, java.time.temporal.ChronoUnit.MILLIS));
    }
}
