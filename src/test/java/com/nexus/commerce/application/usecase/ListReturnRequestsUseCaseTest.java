package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.ReturnRequest;
import com.nexus.commerce.domain.model.ReturnRequestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListReturnRequestsUseCaseTest {

    private final ReturnRequestRepositoryPort repository = mock(ReturnRequestRepositoryPort.class);
    private final ListReturnRequestsUseCase useCase = new ListReturnRequestsUseCase(repository);

    @Test
    void list_turnsEachRequestOfTheStatusIntoAResult() {
        ReturnRequest request = ReturnRequest.create("11111111-1111-1111-1111-111111111111", "Wrong item");
        when(repository.findByStatus(ReturnRequestStatus.REQUESTED)).thenReturn(List.of(request));

        List<ReturnRequestResult> results = useCase.list(ReturnRequestStatus.REQUESTED);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(request.getId());
        assertThat(results.get(0).orderId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(results.get(0).reason()).isEqualTo("Wrong item");
        assertThat(results.get(0).status()).isEqualTo("REQUESTED");
    }

    @Test
    void list_isEmptyWhenNothingIsInThatStatus() {
        when(repository.findByStatus(ReturnRequestStatus.REJECTED)).thenReturn(List.of());

        assertThat(useCase.list(ReturnRequestStatus.REJECTED)).isEmpty();
    }
}
