package com.nexus.commerce.api;

import com.nexus.commerce.api.dto.request.RefundOrderRequest;
import com.nexus.commerce.api.dto.response.OrderResponse;
import com.nexus.commerce.api.mapper.CommerceApiMapper;
import com.nexus.commerce.application.usecase.*;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.core.exception.ForbiddenException;
import com.nexus.common.security.RequiresPrivilege;
import com.nexus.common.security.TokenDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    // ORDER.LIST/VIEW is granted to all 4 roles per the SRS, but Buyer/Seller only ever see
    // their own orders via /orders/me -- only these two roles get the "any order" scope
    // (V9 migration's seeding comment: support staff "can view/list/cancel any order").
    private static final Set<String> CAN_LIST_ALL_ORDERS = Set.of("ADMIN", "SUPPORT_STAFF");

    private final StartCheckoutUseCase startCheckoutUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final ListAllOrdersUseCase listAllOrdersUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final RefundOrderUseCase refundOrderUseCase;
    private final CommerceApiMapper mapper;

    public OrderController(StartCheckoutUseCase startCheckoutUseCase, GetOrderUseCase getOrderUseCase,
                            ListOrdersUseCase listOrdersUseCase, ListAllOrdersUseCase listAllOrdersUseCase,
                            CancelOrderUseCase cancelOrderUseCase, RefundOrderUseCase refundOrderUseCase,
                            CommerceApiMapper mapper) {
        this.startCheckoutUseCase = startCheckoutUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
        this.listAllOrdersUseCase = listAllOrdersUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.refundOrderUseCase = refundOrderUseCase;
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

    @RequiresPrivilege("ORDER.LIST")
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listAllOrders(Authentication authentication) {
        String role = authentication.getDetails() instanceof TokenDetails details ? details.role() : null;
        if (!CAN_LIST_ALL_ORDERS.contains(role)) {
            throw new ForbiddenException("ORDER_LIST_ALL_FORBIDDEN", "Only Admin or Support Staff may list all orders");
        }
        List<OrderResponse> results = listAllOrdersUseCase.list().stream().map(mapper::toResponse).toList();
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

    @RequiresPrivilege("ORDER.REFUND")
    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<ApiResponse<OrderResponse>> refundOrder(@PathVariable String orderId,
                                                                      @Valid @RequestBody RefundOrderRequest request) {
        OrderResult result = refundOrderUseCase.refund(orderId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    private static String buyerId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
