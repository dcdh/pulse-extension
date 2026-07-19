package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.obfuscator.runtime.UUIDProvider;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.UUID;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubUUIDProvider extends UUIDProvider {

    @Override
    public UUID provide() {
        return new UUID(0, 0);
    }
}
