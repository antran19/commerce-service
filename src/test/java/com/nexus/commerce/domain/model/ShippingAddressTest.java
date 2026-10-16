package com.nexus.commerce.domain.model;

import com.nexus.common.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingAddressTest {

    @Test
    void create_trimsTextAndStripsSeparatorsFromThePhone() {
        ShippingAddress address = ShippingAddress.create("  Nguyen Van A ", "090 123-4567", " 12 Le Loi ", " HCM ");

        assertThat(address.recipientName()).isEqualTo("Nguyen Van A");
        assertThat(address.phone()).isEqualTo("0901234567");
        assertThat(address.addressLine()).isEqualTo("12 Le Loi");
        assertThat(address.city()).isEqualTo("HCM");
    }

    @Test
    void create_acceptsAnInternationalPhone() {
        assertThat(ShippingAddress.create("A", "+84901234567", "12 Le Loi", "HCM").phone())
                .isEqualTo("+84901234567");
    }

    @Test
    void create_rejectsBlankFields() {
        assertThatThrownBy(() -> ShippingAddress.create(" ", "0901234567", "12 Le Loi", "HCM"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ShippingAddress.create("A", "0901234567", "", "HCM"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ShippingAddress.create("A", "0901234567", "12 Le Loi", null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsAPhoneThatIsTooShortOrHasLetters() {
        assertThatThrownBy(() -> ShippingAddress.create("A", "12345", "12 Le Loi", "HCM"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ShippingAddress.create("A", "09012abcde", "12 Le Loi", "HCM"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_rejectsATextLongerThanItsColumn() {
        assertThatThrownBy(() -> ShippingAddress.create("A", "0901234567", "x".repeat(501), "HCM"))
                .isInstanceOf(ValidationException.class);
    }
}
