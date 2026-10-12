package com.nexus.commerce.infrastructure.scheduling;

import com.nexus.commerce.application.usecase.ExpireInactiveCartsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpireInactiveCartsJob {

    private static final Logger log = LoggerFactory.getLogger(ExpireInactiveCartsJob.class);

    private final ExpireInactiveCartsUseCase expireInactiveCartsUseCase;

    public ExpireInactiveCartsJob(ExpireInactiveCartsUseCase expireInactiveCartsUseCase) {
        this.expireInactiveCartsUseCase = expireInactiveCartsUseCase;
    }

    @Scheduled(fixedDelayString = "${nexus.cart.expiry-check-interval-ms:3600000}")
    public void run() {
        int deleted = expireInactiveCartsUseCase.expire();
        if (deleted > 0) {
            log.info("Expired {} inactive cart(s)", deleted);
        }
    }
}
