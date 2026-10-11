package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.ReturnRequestNotFoundException;
import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.ReturnRequest;
import org.springframework.transaction.annotation.Transactional;

// ORDER.REFUND (Admin/Support Staff). No restock, no event -- just records the decision.
public class RejectReturnUseCase {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;

    public RejectReturnUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
    }

    @Transactional
    public ReturnRequestResult reject(String returnRequestId, String adminId) {
        ReturnRequest returnRequest = returnRequestRepositoryPort.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new ReturnRequestNotFoundException(returnRequestId));

        ReturnRequest saved = returnRequestRepositoryPort.save(returnRequest.reject(adminId));
        return ReturnRequestResult.from(saved);
    }
}
