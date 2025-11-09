package com.damdamdeo.pulse.extension.common.runtime.datasource;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Objects;
import java.util.Optional;

@Singleton
@Unremovable
public class InitScriptUsageChecker {

    private final Optional<String> initScriptPath;

    public InitScriptUsageChecker(@ConfigProperty(name = "quarkus.datasource.devservices.init-script-path") final Optional<String> initScriptPath) {
        this.initScriptPath = Objects.requireNonNull(initScriptPath);
    }

    void onStart(@Observes @Priority(1) final StartupEvent startupEvent) {
        if (initScriptPath.isPresent()) {
            throw new IllegalArgumentException("init script is not supported by compose devservices and will not be executed");
        }
    }
}
