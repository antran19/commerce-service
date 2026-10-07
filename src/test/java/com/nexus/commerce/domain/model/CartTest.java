package com.nexus.commerce.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    @Test
    void create_startsEmpty() {
        Cart cart = Cart.create("buyer-1");

        assertThat(cart.getId()).isNotBlank();
        assertThat(cart.getBuyerId()).isEqualTo("buyer-1");
        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void addItem_appendsNewProduct() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.findItem("product-1")).isPresent();
        assertThat(cart.totalAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    void addItem_mergesQuantityWhenProductAlreadyInCart() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1))
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 2));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.findItem("product-1").orElseThrow().getQuantity()).isEqualTo(3);
    }

    @Test
    void updateItemQuantity_setsExactQuantity() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1))
                .updateItemQuantity("product-1", 5);

        assertThat(cart.findItem("product-1").orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    void removeItem_dropsTheProduct() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1))
                .removeItem("product-1");

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void clear_emptiesAllItems() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1))
                .addItem(CartItem.create("product-2", "seller-2", "Mouse", new BigDecimal("20.00"), 2))
                .clear();

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void totalAmount_sumsAllLineSubtotals() {
        Cart cart = Cart.create("buyer-1")
                .addItem(CartItem.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"), 1))
                .addItem(CartItem.create("product-2", "seller-2", "Mouse", new BigDecimal("20.00"), 2));

        assertThat(cart.totalAmount()).isEqualByComparingTo("1039.00");
    }
}
