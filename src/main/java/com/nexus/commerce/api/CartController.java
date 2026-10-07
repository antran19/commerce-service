package com.nexus.commerce.api;

import com.nexus.commerce.api.dto.request.AddCartItemRequest;
import com.nexus.commerce.api.dto.request.UpdateCartItemRequest;
import com.nexus.commerce.api.dto.response.CartResponse;
import com.nexus.commerce.api.mapper.CommerceApiMapper;
import com.nexus.commerce.application.usecase.*;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.security.RequiresPrivilege;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    private final AddCartItemUseCase addCartItemUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final GetCartUseCase getCartUseCase;
    private final CommerceApiMapper mapper;

    public CartController(AddCartItemUseCase addCartItemUseCase, UpdateCartItemUseCase updateCartItemUseCase,
                           RemoveCartItemUseCase removeCartItemUseCase, GetCartUseCase getCartUseCase,
                           CommerceApiMapper mapper) {
        this.addCartItemUseCase = addCartItemUseCase;
        this.updateCartItemUseCase = updateCartItemUseCase;
        this.removeCartItemUseCase = removeCartItemUseCase;
        this.getCartUseCase = getCartUseCase;
        this.mapper = mapper;
    }

    @RequiresPrivilege("CART.VIEW")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Authentication authentication) {
        CartResult result = getCartUseCase.getCart(buyerId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("CART.CREATE")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(Authentication authentication,
                                                               @Valid @RequestBody AddCartItemRequest request) {
        CartResult result = addCartItemUseCase.addItem(buyerId(authentication), request.productId(),
                request.sellerId(), request.productName(), request.unitPrice(), request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("CART.UPDATE")
    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(Authentication authentication,
                                                                   @PathVariable String productId,
                                                                   @Valid @RequestBody UpdateCartItemRequest request) {
        CartResult result = updateCartItemUseCase.updateQuantity(buyerId(authentication), productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("CART.REMOVE_ITEM")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(Authentication authentication,
                                                                  @PathVariable String productId) {
        CartResult result = removeCartItemUseCase.removeItem(buyerId(authentication), productId);
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    // buyerId MUST come from the JWT, never a request param -- a caller cannot act on another
    // buyer's cart by passing a different id.
    private static String buyerId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
