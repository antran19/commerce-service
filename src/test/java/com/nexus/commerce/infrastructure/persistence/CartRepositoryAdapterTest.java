package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CartRepositoryAdapter.class)
class CartRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("commerce_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private CartRepositoryAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    @Test
    void save_thenFindByBuyerId_roundTripsItems() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 2));
        adapter.save(cart);
        // Clears the persistence context so the read below hits a fresh proxy, the same way a
        // real HTTP request would after open-in-view is disabled -- without this, a lazy
        // @ElementCollection bug would stay hidden since @DataJpaTest keeps one session open
        // for the whole test method.
        entityManager.flush();
        entityManager.clear();

        Optional<Cart> reloaded = adapter.findByBuyerId("buyer-1");

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getItems()).hasSize(1);
        assertThat(reloaded.get().findItem("product-1").orElseThrow().getQuantity()).isEqualTo(2);
    }

    @Test
    void save_overwritesPreviousItemsOnUpdate() {
        Cart cart = Cart.create("buyer-2")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1));
        adapter.save(cart);

        Cart reloaded = adapter.findByBuyerId("buyer-2").orElseThrow();
        adapter.save(reloaded.removeItem("product-1"));

        Optional<Cart> afterRemoval = adapter.findByBuyerId("buyer-2");
        assertThat(afterRemoval.get().isEmpty()).isTrue();
    }

    @Test
    void findByBuyerId_returnsEmptyWhenNoCart() {
        assertThat(adapter.findByBuyerId("no-such-buyer")).isEmpty();
    }

    @Test
    void deleteInactiveSince_removesOnlyCartsNotTouchedSinceTheCutoff() {
        java.time.Instant old = java.time.Instant.now().minus(java.time.Duration.ofDays(40));
        CartItem item = CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("10.00"), 1);
        adapter.save(Cart.reconstitute(java.util.UUID.randomUUID().toString(), "buyer-stale",
                java.util.List.of(item), old, old));
        adapter.save(Cart.create("buyer-fresh").addItem(item));
        entityManager.flush();
        entityManager.clear();

        int deleted = adapter.deleteInactiveSince(java.time.Instant.now().minus(java.time.Duration.ofDays(30)));

        assertThat(deleted).isEqualTo(1);
        assertThat(adapter.findByBuyerId("buyer-stale")).isEmpty();
        assertThat(adapter.findByBuyerId("buyer-fresh")).isPresent();
    }
}
