package com.nexus.commerce.domain.model;

import com.nexus.common.core.FieldError;
import com.nexus.common.core.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// Where the Seller should ship an order. Plain text on purpose (no separate district/ward):
// there is no carrier integration, so the Seller only needs something a human can read.
// Lengths match the ship_* columns added in V7.
public record ShippingAddress(String recipientName, String phone, String addressLine, String city) {

    // Digits with an optional leading '+', 8 to 15 digits once spaces, dots and dashes are
    // removed -- loose enough for local and international formats.
    private static final Pattern PHONE = Pattern.compile("\\+?\\d{8,15}");

    public static ShippingAddress create(String recipientName, String phone, String addressLine, String city) {
        List<FieldError> errors = new ArrayList<>();
        String name = require("recipientName", recipientName, 255, errors);
        String line = require("addressLine", addressLine, 500, errors);
        String town = require("city", city, 100, errors);
        String normalizedPhone = null;
        if (phone == null || phone.isBlank()) {
            errors.add(new FieldError("phone", "phone must not be blank"));
        } else {
            normalizedPhone = phone.replaceAll("[\\s.\\-]", "");
            if (!PHONE.matcher(normalizedPhone).matches()) {
                errors.add(new FieldError("phone", "phone must have 8 to 15 digits"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new ShippingAddress(name, normalizedPhone, line, town);
    }

    private static String require(String field, String value, int maxLength, List<FieldError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new FieldError(field, field + " must not be blank"));
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            errors.add(new FieldError(field, field + " must be at most " + maxLength + " characters"));
        }
        return trimmed;
    }
}
