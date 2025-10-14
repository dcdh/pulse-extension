package com.damdamdeo.pulse.extension.runtime;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

@ApplicationScoped
@Unremovable
@DefaultBean
public class DefaultInstantProvider implements InstantProvider {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
