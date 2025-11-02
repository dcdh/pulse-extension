package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Unremovable
public class SchemaNamingProvider {

    private final SchemaNaming schemaNaming;

    public SchemaNamingProvider(@ConfigProperty(name = "quarkus.application.name") final String quarkusApplicationName) {
        this.schemaNaming = new SchemaNaming(quarkusApplicationName.toLowerCase());
    }

    public SchemaNaming provide() {
        return schemaNaming;
    }
}
