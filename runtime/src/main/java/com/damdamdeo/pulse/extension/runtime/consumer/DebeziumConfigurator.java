package com.damdamdeo.pulse.extension.runtime.consumer;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
@Unremovable
@DefaultBean
public final class DebeziumConfigurator {

    private final boolean enabled;

    // FCK exposer la conf de la maniere normale via une Interface annotée ! pour faire la doc en plus !!
    public DebeziumConfigurator(final DebeziumConfiguration debeziumConfiguration,
                                @ConfigProperty(name = "quarkus.application.name") final String name) {
        // FCK le nom de la topic doit prendre le application name
        // "%s_t_event".formatted(applicationInfoBuildItem.getName()),
        this.enabled = debeziumConfiguration.enabled();
    }

//    FCK je dois creer le debezium remote ! je devrais peu être générer de la conf ...
//    pour les tests je dois desactiver le StartUpEvent ... ha mais fuck non parce que je l'utilise déjà pour init la base ...
}
