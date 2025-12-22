package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
@DefaultBean
@Unremovable
public final class UnknownClientProvider implements ClientProvider {

    @Override
    public Client provide() {
        return new UnknownClient(UUID.randomUUID());
    }
}
