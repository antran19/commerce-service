package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.exception.ReturnRequestNotFoundException;
import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.domain.model.ReturnRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RejectReturnUseCaseTest {

    private ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private RejectReturnUseCase useCase;

    @BeforeEach
    void setUp() {
        returnRequestRepositoryPort = mock(ReturnRequestRepositoryPort.class);
        useCase = new RejectReturnUseCase(returnRequestRepositoryPort);
        when(returnRequestRepositoryPort.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reject_marksRejected() {
        ReturnRequest returnRequest = ReturnRequest.create("order-1", "Changed mind");
        when(returnRequestRepositoryPort.findByIdForUpdate(returnRequest.getId())).thenReturn(Optional.of(returnRequest));

        ReturnRequestResult result = useCase.reject(returnRequest.getId(), "admin-1");

        assertThat(result.status()).isEqualTo("REJECTED");
    }

    @Test
    void reject_throwsForAnUnknownReturnRequest() {
        when(returnRequestRepositoryPort.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.reject("missing", "admin-1"))
                .isInstanceOf(ReturnRequestNotFoundException.class);
    }
}
