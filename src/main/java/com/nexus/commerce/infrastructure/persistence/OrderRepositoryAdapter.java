package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.domain.model.Order;
import com.nexus.commerce.domain.model.OrderItem;
import com.nexus.commerce.domain.model.OrderStatus;
import com.nexus.commerce.domain.model.ShippingAddress;
import com.nexus.commerce.infrastructure.persistence.entity.OrderItemEmbeddable;
import com.nexus.commerce.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
    public List<Order> findBySellerId(String sellerId) {
        return jpaRepository.findBySellerId(sellerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findUnpaidDirectOrdersCreatedBefore(Instant cutoff) {
        return jpaRepository.findUnpaidDirectOrdersCreatedBefore(cutoff).stream().map(this::toDomain).toList();
    }

    @Override
    public Order save(Order order) {
        List<OrderItemEmbeddable> items = order.getItems().stream()
                .map(i -> new OrderItemEmbeddable(i.getProductId(), i.getSellerId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();
        ShippingAddress address = order.getShippingAddress();
        OrderJpaEntity entity = new OrderJpaEntity(UUID.fromString(order.getId()), order.getBuyerId(),
                order.getAuctionId(), items, order.getTotalAmount(), order.getStatus().name(),
                order.getCreatedAt(), order.getPaidAt(), order.getPaymentReference(), order.getShippedAt(),
                address == null ? null : address.recipientName(), address == null ? null : address.phone(),
                address == null ? null : address.addressLine(), address == null ? null : address.city());
        jpaRepository.save(entity);
        return order;
    }

    // All four columns are written together, so a null recipient name means "no address set".
    private static ShippingAddress shippingAddressOf(OrderJpaEntity entity) {
        if (entity.getShipRecipientName() == null) {
            return null;
        }
        return new ShippingAddress(entity.getShipRecipientName(), entity.getShipPhone(),
                entity.getShipAddressLine(), entity.getShipCity());
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(i.getProductId(), i.getSellerId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();
        return Order.reconstitute(entity.getId().toString(), entity.getBuyerId(), entity.getAuctionId(), items,
                entity.getTotalAmount(), OrderStatus.valueOf(entity.getStatus()), entity.getCreatedAt(),
                entity.getPaidAt(), entity.getPaymentReference(), entity.getShippedAt(), shippingAddressOf(entity));
    }
}
