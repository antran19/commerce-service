package com.nexus.commerce.application.port.out;

import com.nexus.common.events.DomainEvent;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}
