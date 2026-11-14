package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.ReturnRequestStatus;

import java.util.List;

// ORDER.REFUND (Admin/Support Staff), like every other return endpoint. Without a list, a return request could only
// be answered by someone who had kept the id from the moment it was created: this is how the pending ones are found.
public class ListReturnRequestsUseCase {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;

    public ListReturnRequestsUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort) {
        this.returnRequestRepositoryPort = returnRequestRepositoryPort;
    }

    public List<ReturnRequestResult> list(ReturnRequestStatus status) {
        return returnRequestRepositoryPort.findByStatus(status).stream().map(ReturnRequestResult::from).toList();
    }
}
