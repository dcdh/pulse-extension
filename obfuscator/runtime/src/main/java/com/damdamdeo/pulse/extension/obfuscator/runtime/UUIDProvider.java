package com.damdamdeo.pulse.extension.obfuscator.runtime;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
@Unremovable
@DefaultBean
public class UUIDProvider {

    public UUID provide() {
        return UUID.randomUUID();
    }
}
