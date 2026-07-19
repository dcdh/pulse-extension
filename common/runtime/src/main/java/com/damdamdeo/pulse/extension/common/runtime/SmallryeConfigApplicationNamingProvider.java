package com.damdamdeo.pulse.extension.common.runtime;

import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.ApplicationNamingProvider;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Unremovable
public final class SmallryeConfigApplicationNamingProvider implements ApplicationNamingProvider {

    private final ApplicationNaming applicationNaming;

    public SmallryeConfigApplicationNamingProvider(@ConfigProperty(name = "quarkus.application.name") final String quarkusApplicationName) {
        this.applicationNaming = new ApplicationNaming(quarkusApplicationName);
    }

    @Override
    public ApplicationNaming provide() {
        return applicationNaming;
    }
}
