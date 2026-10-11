package com.nexus.commerce.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnRequestTest {

    @Test
    void create_startsRequestedWithNoResolution() {
        ReturnRequest returnRequest = ReturnRequest.create("order-1", "Item arrived broken");

        assertThat(returnRequest.getId()).isNotBlank();
        assertThat(returnRequest.getOrderId()).isEqualTo("order-1");
        assertThat(returnRequest.getReason()).isEqualTo("Item arrived broken");
        assertThat(returnRequest.getStatus()).isEqualTo(ReturnRequestStatus.REQUESTED);
        assertThat(returnRequest.getResolvedAt()).isNull();
        assertThat(returnRequest.getResolvedBy()).isNull();
    }

    @Test
    void receive_setsStatusReceivedAndResolution() {
        ReturnRequest returnRequest = ReturnRequest.create("order-1", "Item arrived broken");

        ReturnRequest received = returnRequest.receive("admin-1");

        assertThat(received.getStatus()).isEqualTo(ReturnRequestStatus.RECEIVED);
        assertThat(received.getResolvedBy()).isEqualTo("admin-1");
        assertThat(received.getResolvedAt()).isNotNull();
    }

    @Test
    void reject_setsStatusRejectedAndResolution() {
        ReturnRequest returnRequest = ReturnRequest.create("order-1", "Item arrived broken");

        ReturnRequest rejected = returnRequest.reject("admin-1");

        assertThat(rejected.getStatus()).isEqualTo(ReturnRequestStatus.REJECTED);
        assertThat(rejected.getResolvedBy()).isEqualTo("admin-1");
        assertThat(rejected.getResolvedAt()).isNotNull();
    }
}
