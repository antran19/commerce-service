package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.commerce.infrastructure.persistence.entity.OrderItemEmbeddable;
import com.nexus.commerce.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Order> findById(String id) {
        return UuidIds.tryParse(id).flatMap(jpaRepository::findById).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdForUpdate(String id) {
        return UuidIds.tryParse(id).flatMap(jpaRepository::findByIdForUpdate).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByAuctionId(String auctionId) {
        return jpaRepository.findByAuctionId(auctionId).map(this::toDomain);
    }

    @Override
    public List<Order> findByBuyerId(String buyerId) {
        return jpaRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public Order save(Order order) {
        List<OrderItemEmbeddable> items = order.getItems().stream()
                .map(i -> new OrderItemEmbeddable(i.getProductId(), i.getSellerId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();
        OrderJpaEntity entity = new OrderJpaEntity(UUID.fromString(order.getId()), order.getBuyerId(),
                order.getAuctionId(), items, order.getTotalAmount(), order.getStatus().name(),
                order.getCreatedAt(), order.getPaidAt(), order.getPaymentReference());
        jpaRepository.save(entity);
        return order;
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getSellerId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();
        return Order.reconstitute(entity.getId().toString(), entity.getBuyerId(), entity.getAuctionId(), items,
                entity.getTotalAmount(), OrderStatus.valueOf(entity.getStatus()), entity.getCreatedAt(),
                entity.getPaidAt(), entity.getPaymentReference());
    }
}
