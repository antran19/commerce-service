package com.nexus.commerce.infrastructure.persistence;

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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductStockRepositoryAdapter.class)
class ProductStockRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("commerce_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ProductStockRepositoryAdapter adapter;
    @Autowired private TestEntityManager entityManager;

    private static final Instant T1 = Instant.parse("2026-10-17T05:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-17T05:00:05Z");

    @Test
    void unknownProduct_hasZeroAvailable() {
        assertThat(adapter.availableQuantity("nope")).isZero();
    }

    @Test
    void applyIfNewer_storesThenUpdatesWithANewerEvent() {
        adapter.applyIfNewer("product-1", 5, T1);
        adapter.applyIfNewer("product-1", 3, T2);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.availableQuantity("product-1")).isEqualTo(3);
    }

    @Test
    void applyIfNewer_ignoresAnOlderEvent_soARedeliveryCannotRollTheLevelBack() {
        adapter.applyIfNewer("product-1", 3, T2);
        adapter.applyIfNewer("product-1", 5, T1);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.availableQuantity("product-1")).isEqualTo(3);
    }
}
