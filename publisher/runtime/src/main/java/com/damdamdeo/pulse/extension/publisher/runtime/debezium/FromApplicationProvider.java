package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Unremovable
public final class FromApplicationProvider {

    private final FromApplication fromApplication;

    public FromApplicationProvider(@ConfigProperty(name = "quarkus.application.name") final String quarkusApplicationName) {
        this.fromApplication = FromApplication.from(quarkusApplicationName);
    }

    public FromApplication provide() {
        return fromApplication;
    }
}
