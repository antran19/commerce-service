package com.nexus.commerce.infrastructure.messaging;

import com.nexus.commerce.infrastructure.persistence.OutboxJpaRepository;
import com.nexus.common.events.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OutboxEventPublisherAdapter.class, JacksonAutoConfiguration.class})
class OutboxEventPublisherAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("commerce_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private OutboxEventPublisherAdapter adapter;
    @Autowired private OutboxJpaRepository outboxJpaRepository;

    @Test
    void publish_writesUnpublishedOutboxRow() {
        adapter.publish(new OrderCreatedEvent("order-1", null, "product-1", "seller-1", "buyer-1",
                new BigDecimal("150.00")));

        var rows = outboxJpaRepository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getEventType()).isEqualTo("OrderCreated");
        assertThat(rows.get(0).getPublishedAt()).isNull();
    }
}
