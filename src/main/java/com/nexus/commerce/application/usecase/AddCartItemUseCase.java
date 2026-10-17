package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.common.core.exception.ConflictException;

public class AddCartItemUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final ProductSnapshotRepositoryPort productSnapshotRepositoryPort;

    public AddCartItemUseCase(CartRepositoryPort cartRepositoryPort,
                              ProductSnapshotRepositoryPort productSnapshotRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.productSnapshotRepositoryPort = productSnapshotRepositoryPort;
    }

    // Price, name and seller come from commerce's own copy of the catalog, never from the
    // request: a client could otherwise buy anything for 1đ or file the order under another seller.
    public CartResult addItem(String buyerId, String productId, int quantity) {
        ProductSnapshot product = productSnapshotRepositoryPort.findById(productId)
                .filter(ProductSnapshot::isPurchasable)
                .orElseThrow(() -> new ConflictException("PRODUCT_NOT_AVAILABLE",
                        "Product is not available for purchase: " + productId));
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId).orElseGet(() -> Cart.create(buyerId));
        Cart updated = cart.addItem(product.toCartItem(quantity));
        return CartResult.from(cartRepositoryPort.save(updated));
    }
}
