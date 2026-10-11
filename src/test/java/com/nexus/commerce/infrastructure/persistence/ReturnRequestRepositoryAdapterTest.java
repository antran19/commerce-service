package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.ReturnRequest;
import com.nexus.commerce.domain.model.ReturnRequestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReturnRequestRepositoryAdapter.class, OrderRepositoryAdapter.class})
class ReturnRequestRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("commerce_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private ReturnRequestRepositoryAdapter adapter;
    @Autowired private OrderRepositoryAdapter orderAdapter;

    private String persistOrder() {
        Order order = Order.create("buyer-1", null,
                List.of(new OrderItem("product-1", "seller-1", "Laptop X", new BigDecimal("100.00"), 1)));
        orderAdapter.save(order);
        return order.getId();
    }

    @Test
    void save_thenFindById_roundTripsAllFields() {
        String orderId = persistOrder();
        ReturnRequest returnRequest = ReturnRequest.create(orderId, "Wrong item received");

        adapter.save(returnRequest);
        Optional<ReturnRequest> reloaded = adapter.findById(returnRequest.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getOrderId()).isEqualTo(orderId);
        assertThat(reloaded.get().getReason()).isEqualTo("Wrong item received");
        assertThat(reloaded.get().getStatus()).isEqualTo(ReturnRequestStatus.REQUESTED);
    }

    @Test
    void save_persistsReceivedResolution() {
        String orderId = persistOrder();
        ReturnRequest returnRequest = ReturnRequest.create(orderId, "Wrong item received");
        adapter.save(returnRequest);

        adapter.save(returnRequest.receive("admin-1"));

        ReturnRequest reloaded = adapter.findById(returnRequest.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReturnRequestStatus.RECEIVED);
        assertThat(reloaded.getResolvedBy()).isEqualTo("admin-1");
        assertThat(reloaded.getResolvedAt()).isNotNull();
    }

    @Test
    void findByOrderId_returnsOnlyThatOrdersRequests() {
        String orderId = persistOrder();
        String otherOrderId = persistOrder();
        adapter.save(ReturnRequest.create(orderId, "reason 1"));
        adapter.save(ReturnRequest.create(otherOrderId, "reason 2"));

        List<ReturnRequest> results = adapter.findByOrderId(orderId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOrderId()).isEqualTo(orderId);
    }
}
