package com.nexus.commerce.infrastructure.persistence;

import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.ReturnRequest;
import com.nexus.commerce.domain.model.ReturnRequestStatus;
import com.nexus.commerce.infrastructure.persistence.entity.ReturnRequestJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReturnRequestRepositoryAdapter implements ReturnRequestRepositoryPort {

    private final ReturnRequestJpaRepository jpaRepository;

    public ReturnRequestRepositoryAdapter(ReturnRequestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ReturnRequest> findById(String id) {
        return UuidIds.tryParse(id).flatMap(jpaRepository::findById).map(this::toDomain);
    }

    @Override
    public Optional<ReturnRequest> findByIdForUpdate(String id) {
        return UuidIds.tryParse(id).flatMap(jpaRepository::findByIdForUpdate).map(this::toDomain);
    }

    @Override
    public List<ReturnRequest> findByOrderId(String orderId) {
        return UuidIds.tryParse(orderId)
                .map(id -> jpaRepository.findByOrderId(id).stream().map(this::toDomain).toList())
                .orElseGet(List::of);
    }

    @Override
    public ReturnRequest save(ReturnRequest returnRequest) {
        ReturnRequestJpaEntity entity = new ReturnRequestJpaEntity(UUID.fromString(returnRequest.getId()),
                UUID.fromString(returnRequest.getOrderId()), returnRequest.getReason(),
                returnRequest.getStatus().name(), returnRequest.getCreatedAt(), returnRequest.getResolvedAt(),
                returnRequest.getResolvedBy());
        jpaRepository.save(entity);
        return returnRequest;
    }

    private ReturnRequest toDomain(ReturnRequestJpaEntity entity) {
        return ReturnRequest.reconstitute(entity.getId().toString(), entity.getOrderId().toString(),
                entity.getReason(), ReturnRequestStatus.valueOf(entity.getStatus()), entity.getCreatedAt(),
                entity.getResolvedAt(), entity.getResolvedBy());
    }
}
