package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.application.port.out.ProductStockRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.common.core.exception.ConflictException;

public class AddCartItemUseCase {

    private final CartRepositoryPort cartRepositoryPort;
    private final ProductSnapshotRepositoryPort productSnapshotRepositoryPort;
    private final ProductStockRepositoryPort productStockRepositoryPort;

    public AddCartItemUseCase(CartRepositoryPort cartRepositoryPort,
                              ProductSnapshotRepositoryPort productSnapshotRepositoryPort,
                              ProductStockRepositoryPort productStockRepositoryPort) {
        this.cartRepositoryPort = cartRepositoryPort;
        this.productSnapshotRepositoryPort = productSnapshotRepositoryPort;
        this.productStockRepositoryPort = productStockRepositoryPort;
    }

    // Price, name and seller come from commerce's own copy of the catalog, never from the
    // request: a client could otherwise buy anything for 1đ or file the order under another seller.
    public CartResult addItem(String buyerId, String productId, int quantity) {
        ProductSnapshot product = productSnapshotRepositoryPort.findById(productId)
                .filter(ProductSnapshot::isPurchasable)
                .orElseThrow(() -> new ConflictException("PRODUCT_NOT_AVAILABLE",
                        "Product is not available for purchase: " + productId));
        Cart cart = cartRepositoryPort.findByBuyerId(buyerId).orElseGet(() -> Cart.create(buyerId));
        int alreadyInCart = cart.findItem(productId).map(CartItem::getQuantity).orElse(0);
        StockGuard.requireAvailable(productStockRepositoryPort, productId, alreadyInCart + quantity);
        Cart updated = cart.addItem(product.toCartItem(quantity));
        return CartResult.from(cartRepositoryPort.save(updated));
    }
}
