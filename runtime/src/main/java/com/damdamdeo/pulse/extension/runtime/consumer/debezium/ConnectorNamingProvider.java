package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Unremovable
public class ConnectorNamingProvider {

    private final ConnectorNaming connectorNaming;

    public ConnectorNamingProvider(@ConfigProperty(name = "quarkus.application.name") final String quarkusApplicationName) {
        this.connectorNaming = new ConnectorNaming(quarkusApplicationName.toLowerCase());
    }

    public ConnectorNaming provide() {
        return connectorNaming;
    }
}
