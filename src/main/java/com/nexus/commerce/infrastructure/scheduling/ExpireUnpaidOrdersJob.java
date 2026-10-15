package com.nexus.commerce.infrastructure.scheduling;

import com.nexus.commerce.application.usecase.ExpireUnpaidOrdersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpireUnpaidOrdersJob {

    private static final Logger log = LoggerFactory.getLogger(ExpireUnpaidOrdersJob.class);

    private final ExpireUnpaidOrdersUseCase expireUnpaidOrdersUseCase;

    public ExpireUnpaidOrdersJob(ExpireUnpaidOrdersUseCase expireUnpaidOrdersUseCase) {
        this.expireUnpaidOrdersUseCase = expireUnpaidOrdersUseCase;
    }

    @Scheduled(fixedDelayString = "${nexus.order.unpaid-expiry-check-interval-ms:60000}")
    public void run() {
        for (String orderId : expireUnpaidOrdersUseCase.findDueOrderIds()) {
            try {
                expireUnpaidOrdersUseCase.expire(orderId);
            } catch (RuntimeException e) {
                // One bad order must not stop the rest of this tick; the next tick retries it.
                log.warn("Failed to expire unpaid order {}", orderId, e);
            }
        }
    }
}
