package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.domain.model.Order;
import com.nexus.common.core.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

// A "double click": two checkout requests for the same buyer start at the same moment, against a
// real database with the real use case (transaction, row lock and all). Mocks cannot show this.
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CheckoutConcurrencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("commerce_db").withUsername("nexus").withPassword("nexus");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired private StartCheckoutUseCase startCheckoutUseCase;
    @Autowired private CartRepositoryPort cartRepositoryPort;
    @Autowired private OrderRepositoryPort orderRepositoryPort;
    @Autowired private JdbcTemplate jdbc;

    private String buyerId;

    @BeforeEach
    void setUp() {
        buyerId = "buyer-" + UUID.randomUUID();
        // The catalog copy and stock level checkout re-reads (normally filled from Kafka events).
        jdbc.update("insert into product_snapshots(product_id, seller_id, name, price, status, updated_at) "
                + "values ('p-race', 'seller-1', 'Laptop X', 999.00, 'ACTIVE', now()) on conflict do nothing");
        jdbc.update("insert into product_stock_levels(product_id, available_quantity, as_of) "
                + "values ('p-race', 100, now()) on conflict do nothing");
        cartRepositoryPort.save(Cart.create(buyerId)
                .addItem(CartItem.create("p-race", "seller-1", "Laptop X", new BigDecimal("999.00"), 1)));
    }

    private List<Object> runTwoAtOnce(Callable<Object> first, Callable<Object> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<Object> task : List.of(first, second)) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    return task.call();
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        ready.await();
        go.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<Object> f : futures) {
            results.add(f.get());
        }
        pool.shutdown();
        return results;
    }

    private int outboxRowsFor(String orderId) {
        return jdbc.queryForObject("select count(*) from outbox where aggregate_id = ?", Integer.class, orderId);
    }

    // Without a key: one request wins, the other waits for the lock, then finds the cart emptied.
    @Test
    void twoSimultaneousCheckouts_createExactlyOneOrder() throws Exception {
        List<Object> results = runTwoAtOnce(
                () -> startCheckoutUseCase.checkout(buyerId, null),
                () -> startCheckoutUseCase.checkout(buyerId, null));

        List<Order> orders = orderRepositoryPort.findByBuyerId(buyerId);
        assertThat(orders).hasSize(1);
        assertThat(results).filteredOn(r -> r instanceof OrderResult).hasSize(1);
        assertThat(results).filteredOn(r -> r instanceof ConflictException)
                .singleElement()
                .hasFieldOrPropertyWithValue("errorCode", "CART_EMPTY");
        // OrderCreated + InventoryReservationRequested, once.
        assertThat(outboxRowsFor(orders.get(0).getId())).isEqualTo(2);
    }

    // With the same key: both callers get the SAME order, and it exists only once.
    @Test
    void twoSimultaneousCheckoutsWithTheSameKey_bothGetTheSameSingleOrder() throws Exception {
        List<Object> results = runTwoAtOnce(
                () -> startCheckoutUseCase.checkout(buyerId, "key-race"),
                () -> startCheckoutUseCase.checkout(buyerId, "key-race"));

        assertThat(results).allMatch(r -> r instanceof OrderResult);
        assertThat(((OrderResult) results.get(0)).id()).isEqualTo(((OrderResult) results.get(1)).id());
        assertThat(orderRepositoryPort.findByBuyerId(buyerId)).hasSize(1);
        assertThat(outboxRowsFor(((OrderResult) results.get(0)).id())).isEqualTo(2);
    }

    // The slow-network case: the retry arrives after the first request is long finished.
    @Test
    void retryWithTheSameKeyAfterTheFirstFinished_returnsTheFirstOrder() {
        OrderResult first = startCheckoutUseCase.checkout(buyerId, "key-retry");

        OrderResult retry = startCheckoutUseCase.checkout(buyerId, "key-retry");

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(orderRepositoryPort.findByBuyerId(buyerId)).hasSize(1);
    }
}
