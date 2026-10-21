package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.infrastructure.persistence.entity.CartItemEmbeddable;
import com.nexus.commerce.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartRepositoryAdapter implements CartRepositoryPort {

    private final CartJpaRepository jpaRepository;

    public CartRepositoryAdapter(CartJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Cart> findByBuyerId(String buyerId) {
        return jpaRepository.findByBuyerId(buyerId).map(this::toDomain);
    }

    @Override
    public Optional<Cart> findByBuyerIdForUpdate(String buyerId) {
        return jpaRepository.findByBuyerIdForUpdate(buyerId).map(this::toDomain);
    }

    @Override
    public Cart save(Cart cart) {
        List<CartItemEmbeddable> items = cart.getItems().stream()
                .map(i -> new CartItemEmbeddable(i.getProductId(), i.getSellerId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();
        CartJpaEntity entity = new CartJpaEntity(UUID.fromString(cart.getId()), cart.getBuyerId(), items,
                cart.getCreatedAt(), cart.getUpdatedAt());
        jpaRepository.save(entity);
        return cart;
    }

    private Cart toDomain(CartJpaEntity entity) {
        List<CartItem> items = entity.getItems().stream()
                .map(i -> CartItem.create(i.getProductId(), i.getSellerId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();
        return Cart.reconstitute(entity.getId().toString(), entity.getBuyerId(), items,
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Override
    public int deleteInactiveSince(Instant cutoff) {
        return jpaRepository.deleteByUpdatedAtBefore(cutoff);
    }
}
