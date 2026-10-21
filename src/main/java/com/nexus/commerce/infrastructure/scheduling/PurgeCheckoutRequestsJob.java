package com.nexus.commerce.infrastructure.scheduling;

import com.nexus.commerce.application.usecase.PurgeCheckoutRequestsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PurgeCheckoutRequestsJob {

    private static final Logger log = LoggerFactory.getLogger(PurgeCheckoutRequestsJob.class);

    private final PurgeCheckoutRequestsUseCase purgeCheckoutRequestsUseCase;

    public PurgeCheckoutRequestsJob(PurgeCheckoutRequestsUseCase purgeCheckoutRequestsUseCase) {
        this.purgeCheckoutRequestsUseCase = purgeCheckoutRequestsUseCase;
    }

    @Scheduled(initialDelayString = "${nexus.checkout.purge-initial-delay-ms:60000}",
               fixedDelayString = "${nexus.checkout.purge-interval-ms:3600000}")
    public void run() {
        int deleted = purgeCheckoutRequestsUseCase.purge();
        if (deleted > 0) {
            log.info("Purged {} old checkout idempotency record(s)", deleted);
        }
    }
}
