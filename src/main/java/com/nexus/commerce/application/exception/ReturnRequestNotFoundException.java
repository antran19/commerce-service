package com.nexus.commerce.application.exception;

import com.nexus.common.core.exception.NotFoundException;

public class ReturnRequestNotFoundException extends NotFoundException {
    public ReturnRequestNotFoundException(String id) {
        super("RETURN_REQUEST_NOT_FOUND", "Return request not found: " + id);
    }
}
