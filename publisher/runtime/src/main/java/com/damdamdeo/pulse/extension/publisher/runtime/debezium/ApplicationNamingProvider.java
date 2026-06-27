package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Unremovable
public final class ApplicationNamingProvider {

    private final ApplicationNaming applicationNaming;

    public ApplicationNamingProvider(@ConfigProperty(name = "quarkus.application.name") final String quarkusApplicationName) {
        this.applicationNaming = new ApplicationNaming(quarkusApplicationName);
    }

    public ApplicationNaming provide() {
        return applicationNaming;
    }
}
