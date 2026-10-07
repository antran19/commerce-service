package com.nexus.commerce.api;

import com.nexus.commerce.api.dto.response.OrderResponse;
import com.nexus.commerce.api.mapper.CommerceApiMapper;
import com.nexus.commerce.application.usecase.*;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.security.RequiresPrivilege;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final StartCheckoutUseCase startCheckoutUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final CommerceApiMapper mapper;

    public OrderController(StartCheckoutUseCase startCheckoutUseCase, GetOrderUseCase getOrderUseCase,
                            ListOrdersUseCase listOrdersUseCase, CancelOrderUseCase cancelOrderUseCase,
                            CommerceApiMapper mapper) {
        this.startCheckoutUseCase = startCheckoutUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.mapper = mapper;
    }

    @RequiresPrivilege("CHECKOUT.START")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(Authentication authentication) {
        OrderResult result = startCheckoutUseCase.checkout(buyerId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("ORDER.LIST")
    @GetMapping("/orders/me")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listMyOrders(Authentication authentication) {
        List<OrderResponse> results = listOrdersUseCase.listForBuyer(buyerId(authentication))
                .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @RequiresPrivilege("ORDER.VIEW")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderId) {
        OrderResult result = getOrderUseCase.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("ORDER.CANCEL")
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(Authentication authentication,
                                                                     @PathVariable String orderId) {
        OrderResult result = cancelOrderUseCase.cancel(orderId, buyerId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    private static String buyerId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
