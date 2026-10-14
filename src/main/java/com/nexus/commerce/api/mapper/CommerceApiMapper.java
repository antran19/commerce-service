package com.nexus.commerce.api.mapper;

import com.nexus.commerce.api.dto.response.CartItemResponse;
import com.nexus.commerce.api.dto.response.CartResponse;
import com.nexus.commerce.api.dto.response.CheckoutSessionResponse;
import com.nexus.commerce.api.dto.response.InvoiceResponse;
import com.nexus.commerce.api.dto.response.OrderItemResponse;
import com.nexus.commerce.api.dto.response.OrderResponse;
import com.nexus.commerce.api.dto.response.ReturnRequestResponse;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.application.usecase.CartItemResult;
import com.nexus.commerce.application.usecase.CartResult;
import com.nexus.commerce.application.usecase.InvoiceResult;
import com.nexus.commerce.application.usecase.OrderItemResult;
import com.nexus.commerce.application.usecase.OrderResult;
import com.nexus.commerce.application.usecase.ReturnRequestResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommerceApiMapper {
    CartItemResponse toResponse(CartItemResult result);
    CartResponse toResponse(CartResult result);
    OrderItemResponse toResponse(OrderItemResult result);
    OrderResponse toResponse(OrderResult result);
    InvoiceResponse toResponse(InvoiceResult result);
    CheckoutSessionResponse toResponse(PaymentGatewayPort.CheckoutSession result);
    ReturnRequestResponse toResponse(ReturnRequestResult result);
}
