package com.nexus.commerce.application.port.out;

import com.nexus.commerce.domain.model.ReturnRequest;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepositoryPort {
    Optional<ReturnRequest> findById(String id);
    Optional<ReturnRequest> findByIdForUpdate(String id);
    List<ReturnRequest> findByOrderId(String orderId);
    ReturnRequest save(ReturnRequest returnRequest);
}
