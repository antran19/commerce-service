package com.nexus.commerce.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SetShippingAddressRequest(@NotBlank String recipientName, @NotBlank String phone,
                                        @NotBlank String addressLine, @NotBlank String city) {
}
