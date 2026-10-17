package com.nexus.commerce.application.usecase;

import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.domain.model.ProductSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncProductSnapshotUseCaseTest {

    private ProductSnapshotRepositoryPort repositoryPort;
    private SyncProductSnapshotUseCase useCase;

    @BeforeEach
    void setUp() {
        repositoryPort = mock(ProductSnapshotRepositoryPort.class);
        useCase = new SyncProductSnapshotUseCase(repositoryPort);
    }

    @Test
    void onCreated_insertsADraftSnapshotWithSellerNameAndPrice() {
        useCase.onCreated("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"));

        ArgumentCaptor<ProductSnapshot> captor = ArgumentCaptor.forClass(ProductSnapshot.class);
        verify(repositoryPort).insertIfAbsent(captor.capture());
        ProductSnapshot snapshot = captor.getValue();
        assertThat(snapshot.getProductId()).isEqualTo("product-1");
        assertThat(snapshot.getSellerId()).isEqualTo("seller-1");
        assertThat(snapshot.getName()).isEqualTo("Laptop X");
        assertThat(snapshot.getPrice()).isEqualByComparingTo("999.00");
        assertThat(snapshot.getStatus()).isEqualTo("DRAFT");
        assertThat(snapshot.isPurchasable()).isFalse();
    }

    @Test
    void onCreated_skipsAnOldEventWithoutPriceOrSeller() {
        useCase.onCreated("product-1", "seller-1", "Laptop X", null);
        useCase.onCreated("product-2", null, "Phone", new BigDecimal("10.00"));

        verify(repositoryPort, never()).insertIfAbsent(any());
    }

    @Test
    void onStatusChanged_updatesTheExistingSnapshot() {
        when(repositoryPort.findById("product-1")).thenReturn(Optional.of(
                ProductSnapshot.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"))));

        useCase.onStatusChanged("product-1", "ACTIVE");

        ArgumentCaptor<ProductSnapshot> captor = ArgumentCaptor.forClass(ProductSnapshot.class);
        verify(repositoryPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().isPurchasable()).isTrue();
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("999.00");
    }

    @Test
    void onRenamed_updatesOnlyTheName() {
        when(repositoryPort.findById("product-1")).thenReturn(Optional.of(
                ProductSnapshot.create("product-1", "seller-1", "Laptop X", new BigDecimal("999.00"))
                        .withStatus("ACTIVE")));

        useCase.onRenamed("product-1", "Laptop X Pro");

        ArgumentCaptor<ProductSnapshot> captor = ArgumentCaptor.forClass(ProductSnapshot.class);
        verify(repositoryPort).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Laptop X Pro");
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void updatesForAProductWithoutSnapshot_areIgnored() {
        when(repositoryPort.findById("unknown")).thenReturn(Optional.empty());

        useCase.onStatusChanged("unknown", "ACTIVE");
        useCase.onRenamed("unknown", "Name");

        verify(repositoryPort, never()).save(any());
    }
}
