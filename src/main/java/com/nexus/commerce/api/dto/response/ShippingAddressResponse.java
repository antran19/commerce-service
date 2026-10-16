package com.nexus.commerce.api.dto.response;

public record ShippingAddressResponse(String recipientName, String phone, String addressLine, String city) {
}
