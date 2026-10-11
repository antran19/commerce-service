package com.nexus.commerce.api;

import com.nexus.commerce.api.dto.request.CreateReturnRequestRequest;
import com.nexus.commerce.api.dto.response.ReturnRequestResponse;
import com.nexus.commerce.api.mapper.CommerceApiMapper;
import com.nexus.commerce.application.usecase.CreateReturnRequestUseCase;
import com.nexus.commerce.application.usecase.ReceiveReturnUseCase;
import com.nexus.commerce.application.usecase.RejectReturnUseCase;
import com.nexus.commerce.application.usecase.ReturnRequestResult;
import com.nexus.common.core.ApiResponse;
import com.nexus.common.security.RequiresPrivilege;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// ORDER.REFUND (Admin/Support Staff) across every endpoint here -- same actor set already
// handling money refunds, the manual Return flow is their follow-up action, not a new role.
@RestController
@RequestMapping("/api/v1")
public class ReturnController {

    private final CreateReturnRequestUseCase createReturnRequestUseCase;
    private final ReceiveReturnUseCase receiveReturnUseCase;
    private final RejectReturnUseCase rejectReturnUseCase;
    private final CommerceApiMapper mapper;

    public ReturnController(CreateReturnRequestUseCase createReturnRequestUseCase,
                             ReceiveReturnUseCase receiveReturnUseCase, RejectReturnUseCase rejectReturnUseCase,
                             CommerceApiMapper mapper) {
        this.createReturnRequestUseCase = createReturnRequestUseCase;
        this.receiveReturnUseCase = receiveReturnUseCase;
        this.rejectReturnUseCase = rejectReturnUseCase;
        this.mapper = mapper;
    }

    @RequiresPrivilege("ORDER.REFUND")
    @PostMapping("/orders/{orderId}/returns")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> createReturnRequest(
            @PathVariable String orderId, @Valid @RequestBody CreateReturnRequestRequest request) {
        ReturnRequestResult result = createReturnRequestUseCase.create(orderId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("ORDER.REFUND")
    @PostMapping("/returns/{returnId}/receive")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> receiveReturn(Authentication authentication,
                                                                               @PathVariable String returnId) {
        ReturnRequestResult result = receiveReturnUseCase.receive(returnId, adminId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    @RequiresPrivilege("ORDER.REFUND")
    @PostMapping("/returns/{returnId}/reject")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> rejectReturn(Authentication authentication,
                                                                              @PathVariable String returnId) {
        ReturnRequestResult result = rejectReturnUseCase.reject(returnId, adminId(authentication));
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(result)));
    }

    private static String adminId(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
