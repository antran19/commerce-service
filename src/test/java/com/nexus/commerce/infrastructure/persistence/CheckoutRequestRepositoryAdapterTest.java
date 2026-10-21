package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CheckoutRequestRepositoryAdapter.class, OrderRepositoryAdapter.class})
class CheckoutRequestRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("commerce_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private CheckoutRequestRepositoryAdapter adapter;
    @Autowired private OrderRepositoryAdapter orders;
    @Autowired private TestEntityManager entityManager;

    private String newOrderId(String buyerId) {
        return orders.save(Order.create(buyerId, null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("10.00"), 1)))).getId();
    }

    @Test
    void save_thenFind_returnsTheOrderForThatBuyerAndKey() {
        String orderId = newOrderId("buyer-1");

        adapter.save("buyer-1", "key-1", orderId);

        assertThat(adapter.findOrderId("buyer-1", "key-1")).contains(orderId);
        assertThat(adapter.findOrderId("buyer-1", "other-key")).isEmpty();
        assertThat(adapter.findOrderId("buyer-2", "key-1")).isEmpty();
    }

    // The database itself refuses a second order for the same (buyer, key), even if two requests
    // got past the application-level check.
    @Test
    void save_sameBuyerAndKeyTwice_isRejectedByTheUniqueConstraint() {
        String first = newOrderId("buyer-1");
        String second = newOrderId("buyer-1");
        adapter.save("buyer-1", "key-1", first);
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save("buyer-1", "key-1", second);
            entityManager.flush();
        }).hasMessageContaining("uq_checkout_requests_buyer_key");
    }

    @Test
    void sameKeyForDifferentBuyers_isAllowed() {
        adapter.save("buyer-1", "key-1", newOrderId("buyer-1"));
        adapter.save("buyer-2", "key-1", newOrderId("buyer-2"));
        entityManager.flush();

        assertThat(adapter.findOrderId("buyer-2", "key-1")).isPresent();
    }

    @Test
    void deleteOlderThan_removesOnlyRecordsBeforeTheCutoff() {
        adapter.save("buyer-1", "old", newOrderId("buyer-1"));
        entityManager.flush();
        // Age the first record by hand (the adapter stamps "now").
        entityManager.getEntityManager().createNativeQuery(
                        "update checkout_requests set created_at = :t where idempotency_key = 'old'")
                .setParameter("t", Instant.now().minus(3, ChronoUnit.DAYS)).executeUpdate();
        adapter.save("buyer-1", "recent", newOrderId("buyer-1"));
        entityManager.flush();

        int removed = adapter.deleteOlderThan(Instant.now().minus(1, ChronoUnit.DAYS));

        assertThat(removed).isEqualTo(1);
        assertThat(adapter.findOrderId("buyer-1", "old")).isEmpty();
        assertThat(adapter.findOrderId("buyer-1", "recent")).isPresent();
    }
}
