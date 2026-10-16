package com.nexus.commerce.application.usecase;

import com.nexus.commerce.domain.model.ShippingAddress;

public record ShippingAddressResult(String recipientName, String phone, String addressLine, String city) {

    public static ShippingAddressResult from(ShippingAddress address) {
        return address == null ? null
                : new ShippingAddressResult(address.recipientName(), address.phone(), address.addressLine(),
                address.city());
    }
}
