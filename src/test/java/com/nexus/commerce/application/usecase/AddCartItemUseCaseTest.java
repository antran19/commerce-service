package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.domain.model.Cart;
import com.nexus.commerce.domain.model.CartItem;
import com.nexus.commerce.domain.model.ProductSnapshot;
import com.nexus.common.core.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddCartItemUseCaseTest {

    private CartRepositoryPort cartRepositoryPort;
    private ProductSnapshotRepositoryPort productSnapshotRepositoryPort;
    private AddCartItemUseCase useCase;

    @BeforeEach
    void setUp() {
        cartRepositoryPort = mock(CartRepositoryPort.class);
        productSnapshotRepositoryPort = mock(ProductSnapshotRepositoryPort.class);
        useCase = new AddCartItemUseCase(cartRepositoryPort, productSnapshotRepositoryPort);
        when(cartRepositoryPort.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productSnapshotRepositoryPort.findById("product-1")).thenReturn(Optional.of(
                ProductSnapshot.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"))
                        .withStatus("ACTIVE")));
    }

    @Test
    void addItem_createsCartWhenBuyerHasNone() {
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.empty());

        CartResult result = useCase.addItem("buyer-1", "product-1", 1);

        assertThat(result.buyerId()).isEqualTo("buyer-1");
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    void addItem_takesPriceNameAndSellerFromTheCatalogCopy() {
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.empty());

        CartResult result = useCase.addItem("buyer-1", "product-1", 2);

        CartItemResult item = result.items().get(0);
        assertThat(item.unitPrice()).isEqualByComparingTo("999.00");
        assertThat(item.productName()).isEqualTo("Laptop X");
        assertThat(item.sellerId()).isEqualTo("seller-1");
        assertThat(result.totalAmount()).isEqualByComparingTo("1998.00");
    }

    @Test
    void addItem_mergesIntoExistingCart() {
        Cart existing = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1));
        when(cartRepositoryPort.findByBuyerId("buyer-1")).thenReturn(Optional.of(existing));

        CartResult result = useCase.addItem("buyer-1", "product-1", 2);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void addItem_rejectsAProductWithoutCatalogCopy() {
        when(productSnapshotRepositoryPort.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.addItem("buyer-1", "unknown", 1))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PRODUCT_NOT_AVAILABLE");
        verify(cartRepositoryPort, never()).save(any());
    }

    @Test
    void addItem_rejectsAProductThatIsNotActive() {
        when(productSnapshotRepositoryPort.findById("draft-1")).thenReturn(Optional.of(
                ProductSnapshot.create("draft-1", "seller-1", "Draft", new BigDecimal("5.00"))));

        assertThatThrownBy(() -> useCase.addItem("buyer-1", "draft-1", 1))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PRODUCT_NOT_AVAILABLE");
        verify(cartRepositoryPort, never()).save(any());
    }
}
